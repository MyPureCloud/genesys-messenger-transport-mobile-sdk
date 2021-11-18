import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
    kotlin("plugin.serialization") version "1.4.31"
    id("com.android.library")
    id("org.jetbrains.dokka") version "1.4.30"
    id("org.jmailen.kotlinter")
    id("maven-publish")
    id("signing")
}


android {
    compileSdk = 30
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = 21
        targetSdk = 30
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
val configuration: String? by project
val sdk: String? by project
val bitcode: String = if ("release".equals(configuration, true)) "bitcode" else "marker"
version = project.rootProject.version

kotlin {
    android {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }

    val iosTarget: (String, KotlinNativeTarget.() -> Unit) -> KotlinNativeTarget = when {
        System.getenv("SDK_NAME")?.startsWith("iphoneos") == true -> ::iosArm64
        System.getenv("NATIVE_ARCH")?.startsWith("arm") == true -> ::iosSimulatorArm64
        else -> ::iosX64
    }

    iosTarget("ios") {}

    cocoapods {
        summary = "Genesys Cloud Messenger Transport Framework"
        homepage = "https://www.genesys.com"
        ios.deploymentTarget = "11.0"
        podfile = project.file("../iosApp/Podfile")
        framework {
            // The default name for an iOS framework is `<project name>.framework`. To set a custom name, use the `baseName` option. This will also set the module name.
            baseName = "MessengerTransport"
            // To specify a custom Objective-C prefix/name for the Kotlin framework, use the `-module-name` compiler option or matching Gradle DSL statement.
            freeCompilerArgs += listOf("-module-name", "GCM")
            export("co.touchlab:kermit:$kermitVersion")
            embedBitcode(bitcode)
        }
        pod("jetfire", "~> 0.1.5")
    }

    sourceSets {
        val ktorVersion = "1.6.0"
        val assertkVersion = "0.23.1"

        commonMain {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.1.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.3-native-mt")
                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation("io.ktor:ktor-client-serialization:$ktorVersion")
                implementation("io.ktor:ktor-client-json:$ktorVersion")
                implementation("io.ktor:ktor-client-logging:$ktorVersion")
                implementation("ch.qos.logback:logback-classic:1.2.3")
                implementation("com.liftric:kvault:1.7.0")
                api("co.touchlab:kermit:$kermitVersion")
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation("com.willowtreeapps.assertk:assertk:$assertkVersion")
                implementation("io.ktor:ktor-client-mock:$ktorVersion")
            }
        }
        val androidMain by getting {
            dependencies {
                val okhttpVersion = "4.9.1"
                implementation("com.squareup.okhttp3:okhttp:$okhttpVersion")
                implementation("com.squareup.okhttp3:logging-interceptor:$okhttpVersion")
                api("io.ktor:ktor-client-android:$ktorVersion")
                implementation("io.ktor:ktor-client-logging-jvm:$ktorVersion")
            }
        }
        val androidTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation("junit:junit:4.13.2")
                implementation("com.willowtreeapps.assertk:assertk-jvm:$assertkVersion")
                implementation("com.squareup.okhttp3:mockwebserver:4.9.0")
                implementation("io.mockk:mockk:1.11.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.4.3-native-mt")
            }
        }
        val iosMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-ios:$ktorVersion")
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
                        listOf("androidMainImplementation", "commonMainImplementation" , "commonMainApi").forEach {
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