package org.jqassistant.plugin.sarif.report.impl;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.buschmais.jqassistant.core.report.api.ReportContext;
import com.buschmais.jqassistant.core.report.api.ReportException;
import com.buschmais.jqassistant.core.report.api.ReportPlugin;
import com.buschmais.jqassistant.core.report.api.ReportPlugin.Default;
import com.buschmais.jqassistant.core.report.api.model.Column;
import com.buschmais.jqassistant.core.report.api.model.Result;
import com.buschmais.jqassistant.core.report.api.model.Row;
import com.buschmais.jqassistant.core.report.api.model.source.FileLocation;
import com.buschmais.jqassistant.core.report.api.model.source.SourceLocation;
import com.buschmais.jqassistant.core.rule.api.model.Constraint;
import com.buschmais.jqassistant.core.rule.api.model.ExecutableRule;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jqassistant.plugin.sarif.report.api.impl.model.Location;
import org.jqassistant.plugin.sarif.report.api.impl.model.Run;
import org.jqassistant.plugin.sarif.report.api.impl.model.SarifReport;
import org.jqassistant.plugin.sarif.report.api.impl.model.SarifResult;
import org.mapstruct.factory.Mappers;

import static com.buschmais.jqassistant.core.report.api.model.Result.Status.FAILURE;
import static com.buschmais.jqassistant.core.report.api.model.Result.Status.WARNING;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static java.util.Optional.empty;

@Default
@Slf4j
public class SarifReportPlugin implements ReportPlugin {

