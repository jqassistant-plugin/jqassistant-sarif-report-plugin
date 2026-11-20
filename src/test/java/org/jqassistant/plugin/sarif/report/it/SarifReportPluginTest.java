package org.jqassistant.plugin.sarif.report.it;

import java.lang.reflect.Field;
import java.util.Map;

import com.buschmais.jqassistant.core.report.api.ReportContext;

import org.jqassistant.plugin.sarif.report.api.model.Location;
import org.jqassistant.plugin.sarif.report.impl.SarifReportPlugin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

public class SarifReportPluginTest {
    private final SarifReportPlugin testPlugin = new SarifReportPlugin();
    private final ReportContext testContext = mock(ReportContext.class);
    private final String defaultLocationProperty = "sarif.report.default-location";

    @Test
    void defaultLocationMustReferToExistingFile() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> testPlugin.configure(testContext, Map.of(defaultLocationProperty, "non-existing-file")));
        assertEquals("File not found. The value of '" + defaultLocationProperty + "' in '.jqassistant.yml' is not valid.", exception.getMessage());
    }

    @Test
    void defaultLocationDefaultValue() throws NoSuchFieldException, IllegalAccessException {
        testPlugin.configure(testContext, Map.of());

        Field locationField = SarifReportPlugin.class.getDeclaredField("defaultLocation");
        locationField.setAccessible(true);
        Location actualLocation = (Location) locationField.get(testPlugin);
        Location expectedLocation = Location.builder()
            .physicalLocation(Location.PhysicalLocation.builder()
                .artifactLocation(Location.PhysicalLocation.ArtifactLocation.builder()
                    .uri(".jqassistant.yml")
                    .build())
                .region(Location.PhysicalLocation.Region.builder()
                    .startLine(1)
                    .endLine(1)
                    .build())
                .build())
            .build();
        assertEquals(actualLocation.toString(), expectedLocation.toString());

    }
}
