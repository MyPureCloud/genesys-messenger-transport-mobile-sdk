import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import org.jetbrains.kotlin.gradle.plugin.mpp.BitcodeEmbeddingMode

plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
    kotlin("plugin.serialization") version "1.4.31"
    id("com.android.library")
    id("org.jetbrains.dokka") version "1.4.30"
    id("org.jmailen.kotlinter")
    id("maven-publish")
    id("signing")
    id("transportValidationPlugin")
    id("com.codingfeline.buildkonfig")
}

version = project.rootProject.version
group = project.rootProject.group

val iosFrameworkName = "MessengerTransport"
val iosMinimumOSVersion = "13.0"
val iosCocoaPodName = "GenesysCloudMessengerTransport"

buildkonfig {
    // Set the package name where BuildKonfig is being placed. Required.
    packageName = "com.genesys.cloud.messenger.transport.config"
    // Set values which you want to have in common. Required.
    defaultConfigs {
        buildConfigField(STRING, "sdkVersion", version.toString())
    }
}

android {
    compileSdk = Deps.Android.compileSdk
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = Deps.Android.minSdk
        targetSdk = Deps.Android.targetSdk
        consumerProguardFiles("transport-proguard-rules.txt")
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    configurations {
        create("androidTestApi")
        create("androidTestDebugApi")
        create("androidTestReleaseApi")
        create("testApi")
        create("testDebugApi")
        create("testReleaseApi")
    }
    packagingOptions {
        resources {
            excludes += "META-INF/*.kotlin_module"
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE-notice.md"
        }
    }
    namespace = "com.genesys.cloud.messenger"
}

kotlin {
    android {
        compilations.all {
            kotlinOptions {
                jvmTarget = Deps.Android.jvmTarget
            }
        }
        publishLibraryVariants("release", "debug")
        publishLibraryVariantsGroupedByFlavor = true
    }

    val xcf = XCFramework(iosFrameworkName)
    ios {
        binaries.framework {
            embedBitcode = BitcodeEmbeddingMode.DISABLE
            baseName = iosFrameworkName
            xcf.add(this)
        }
    }
    iosSimulatorArm64 {
        binaries.framework {
            embedBitcode = BitcodeEmbeddingMode.DISABLE
            baseName = iosFrameworkName
            xcf.add(this)
        }
    }

    cocoapods {
        summary = "Genesys Cloud Messenger Transport Framework - Development podspec for use with local testbed app."
        homepage = "https://github.com/MyPureCloud/genesys-messenger-transport-mobile-sdk"
        license = "MIT"
        authors = "Genesys Cloud Services, Inc."
        ios.deploymentTarget = iosMinimumOSVersion
        podfile = project.file("../iosApp/Podfile")
        framework {
            // The default name for an iOS framework is `<project name>.framework`. To set a custom name, use the `baseName` option. This will also set the module name.
            baseName = iosFrameworkName
            // To specify a custom Objective-C prefix/name for the Kotlin framework, use the `-module-name` compiler option or matching Gradle DSL statement.
            freeCompilerArgs += listOf("-module-name", "GCM")
            isStatic = false
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation(Deps.Libs.Kotlinx.serializationJson)
                implementation(Deps.Libs.Kotlinx.coroutinesCore)
                implementation(Deps.Libs.Ktor.core)
                implementation(Deps.Libs.Ktor.serialization)
                implementation(Deps.Libs.Ktor.json)
                implementation(Deps.Libs.Ktor.logging)
                implementation(Deps.Libs.Ktor.contentNegotiation)
                implementation(Deps.Libs.Ktor.kotlinxSerialization)
                implementation(Deps.Libs.klock)
                api(Deps.Libs.kermit)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(Deps.Libs.Assertk.common)
                implementation(Deps.Libs.Ktor.mock)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(Deps.Libs.OkHttp.client)
                implementation(Deps.Libs.OkHttp.loggingInterceptor)
                api(Deps.Libs.Ktor.android)
                implementation(Deps.Libs.Ktor.loggingJvm)
                implementation(Deps.Libs.Kotlinx.coroutinesAndroid)
            }
        }
        val androidTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation(Deps.Libs.junit)
                implementation(Deps.Libs.Assertk.jvm)
                implementation(Deps.Libs.OkHttp.mockWebServer)
                implementation(Deps.Libs.mockk)
                implementation(Deps.Libs.Kotlinx.coroutinesTest)
            }
        }
        val iosMain by getting {
            dependencies {
                implementation(Deps.Libs.Ktor.ios)
            }
        }
        val iosTest by getting
        val iosSimulatorArm64Main by getting
        val iosSimulatorArm64Test by getting

        // Set up dependencies between the source sets
        iosSimulatorArm64Main.dependsOn(iosMain)
        iosSimulatorArm64Test.dependsOn(iosTest)
    }
}

