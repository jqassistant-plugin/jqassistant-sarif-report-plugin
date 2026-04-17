pipeline {
    agent any

    tools {
            maven 'Maven'
        }

    stages {
        stage('Archive Results') {
            steps {
                archiveArtifacts artifacts: '**/jqassistant-sarif-report.json',
                                 fingerprint: true,
                                 onlyIfSuccessful: true
            }
        }
    }

    post {
        always {
             recordIssues(
                tool: sarif(pattern: 'src/test/resources/reference/ConstraintWithFailures.json'),
                id: 'reference-report',
                name: 'reference-report'
            )
            recordIssues(
                tool: sarif(pattern: 'src/test/resources/markdown-trial.json'),
                id: 'test-markdown'
            )
        }
    }
}
