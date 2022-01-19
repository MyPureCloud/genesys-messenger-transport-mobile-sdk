#!/usr/bin/env bash
# shellcheck source=scripts/lib.sh
source "$(dirname "$0")/lib.sh"

# Scan project

# Environment prep/validation
ecr_login
if test -n "$JENKINS_HOME"; then
  # Jenkins build concerns
  require_environment_variable "JENKINS_HOME" "$JENKINS_HOME"
  require_environment_variable "DEPLOYMENT_ID" "$DEPLOYMENT_ID"
  require_environment_variable "DEPLOYMENT_DOMAIN" "$DEPLOYMENT_DOMAIN"
  require_environment_variable "SNYK_TOKEN", "$SNYK_TOKEN"
  require_environment_variable "SNYK_ORG", "$SNYK_ORG"
  require_environment_variable "SNYK_PROJECT_NAME", "$SNYK_PROJECT_NAME"
  require_environment_variable "SNYK_SUB_PROJECT", "$SNYK_SUB_PROJECT"
  require_environment_variable "SNYK_CONFIGURATION", "$SNYK_CONFIGURATION"
else
  echo "Not yet supported outside of Jenkins"
  exit 1
fi

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

docker exec \
  --env JENKINS_HOME="$JENKINS_HOME" \
  --env DEPLOYMENT_ID="$DEPLOYMENT_ID" \
  --env DEPLOYMENT_DOMAIN="$DEPLOYMENT_DOMAIN" \
  --env SNYK_TOKEN="$SNYK_TOKEN" \
  --env SNYK_ORG="$SNYK_ORG" \
  --env SNYK_PROJECT_NAME="$SNYK_PROJECT_NAME" \
  --env SNYK_SUB_PROJECT="$SNYK_SUB_PROJECT" \
  --env SNYK_CONFIGURATION="$SNYK_CONFIGURATION" \
  -w /home/repo \
  "$container" \
  snyk test \
    --org="$SNYK_ORG" \
    --project-name="$SNYK_PROJECT_NAME" \
    --sub-project="$SNYK_SUB_PROJECT" \
    --configuration-matching="$SNYK_CONFIGURATION" \
    --json-file-output=snyk-test.json
test_exit_code=$?
process_snyk_exit_code $test_exit_code

# convert test output to html
docker exec -w /home/repo "$container"  \
  snyk-to-html --input snyk-test.json --output snyk-test.html -a

# report for monitoring
docker exec \
  --env JENKINS_HOME="$JENKINS_HOME" \
  --env DEPLOYMENT_ID="$DEPLOYMENT_ID" \
  --env DEPLOYMENT_DOMAIN="$DEPLOYMENT_DOMAIN" \
  --env SNYK_TOKEN="$SNYK_TOKEN" \
  --env SNYK_ORG="$SNYK_ORG" \
  --env SNYK_PROJECT_NAME="$SNYK_PROJECT_NAME" \
  --env SNYK_SUB_PROJECT="$SNYK_SUB_PROJECT" \
  --env SNYK_CONFIGURATION="$SNYK_CONFIGURATION" \
  -w /home/repo \
  "$container" \
  snyk monitor \
    --org="$SNYK_ORG" \
    --project-name="$SNYK_PROJECT_NAME" \
    --sub-project="$SNYK_SUB_PROJECT" \
    --configuration-matching="$SNYK_CONFIGURATION"
monitor_exit_code=$?
process_snyk_exit_code $monitor_exit_code

docker stop "$container"
docker rm "$container"
