#!/bin/bash

# Script to run Android integration tests for the Compose Multiplatform template
# 
# This script:
# - Builds the Android app
# - Runs unit tests
# - Runs instrumentation tests
# - Generates test reports
#
# Requirements addressed:
# - 2.5: Automated testing of Android app functionality
# - 3.5: Verification of shared module integration with Android

set -e

echo "🚀 Running Android Integration Tests for Compose Multiplatform Template"
echo "=================================================================="

# Check if Android SDK is available
if [ -z "$ANDROID_HOME" ]; then
    echo "❌ ANDROID_HOME is not set. Please set up Android SDK."
    exit 1
fi

# Navigate to project root
cd "$(dirname "$0")/.."

echo "📦 Building Android app..."
./gradlew :composeApp:assembleDebug

echo "🧪 Running unit tests..."
./gradlew :composeApp:testDebugUnitTest

echo "🧪 Running shared module tests..."
./gradlew :shared:testDebugUnitTest

echo "📱 Running instrumentation tests..."
# Note: This requires a connected device or emulator
if adb devices | grep -q "device$"; then
    echo "📱 Device detected, running instrumentation tests..."
    ./gradlew :composeApp:connectedDebugAndroidTest
else
    echo "⚠️  No device detected. Skipping instrumentation tests."
    echo "   To run instrumentation tests, connect a device or start an emulator."
fi

echo "📊 Generating test reports..."
./gradlew :composeApp:testDebugUnitTest --continue
./gradlew :shared:testDebugUnitTest --continue

echo "✅ Android integration tests completed!"
echo ""
echo "📊 Test reports available at:"
echo "   - Android app unit tests: composeApp/build/reports/tests/testDebugUnitTest/index.html"
echo "   - Shared module tests: shared/build/reports/tests/testDebugUnitTest/index.html"
if adb devices | grep -q "device$"; then
    echo "   - Instrumentation tests: composeApp/build/reports/androidTests/connected/index.html"
fi
echo ""
echo "🎉 All tests completed successfully!"