package org.jqassistant.plugin.sarif.report.api.impl.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
public class Location {

    private PhysicalLocation physicalLocation;

    @Builder
    @Getter
    @ToString
    public static class PhysicalLocation {

        private ArtifactLocation artifactLocation;

        @Builder
        @Getter
        @ToString
        public static class ArtifactLocation {
            private String uri;
        }

        private Region region;

        @Builder
        @Getter
        @ToString
        public static class Region {

            int startLine;

            int endLine;
        }
    }
}
