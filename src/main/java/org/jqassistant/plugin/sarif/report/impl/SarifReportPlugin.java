package org.jqassistant.plugin.sarif.report.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import com.buschmais.jqassistant.core.report.api.ReportContext;
import com.buschmais.jqassistant.core.report.api.ReportException;
import com.buschmais.jqassistant.core.report.api.ReportPlugin;
import com.buschmais.jqassistant.core.report.api.ReportPlugin.Default;
import com.buschmais.jqassistant.core.report.api.model.Column;
import com.buschmais.jqassistant.core.report.api.model.Row;
import com.buschmais.jqassistant.core.report.api.model.source.FileLocation;
import com.buschmais.jqassistant.core.rule.api.model.Constraint;
import com.buschmais.jqassistant.core.rule.api.model.ExecutableRule;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jqassistant.plugin.sarif.report.api.model.*;
import org.mapstruct.factory.Mappers;

import static com.buschmais.jqassistant.core.report.api.model.Result.Status.FAILURE;
import static com.buschmais.jqassistant.core.report.api.model.Result.Status.WARNING;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Default
@Slf4j
public class SarifReportPlugin implements ReportPlugin {

    public static final String REPORT_DIRECTORY = "sarif";
    public static final String REPORT_FILE = "jqassistant-sarif-report.json";
    private static final String PROPERTY_DEFAULT_LOCATION = "sarif.report.default-location";
    private static final LevelMapper LEVEL_MAPPER = Mappers.getMapper(LevelMapper.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().setDefaultPropertyInclusion(NON_NULL);

    private ReportContext reportContext;
    private List<Result> results;
    private List<Rule> rules;
    private Set<String> registeredRuleIds;
    private Location defaultLocation;

    @Override
    public void configure(ReportContext reportContext, Map<String, Object> properties) {
        this.reportContext = reportContext;
        String location = (String) properties.get(PROPERTY_DEFAULT_LOCATION);
        if (location == null) {
            this.defaultLocation = Location.builder()
                .physicalLocation(Location.PhysicalLocation.builder()
                    .artifactLocation(Location.PhysicalLocation.ArtifactLocation.builder()
                        .uri(".jqassistant.yml")
                        .build())
                    .region(Location.PhysicalLocation.Region.builder()
                        .startLine(1)
                        .endLine(1)
                        .build())
                    .build())
                .build();
        } else if (!Files.exists(Path.of(location))) {
            throw new IllegalStateException("File not found. The value of '" + PROPERTY_DEFAULT_LOCATION + "' in '.jqassistant.yml' is not valid.");
        }
    }

    @Override
    public void begin() {
        results = new LinkedList<>();
        rules = new ArrayList<>();
        registeredRuleIds = new HashSet<>();
    }

    @Override
    public void setResult(com.buschmais.jqassistant.core.report.api.model.Result<? extends ExecutableRule> result) {
        com.buschmais.jqassistant.core.report.api.model.Result.Status status = result.getStatus();
        if (FAILURE.equals(status) || WARNING.equals(status)) {
            ExecutableRule<?> executableRule = result.getRule();
            Constraint constraint = (Constraint) executableRule;

            Rule.DefaultConfiguration.DefaultConfigurationBuilder levelBuilder = Rule.DefaultConfiguration.builder();
            if (registeredRuleIds.add(constraint.getId())) {
                rules.add(Rule.builder()
                    .id(constraint.getId())
                    .shortDescription(Rule.MultiformatMessageString.builder()
                        .text(constraint.getDescription())
                        .build())
                    .defaultConfiguration(levelBuilder.level(LEVEL_MAPPER.toReport(result.getStatus()))
                        .build())
                    .build());
            }
            for (Row row : result.getRows()) {
                if (!row.isHidden()) {
                    results.add(getSarifResult(result, constraint, row));
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
                        .rules(rules)
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

    private Result getSarifResult(com.buschmais.jqassistant.core.report.api.model.Result<? extends ExecutableRule> result, Constraint constraint, Row row) {
        Result.ResultBuilder resultBuilder = Result.builder()
            .properties(Result.SarifProperties.builder()
                .checkName("[jQAssistant]" + constraint.getId())
                .build())
            .ruleId(constraint.getId());

        String message = formatColumns(constraint.getDescription(), row);

        resultBuilder.message(Result.Message.builder()
            .text(message)
            .build());
        resultBuilder.location(getLocation(result, row));
        return resultBuilder.build();
    }

    private Location getLocation(com.buschmais.jqassistant.core.report.api.model.Result<? extends ExecutableRule> result, Row row) {
        return result.getPrimaryColumn()
            .map(primaryColumnName -> row.getColumns()
                .get(primaryColumnName))
            .flatMap(Column::getSourceLocation)
            .filter(location -> location instanceof FileLocation)
            .map(location -> (FileLocation) location)
            .filter(location -> location.getPath() != null)
            .map(SarifReportPlugin::getLocation)
            .orElse(this.defaultLocation);
    }

    private static Location getLocation(FileLocation location) {
        Location.LocationBuilder locationBuilder = Location.builder();
        Location.PhysicalLocation.PhysicalLocationBuilder physicalLocationBuilder = Location.PhysicalLocation.builder();
        physicalLocationBuilder.artifactLocation(Location.PhysicalLocation.ArtifactLocation.builder()
            .uri(location.getPath())
            .build());
        Location.PhysicalLocation.Region.RegionBuilder regionBuilder = Location.PhysicalLocation.Region.builder()
            .startLine(location.getStartLine()
                .orElse(1))
            .endLine(location.getEndLine()
                .orElse(1));
        physicalLocationBuilder.region(regionBuilder.build());
        return locationBuilder.physicalLocation(physicalLocationBuilder.build())
            .build();
    }

    private String formatColumns(String description, Row row) {
        StringBuilder message = new StringBuilder();
        if (description != null) {
            message.append(description)
                .append(" ");
        }
        row.getColumns()
            .forEach((key, value) -> {
                if (!"location".equalsIgnoreCase(key)) {
                    message.append("| ")
                        .append(key)
                        .append(": ")
                        .append(value.getLabel())
                        .append(" ");
                }
            });
        return message.toString();
    }

}