tasks {

    create<Jar>("fakeJavadocJar") {
        archiveClassifier.set("javadoc")
        from("./deployment")
    }

    listOf("Debug", "Release").forEach { buildVariant ->
        named("assemble${iosFrameworkName}${buildVariant}XCFramework") {
            doLast {
                listOf("ios-arm64", "ios-arm64_x86_64-simulator").forEach { arch ->
                    val xcframeworkPath =
                        "build/XCFrameworks/${buildVariant.toLowerCase()}/$iosFrameworkName.xcframework/$arch/$iosFrameworkName.framework"
                    val infoPlistPath = "$xcframeworkPath/Info.plist"
                    val propertiesMap = mapOf(
                        "CFBundleShortVersionString" to version,
                        "CFBundleVersion" to version,
                        "MinimumOSVersion" to iosMinimumOSVersion
                    )
                    println("Updating framework metadata at: ${this.project.projectDir}/$infoPlistPath")
                    for ((key, value) in propertiesMap) {
                        println("  $key = $value")
                        exec {
                            commandLine("plutil", "-replace", key, "-string", value, infoPlistPath)
                        }
                    }
                }
            }
        }
    }

    val generatePodspecTaskName = "generate${iosCocoaPodName}Podspec"
    register(generatePodspecTaskName) {
        val podspecFileName = "${iosCocoaPodName}.podspec"
        group = "publishing"
        description = "Generates the $podspecFileName file for publication to CocoaPods."
        doLast {
            val content = file("${podspecFileName}_template").readText()
                .replace(oldValue = "<VERSION>", newValue = version.toString())
                .replace(
                    oldValue = "<SOURCE_HTTP_URL>",
                    newValue = "https://github.com/MyPureCloud/genesys-messenger-transport-mobile-sdk/releases/download/v${version}/MessengerTransport.xcframework.zip"
                )
            file(podspecFileName, PathValidation.NONE).writeText(content)
            println("CocoaPods podspec for Pod $iosCocoaPodName written to: ${this.project.projectDir}/$podspecFileName")
        }
    }

    "lintKotlinCommonMain"(org.jmailen.gradle.kotlinter.tasks.LintTask::class) {
        exclude("**/BuildKonfig.kt")
    }
}

publishing {
    publications {
        withType<MavenPublication> {
            artifact(tasks["fakeJavadocJar"])

            pom {
                name.set("Genesys Cloud Mobile Messenger Transport SDK")
                description.set("This library provides methods for connecting to Genesys Cloud Messenger chat APIs and WebSockets from mobile applications.")
                url.set("https://github.com/MyPureCloud/genesys-messenger-transport-mobile-sdk")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("http://www.opensource.org/licenses/mit-license.php")
                    }
                }

                developers {
                    developer {
                        name.set("Genesys Cloud Mobile Dev")
                        email.set("GenesysCloudMobileDev@genesys.com")
                    }
                }

                scm {
                    url.set("https://github.com/MyPureCloud/genesys-messenger-transport-mobile-sdk.git")
                }
            }
        }
    }
}

afterEvaluate {
    configure<PublishingExtension> {
        publications.all {
            val mavenPublication = this as? MavenPublication
            mavenPublication?.artifactId =
                "messenger-transport-mobile-sdk${"-$name".takeUnless { "kotlinMultiplatform" in name }.orEmpty()}"
        }
    }
}


signing {
    // Signing configuration is setup in the ~/.gradle/gradle.properties file on the Jenkins machine
    isRequired = true

    sign(publishing.publications)
}

apply(from = "${rootDir}/jacoco.gradle.kts")