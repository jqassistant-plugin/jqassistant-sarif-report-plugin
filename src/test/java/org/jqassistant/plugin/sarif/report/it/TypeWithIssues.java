package org.jqassistant.plugin.sarif.report.it;

import com.buschmais.jqassistant.plugin.java.annotation.jQASuppress;

public class TypeWithIssues {

    private static String issueField;

    public void issueMethod() {
        System.out.println("issueMethod");
    }

    @jQASuppress(value = { "sarif-report-it:ConstraintWithFailures", "sarif-report-it:ConstraintWitWarnings" })
    public void suppressedIssueMethod() {
        System.out.println("suppressedIssueMethod");
    }

    public void nonIssueMethod() {
        System.out.println("nonIssueMethod");
    }
}
