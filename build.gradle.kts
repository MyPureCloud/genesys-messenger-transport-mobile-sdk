buildscript {

    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Deps.kotlinVersion}")
        classpath("com.android.tools.build:gradle:${Deps.agp}")
        classpath("org.jmailen.gradle:kotlinter-gradle:${Deps.kotlinterVersion}")
        classpath("com.codingfeline.buildkonfig:buildkonfig-gradle-plugin:${Deps.buildKonfig}")
        classpath("com.google.gms:google-services:${Deps.googleServices}")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    id("io.github.gradle-nexus.publish-plugin") version Deps.nexusPublish
}

// CocoaPods requires the podspec to have a `version`
val buildVersion = "2.9.2-rc2"
val snapshot = System.getenv("SNAPSHOT_BUILD") ?: ""
version = "${buildVersion}${snapshot}"
group = "cloud.genesys"

nexusPublishing {
    repositories {
        sonatype {
            username.set(System.getenv("SONATYPE_USERNAME"))
            password.set(System.getenv("SONATYPE_PASSWORD"))
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/content/repositories/snapshots/"))
        }
    }
}
