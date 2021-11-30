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
    }
    post{
        success{
            setBuildStatus("Build complete.", "SUCCESS")
        }
        failure{
            setBuildStatus("Build complete.", "FAILURE")
        }
        always{
            cleanWs()
        }
    }
}
