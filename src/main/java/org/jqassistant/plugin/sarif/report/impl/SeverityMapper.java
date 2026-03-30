package org.jqassistant.plugin.sarif.report.impl;

import org.jqassistant.plugin.sarif.report.api.impl.model.Severity;
import org.mapstruct.Mapper;

@Mapper
public interface SeverityMapper {

    Severity toReport(com.buschmais.jqassistant.core.rule.api.model.Severity severity);

}
