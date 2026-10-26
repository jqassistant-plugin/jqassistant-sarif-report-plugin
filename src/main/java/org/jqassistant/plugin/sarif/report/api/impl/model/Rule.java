package org.jqassistant.plugin.sarif.report.api.impl.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
public class Rule {

    private String id;
    private DefaultConfiguration defaultConfiguration;

    @Builder
    @Getter
    @ToString
    public static class DefaultConfiguration {

        private Level level;
    }
}
