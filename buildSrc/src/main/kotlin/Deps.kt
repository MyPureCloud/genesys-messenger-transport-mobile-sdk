object Deps {
    private const val assertkVersion = "0.25"
    private const val coroutinesVersion = "1.6.0-native-mt"
    private const val junitVersion = "4.13.2"
    private const val kotlinxSerializationJsonVersion = "1.6.3"
    private const val ktorVersion = "2.3.8"
    private const val mockWebServerVersion = "4.9.0"
    private const val mockkVersion = "1.13.3"
    private const val okhttpVersion = "4.10.0"
    private const val klockVersion = "2.4.13"
    const val kotlinVersion = "1.9.22"
    const val composeVersion = "1.5.10"
    const val fragmentVersion = "1.3.5"
    const val activityComposeVersion = "1.8.2"
    const val lifecycleViewModelComposeVersion = "1.0.0-alpha05"
    const val agp = "8.2.0"
    const val buildKonfig = "0.15.1"
    const val nexusPublish = "1.3.0"


    object Libs {
        const val junit = "junit:junit:$junitVersion"
        const val mockk = "io.mockk:mockk:$mockkVersion"
        const val klock = "com.soywiz.korlibs.klock:klock:$klockVersion"

        object Assertk {
            const val common = "com.willowtreeapps.assertk:assertk:$assertkVersion"
            const val jvm = "com.willowtreeapps.assertk:assertk-jvm:$assertkVersion"
        }

        object OkHttp {
            const val client = "com.squareup.okhttp3:okhttp:$okhttpVersion"
            const val loggingInterceptor = "com.squareup.okhttp3:logging-interceptor:$okhttpVersion"
            const val mockWebServer = "com.squareup.okhttp3:mockwebserver:$mockWebServerVersion"
        }

        object Kotlinx {
            const val serializationJson =
                "org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationJsonVersion"
            const val coroutinesCore =
                "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion"
            const val coroutinesAndroid =
                "org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion"
            const val coroutinesTest =
                "org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion"
        }

        object Ktor {
            const val core = "io.ktor:ktor-client-core:$ktorVersion"
            const val serialization = "io.ktor:ktor-client-serialization:$ktorVersion"
            const val json = "io.ktor:ktor-client-json:$ktorVersion"
            const val logging = "io.ktor:ktor-client-logging:$ktorVersion"
            const val loggingJvm = "io.ktor:ktor-client-logging-jvm:$ktorVersion"
            const val android = "io.ktor:ktor-client-android:$ktorVersion"
            const val ios = "io.ktor:ktor-client-ios:$ktorVersion"
            const val mock = "io.ktor:ktor-client-mock:$ktorVersion"
            const val contentNegotiation = "io.ktor:ktor-client-content-negotiation:$ktorVersion"
            const val kotlinxSerialization = "io.ktor:ktor-serialization-kotlinx-json:$ktorVersion"
        }

        object AndroidX {
            const val coreKtx = "androidx.core:core-ktx:1.3.2"
            const val appcompat = "androidx.appcompat:appcompat:1.2.0"
            const val material = "com.google.android.material:material:1.3.0"
            const val lifecycleRuntimeKtx = "androidx.lifecycle:lifecycle-runtime-ktx:2.3.0"
            const val fragmentKtx = "androidx.fragment:fragment-ktx:$fragmentVersion"
            const val composeUi = "androidx.compose.ui:ui:$composeVersion"
            const val composeMaterial = "androidx.compose.material:material:$composeVersion"
            const val composeUiTooling = "androidx.compose.ui:ui-tooling:$composeVersion"
            const val composeFoundation = "androidx.compose.foundation:foundation:$composeVersion"
            const val lifecycleViewModelCompose = "androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleViewModelComposeVersion"
            const val activityCompose = "androidx.activity:activity-compose:$activityComposeVersion"
            const val browser = "androidx.browser:browser:1.4.0"
        }
    }

    object Android {
        const val compileSdk = 34
        const val minSdk = 21
        const val targetSdk = 33
        const val jvmTarget = "1.8"
    }
}
