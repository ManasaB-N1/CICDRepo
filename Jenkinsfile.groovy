//@Library("deployScript@main") _

pipeline{
    agent any 

    environment {

    CICD_GIT_REPO=sh(returnStdout: true, script: 'aws secretsmanager get-secret-value --secret-id "${JOB_NAME}" | jq --raw-output .SecretString | jq -r ."CICD_GIT_REPO"').trim()
    CICD_GIT_BRANCH=sh(returnStdout: true, script: 'aws secretsmanager get-secret-value --secret-id "${JOB_NAME}" | jq --raw-output .SecretString | jq -r ."CICD_GIT_BRANCH"').trim()
    CICD_CREDENTIALS_ID=sh(returnStdout: true, script: 'aws secretsmanager get-secret-value --secret-id "${JOB_NAME}" | jq --raw-output .SecretString | jq -r ."CICD_CREDENTIALS_ID"').trim()
    APP_GIT_REPO=sh(returnStdout: true, script: 'aws secretsmanager get-secret-value --secret-id "${JOB_NAME}" | jq --raw-output .SecretString | jq -r ."APP_GIT_REPO"').trim()
    APP_GIT_BRANCH=sh(returnStdout: true, script: 'aws secretsmanager get-secret-value --secret-id "${JOB_NAME}" | jq --raw-output .SecretString | jq -r ."APP_GIT_BRANCH"').trim()
    APP_CREDENTIALS_ID=sh(returnStdout: true, script: 'aws secretsmanager get-secret-value --secret-id "${JOB_NAME}" | jq --raw-output .SecretString | jq -r ."APP_CREDENTIALS_ID"').trim()
    BUILD_IMAGE_NAME=sh(returnStdout: true, script: 'aws secretsmanager get-secret-value --secret-id "${JOB_NAME}" | jq --raw-output .SecretString | jq -r ."BUILD_IMAGE_NAME"').trim()
    REPO_URL=sh(returnStdout: true, script: 'aws secretsmanager get-secret-value --secret-id "${JOB_NAME}" | jq --raw-output .SecretString | jq -r ."REPO_URL"').trim()
    REPO_LOGIN=sh(returnStdout: true, script: 'aws secretsmanager get-secret-value --secret-id "${JOB_NAME}" | jq --raw-output .SecretString | jq -r ."REPO_LOGIN"').trim()
    DEPLOYMENT_NAME=sh(returnStdout: true, script: 'aws secretsmanager get-secret-value --secret-id "${JOB_NAME}" | jq --raw-output .SecretString | jq -r ."DEPLOYMENT_NAME"').trim()
    REGION=sh(returnStdout: true, script: 'aws secretsmanager get-secret-value --secret-id "${JOB_NAME}" | jq --raw-output .SecretString | jq -r ."REGION"').trim()
    NAMESPACE=sh(returnStdout: true, script: 'aws secretsmanager get-secret-value --secret-id "${JOB_NAME}" | jq --raw-output .SecretString | jq -r ."NAMESPACE"').trim()
    CONTAINER_NAME=sh(returnStdout: true, script: 'aws secretsmanager get-secret-value --secret-id "${JOB_NAME}" | jq --raw-output .SecretString | jq -r ."CONTAINER_NAME"').trim()
    FROM_EMAIL=sh(returnStdout: true, script: 'aws secretsmanager get-secret-value --secret-id "${JOB_NAME}" | jq --raw-output .SecretString | jq -r ."FROM_EMAIL"').trim()
    TO_EMAIL=sh(returnStdout: true, script: 'aws secretsmanager get-secret-value --secret-id "${JOB_NAME}" | jq --raw-output .SecretString | jq -r ."TO_EMAIL"').trim()

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
                buildImage("${env.BUILD_IMAGE_NAME}", "${env.DEPLOYMENT_NAME}", "${env.NAMESPACE}")
            }
        }

        stage("Push Image") {
        steps {
            echo "Login into ECR Repo..."
            
            pushImage("${env.REGION}", "${env.REPO_LOGIN}", "${env.REPO_URL}", "${env.BUILD_IMAGE_NAME}", "${env.DEPLOYMENT_NAME}", "${env.NAMESPACE}")
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

        stage("Success Notification"){
        steps{
            script{
                notificationEmail("${env.FROM_EMAIL}", "${env.TO_EMAIL}", "${env.REGION}", "${STAGE_NAME}", "${JOB_BASE_NAME}", "success")
            }
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
