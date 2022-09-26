# Genesys Cloud Messenger Transport SDK

Genesys Cloud Messenger Transport SDK provides a library of methods for connecting to Genesys Cloud Web Messaging APIs and WebSockets from Android and iOS native applications. 

## Installation

To import and install the Messenger Transport SDK in an app, follow the instructions below for the appropriate platform.

### Install Messenger Transport on Android

Messenger Transport supports versions of Android 21 and up.

Add the following dependency to the `dependencies` section of your app's `build.gradle` file.

```
implementation 'cloud.genesys:messenger-transport-mobile-sdk-android:<version>'
```

### Install Messenger Transport on iOS

Messenger Transport SDK supports versions of iOS 13.0 and up.

#### Installation with CocoaPods

To install Messenger Transport SDK in your app with CocoaPods, follow this guidance.

In your `Podfile`, configure your target to include the `GenesysCloudMessengerTransport` pod dependency.

```
target 'TargetNameInYourXcodeProject' do
  pod 'GenesysCloudMessengerTransport'
end
```

In a Terminal window, navigate to the project directory with your Podfile and Xcode project and run the CocoaPods install command:

```
$ pod install
```

CocoaPods will download and install the GenesysCloudMessengerTransport pod and any necessary dependencies.

The `MessengerTransport` module may now be imported and used in your project.

```
import MessengerTransport
```

### Install Messenger Transport in Kotlin Multiplatform Mobile Project

Add the following dependency to the `dependencies` section of your library's `build.gradle` file.

```{ "title": "build.gradle" }
implementation 'cloud.genesys:messenger-transport-mobile-sdk:<version>'
```

## Documentation

Detailed documentation for Messenger Transport, including how to use the SDK and how to contribute to the project, can be found on the [Wiki](https://github.com/MyPureCloud/genesys-messenger-transport-mobile-sdk/wiki).
