object Deps {
    private const val assertkVersion = "0.25"
    private const val coroutinesVersion = "1.6.0-native-mt"
    private const val junitVersion = "4.13.2"
    private const val kermitVersion = "1.1.2"
    private const val kotlinxSerializationJsonVersion = "1.3.2"
    private const val ktorVersion = "2.2.2"
    private const val mockWebServerVersion = "4.9.0"
    private const val mockkVersion = "1.13.3"
    private const val okhttpVersion = "4.10.0"
    private const val klockVersion = "2.4.13"

    object Libs {
        const val junit = "junit:junit:$junitVersion"
        const val mockk = "io.mockk:mockk:$mockkVersion"
        const val kermit = "co.touchlab:kermit:$kermitVersion"
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
    }

    object Android {
        const val compileSdk = 30
        const val minSdk = 21
        const val targetSdk = 30
        const val jvmTarget = "1.8"
    }
}
