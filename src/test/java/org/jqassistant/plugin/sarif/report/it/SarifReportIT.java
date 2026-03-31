package org.jqassistant.plugin.sarif.report.it;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.buschmais.jqassistant.core.report.api.model.Result;
import com.buschmais.jqassistant.core.rule.api.model.Constraint;
import com.buschmais.jqassistant.core.rule.api.model.RuleException;
import com.buschmais.jqassistant.plugin.java.test.AbstractJavaPluginIT;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import static com.buschmais.jqassistant.core.report.api.model.Result.Status.FAILURE;
import static com.buschmais.jqassistant.core.report.api.model.Result.Status.WARNING;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.org.webcompere.modelassert.json.JsonAssertions.assertJson;

public class SarifReportIT extends AbstractJavaPluginIT {

    @Test
    void constraintWithFailures() throws RuleException, IOException {
        verify("ConstraintWithFailures", FAILURE);
    }

    @Test
    void constraintWithWarnings() throws RuleException, IOException {
        verify("ConstraintWithWarnings", WARNING);
    }

    private void verify(String constraintId, Result.Status expectedStatus) throws RuleException, IOException {
        scanClassPathDirectory(getClassesDirectory(TypeWithIssues.class));
        Result<Constraint> result = validateConstraint("sarif-report-it:" + constraintId, Map.of("fqn", TypeWithIssues.class.getName()));

        assertThat(result.getStatus()).isEqualTo(expectedStatus);

        File sarifReport = new File("target/jqassistant/report/sarif/jqassistant-sarif-report.json");
        assertThat(sarifReport).exists();
        String expectedJson = IOUtils.toString(SarifReportIT.class.getResourceAsStream("/reference/" + constraintId + ".json"), UTF_8);
        assertJson(sarifReport).isEqualTo(expectedJson);
    }
}
