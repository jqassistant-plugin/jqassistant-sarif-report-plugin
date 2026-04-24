package org.jqassistant.plugin.sarif.report.impl;

import com.buschmais.jqassistant.core.report.api.model.Result.Status;

import org.jqassistant.plugin.sarif.report.api.impl.model.Level;
import org.mapstruct.Mapper;

@Mapper
public interface LevelMapper {
    Level toReport(com.buschmais.jqassistant.core.report.api.model.Result.Status status);
}
