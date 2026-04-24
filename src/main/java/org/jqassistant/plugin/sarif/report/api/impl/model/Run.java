package org.jqassistant.plugin.sarif.report.api.impl.model;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
public class Run {

    private Tool tool;
    private List<SarifResult> results;

    @Builder
    @Getter
    @ToString
    public static class Tool {

        private Driver driver;

        @Builder
        @Getter
        @ToString
        public static class Driver {

            @Builder.Default
            private final String name = "jQAssistant";
        }
    }
}
