pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
    
}
rootProject.name = "messenger-mobile-sdk"

include(":transport")
include(":shared")

include(":androidComposePrototype")
include(":composeApp")
include(":iosComposeApp")
