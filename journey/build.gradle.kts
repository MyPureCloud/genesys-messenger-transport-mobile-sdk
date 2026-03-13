import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version libs.versions.kotlin.get()
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlinter)
    id("com.codingfeline.buildkonfig")
    alias(libs.plugins.kover)
}

version = project.rootProject.version
group = project.rootProject.group

buildkonfig {
    packageName = "com.genesys.cloud.messenger.journey.config"
    defaultConfigs {
        buildConfigField(STRING, "sdkVersion", version.toString())
    }
}

kotlin {
    targets.configureEach {
        compilations.configureEach {
            compilerOptions.configure {
                freeCompilerArgs.add("-Xexpect-actual-classes")
            }
        }
    }
    androidLibrary {
        namespace = "com.genesys.cloud.messenger.journey"
        compileSdk = Deps.Android.compileSdk
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
        withHostTest { }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { target ->
        target.binaries.framework {
            baseName = "MessengerJourney"
            isStatic = false
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation(libs.kotlinx.serialization.json)
                implementation(Deps.Libs.Kotlinx.coroutinesCore)
                implementation(Deps.Libs.Ktor.core)
                implementation(Deps.Libs.Ktor.serialization)
                implementation(Deps.Libs.Ktor.logging)
                implementation(Deps.Libs.Ktor.contentNegotiation)
                implementation(Deps.Libs.Ktor.kotlinxSerialization)
                implementation(Deps.Libs.Kotlinx.datetime)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(Deps.Libs.Assertk.common)
                implementation(Deps.Libs.Ktor.mock)
                implementation(Deps.Libs.Kotlinx.coroutinesTest)
            }
        }
        androidMain {
            dependencies {
                implementation(Deps.Libs.OkHttp.clientJvm)
                implementation(Deps.Libs.OkHttp.loggingInterceptor) {
                    exclude(group = "com.squareup.okhttp3", module = "okhttp")
                }
                implementation(Deps.Libs.Ktor.okhttp) {
                    exclude(group = "com.squareup.okhttp3", module = "okhttp")
                }
                implementation(Deps.Libs.Ktor.loggingJvm)
                implementation(Deps.Libs.Kotlinx.coroutinesAndroid)
            }
        }
        val androidHostTest by getting {
            kotlin.srcDirs("src/androidUnitTest/kotlin")
            resources.srcDirs("src/androidUnitTest/resources")
            dependencies {
                implementation(kotlin("test-junit"))
                implementation(libs.junit)
                implementation(Deps.Libs.Assertk.jvm)
                implementation(Deps.Libs.mockk)
                implementation(Deps.Libs.Kotlinx.coroutinesTest)
                implementation(Deps.Libs.Ktor.mock)
            }
        }
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependencies {
                implementation(Deps.Libs.Ktor.ios)
                implementation(Deps.Libs.Ktor.darwin)
            }
            dependsOn(commonMain.get())
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }
    }
}

afterEvaluate {
    tasks.withType(org.jmailen.gradle.kotlinter.tasks.LintTask::class.java).configureEach {
        exclude("**/BuildKonfig.kt")
    }
}

kover {
    useJacoco()
}
