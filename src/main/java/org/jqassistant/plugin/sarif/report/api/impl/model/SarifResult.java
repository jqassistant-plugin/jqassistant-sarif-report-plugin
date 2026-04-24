package org.jqassistant.plugin.sarif.report.api.impl.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;

@Builder
@Getter
@ToString
public class SarifResult {

    private String ruleId;
    private Level level;
    private Message message;

    @Singular
    private List<Location> locations;
    private SarifProperties properties;

    @Builder
    @Getter
    @ToString
    public static class Message {

        private String text;
        private String markdown;
    }

    @Builder
    @Getter
    @ToString
    public static class SarifProperties {

        @Builder.Default
        private final String type = "issue";

        @JsonProperty("check_name")
        private String checkName;
    }
}
