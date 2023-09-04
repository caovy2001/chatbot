pipeline {
    agent any
    stages {
        stage('Clone') {
            steps {
                git 'https://github.com/vyphotphet100/chatbot.git'
            }
        }
        stage('Build stage') {
            steps {
                echo '======= START BUILD STAGE ========'
                sh 'docker compose up -d --build'
                sh 'echo "y" | docker system prune -a'
                echo '======= FINISH BUILD STAGE ========'
            }
        }
    }
}