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
import com.buschmais.jqassistant.core.rule.api.model.Constraint;
import com.buschmais.jqassistant.core.rule.api.model.ExecutableRule;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jqassistant.plugin.sarif.report.api.impl.model.*;
import org.mapstruct.factory.Mappers;

import static com.buschmais.jqassistant.core.report.api.model.Result.Status.FAILURE;
import static com.buschmais.jqassistant.core.report.api.model.Result.Status.WARNING;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Default
@Slf4j
public class SarifReportPlugin implements ReportPlugin {

    public static final String REPORT_DIRECTORY = "sarif";
    public static final String REPORT_FILE = "jqassistant-sarif-report.json";
    private static final LevelMapper LEVEL_MAPPER = Mappers.getMapper(LevelMapper.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().setDefaultPropertyInclusion(NON_NULL);
    private static final Location DEFAULT_LOCATION = Location.builder()
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

    private ReportContext reportContext;
    private List<SarifResult> sarifResults;
    private List<Rule> rules;
    private Set<String> registeredRuleIds;

    @Override
    public void configure(ReportContext reportContext, Map<String, Object> properties) {

        this.reportContext = reportContext;
    }

    @Override
    public void begin() {
        sarifResults = new LinkedList<>();
        rules = new ArrayList<>();
        registeredRuleIds = new HashSet<>();
    }

    @Override
    public void setResult(Result<? extends ExecutableRule> result) {
        Result.Status status = result.getStatus();
        if (FAILURE.equals(status) || WARNING.equals(status)) {
            ExecutableRule<?> executableRule = result.getRule();
            Constraint constraint = (Constraint) executableRule;
            Rule.DefaultConfiguration.DefaultConfigurationBuilder levelBuilder = Rule.DefaultConfiguration.builder();
            for (Row row : result.getRows()) {
                if (!row.isHidden()) {
                    sarifResults.add(getSarifResult(result, constraint, row));
                    levelBuilder.level(LEVEL_MAPPER.toReport(row.getStatus()));
                }
            }
            if (registeredRuleIds.add(constraint.getId())) {
                rules.add(Rule.builder()
                    .id(constraint.getId())
                    .shortDescription(Text.builder()
                        .text(constraint.getDescription())
                        .build())
                    .defaultConfiguration(levelBuilder.build())
                    .build());
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
                .results(this.sarifResults)
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

    private SarifResult getSarifResult(Result<? extends ExecutableRule> result, Constraint constraint, Row row) {
        SarifResult.SarifResultBuilder resultBuilder = SarifResult.builder()
            .properties(SarifResult.SarifProperties.builder()
                .checkName("[jQAssistant]" + constraint.getId())
                .build())
            .ruleId(constraint.getId());

        String text = formatColumns(constraint.getDescription(), row);

        resultBuilder.message(Text.builder()
            .text(text)
            .build());
        resultBuilder.location(getLocation(result, row));
        return resultBuilder.build();
    }

    private Location getLocation(Result<? extends ExecutableRule> result, Row row) {
        return result.getPrimaryColumn()
            .map(primaryColumnName -> row.getColumns()
                .get(primaryColumnName))
            .flatMap(Column::getSourceLocation)
            .filter(location -> location instanceof FileLocation)
            .map(location -> (FileLocation) location)
            .filter(location -> location.getPath() != null)
            .map(SarifReportPlugin::getLocation)
            .orElse(DEFAULT_LOCATION);
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
