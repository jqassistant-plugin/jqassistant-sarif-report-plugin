package org.jqassistant.plugin.sarif.report.api.impl.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;


@Builder
@Getter
@ToString
public class Issue {

    private final String type = "issue";

    private String description;

    @JsonProperty("check_name")
    private String checkName;

    private String ruleId;

    private Severity severity;

    private Location location;

}
