def isDevelopBranch() { env.BRANCH_NAME == 'develop' }
def isMainBranch() { env.BRANCH_NAME == 'main' }
def isReleaseBranch() { env.BRANCH_NAME.startsWith('release/') }
def isFeatureBranch() { env.BRANCH_NAME.startsWith('feature/') }

def oktaproperties = ''
def googleservices = ''

void setBuildStatus(String message, String state) {
  step([
      $class: "GitHubCommitStatusSetter",
      reposSource: [$class: "ManuallyEnteredRepositorySource", url: "https://github.com/MyPureCloud/genesys-messenger-transport-mobile-sdk"],
      contextSource: [$class: "ManuallyEnteredCommitContextSource", context: "ci/jenkins/build-status"],
      errorHandlers: [[$class: "ChangingBuildStatusErrorHandler", result: "UNSTABLE"]],
      statusResultSource: [ $class: "ConditionalStatusResultSource", results: [[$class: "AnyBuildResult", message: message, state: state]] ]
  ])
}

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
    }
    stages {
        stage("Prepare") {
            steps {
                sh 'printenv | sort'
                setBuildStatus("Preparing", "PENDING")
            }
        }

        stage('Get secrets from secretstash') {
            agent {
                node {
                    label 'dev_mesos_v2'
                    customWorkspace "jenkins-mtsdk-${currentBuild.number}"
                }
            }
            steps {
                script {
                    def pipelineLibrary = library('pipeline-library').com.genesys.jenkins
                    def testing = pipelineLibrary.Testing.new()

                    oktaproperties = testing.getSecretStashSecret(
                        'dev',
                        'us-east-1',
                        'transportsdk',
                        'okta.properties',
                        env.WORKSPACE
                    )
                    echo "okta.properties fetched successfully."

                    googleservices = testing.getSecretStashSecret(
                        'dev',
                        'us-east-1',
                        'transportsdk',
                        'google-services-transport',
                        env.WORKSPACE
                    )
                    echo "google-services.json fetched successfully."
                }
            }
        }

        stage("Continue build") {
                    steps {
                        script {
                            writeFile file: "${env.WORKSPACE}/okta.properties", text: oktaproperties
                            writeFile file: "${env.WORKSPACE}/androidComposePrototype/google-services.json", text: googleservices
                            def props = readProperties file: "${env.WORKSPACE}/okta.properties"
                            env.OKTA_DOMAIN = props['OKTA_DOMAIN']
                            env.CLIENT_ID = props['CLIENT_ID']
                            env.SIGN_IN_REDIRECT_URI = props['SIGN_IN_REDIRECT_URI']
                            env.SIGN_OUT_REDIRECT_URI = props['SIGN_OUT_REDIRECT_URI']
                            env.OKTA_STATE = props['OKTA_STATE']
                            env.CODE_CHALLENGE = props['CODE_CHALLENGE']
                            env.CODE_CHALLENGE_METHOD = props['CODE_CHALLENGE_METHOD']
                            env.CODE_VERIFIER = props['CODE_VERIFIER']
                        }
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
