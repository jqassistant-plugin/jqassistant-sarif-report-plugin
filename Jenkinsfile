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
        stage('Record Report') {
            steps {
                sh "sed -i 's/\\\\n/<br>/g' src/test/resources/reference/ConstraintWithFailures.json"
                recordIssues tool: sarif(pattern: 'src/test/resources/reference/ConstraintWithFailures.json')
            }
        }
    }
}
