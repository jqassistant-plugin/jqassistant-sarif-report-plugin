package org.jqassistant.plugin.sarif.report.api.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
public class Text {

    private String text;
}
