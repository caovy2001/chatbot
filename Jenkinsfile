pipeline {
    agent any
    stages {
        stage('Clone') {
            steps {
                git branch: 'main', credentialsId: 'vyphotphet100-github-2', url: 'https://github.com/vyphotphet100/chatbot.git'
            }
        }
        stage('Build stage') {
            steps {
                echo '======= START BUILD STAGE ========'
                sh 'sudo docker compose up -d --build'
                sh 'echo "y" | sudo docker system prune -a'
                echo '======= FINISH BUILD STAGE ========'
            }
        }
    }
}