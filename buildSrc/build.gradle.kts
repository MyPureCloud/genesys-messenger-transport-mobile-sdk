plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("TransportValidationPlugin") {
            id = "transportValidationPlugin"
            implementationClass = "com.genesys.cloud.messenger.TransportValidationPlugin"
        }
    }
}
