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
import static com.buschmais.jqassistant.core.scanner.api.DefaultScope.NONE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.org.webcompere.modelassert.json.JsonAssertions.assertJson;

public class SarifReportIT {

    protected abstract static class AbstractNestedSarifReportIT extends AbstractJavaPluginIT {
        protected void scanSourcesAndClasspathDirectory() {
            File classesDirectory = getClassesDirectory(TypeWithIssues.class);
            File sourceDirectory = new File(classesDirectory, "../../src/test/java");
            getScanner().scan(sourceDirectory, null, NONE);
            scanClassPathDirectory(classesDirectory);
        }
    }

    @Nested
    public class WithTextAndDetailsIT extends AbstractNestedSarifReportIT {

        @Override
        protected Map<String, Object> getReportProperties() {
            return Map.of("sarif.report.message.text", "TITLE", "sarif.report.message.markdown", "DETAILS");
        }

        @Test
        void verifyTitleAndDetailsOnWarning() throws RuleException, IOException {
            scanSourcesAndClasspathDirectory();
            Result<Constraint> result = validateConstraint("sarif-report-it:ConstraintWithWarnings", Map.of("fqn", TypeWithIssues.class.getName()));

            verify("ConstraintWithWarnings", "TitleAndDetails", WARNING, result);
        }

        @Test
        void verifyTitleAndDetailsOnFailure() throws RuleException, IOException {
            scanSourcesAndClasspathDirectory();
            Result<Constraint> result = validateConstraint("sarif-report-it:ConstraintWithFailures", Map.of("fqn", TypeWithIssues.class.getName()));

            verify("ConstraintWithFailures", "TitleAndDetails", FAILURE, result);
        }
    }

    @Nested
    public class FullContentIT extends AbstractNestedSarifReportIT {

        @Override
        protected Map<String, Object> getReportProperties() {
            return Map.of("sarif.report.message.text", "FULL", "sarif.report.message.markdown", "FULL");
        }

        @Test
        void verifyFullContentOnWarning() throws RuleException, IOException {
            scanSourcesAndClasspathDirectory();
            Result<Constraint> result = validateConstraint("sarif-report-it:ConstraintWithWarnings", Map.of("fqn", TypeWithIssues.class.getName()));

            verify("ConstraintWithWarnings", "Full", WARNING, result);
        }

        @Test
        void verifyFullContentOnFailure() throws RuleException, IOException {
            scanSourcesAndClasspathDirectory();
            Result<Constraint> result = validateConstraint("sarif-report-it:ConstraintWithFailures", Map.of("fqn", TypeWithIssues.class.getName()));

            verify("ConstraintWithFailures", "Full", FAILURE, result);
        }
    }

    @Nested
    public class TextOnlyContentIT extends AbstractNestedSarifReportIT {

        @Override
        protected Map<String, Object> getReportProperties() {
            return Map.of("sarif.report.message.text", "FULL", "sarif.report.message.markdown", "NONE");
        }

        @Test
        void verifyTextOnlyContentOnWarning() throws RuleException, IOException {
            scanSourcesAndClasspathDirectory();
            Result<Constraint> result = validateConstraint("sarif-report-it:ConstraintWithWarnings", Map.of("fqn", TypeWithIssues.class.getName()));

            verify("ConstraintWithWarnings", "TextOnly", WARNING, result);
        }

        @Test
        void verifyTextOnlyContentOnFailure() throws RuleException, IOException {
            scanSourcesAndClasspathDirectory();

            Result<Constraint> result = validateConstraint("sarif-report-it:ConstraintWithFailures", Map.of("fqn", TypeWithIssues.class.getName()));

            verify("ConstraintWithFailures", "TextOnly", FAILURE, result);
        }
    }

    @Nested
    public class MissingConfigToDefaultIT extends AbstractNestedSarifReportIT {

        @Test
        void verifyDefaultOnWarning() throws RuleException, IOException {
            scanSourcesAndClasspathDirectory();

            Result<Constraint> result = validateConstraint("sarif-report-it:ConstraintWithWarnings", Map.of("fqn", TypeWithIssues.class.getName()));

            verify("ConstraintWithWarnings", "Full", WARNING, result);
        }

        @Test
        void verifyDefaultOnFailure() throws RuleException, IOException {
            scanSourcesAndClasspathDirectory();

            Result<Constraint> result = validateConstraint("sarif-report-it:ConstraintWithFailures", Map.of("fqn", TypeWithIssues.class.getName()));

            verify("ConstraintWithFailures", "Full", FAILURE, result);
        }

    }

    @Nested
    public class ConstraintWithoutLocation extends AbstractNestedSarifReportIT {

        @Test
        void verifyConstraintWithoutLocation() throws RuleException, IOException {
            Result<Constraint> result = validateConstraint("sarif-report-it:ConstraintWithoutLocation");

            verify("ConstraintWithoutLocation", "Full", FAILURE, result);
        }

    }


    private void verify(String constraintId, String referencePath, Result.Status expectedStatus, Result<Constraint> result) throws IOException {
        assertThat(result.getStatus()).isEqualTo(expectedStatus);
        File sarifReport = new File("target/jqassistant/report/sarif/jqassistant-sarif-report.json");
        assertThat(sarifReport).exists();
        String expectedJson = IOUtils.toString(SarifReportIT.class.getResourceAsStream("/reference/" + constraintId + referencePath + ".json"), UTF_8);
        assertJson(sarifReport).isEqualTo(expectedJson);
    }
}
