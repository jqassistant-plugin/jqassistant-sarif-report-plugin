package org.jqassistant.plugin.sarif.report.api.impl.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Level {

    @JsonProperty(value = "note") SUCCESS,
    @JsonProperty(value = "warning") WARNING,
    @JsonProperty(value = "error") FAILURE,
    @JsonProperty(value = "note") SKIPPED
}
