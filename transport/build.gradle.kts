import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

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
}

android {
    compileSdk = Deps.Android.compileSdk
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = Deps.Android.minSdk
        targetSdk = Deps.Android.targetSdk
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
        }
    }
}

val kermitVersion = "0.1.9"
val iosFrameworkName = "MessengerTransport"
version = project.rootProject.version

kotlin {
    android {
        compilations.all {
            kotlinOptions {
                jvmTarget = Deps.Android.jvmTarget
            }
        }
    }

    if (properties.containsKey("android.injected.invoked.from.ide")) {
        // When running from Android Studio, the shared iOS source set needs this workaround for IDE features like code-completion/highlighting with 3rd party iOS libs
        // https://kotlinlang.org/docs/kmm-add-dependencies.html#workaround-to-enable-ide-support-for-the-shared-ios-source-set
        val iosTarget: (String, KotlinNativeTarget.() -> Unit) -> KotlinNativeTarget = when {
            System.getenv("SDK_NAME")?.startsWith("iphoneos") == true -> ::iosArm64
            System.getenv("NATIVE_ARCH")?.startsWith("arm") == true -> ::iosSimulatorArm64
            else -> ::iosX64
        }
        iosTarget("ios") {}
    } else {
        val xcf = XCFramework(iosFrameworkName)
        ios {
            binaries.framework {
                baseName = iosFrameworkName
                xcf.add(this)
            }
        }
    }

    cocoapods {
        summary = "Genesys Cloud Messenger Transport Framework"
        homepage = "https://www.genesys.com"
        ios.deploymentTarget = "11.0"
        podfile = project.file("../iosApp/Podfile")
        framework {
            // The default name for an iOS framework is `<project name>.framework`. To set a custom name, use the `baseName` option. This will also set the module name.
            baseName = iosFrameworkName
            // To specify a custom Objective-C prefix/name for the Kotlin framework, use the `-module-name` compiler option or matching Gradle DSL statement.
            freeCompilerArgs += listOf("-module-name", "GCM")
            export(Deps.Libs.kermit)
        }
        pod("jetfire", "~> 0.1.5")
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
                implementation(Deps.Libs.logback)
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
    }
}

tasks {
    create<Jar>("kotlinSourcesJar") {
        archiveClassifier.set("sources")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from("./src/androidMain", "./src/commonMain")
    }

    create<Jar>("fakeJavadocJar") {
        archiveClassifier.set("javadoc")
        from("./deployment")
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifact(File("build/outputs/aar/transport-release.aar"))
            artifact(tasks["kotlinSourcesJar"])
            artifact(tasks["fakeJavadocJar"])
            groupId = rootProject.group as String?
            artifactId = "messenger-transport-mobile-sdk"
            version = version

            pom {
                name.set("Genesys Cloud Mobile Messenger Transport SDK")
                description.set("This library provides methods for connecting to Genesys Cloud Messenger chat APIs and WebSockets from Android native applications.")
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

                withXml {
                    asNode().appendNode("dependencies").let { dependenciesNode ->
                        listOf(
                            "androidMainImplementation",
                            "androidMainApi",
                            "commonMainImplementation",
                            "commonMainApi"
                        ).forEach {
                            for (dependency in configurations[it].dependencies) {
                                if (dependency.name != "unspecified") {
                                    dependenciesNode.appendNode("dependency").let { node ->
                                        node.appendNode("groupId", dependency.group)
                                        node.appendNode("artifactId", dependency.name)
                                        node.appendNode("version", dependency.version)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

signing {
    // Signing configuration is setup in the ~/.gradle/gradle.properties file on the Jenkins machine
    isRequired = true
    sign(tasks["kotlinSourcesJar"])
    sign(tasks["fakeJavadocJar"])
    sign(publishing.publications["maven"])
}

apply(from = "${rootDir}/jacoco.gradle.kts")