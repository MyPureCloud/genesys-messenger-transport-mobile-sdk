# Mobile Messenger Transport SDK

This library provides methods for connecting to Genesys Cloud Messenger chat APIs and WebSockets from iOS and Android native applications. 

See `com.genesys.cloud.messenger.transport.MessagingClient` for the exposed interface.

### Android Studio Settings

For linting these Android Studio settings can assist.

File -> Manage IDE Settings -> Import Settings -> `./android-studio-settings.zip` (repo root folder)

### Xcode Version Dependency

Currently, Xcode 12 is required to build the iOS dependencies correctly. This KMM Application uses CocoaPods to manage its iOS dependencies. There is currently a known issue with Kotlin versions 1.5, CocoaPods for dependency managment, and Xcode 13. This issue results in the execution of the cinterop tasks for the dependent pods producing a fatal `TaskExecutionException` as discovered here: https://kotlinlang.slack.com/archives/C3SGXARS6/p1635262123074100.

## Region Code Generator

The `Region` enum is code-generated using the `generate-region.main.kts` Kotlin script. It's intended to be executed infrequently, manually, on an as-needed basis, whenever the available regions change, or the requirements of the `Region` class change.

Using a code-generator with minimal automated adaptations for code style should make it easier for the user-facing snippet generator to produce consistent output, ie Region.PROD using the same input data from the service environment, allowing the mobile SDK to take advantage of type safety as much as possible.

Pre-requisites:

- Kotlin ([brew](https://formulae.brew.sh/formula/kotlin), [sdkman](https://sdkman.io/sdks#kotlin))

```shell
./generate-region.main.kts
```
