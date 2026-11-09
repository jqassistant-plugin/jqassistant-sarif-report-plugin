package org.jqassistant.plugin.sarif.report.api.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
public class Rule {

    private String id;
    private Text shortDescription;
    private DefaultConfiguration defaultConfiguration;

    @Builder
    @Getter
    @ToString
    public static class DefaultConfiguration {

        private Level level;
    }
}
