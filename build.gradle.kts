buildscript {

    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.kotlin.gradle.plugin)
        classpath(libs.android.gradle.plugin)
        classpath(libs.buildkonfig.gradle.plugin)
        classpath(libs.google.services.gradle)
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.nexus.publish)
}

// Version can be overridden via -PversionParam
val buildVersion = project.findProperty("versionParam")?.toString()
    ?: file("VERSION.txt").readText().trim()
version = buildVersion
group = "cloud.genesys"

val publishTarget = project.findProperty("publishTarget")?.toString() ?: "jfrog"

if (publishTarget == "mavenCentral") {
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
}
