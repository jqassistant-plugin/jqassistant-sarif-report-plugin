pipeline {
    agent any

    tools {
            maven 'Maven'
        }

    stages {
        stage('build') {
            steps {
                sh 'mvn verify -Dtest=SarifReportIT'
            }
        }
        stage('Archive Results') {
            steps {
                archiveArtifacts artifacts: 'target/jqassistant/report/sarif/jqassistant-sarif-report.json',
                                 fingerprint: true,
                                 onlyIfSuccessful: true
            }
        }
    }

    post {
        always {
             recordIssues(
                tool: sarif(pattern: 'target/jqassistant/report/sarif/jqassistant-sarif-report.json'),
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
