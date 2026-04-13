package org.jqassistant.plugin.sarif.report.api.impl.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
@JsonPropertyOrder({ "$schema", "version", "runs" })
public class SarifReport {

    @Builder.Default
    @JsonProperty("$schema")
    private final String schema = "https://schemastore.azurewebsites.net/schemas/json/sarif-2.1.0-rtm.5.json";

    @Builder.Default
    private final String version = "2.1.0";

    private List<Run> runs;

}
