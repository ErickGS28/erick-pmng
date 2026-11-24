pipeline {
    agent any

    environment {
        COMPOSE_PROJECT_NAME = 'pmng'
        COMPOSE_FILE = 'docker-compose.yml'
        ENV_FILE = credentials('pmng-env')
        DC = 'docker compose'
    }

    stages {
        stage('Parar servicios') {
            steps {
                echo 'Deteniendo servicios'
                sh '''
                    $DC -p $COMPOSE_PROJECT_NAME -f $COMPOSE_FILE --env-file $ENV_FILE down || true
                    docker rm -f pmng-mysql pmng-services pmng-front || true
                '''
            }
        }

        stage('Eliminar imagenes') {
            steps {
                echo 'Eliminando imagenes'
                sh '''
                    IMAGES=$($DC -p $COMPOSE_PROJECT_NAME -f $COMPOSE_FILE --env-file $ENV_FILE config --images | sort -u)
                    for IMAGE in $IMAGES; do
                        echo "Eliminando imagen: $IMAGE"
                        docker rmi -f $IMAGE || true
                    done
                '''
            }
        }

        stage('Construir imagenes') {
            steps {
                echo 'Construyendo imagenes'
                sh '$DC -p $COMPOSE_PROJECT_NAME -f $COMPOSE_FILE --env-file $ENV_FILE build'
            }
        }

        stage('Levantar contenedores') {
            steps {
                echo 'Levantando contenedores'
                sh '$DC -p $COMPOSE_PROJECT_NAME -f $COMPOSE_FILE --env-file $ENV_FILE up -d'
            }
        }
    }

    post {
        success {
            echo 'Pipeline completado con exito'
        }
        failure {
            echo 'Pipeline fallido'
        }
    }
}
