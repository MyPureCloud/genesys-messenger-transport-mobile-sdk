object Deps {
    private const val assertkVersion = "0.28.1"
    private const val coroutinesVersion = "1.10.2"
    private const val kotlinxDatetimeVersion = "0.7.1"
    private const val ktorVersion = "3.4.0"
    private const val mockkVersion = "1.14.9"
    private const val okhttpVersion = "4.12.0"
    const val kotlinVersion = "2.3.10"
    const val agp = "8.6.1"
    const val buildKonfig = "0.17.1"
    const val nexusPublish = "1.3.0"
    const val googleServices = "4.4.4"

    object Libs {
        const val mockk = "io.mockk:mockk:$mockkVersion"

        object Assertk {
            const val common = "com.willowtreeapps.assertk:assertk:$assertkVersion"
            const val jvm = "com.willowtreeapps.assertk:assertk-jvm:$assertkVersion"
        }

        object OkHttp {
            const val client = "com.squareup.okhttp3:okhttp:$okhttpVersion"
            const val loggingInterceptor = "com.squareup.okhttp3:logging-interceptor:$okhttpVersion"
            const val mockWebServer = "com.squareup.okhttp3:mockwebserver:$okhttpVersion"
        }

        object Kotlinx {
            const val datetime =
                "org.jetbrains.kotlinx:kotlinx-datetime:$kotlinxDatetimeVersion"
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
        const val compileSdk = 35
        const val minSdk = 21
        const val targetSdk = 35
    }
}
