buildscript {
    val compose_version by extra("1.0.3")

    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.30")
        classpath("com.android.tools.build:gradle:7.1.0-alpha05")
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
