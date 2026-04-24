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
    void testExceptionThrowWhenTextIsNone() {

        ReportException exception = assertThrows(ReportException.class, () -> testPlugin.configure(testContext, Map.of("sarif.report.message.text", "NONE")));
        assertEquals("sarif.report.message.text cannot be NONE", exception.getMessage());
    }
}
