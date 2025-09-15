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
            
            // Specify bundle ID to suppress warning
            freeCompilerArgs += "-Xbinary=bundleId=com.genesys.cloud.messenger.shared"
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
                
                // DateTime
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")
                
                // Navigation
                implementation(Deps.Libs.ComposeMultiplatform.navigation)
                
                // ViewModel - using kotlinx coroutines for state management
                // Note: Using coroutines-based state management instead of ViewModel for better multiplatform support
                
                // Transport module dependency
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
        
        val androidUnitTest by getting {
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
        
        // Performance optimizations
        vectorDrawables.useSupportLibrary = true
        
        // Proguard optimization for release builds
        consumerProguardFiles("consumer-rules.pro")
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    
    buildFeatures {
        compose = true
        buildConfig = false // Disable BuildConfig generation for better performance
        resValues = false   // Disable resource value generation
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    
    // Performance optimizations
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/versions/9/previous-compilation-data.bin"
        }
    }
    
    // Build type optimizations
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}