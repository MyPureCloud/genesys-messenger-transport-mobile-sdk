def isDevelopBranch() { env.BRANCH_NAME == 'develop' }
def isMainBranch() { env.BRANCH_NAME == 'main' }
def isReleaseBranch() { env.BRANCH_NAME.startsWith('release/') }
def isFeatureBranch() { env.BRANCH_NAME.startsWith('feature/') }

void setBuildStatus(String message, String state) {
  step([
      $class: "GitHubCommitStatusSetter",
      reposSource: [$class: "ManuallyEnteredRepositorySource", url: "https://github.com/MyPureCloud/genesys-messenger-transport-mobile-sdk"],
      contextSource: [$class: "ManuallyEnteredCommitContextSource", context: "ci/jenkins/build-status"],
      errorHandlers: [[$class: "ChangingBuildStatusErrorHandler", result: "UNSTABLE"]],
      statusResultSource: [ $class: "ConditionalStatusResultSource", results: [[$class: "AnyBuildResult", message: message, state: state]] ]
  ]);
}

pipeline{
    agent{
        label "mmsdk"
    }
    options{
        parallelsAlwaysFailFast()
    }
    environment {
        DEPLOYMENT_ID = credentials("messenger-mobile-sdk-deployment-id")
        DEPLOYMENT_DOMAIN = 'inindca.com'
        HOME = """${sh(
            returnStdout: true,
            script: 'if [ -z "$HOME" ]; then echo "/Users/$(whoami)"; else echo "$HOME"; fi'
        ).trim()}"""
    }
    stages{
        stage("Prepare"){
            steps{
                sh 'printenv | sort'
                setBuildStatus("Preparing", "PENDING")
            }
        }
        stage("CI Static Analysis"){
            parallel{
                stage("Lint"){
                    steps{
                        sh './gradlew lintKotlin'
                    }
                }
            }
        }
        stage("CI Unit Tests"){
            steps{
                sh './gradlew :transport:test :transport:jacocoTestReportDebug :transport:jacocoTestReportRelease'
                jacoco classPattern: '**/kotlin-classes/debug,**/kotlin-classes/release', inclusionPattern: '**/*.class', sourcePattern: '**/src/*main/kotlin'
            }
        }
        stage("Dependency validation") {
            steps {
                sh './gradlew :transport:checkForAndroidxDependencies'
            }
        }
        stage("CI Build - transport Module debug"){
            steps{
                sh './gradlew :transport:assembleDebug'
            }
        }
        stage("CI Build - Android Testbed"){
            steps{
                sh './gradlew :androidComposePrototype:assembleDebug'
            }
        }
        stage("CI Build - transport Module release"){
            steps{
                sh './gradlew :transport:assembleRelease'
            }
        }
        stage("CI Build - transport POM creation"){
            steps{
                sh './gradlew :transport:generatePomFileForMavenPublication'
            }
        }
        stage("CI Build - iOS Testbed"){
            steps{
                sh '''
                    if [ -e deployment.properties ]; then
                      echo "deployment.properties file already exists"
                    else
                      echo "creating deployment.properties file based on environment variables"
                      echo "deploymentId=${DEPLOYMENT_ID}" >> deployment.properties
                      echo "deploymentDomain=${DEPLOYMENT_DOMAIN}" >> deployment.properties
                    fi
                    cd iosApp
                    pod install --verbose
                    xcodebuild clean build -verbose -workspace iosApp.xcworkspace -scheme iosApp -configuration Debug CODE_SIGNING_ALLOWED=NO EXCLUDED_ARCHS=armv7
                '''
            }
        }
        stage("CI Build - iOS XCFramework"){
            steps{
                sh './gradlew :transport:assembleMessengerTransportReleaseXCFramework'
            }
        }
        stage("CI Build - CocoaPods podspec creation for publication"){
            steps{
                sh './gradlew :transport:generateGenesysCloudMessengerTransportPodspec'
            }
        }
    }
    post{
        success{
            setBuildStatus("Build complete.", "SUCCESS")
        }
        failure{
            setBuildStatus("Build complete.", "FAILURE")
            emailext attachLog: false, body: "Build Job: ${BUILD_URL}", recipientProviders: [culprits(), requestor(), brokenBuildSuspects()], subject: "Build failed: ${JOB_NAME}-${BUILD_NUMBER}"
        }
        always{
            cleanWs()
        }
    }
}
