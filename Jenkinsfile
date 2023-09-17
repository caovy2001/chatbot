pipeline {
    agent any
    stages {
        stage('Clone ') {
            steps {
                git branch: 'main', credentialsId: 'vyphotphet100-github-2', url: 'https://github.com/vyphotphet100/chatbot.git'
            }
        }
        stage('Build stage') {
            steps {
                echo '======= START BUILD STAGE ========'
                sh 'cd /'
                sh 'cd root/test_jenkins_folder'
                sh 'mkdir test'
                //sh 'docker compose up -d --build'
                //sh 'echo "y" | docker system prune -a'
                echo '======= FINISH BUILD STAGE ========'
            }
        }
    }
}
