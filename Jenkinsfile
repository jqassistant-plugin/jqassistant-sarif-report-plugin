pipeline {
    agent any

    tools {
            maven 'maven'
        }

    stages {
        stage('Test') {
            steps {
                sh 'mvn verify -Dit.test=SarifReportIT'
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
