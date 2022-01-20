#!/usr/bin/env bash
# shellcheck source=scripts/lib.sh
source "$(dirname "$0")/lib.sh"

# Scan project

# Environment prep/validation
ecr_login
if test -n "$JENKINS_HOME"; then
  # Jenkins build concerns
  require_environment_variable "JENKINS_HOME" "$JENKINS_HOME"
  require_environment_variable "SNYK_TOKEN", "$SNYK_TOKEN"
else
  echo "Not yet supported outside of Jenkins"
  exit 1
fi

deployment_id="1"
deployment_domain="1"
snyk_org="messenger-mobile-sdk"
snyk_project_name="genesys-messenger-transport-mobile-sdk"
snyk_android_subproject="transport"
snyk_android_configuration="releaseRuntimeClasspath"

if ! docker pull "$android_sdk_docker_image"
then
  exit 1
fi
container=$(docker run -d -it \
  --mount type=bind,source="$(pwd)",target=/home/repo \
  "$android_sdk_docker_image")

# install snyk
docker exec "$container" apt update -y
docker exec "$container" apt upgrade -y
docker exec "$container" apt install -y npm
docker exec "$container" npm install -g snyk snyk-to-html

# the version of Node.js is too old in this image. Use a more recent version
docker exec "$container" npm install -g n
docker exec "$container" n 16.13.2

# configure snyk
if ! docker exec "$container" snyk auth "$SNYK_TOKEN"
then
  echo "snyk failed to authenticate"
  exit 1
fi

function process_snyk_exit_code() {
  exit_code=$1
  case $exit_code in

    0)
      echo "Success, no vulns found"
      ;;

    1)
      echo "Vulns found, but that doesn't fail the job (yet)"
      ;;

    2)
      echo "Undefined failure, try again"
      exit 1
      ;;

    3)
      echo "No supported projects detected"
      exit 1
      ;;

    *)
      echo "Unexpected failure"
      exit 1
      ;;
  esac
}

# Start with Android

echo "--------------------------------------------------"
echo "Starting Android Test"
echo "--------------------------------------------------"

docker exec \
  --env JENKINS_HOME="$JENKINS_HOME" \
  --env DEPLOYMENT_ID="$deployment_id" \
  --env DEPLOYMENT_DOMAIN="$deployment_domain" \
  --env SNYK_TOKEN="$SNYK_TOKEN" \
  -w /home/repo \
  "$container" \
  snyk test \
    --org="$snyk_org" \
    --project-name="$snyk_project_name" \
    --sub-project="$snyk_android_subproject" \
    --configuration-matching="$snyk_android_configuration" \
    --json-file-output=snyk-android-test.json
test_exit_code=$?
process_snyk_exit_code $test_exit_code

# convert test output to html
docker exec -w /home/repo "$container"  \
  snyk-to-html --input snyk-android-test.json --output snyk-android-test.html -a

# report for monitoring
echo "--------------------------------------------------"
echo "Starting Android Monitor"
echo "--------------------------------------------------"
docker exec \
  --env JENKINS_HOME="$JENKINS_HOME" \
  --env DEPLOYMENT_ID="$deployment_id" \
  --env DEPLOYMENT_DOMAIN="$deployment_domain" \
  --env SNYK_TOKEN="$SNYK_TOKEN" \
  -w /home/repo \
  "$container" \
  snyk monitor \
    --org="$snyk_org" \
    --project-name="$snyk_project_name" \
    --sub-project="$snyk_android_subproject" \
    --configuration-matching="$snyk_android_configuration"
monitor_exit_code=$?
process_snyk_exit_code $monitor_exit_code

#Now iOS
echo "--------------------------------------------------"
echo "Starting iOS Test"
echo "--------------------------------------------------"
docker exec \
  --env JENKINS_HOME="$JENKINS_HOME" \
  --env SNYK_TOKEN="$SNYK_TOKEN" \
  -w /home/repo \
  "$container" \
  snyk test \
    --org="$snyk_org" \
    --project-name="$snyk_project_name" \
    --package-manager=cocoapods \
    --file=./iosApp/Podfile.lock \
    --json-file-output=snyk-ios-test.json
test_exit_code=$?
process_snyk_exit_code $test_exit_code

# convert test output to html
docker exec -w /home/repo "$container"  \
  snyk-to-html --input snyk-ios-test.json --output snyk-ios-test.html -a

echo "--------------------------------------------------"
echo "Starting iOS Monitor"
echo "--------------------------------------------------"
docker exec \
  --env JENKINS_HOME="$JENKINS_HOME" \
  --env SNYK_TOKEN="$SNYK_TOKEN" \
  -w /home/repo \
  "$container" \
  snyk monitor \
    --org="$snyk_org" \
    --project-name="$snyk_project_name" \
    --package-manager=cocoapods \
    --file=./iosApp/Podfile.lock
test_exit_code=$?
process_snyk_exit_code $test_exit_code

docker stop "$container"
docker rm "$container"
