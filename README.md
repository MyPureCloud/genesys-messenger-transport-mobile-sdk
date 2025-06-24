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

#### Installation with SPM
We are excited to announce that Transport SDK now supports distribution via Swift Package Manager for iOS!

**Note:** Swift Package Manager support is available starting with Messenger Transport SDK **2.8.5**.

To install Messenger Transport SDK in your app with CocoaPods, follow this guidance.

1. Open your iOS project in Xcode
If this is your first time using SPM we recommend reading the [official documentation](https://developer.apple.com/documentation/xcode/adding-package-dependencies-to-your-app).

2. Add GenesysCloud dependency to your project
* Go to File > Add package Dependencies....
* Copy the package URL and paste it to the Enter Package URL field: https://github.com/MyPureCloud/mm-genesyscloudmessengertransport-spm.git
* After you see the package show up under name mm-genesyscloudmessengertransport-spm you can change Dependency Rule to match your preferences
* Click Add Package
* Xcode will automatically resolve the MessengerTransport package and its dependencies.

The MessengerTransport module may now be imported and used in your project.

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

## Questions and Support

For help, troubleshooting, or to share feedback about the Messenger Transport SDK, please post in the [Genesys Cloud Community Forum](https://community.genesys.com/home).
It's the best place to get quick responses from Genesys experts and the developer community.

## Known Issues

| Issue | Description | Details | Resolution | Affected Version |
| --- | --- | --- | --- | --- |
| completionHandler does not return response to `fetchDeploymentConfig()` and `nextPage()` | This issue is caused by a bug in an older version of `Ktor`, the Transport SDK's underlying network engine.| If the Logging plugin is installed, the Ktor client can deadlock or block after BODY START is printed to the console. Although the response is still returned to the calling function, any future requests will block. | On Ktor `2.x` this issue was resolved. It is recommended to use Transport `2.3.0` or higher, that implements `Ktor 2.x`. In case a lower version is required, make sure to disable Ktor Login Plugin by setting the `logging=false` in the Configuration object on iOS. | iOS Transport `2.2.0` and lower |
