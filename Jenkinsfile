pipeline {
    agent any
    options {
        skipStagesAfterUnstable()
    }
    stages {
        stage('Build') {
            steps {
                sh 'mvn -B -DskipTests clean package'
            }
        }
        stage('Test') {
            steps {
                // verify = pruebas + reporte y revision de cobertura de JaCoCo
                sh 'mvn -B verify'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }
        stage('Deliver') {
            steps {
                // Corre el programa: imprime la Tabla 1 (p, dof, x esperado y x actual)
                sh 'java -jar target/program6.jar < /dev/null'
            }
        }
    }
}
