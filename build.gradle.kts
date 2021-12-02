buildscript {
    val compose_version by extra("1.0.3")

    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.30")
        classpath("com.android.tools.build:gradle:7.1.0-beta03")
        classpath("org.jmailen.gradle:kotlinter-gradle:3.4.0")
        classpath("com.vanniktech:gradle-android-junit-jacoco-plugin:0.16.0")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        jcenter()
    }
}


plugins {
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
}

// CocoaPods requires the podspec to have a version.
val envVersion = System.getenv("BUILD_VERSION") ?: "1.1"
val buildNumber = System.getenv("BUILD_NUMBER") ?: "0"
val snapshot = System.getenv("SNAPSHOT_BUILD") ?: ""
version = "${envVersion}.${buildNumber}${snapshot}"
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