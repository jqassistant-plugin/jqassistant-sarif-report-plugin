package org.jqassistant.plugin.sarif.report.api.impl.model.codeclimate;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.jqassistant.plugin.sarif.report.api.impl.model.codeclimate.Location;
import org.jqassistant.plugin.sarif.report.api.impl.model.Severity;

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
