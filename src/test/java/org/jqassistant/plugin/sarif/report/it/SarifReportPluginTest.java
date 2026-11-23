package org.jqassistant.plugin.sarif.report.it;

import java.util.Map;

import com.buschmais.jqassistant.core.report.api.ReportContext;
import com.buschmais.jqassistant.core.report.api.ReportException;

import org.jqassistant.plugin.sarif.report.impl.SarifReportPlugin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

public class SarifReportPluginTest {
    private final SarifReportPlugin testPlugin = new SarifReportPlugin();
    private final ReportContext testContext = mock(ReportContext.class);

    @Test
    void defaultLocationMustReferToExistingFile() {
        ReportException exception = assertThrows(ReportException.class,
            () -> testPlugin.configure(testContext, Map.of(SarifReportPlugin.PROPERTY_DEFAULT_LOCATION, "non-existing-file")));
        assertEquals("File not found. The value 'non-existing-file' of property '" + SarifReportPlugin.PROPERTY_DEFAULT_LOCATION + "' is not valid.", exception.getMessage());
    }
}
