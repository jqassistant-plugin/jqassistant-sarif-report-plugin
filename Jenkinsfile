pipeline {
    agent any

    tools {
            maven 'Maven'
        }

    stages {
        stage('Test') {
            steps {
                sh 'mvn clean verify -Dtest=SarifReportIT'
                sh 'find target -name "jqassistant-sarif-report.json"'
            }
        }

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
            junit 'target/failsafe-reports/*.xml'
        }
    }
}
