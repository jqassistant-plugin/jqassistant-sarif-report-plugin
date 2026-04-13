pipeline {
    agent any

    tools {
            maven 'Maven'
        }

    stages {
        stage('Test') {
            steps {
                sh 'mvn verify jqassistant:report -Dit.test=SarifReportIT'
                sh 'find target -name "*.json" || true'
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
            junit 'target/failsafe-reports/*.xml'
        }
    }
}
