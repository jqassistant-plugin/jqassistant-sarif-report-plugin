pipeline {
    agent any

    tools {
            maven 'Maven'
        }

    stages {
        stage('build') {
            steps {
                sh 'mvn clean verify'
                sh 'mvn test -Dtest=SarifReportIT'
            }
        }
        stage('Archive Results') {
            steps {
                archiveArtifacts artifacts: 'target/jqassistant/report/sarif/jqassistant-sarif-report.json',
                                 fingerprint: true,
                                 onlyIfSuccessful: true
            }
        }
        stage('Debug File') {
            steps {
                // This prints the timestamp of the report file
                sh 'ls -l target/jqassistant/report/sarif/jqassistant-sarif-report.json || echo "File not found!"'
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
        }
    }
}
