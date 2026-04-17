package org.jqassistant.plugin.sarif.report.it;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.buschmais.jqassistant.core.report.api.model.Result;
import com.buschmais.jqassistant.core.rule.api.model.Constraint;
import com.buschmais.jqassistant.core.rule.api.model.RuleException;
import com.buschmais.jqassistant.plugin.java.test.AbstractJavaPluginIT;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.buschmais.jqassistant.core.report.api.model.Result.Status.FAILURE;
import static com.buschmais.jqassistant.core.report.api.model.Result.Status.WARNING;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.org.webcompere.modelassert.json.JsonAssertions.assertJson;

public class SarifReportIT extends AbstractJavaPluginIT {

    @Nested
    public class TextContentTitleIT extends AbstractJavaPluginIT {

        @Override
        protected Map<String, Object> getReportProperties() {
            return Map.of("sarif.report.message.text", "DETAILS");
        }

        @Test
        void verifyTitleOnlyForFailure() throws RuleException, IOException {
            verify("ConstraintWithFailure", "-titleOnly", FAILURE);
        }

        @Test
        void verifyTitleOnlyForWarning() throws RuleException, IOException {
            verify("ConstraintWithWarning", "-titleOnly", FAILURE);
        }
    }

    @Nested
    public class Nested2IT extends AbstractJavaPluginIT {

        @Override
        protected Map<String, Object> getReportProperties() {
            return Map.of("sarif.report....", "FULL");
        }

      //  @Test
      //  void constraintWithWarnings() throws RuleException, IOException {
      //      verify("ConstraintWithWarnings", WARNING);
      //  }
    }

    private void verify(String constraintId, String referencePath, Result.Status expectedStatus) throws RuleException, IOException {
        scanClassPathDirectory(getClassesDirectory(TypeWithIssues.class));
        Result<Constraint> result = validateConstraint("sarif-report-it:" + constraintId, Map.of("fqn", TypeWithIssues.class.getName()));

        assertThat(result.getStatus()).isEqualTo(expectedStatus);

        File sarifReport = new File("target/jqassistant/report/sarif/jqassistant-sarif-report.json");
        assertThat(sarifReport).exists();
        String expectedJson = IOUtils.toString(SarifReportIT.class.getResourceAsStream("/reference/" + constraintId + referencePath + ".json"), UTF_8);
        assertJson(sarifReport).isEqualTo(expectedJson);
    }
}
