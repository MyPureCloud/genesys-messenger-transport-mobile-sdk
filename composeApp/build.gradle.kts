plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.compose")
}

android {
    namespace = "com.genesys.cloud.messenger.composeapp"
    compileSdk = Deps.Android.compileSdk

    defaultConfig {
        applicationId = "com.genesys.cloud.messenger.composeapp"
        minSdk = Deps.Android.minSdk
        targetSdk = Deps.Android.targetSdk
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    
    kotlinOptions {
        jvmTarget = Deps.Android.jvmTarget
    }
    
    buildFeatures {
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":transport"))
    
    implementation(Deps.Libs.AndroidX.coreKtx)
    implementation(Deps.Libs.AndroidX.lifecycleRuntimeKtx)
    implementation(Deps.Libs.AndroidX.activityCompose)
    implementation(Deps.Libs.AndroidX.composeUi)
    implementation(Deps.Libs.AndroidX.composeFoundation)
    implementation(Deps.Libs.AndroidX.composeUiTooling)
    implementation(Deps.Libs.AndroidX.lifecycleViewModelCompose)
    implementation(Deps.Libs.ComposeMultiplatform.material3)
    
    testImplementation(Deps.Libs.junit)
}