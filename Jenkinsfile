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
  ])
}

@Library('pipeline-library@master') _
import com.genesys.jenkins.Testing

pipeline {
    agent {
        label "mobile-sdk-dev"
    }
    options {
        parallelsAlwaysFailFast()
    }
    environment {
        DEPLOYMENT_ID = credentials("messenger-mobile-sdk-deployment-id")
        DEPLOYMENT_DOMAIN = 'inindca.com'
        OKTA_DOMAIN = 'dev-2518047.okta.com'
        CLIENT_ID = credentials("messenger-mobile-sdk-okta-client-id")
        SIGN_IN_REDIRECT_URI = 'com.okta.dev-2518047://oauth2/code'
        SIGN_OUT_REDIRECT_URI = 'com.okta.dev-2518047:/'
        OKTA_STATE = credentials("messenger-mobile-sdk-okta-state")
        CODE_CHALLENGE = 'Cc6VZuBMOjDa9wKlFZLK-9lLPr_Q5e7mJsnVooFnBWA'
        CODE_CHALLENGE_METHOD = 'S256'
        CODE_VERIFIER = 'BtNSLgCNFlZPEOodtxgIp7c-SlnC0RaLilxRaYuZ7DI'
        HOME = """${sh(
            returnStdout: true,
            script: 'if [ -z "$HOME" ]; then echo "/Users/$(whoami)"; else echo "$HOME"; fi'
        ).trim()}"""
    }
    stages {
        stage("Prepare") {
            steps {
                sh 'printenv | sort'
                setBuildStatus("Preparing", "PENDING")
            }
        }

        stage("CI Static Analysis") {
            parallel {
                stage("Lint") {
                    steps {
                        sh './gradlew lintKotlin'
                    }
                }
            }
        }

        stage("Get okta.properties") {
            steps {
                script {
                    def lib = library('pipeline-library').com.genesys.jenkins
                    def oktaproperties = lib.Testing.new().getSecretStashSecret('dev', 'us-east-1', 'transportsdk', 'okta-properties')
                    writeFile file: "${env.WORKSPACE}/okta.properties", text: oktaproperties
                    echo "okta.properties fetched successfully."
                }
            }
        }

        stage("Continue build") {
            steps {
                script {
                    echo "Using okta.properties from previous stage"
                    sh "cat ${env.WORKSPACE}/okta.properties"
                }
            }
        }

        stage("CI Unit Tests") {
            steps {
                sh './gradlew :transport:test :transport:koverXmlReportDebug :transport:koverXmlReportRelease'
                jacoco classPattern: '**/kotlin-classes/debug,**/kotlin-classes/release', inclusionPattern: '**/*.class', sourcePattern: '**/src/*main/kotlin'
            }
        }

        stage("Dependency validation") {
            steps {
                sh './gradlew :transport:checkForAndroidxDependencies'
            }
        }

        stage("CI Build - transport Module debug") {
            steps {
                sh './gradlew :transport:assembleDebug'
            }
        }

        stage("CI Build - Android Testbed") {
            steps {
                sh './gradlew :androidComposePrototype:assembleDebug'
            }
        }

        stage("CI Build - transport Module release") {
            steps {
                sh './gradlew :transport:assembleRelease'
            }
        }

        stage("CI Build - transport POM creation") {
            steps {
                sh './gradlew :transport:generatePomFileForKotlinMultiplatformPublication'
            }
        }

        stage("CI Build - iOS Testbed") {
            steps {
                sh '''
                    if [ -e deployment.properties ]; then
                      echo "deployment.properties file already exists"
                    else
                      echo "creating deployment.properties file based on environment variables"
                      echo "deploymentId=${DEPLOYMENT_ID}" >> deployment.properties
                      echo "deploymentDomain=${DEPLOYMENT_DOMAIN}" >> deployment.properties
                    fi
                    if [ -e okta.properties ]; then
                      echo "okta.properties pulled from secrets in earlier stage"
                    else
                      echo "ERROR: okta.properties is missing"
                      exit 1
                    fi
                    if [ -e iosApp/Okta.plist ]; then
                      echo "Okta.plist file already exists"
                    else
                      echo "Creating Okta.plist from template using sed"
                      sed -e "s|\\${OKTA_DOMAIN}|${OKTA_DOMAIN}|g" \
                          -e "s|\\${CLIENT_ID}|${CLIENT_ID}|g" \
                          -e "s|\\${SIGN_IN_REDIRECT_URI}|${SIGN_IN_REDIRECT_URI}|g" \
                          -e "s|\\${SIGN_OUT_REDIRECT_URI}|${SIGN_OUT_REDIRECT_URI}|g" \
                          -e "s|\\${OKTA_STATE}|${OKTA_STATE}|g" \
                          -e "s|\\${CODE_CHALLENGE}|${CODE_CHALLENGE}|g" \
                          -e "s|\\${CODE_VERIFIER}|${CODE_VERIFIER}|g" \
                          -e "s|\\${CODE_CHALLENGE_METHOD}|${CODE_CHALLENGE_METHOD}|g" \
                          iosApp/Okta.plist.template > iosApp/Okta.plist
                    fi
                    ./gradlew -p "transport" :transport:syncFramework \
                      -Pkotlin.native.cocoapods.platform=iphoneos \
                      -Pkotlin.native.cocoapods.archs="arm64" \
                      -Pkotlin.native.cocoapods.configuration=Debug
                    cd iosApp
                    pod install --verbose
                    xcodebuild clean build -verbose -workspace iosApp.xcworkspace -scheme iosApp -configuration Debug CODE_SIGNING_ALLOWED=NO
                '''
            }
        }

        stage("CI Build - iOS XCFramework") {
            steps {
                sh './gradlew :transport:assembleMessengerTransportReleaseXCFramework'
            }
        }

        stage("CI Build - CocoaPods podspec creation for publication") {
            steps {
                sh './gradlew :transport:generateGenesysCloudMessengerTransportPodspec'
            }
        }
    }

    post {
        success {
            setBuildStatus("Build complete.", "SUCCESS")
        }
        failure {
            setBuildStatus("Build complete.", "FAILURE")
            emailext attachLog: false, body: "Build Job: ${BUILD_URL}", recipientProviders: [culprits(), requestor(), brokenBuildSuspects()], subject: "Build failed: ${JOB_NAME}-${BUILD_NUMBER}"
        }
        always {
            archiveArtifacts 'transport/build/reports/tests/testReleaseUnitTest/**/*.html, transport/build/reports/tests/testReleaseUnitTest/**/*.js, transport/build/reports/tests/testReleaseUnitTest/**/*.css'
            junit 'transport/build/test-results/testReleaseUnitTest/*.xml'
            cleanWs()
        }
    }
}
