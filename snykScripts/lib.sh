#!/usr/bin/env bash

export android_sdk_docker_image="490606849374.dkr.ecr.us-east-1.amazonaws.com/android/android-sdk:11.0"

function ecr_login() {
  eval "$(aws ecr get-login --no-include-email --region us-east-1)"
}

function require_environment_variable() {
    variable_name=$1
    variable_value=$2
    if test -z "$variable_value"
    then
      echo "Missing environment variable: $variable_name"
      exit 1
    fi
}
