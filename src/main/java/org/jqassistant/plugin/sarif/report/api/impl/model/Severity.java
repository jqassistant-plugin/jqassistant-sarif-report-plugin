package org.jqassistant.plugin.sarif.report.api.impl.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Severity {

    @JsonProperty(value = "note") INFO,
    @JsonProperty(value = "warning") MINOR,
    @JsonProperty(value = "error") MAJOR,
    @JsonProperty(value = "error") CRITICAL,
    @JsonProperty(value = "error") BLOCKER

}
