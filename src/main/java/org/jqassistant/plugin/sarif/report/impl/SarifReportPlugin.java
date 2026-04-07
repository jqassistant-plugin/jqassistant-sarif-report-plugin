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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jqassistant.plugin.sarif.report.api.impl.model.Run;
import org.jqassistant.plugin.sarif.report.api.impl.model.SarifLog;
import org.jqassistant.plugin.sarif.report.api.impl.model.SarifResult;
import org.jqassistant.plugin.sarif.report.api.impl.model.Location;
import org.mapstruct.factory.Mappers;

import static com.buschmais.jqassistant.core.report.api.model.Result.Status.FAILURE;
import static com.buschmais.jqassistant.core.report.api.model.Result.Status.WARNING;
import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.joining;

@Default
@Slf4j
public class SarifReportPlugin implements ReportPlugin {

    public static final String REPORT_DIRECTORY = "sarif";

    public static final String REPORT_FILE = "jqassistant-sarif-report.json";

    private static final SeverityMapper SEVERITY_MAPPER = Mappers.getMapper(SeverityMapper.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().enable(INDENT_OUTPUT)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private ReportContext reportContext;

    private List<SarifResult> results;

    @Override
    public void configure(ReportContext reportContext, Map<String, Object> properties) {
        this.reportContext = reportContext;
    }

    @Override
    public void begin() { results = new LinkedList<>(); }

    @Override
    public void setResult(Result<? extends ExecutableRule> result) {
        Result.Status status = result.getStatus();
    //    if (FAILURE.equals(status) || WARNING.equals(status)) {
            ExecutableRule<?> executableRule = result.getRule();
            Constraint constraint = (Constraint) executableRule;
            for (Row row : result.getRows()) {
                if (!row.isHidden()) {
                    results.add(getResult(result, constraint, row));
                }
            }
      //  }
    }

    @Override
    public void end() throws ReportException {

        File reportDirectory = reportContext.getReportDirectory(REPORT_DIRECTORY);

        SarifLog report = SarifLog.builder()
                .runs(List.of(
                        Run.builder()
                                .tool(Run.Tool.builder()
                                        .driver(Run.Tool.Driver.builder().build())
                                .build())
                                .results(this.results)
                        .build()
                )).build();
        try {
            File file = new File(reportDirectory, REPORT_FILE).getCanonicalFile();
            log.info("Writing SARIF report to {}.", file);
            OBJECT_MAPPER.writeValue(file, report);
        } catch (IOException e) {
            throw new ReportException("Failed to write SARIF report file.", e);
        }
    }

    private SarifResult getResult(Result<? extends ExecutableRule> result, Constraint constraint, Row row) {
        SarifResult.SarifResultBuilder resultBuilder = SarifResult.builder()
                .properties(SarifResult.SarifProperties.builder().checkName("[jQAssistant]" + constraint.getId()).build())
                .level(SEVERITY_MAPPER.toReport(constraint.getSeverity()))
                .ruleId(row.getKey());

        StringBuilder description = new StringBuilder(constraint.getDescription());
        String columnsValues = row.getColumns()
                .entrySet()
                .stream()
                .map(entry -> entry.getKey() + "='" + entry.getValue()
                        .getLabel() + "'")
                .collect(joining(", "));

        if (!columnsValues.isEmpty()) {
            description.append(" | ")
                    .append(columnsValues);
        }

        resultBuilder.message(SarifResult.Message.builder().text(description.toString()).build());
        getLocation(result, row).ifPresent(resultBuilder::location);
        return resultBuilder.build();
    }

    private Optional<Location> getLocation(Result<? extends ExecutableRule> result, Row row) {
        Optional<String> primaryColumnName = result.getPrimaryColumn();
        if (primaryColumnName.isPresent()) {
            Column<?> column = row.getColumns()
                    .get(primaryColumnName.get());
            Optional<SourceLocation<?>> optionalSourceLocation = column.getSourceLocation();

            if (optionalSourceLocation.isPresent()) {

                SourceLocation<?> sourceLocation = optionalSourceLocation.get();

                Location.LocationBuilder locationBuilder = Location.builder();
                Location.PhysicalLocation.PhysicalLocationBuilder physicalLocationBuilder = Location.PhysicalLocation.builder();

                String rawPath = sourceLocation.getFileName();
                String repoRelativePath = "/src/test/java" + rawPath;

                physicalLocationBuilder.artifactLocation(
                        Location.PhysicalLocation.ArtifactLocation.builder()
                                .uri(repoRelativePath)
                                .build()
                );

                if (sourceLocation instanceof FileLocation) {
                    FileLocation fileLocation = (FileLocation) sourceLocation;

                    fileLocation.getStartLine().ifPresent(start -> {
                        Location.PhysicalLocation.Region.RegionBuilder regionBuilder =
                                Location.PhysicalLocation.Region.builder().startLine(start);

                        fileLocation.getEndLine().ifPresent(regionBuilder::endLine);

                        physicalLocationBuilder.region(regionBuilder.build());
                    });
                }
                Location location = locationBuilder
                        .physicalLocation(physicalLocationBuilder.build())
                        .build();

                return Optional.of(location);
            }
        }
        return empty();
    }
}
