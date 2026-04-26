pipeline {
    agent any

    tools {
        jdk 'Java 17'
    }

    options {
        timeout(time: 15, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    environment {
        PROJECT_NAME = 'scala-minigames'
        ARTIFACTORY_URL = "https://artifactory.scala.gg/artifactory"
        ARTIFACTORY_RELEASE = "gradle-release"
        ARTIFACTORY_RELEASE_LOCAL = "gradle-release-local"
        DISCORD_URL = credentials('discord-webhook-url')
        GRADLE_OPTS = "-Dorg.gradle.jvmargs=--enable-native-access=ALL-UNNAMED"
    }

    stages {
        stage('Announce') {
            steps {
                discordSend(
                    webhookURL: env.DISCORD_URL,
                    title: "Build Started: ${env.PROJECT_NAME} #${env.BUILD_NUMBER}",
                    description: "The build has started for ${env.PROJECT_NAME}.",
                    result: 'SUCCESS'
                )
            }
        }

        stage('Prepare') {
            steps {
                sh 'chmod +x gradlew'
            }
        }

        stage('Build') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'scala-artifactory-creds', passwordVariable: 'PASS', usernameVariable: 'USER')]) {
                    sh "./gradlew build --build-cache --no-daemon -Partifactory_user=$USER -Partifactory_password=$PASS -Partifactory_contextUrl=$ARTIFACTORY_URL -Partifactory_release=$ARTIFACTORY_RELEASE -Partifactory_release_local=$ARTIFACTORY_RELEASE_LOCAL"
                }
            }
        }

        stage('Archive') {
            steps {
                archiveArtifacts artifacts: 'build/libs/*.jar', fingerprint: true
            }
        }

        stage('Publish') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'scala-artifactory-creds', passwordVariable: 'PASS', usernameVariable: 'USER')]) {
                    sh "./gradlew artifactoryPublish --no-daemon -Partifactory_user=$USER -Partifactory_password=$PASS -Partifactory_contextUrl=$ARTIFACTORY_URL -Partifactory_release=$ARTIFACTORY_RELEASE -Partifactory_release_local=$ARTIFACTORY_RELEASE_LOCAL"
                }
            }
        }
    }

    post {
        success {
            discordSend(
                webhookURL: env.DISCORD_URL,
                title: "✅ Build Success: ${env.PROJECT_NAME} #${env.BUILD_NUMBER}",
                description: "The build for ${env.PROJECT_NAME} completed successfully.",
                result: 'SUCCESS'
            )
        }
        failure {
            discordSend(
                webhookURL: env.DISCORD_URL,
                title: "⛔️ Build Failure: ${env.PROJECT_NAME} #${env.BUILD_NUMBER}",
                description: "The build for ${env.PROJECT_NAME} failed.",
                result: 'FAILURE'
            )
        }
        always {
            cleanWs()
        }
    }
}
