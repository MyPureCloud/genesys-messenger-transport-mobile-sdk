# iOS KMM Testbed App

This is an example testbed app that consumes the Messenger IO SDK framework from the Kotlin Multiplatform Mobile project (KMM).

# Dependencies

## Deployment Properties

This project depends on a `deployment.properties` file in the parent KMM project directory. Before building this project that file must exist. There is a template file in that same directory called `deployment.properties.example` that can be used to define the needed properties.

## Cocoapods

This Xcode project is configured to handle the MessengerTransport framework (the framework output of the transport KMM project) dependencies using CocoaPods. Before building the iOS project for the first time performing a `pod install` in the directory with the `Podfile` is necessary. 

