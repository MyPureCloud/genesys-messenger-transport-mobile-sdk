# Integration Tests for Compose Multiplatform Template

This document describes the integration tests for the Compose Multiplatform template applications and how to run them.

## Overview

The integration tests verify that:
- Android and iOS apps launch successfully
- Shared UI components work correctly on both platforms
- Navigation flows work end-to-end
- ViewModels maintain consistent behavior across platforms
- Platform-specific integrations work properly

## Test Structure

### Android Tests

#### Unit Tests (`composeApp/src/test/`)
- Basic unit tests for Android-specific code

#### Instrumentation Tests (`composeApp/src/androidTest/`)
- **MainActivityTest.kt**: End-to-end tests for the Android app
- **SharedModuleIntegrationTest.kt**: Tests for shared module integration on Android

### iOS Tests

#### Unit Tests (`iosComposeApp/iosComposeAppTests/`)
- **iosComposeAppTests.swift**: Integration tests for iOS app functionality
- **SharedModuleIntegrationTests.swift**: Tests for shared module integration on iOS

#### UI Tests (`iosComposeApp/iosComposeAppUITests/`)
- **iosComposeAppUITests.swift**: End-to-end UI tests for iOS app

### Shared Tests (`shared/src/commonTest/`)
- **CrossPlatformIntegrationTest.kt**: Tests that run on both platforms
- **AndroidSpecificIntegrationTest.kt**: Android-specific integration tests
- **IOSSpecificIntegrationTest.kt**: iOS-specific integration tests

## Running Tests

### Android Tests

#### Prerequisites
- Android SDK installed and `ANDROID_HOME` set
- Android device connected or emulator running (for instrumentation tests)

#### Running All Android Tests
```bash
./scripts/run-android-integration-tests.sh
```

#### Running Individual Test Suites
```bash
# Unit tests only
./gradlew :composeApp:testDebugUnitTest

# Shared module tests
./gradlew :shared:testDebugUnitTest

# Instrumentation tests (requires device/emulator)
./gradlew :composeApp:connectedDebugAndroidTest
```

### iOS Tests

#### Prerequisites
- Xcode installed
- iOS Simulator or physical device
- CocoaPods installed

#### Running iOS Tests
```bash
# Build shared framework first
./gradlew :shared:podPublishXCFramework

# Open Xcode project
open iosComposeApp/iosComposeApp.xcworkspace

# Run tests in Xcode:
# - Cmd+U to run all tests
# - Or use Test Navigator to run specific test suites
```

#### Running from Command Line
```bash
# Run unit tests
xcodebuild test -workspace iosComposeApp/iosComposeApp.xcworkspace \
  -scheme iosComposeApp -destination 'platform=iOS Simulator,name=iPhone 15'

# Run UI tests
xcodebuild test -workspace iosComposeApp/iosComposeApp.xcworkspace \
  -scheme iosComposeApp -destination 'platform=iOS Simulator,name=iPhone 15' \
  -only-testing:iosComposeAppUITests
```

### Cross-Platform Tests

#### Running Shared Module Tests
```bash
# Run on all platforms
./gradlew :shared:allTests

# Run on specific platforms
./gradlew :shared:testDebugUnitTest  # Android
./gradlew :shared:iosX64Test         # iOS Simulator
./gradlew :shared:iosArm64Test       # iOS Device
```

## Test Coverage

### Functional Areas Covered

#### Navigation
- ✅ App launches successfully
- ✅ Navigation between screens works
- ✅ Back navigation functions correctly
- ✅ Deep linking (if implemented)

#### UI Components
- ✅ Shared components render correctly on both platforms
- ✅ User interactions work as expected
- ✅ Theme system works consistently
- ✅ Accessibility features function properly

#### State Management
- ✅ ViewModels maintain state correctly
- ✅ State updates propagate to UI
- ✅ Error states are handled properly
- ✅ Loading states work correctly

#### Platform Integration
- ✅ Android-specific features work
- ✅ iOS-specific features work
- ✅ Platform abstractions function correctly
- ✅ Memory management works properly

#### Data Flow
- ✅ Message sending and receiving
- ✅ Settings persistence
- ✅ Input validation
- ✅ Error handling

## Test Reports

After running tests, reports are available at:

### Android
- Unit tests: `composeApp/build/reports/tests/testDebugUnitTest/index.html`
- Shared tests: `shared/build/reports/tests/testDebugUnitTest/index.html`
- Instrumentation tests: `composeApp/build/reports/androidTests/connected/index.html`

### iOS
- Test results are shown in Xcode's Test Navigator
- Detailed reports available in Xcode's Report Navigator

## Continuous Integration

### GitHub Actions Example
```yaml
name: Integration Tests

on: [push, pull_request]

jobs:
  android-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Run Android Tests
        run: ./scripts/run-android-integration-tests.sh

  ios-tests:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v3
      - name: Setup Xcode
        uses: maxim-lobanov/setup-xcode@v1
        with:
          xcode-version: latest-stable
      - name: Run iOS Tests
        run: |
          ./gradlew :shared:podPublishXCFramework
          xcodebuild test -workspace iosComposeApp/iosComposeApp.xcworkspace \
            -scheme iosComposeApp -destination 'platform=iOS Simulator,name=iPhone 15'
```

## Troubleshooting

### Common Issues

#### Android
- **Build failures**: Ensure Android SDK is properly installed and `ANDROID_HOME` is set
- **Test failures**: Check that device/emulator is running and accessible via `adb devices`
- **Dependency issues**: Run `./gradlew clean` and rebuild

#### iOS
- **Framework not found**: Run `./gradlew :shared:podPublishXCFramework` to build shared framework
- **Simulator issues**: Ensure iOS Simulator is running and accessible
- **Code signing**: Check that development team is set in Xcode project settings

#### Shared Module
- **Kotlin/Native issues**: Ensure Kotlin Multiplatform plugin is properly configured
- **Dependency conflicts**: Check that all dependencies are compatible across platforms

### Getting Help

If you encounter issues with the integration tests:

1. Check the test output for specific error messages
2. Verify that all prerequisites are installed and configured
3. Try running tests individually to isolate issues
4. Check the project's issue tracker for known problems
5. Consult the Compose Multiplatform documentation

## Contributing

When adding new features to the template:

1. Add corresponding integration tests for both platforms
2. Update this documentation if new test procedures are needed
3. Ensure all tests pass before submitting changes
4. Consider adding tests for edge cases and error conditions

## Requirements Addressed

This integration test suite addresses the following requirements:

- **Requirement 2.5**: End-to-end testing of both Android and iOS app functionality
- **Requirement 3.5**: Testing shared module integration with both platforms

The tests verify that the Compose Multiplatform template works correctly across platforms and that shared components integrate properly with platform-specific code.