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
        label "mobile-sdk-dev"
    }
    options{
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
                sh './gradlew :transport:test :transport:koverXmlReportDebug :transport:koverXmlReportRelease '
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
                sh './gradlew :transport:generatePomFileForKotlinMultiplatformPublication'
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
                    if [ -e okta.properties ]; then
                      echo "okta.properties file already exists"
                    else
                      echo "creating okta.properties file based on environment variables"
                      echo "oktaDomain=${OKTA_DOMAIN}" >> okta.properties
                      echo "clientId=${CLIENT_ID}" >> okta.properties
                      echo "signInRedirectUri=${SIGN_IN_REDIRECT_URI}" >> okta.properties
                      echo "signOutRedirectUri=${SIGN_OUT_REDIRECT_URI}" >> okta.properties
                      echo "oktaState=${OKTA_STATE}" >> okta.properties
                      echo "codeChallenge=${CODE_CHALLENGE}" >> okta.properties
                      echo "codeChallengeMethod=${CODE_CHALLENGE_METHOD}" >> okta.properties
                      echo "codeVerifier=${CODE_VERIFIER}" >> okta.properties
                    fi
                    if [ -e iosApp/Okta.plist ]; then
                      echo "Okta.plist file already exists"
                    else
                      echo "creating Okta.plist file based on environment variables"
                      cat > iosApp/Okta.plist <<EOF
                    <?xml version="1.0" encoding="UTF-8"?>
                    <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
                    <plist version="1.0">
                    <dict>
                    	<key>oktaDomain</key>
                    	<string>${OKTA_DOMAIN}</string>
                    	<key>clientId</key>
                    	<string>${CLIENT_ID}</string>
                    	<key>scopes</key>
                    	<string>openid profile offline_access</string>
                    	<key>signInRedirectURI</key>
                    	<string>${SIGN_IN_REDIRECT_URI}</string>
                    	<key>logoutRedirectUri</key>
                    	<string>${SIGN_OUT_REDIRECT_URI}</string>
                    	<key>oktaState</key>
                    	<string>${OKTA_STATE}</string>
                    	<key>codeChallenge</key>
                    	<string>${CODE_CHALLENGE}</string>
                    	<key>codeVerifier</key>
                    	<string>${CODE_VERIFIER}</string>
                    	<key>codeChallengeMethod</key>
                    	<string>${CODE_CHALLENGE_METHOD}</string>
                    </dict>
                    </plist>
                    EOF
                    fi
                    ./gradlew -p "transport" :transport:syncFramework \
                      -Pkotlin.native.cocoapods.platform=iphoneos\
                      -Pkotlin.native.cocoapods.archs="arm64" \
                      -Pkotlin.native.cocoapods.configuration=Debug
                    cd iosApp
                    pod install --verbose
                    xcodebuild clean build -verbose -workspace iosApp.xcworkspace -scheme iosApp -configuration Debug CODE_SIGNING_ALLOWED=NO
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
            archiveArtifacts 'transport/build/reports/tests/testReleaseUnitTest/**/*.html, transport/build/reports/tests/testReleaseUnitTest/**/*.js, transport/build/reports/tests/testReleaseUnitTest/**/*.css'

            junit 'transport/build/test-results/testReleaseUnitTest/*.xml'
            cleanWs()
        }
    }
}
