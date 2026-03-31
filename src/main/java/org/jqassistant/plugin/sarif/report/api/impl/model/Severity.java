package org.jqassistant.plugin.sarif.report.api.impl.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Severity {

    @JsonProperty(value = "info")
    INFO,
    @JsonProperty(value = "warning")
    MINOR,
    @JsonProperty (value = "error")
    MAJOR,
    @JsonProperty(value = "critical")
    CRITICAL,
    @JsonProperty(value = "blocker")
    BLOCKER

}
