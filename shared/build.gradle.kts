plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.compose")
    kotlin("native.cocoapods")
}

@OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = Deps.Android.jvmTarget
            }
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = false // Use dynamic framework to avoid duplicate symbols
        }
    }
    
    cocoapods {
        summary = "Shared Compose Multiplatform module for iOS and Android"
        homepage = "https://github.com/genesys/messenger-transport-mobile-sdk"
        version = "1.0.0"
        ios.deploymentTarget = "14.1"
        podfile = project.file("../iosComposeApp/Podfile")
        
        framework {
            baseName = "Shared"
            isStatic = false // Use dynamic framework to avoid duplicate symbols
        }
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                
                // Coroutines
                implementation(Deps.Libs.Kotlinx.coroutinesCore)
                
                // Navigation
                implementation(Deps.Libs.ComposeMultiplatform.navigation)
                
                // ViewModel - using kotlinx coroutines for state management
                // Note: Using coroutines-based state management instead of ViewModel for better multiplatform support
                
                // Transport module dependency - use implementation to avoid duplicate symbols
                implementation(project(":transport"))
            }
        }
        
        val androidMain by getting {
            dependencies {
                implementation(Deps.Libs.AndroidX.coreKtx)
                implementation(Deps.Libs.AndroidX.activityCompose)
                implementation(Deps.Libs.Kotlinx.coroutinesAndroid)
            }
        }
        
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }
        
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(Deps.Libs.Kotlinx.coroutinesTest)
            }
        }
    }
}

android {
    namespace = "com.genesys.cloud.messenger.shared"
    compileSdk = Deps.Android.compileSdk
    
    defaultConfig {
        minSdk = Deps.Android.minSdk
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    
    buildFeatures {
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}