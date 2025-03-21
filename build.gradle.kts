buildscript {

    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Deps.kotlinVersion}")
        classpath("com.android.tools.build:gradle:${Deps.agp}")
        classpath("org.jmailen.gradle:kotlinter-gradle:3.4.0")
        classpath("com.codingfeline.buildkonfig:buildkonfig-gradle-plugin:${Deps.buildKonfig}")
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

// CocoaPods requires the podspec to have a `version`.
val buildVersion = "2.9.0.2"
val snapshot = System.getenv("SNAPSHOT_BUILD") ?: ""
version = "${buildVersion}${snapshot}"
group = "cloud.genesys"

nexusPublishing {
    repositories {
        sonatype {
            username.set(System.getenv("SONATYPE_USERNAME"))
            password.set(System.getenv("SONATYPE_PASSWORD"))
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
        }
    }
}
