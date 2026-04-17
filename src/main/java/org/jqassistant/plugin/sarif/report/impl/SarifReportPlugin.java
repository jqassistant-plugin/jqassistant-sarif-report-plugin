package org.jqassistant.plugin.sarif.report.impl;

import java.io.File;
import java.io.IOException;
import java.util.*;

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

    private static final SeverityMapper SEVERITY_MAPPER = Mappers.getMapper(SeverityMapper.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().setDefaultPropertyInclusion(NON_NULL);

    private ReportContext reportContext;

    private List<SarifResult> results;

    @Override
    public void configure(ReportContext reportContext, Map<String, Object> properties) {
        this.reportContext = reportContext;
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

        StringBuilder description = new StringBuilder(constraint.getDescription());
        ArrayList<String> header = new ArrayList<>();
        ArrayList<String> values = new ArrayList<>();

        row.getColumns()
            .forEach((key, value) -> {
                header.add(key);
                values.add(value.getLabel());
            });

        StringBuilder message = new StringBuilder(description + " | " + header.get(0) + "='" + values.get(0) + "'");
        for (int i = 1; i < header.size(); i++) {
            message.append(", ")
                .append(header.get(i))
                .append("='")
                .append(values.get(i))
                .append("'");
        }

        resultBuilder.message(SarifResult.Message.builder()
            .text(message.toString())
            .markdown(generateMarkdown(description.toString(), header, values))
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

    private String generateMarkdown(String description, ArrayList<String> header, ArrayList<String> values) {
        return "### " + description + "\n\n| " + String.join(" | ", header) + " |\n| :--- | :--- |\n| " + String.join(" | ", values) + " |";
    }
}
