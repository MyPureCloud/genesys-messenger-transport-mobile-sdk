def isDevelopBranch() { env.BRANCH_NAME == 'develop' }
def isMainBranch() { env.BRANCH_NAME == 'main' }
def isReleaseBranch() { env.BRANCH_NAME.startsWith('release/') }
def isFeatureBranch() { env.BRANCH_NAME.startsWith('feature/') }

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
    }
    stages{
        stage("Prepare"){
            steps{
                bitbucketStatusNotify(buildState: 'INPROGRESS')
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
        stage("CI Build - transport Module"){
            steps{
                sh './gradlew :transport:assembleDebug'
            }
        }
        stage("CI Build - Android Testbed"){
            steps{
                sh './gradlew :androidComposePrototype:assembleDebug'
            }
        }
        stage("CI Build - iOS Testbed"){
            steps{
                sh '''
                    if [ -z "$HOME" ]; then export HOME=/Users/$(whoami); fi
                    if [ -e deployment.properties ]; then
                      echo "deployment.properties file already exists"
                    else
                      echo "creating deployment.properties file based on environment variables"
                      echo "deploymentId=${DEPLOYMENT_ID}" >> deployment.properties
                      echo "deploymentDomain=${DEPLOYMENT_DOMAIN}" >> deployment.properties
                    fi
                    echo "iosApp will use the following deployment.properties:"
                    cat deployment.properties
                    cd iosApp
                    pod install --verbose
                    xcodebuild clean build -verbose -workspace iosApp.xcworkspace -scheme iosApp -configuration Debug CODE_SIGNING_ALLOWED=NO EXCLUDED_ARCHS=armv7
                '''
            }
        }
        stage("CI Build - iOS XCFramework"){
            steps{
                sh '''
                    ./make-iOS-framework.sh
                    test -e transport/build/xcframework/MessengerTransport.xcframework
                '''
            }
        }
    }
    post{
        success{
            bitbucketStatusNotify(buildState: 'SUCCESSFUL')
        }
        failure{
            bitbucketStatusNotify(buildState: 'FAILED')
            emailext attachLog: false, body: "Build Job: ${BUILD_URL}", recipientProviders: [culprits(), requestor(), brokenBuildSuspects()], subject: "Build failed: ${JOB_NAME}-${BUILD_NUMBER}"
        }
        always{
            cleanWs()
        }
    }
}
