//@Library("deployScript@main") _

pipeline{
    agent any 

    environment {
    doError = '0'
    CICD_GIT_REPO = "git@uno.brokenenigma.com:raghavendram/cicd-pipeline.git"
    CICD_GIT_BRANCH = "main"
    CICD_CREDENTIALS_ID = "ae59053b-c2aa-456b-98b1-52794e33dcd5"
    APP_GIT_REPO = "git@uno.brokenenigma.com:raghavendram/unocoin-frontend-upgraded.git"
    APP_GIT_BRANCH = "main"
    APP_CREDENTIALS_ID = "ae59053b-c2aa-456b-98b1-52794e33dcd5"
    BUILD_IMAGE_NAME = "frontend-landing-app"
    REPO_URL = "058264316945.dkr.ecr.ap-south-1.amazonaws.com/frontend-landing-app"
    REPO_LOGIN = "058264316945.dkr.ecr.ap-south-1.amazonaws.com"
    DEPLOYMENT_NAME = "fe-landing"
    REGION = "ap-south-1"
    NAMESPACE = "frontend"
    CONTAINER_NAME = "frontend-landing-app"
    }

    stages {

        stage('Clone Application Repo') {
            steps {
                echo "Cloning App Repo..."
                pullGitRepo("${env.APP_GIT_BRANCH}", "${env.APP_CREDENTIALS_ID}", "${env.APP_GIT_REPO}")
                echo "App Repo Cloned."

            }
        }

        stage('Clone CICD Repo') {
            steps {
                dir('..') {

                    echo "Cloning CICD Repo..."
                    pullGitRepo("${env.CICD_GIT_BRANCH}", "${env.CICD_CREDENTIALS_ID}", "${env.CICD_GIT_REPO}")
                    echo "CICD Repo Cloned."   
                }
            }
        }

        stage("Build Image") {
            steps {
                echo "Building the latest image..."
                buildImage("${env.BUILD_IMAGE_NAME}")
            }
        }

        stage("Push Image") {
        steps {
            echo "Login into ECR Repo..."
            
            pushImage("${env.REGION}", "${env.REPO_LOGIN}", "${env.REPO_URL}", "${env.BUILD_IMAGE_NAME}")
            echo "Pushed the image to ECR"
            }
        }

        stage("Deploy Latest Build") {
        steps {
            echo "Deploying the latest build..."
            deployImage("${env.DEPLOYMENT_NAME}", "${env.REPO_URL}", "${env.NAMESPACE}", "${env.CONTAINER_NAME}")
            echo "Deployment is done"
            }
        }

        stage('Cleanup') {
            steps {
                echo "Cleaning up the images..."
                imageCleanup("${env.BUILD_IMAGE_NAME}")
                echo "Image cleanup completed"
            }
        }

    }

    post {

        failure{
            echo "Notifying in slack."
            slackFailureNotification()
            echo "Notified the failure status in slack."  
        }

        success{
            echo "Notifying in slack."
            slackSuccessNotification()
            echo "Notified the success status in slack."
        }
        
    }
}

