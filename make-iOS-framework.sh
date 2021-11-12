#!/bin/sh

#set -v

TRANSPORT_BUILD_DIR=transport/build
GRADLE_SYNCFRAMEWORK_OUTPUT_DIR=$TRANSPORT_BUILD_DIR/cocoapods/framework
DEVICES_BUILD_DIR=$TRANSPORT_BUILD_DIR/devices
SIMULATOR_BUILD_DIR=$TRANSPORT_BUILD_DIR/simulator
XCFRAMEWORK_BUILD_DIR=$TRANSPORT_BUILD_DIR/xcframework
CONFIGURATION=Release

echo "Making Devices Framework"
xcodebuild clean build \
  -workspace ./iosApp/iosApp.xcworkspace \
  -scheme Pods-iosApp \
  -configuration $CONFIGURATION \
  -sdk iphoneos \
  BUILD_LIBRARY_FOR_DISTRIBUTION=YES

echo "Copying Built Framework to $DEVICES_BUILD_DIR"
rm -rf $DEVICES_BUILD_DIR
mkdir $DEVICES_BUILD_DIR
cp -r $GRADLE_SYNCFRAMEWORK_OUTPUT_DIR/MessengerTransport.framework $DEVICES_BUILD_DIR

echo "Making Simulator Framework"
xcodebuild clean build \
  -workspace ./iosApp/iosApp.xcworkspace \
  -scheme Pods-iosApp \
  -configuration $CONFIGURATION \
  -sdk iphonesimulator \
  BUILD_LIBRARY_FOR_DISTRIBUTION=YES

echo "Copying Built Framework to $SIMULATOR_BUILD_DIR"
rm -rf $SIMULATOR_BUILD_DIR
mkdir $SIMULATOR_BUILD_DIR
cp -r $GRADLE_SYNCFRAMEWORK_OUTPUT_DIR/MessengerTransport.framework $SIMULATOR_BUILD_DIR

echo "Making XCFramework"
rm -rf $XCFRAMEWORK_BUILD_DIR
mkdir $XCFRAMEWORK_BUILD_DIR
xcodebuild -create-xcframework \
  -framework $DEVICES_BUILD_DIR/MessengerTransport.framework \
  -framework $SIMULATOR_BUILD_DIR/MessengerTransport.framework \
  -output $XCFRAMEWORK_BUILD_DIR/MessengerTransport.xcframework

