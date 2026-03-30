package org.jqassistant.plugin.sarif.report.api.impl.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Severity {

    @JsonProperty(value = "info")
    INFO,
    @JsonProperty(value = "minor")
    MINOR,
    @JsonProperty (value = "major")
    MAJOR,
    @JsonProperty(value = "critical")
    CRITICAL,
    @JsonProperty(value = "blocker")
    BLOCKER

}
