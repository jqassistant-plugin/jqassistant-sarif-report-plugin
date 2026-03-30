package org.jqassistant.plugin.sarif.report.api.impl.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
public class Location {

    private String path;

    private Lines lines;

    @Builder
    @Getter
    @ToString
    public static class Lines {

        private int begin;

        private int end;

    }
}
