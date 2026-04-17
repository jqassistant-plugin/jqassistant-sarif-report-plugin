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
                tool: sarif(pattern: 'src/test/resources/reference/ConstraintWithFailures.json')
            )
        }
    }
}