    public static final String REPORT_DIRECTORY = "sarif";
    public static final String REPORT_FILE = "jqassistant-sarif-report.json";
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(SarifReportPlugin.class);
    private static final String PROPERTY_TEXT_DATA = "sarif.report.message.text";
    private static final String PROPERTY_MARKDOWN_DATA = "sarif.report.message.markdown";
    private static final SeverityMapper SEVERITY_MAPPER = Mappers.getMapper(SeverityMapper.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().setDefaultPropertyInclusion(NON_NULL);

    private ReportContext reportContext;

    private List<SarifResult> results;
    private MessageContent textContent;
    private MessageContent markdownContent;

    @Override
    public void configure(ReportContext reportContext, Map<String, Object> properties) {

        this.reportContext = reportContext;

        this.textContent = MessageContent.valueOf(((String) properties.getOrDefault(PROPERTY_TEXT_DATA, MessageContent.FULL.name())).toUpperCase());

        this.markdownContent = MessageContent.valueOf(((String) properties.getOrDefault(PROPERTY_MARKDOWN_DATA, MessageContent.NONE.name())).toUpperCase());

    }

    @Override
    public void begin() {
        results = new LinkedList<>();
    }

    @Override
    public void setResult(Result<? extends ExecutableRule> result) {
        Result.Status status = result.getStatus();
        if (FAILURE.equals(status) || WARNING.equals(status)) {
            ExecutableRule<?> executableRule = result.getRule();
            Constraint constraint = (Constraint) executableRule;
            for (Row row : result.getRows()) {
                if (!row.isHidden()) {
                    results.add(getResult(result, constraint, row));
                }
            }
        }
    }

    @Override
    public void end() throws ReportException {

        File reportDirectory = reportContext.getReportDirectory(REPORT_DIRECTORY);

        SarifReport report = SarifReport.builder()
            .runs(List.of(Run.builder()
                .tool(Run.Tool.builder()
                    .driver(Run.Tool.Driver.builder()
                        .build())
                    .build())
                .results(this.results)
                .build()))
            .build();
        try {
            File file = new File(reportDirectory, REPORT_FILE).getCanonicalFile();
            log.info("Writing SARIF report to {}.", file);
            OBJECT_MAPPER.writerWithDefaultPrettyPrinter()
                .writeValue(file, report);
        } catch (IOException e) {
            throw new ReportException("Failed to write SARIF report file.", e);
        }
    }

    private SarifResult getResult(Result<? extends ExecutableRule> result, Constraint constraint, Row row) {
        SarifResult.SarifResultBuilder resultBuilder = SarifResult.builder()
            .properties(SarifResult.SarifProperties.builder()
                .checkName("[jQAssistant]" + constraint.getId())
                .build())
            .level(SEVERITY_MAPPER.toReport(constraint.getSeverity()))
            .ruleId(row.getKey());

        String text;
        if (this.textContent != MessageContent.NONE) {
            text = this.textContent.toText(constraint, row, " ");
        } else {
            LOGGER.warn(
                "sarif.report.message.text NONE indicates text is assigned to NULL, but must be set due to SARIF-structure. The SARIF output will contain FULL information and additional warning");
            text = "[WARNING: sarif.report.message.text cannot be NONE] " + MessageContent.FULL.toText(constraint, row, " ");
        }
        String markdown = this.markdownContent.toText(constraint, row, "\n");

        resultBuilder.message(SarifResult.Message.builder()
            .text(text)
            .markdown(markdown)
            .build());
        getLocation(result, row).ifPresent(resultBuilder::location);
        return resultBuilder.build();
    }

    private Optional<Location> getLocation(Result<? extends ExecutableRule> result, Row row) {
        // if uri bug in jQA is fixed getPath() might be integrated here (therefore input parameters)
        Location.LocationBuilder locationBuilder = Location.builder();
        Location.PhysicalLocation.PhysicalLocationBuilder physicalLocationBuilder = Location.PhysicalLocation.builder();
        physicalLocationBuilder.artifactLocation(Location.PhysicalLocation.ArtifactLocation.builder()
            .uri(".jqassistant.yml")
            .build());
        Location.PhysicalLocation.Region.RegionBuilder regionBuilder = Location.PhysicalLocation.Region.builder()
            .startLine(1)
            .endLine(1);
        physicalLocationBuilder.region(regionBuilder.build());
        Location location = locationBuilder.physicalLocation(physicalLocationBuilder.build())
            .build();
        return Optional.of(location);
    }

    private Optional<String> getPath(Result<? extends ExecutableRule> result, Row row) {
        Optional<String> primaryColumnName = result.getPrimaryColumn();
        if (primaryColumnName.isPresent()) {
            Column<?> column = row.getColumns()
                .get(primaryColumnName.get());
            Optional<SourceLocation<?>> optionalSourceLocation = column.getSourceLocation();
            if (optionalSourceLocation.isPresent() && optionalSourceLocation.get() instanceof FileLocation) {
                String path = optionalSourceLocation.get()
                    .getFileName();
                return Optional.of(path);
            }
        }
        return empty();
    }

    enum MessageContent {
        TITLE {
            @Override
            String toText(ExecutableRule<?> rule, Row row, String filler) {
                return rule.getDescription();
            }
        },
        DETAILS {
            @Override
            String toText(ExecutableRule<?> rule, Row row, String filler) {
                return formatColumns(null, row, filler);
            }
        },
        NONE {
            @Override
            String toText(ExecutableRule<?> rule, Row row, String filler) {
                return null;
            }
        },
        FULL {
            @Override
            String toText(ExecutableRule<?> rule, Row row, String filler) {
                return formatColumns(rule.getDescription(), row, filler);
            }
        };

        protected String formatColumns(String description, Row row, String filler) {
            StringBuilder message = new StringBuilder();
            if (description != null) {
                message.append(description)
                    .append(filler);
            }
            row.getColumns()
                .forEach((key, value) -> {
                    if (!"location".equalsIgnoreCase(key)) {
                        message.append("|")
                            .append(key)
                            .append(": ")
                            .append(value.getLabel())
                            .append(filler);
                    }
                });
            return message.toString();
        }

        abstract String toText(ExecutableRule<?> rule, Row row, String filler);
    }

}
