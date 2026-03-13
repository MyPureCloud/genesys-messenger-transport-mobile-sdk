pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
    
}
rootProject.name = "messenger-mobile-sdk"

include(":transport")

include(":journey")

include(":androidComposePrototype")
