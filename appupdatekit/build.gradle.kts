/**
 * AppUpdateKit
 * build.gradle.kts (appupdatekit module)
 * Purpose: Library build configuration for AppUpdateKit
 */
plugins {
    alias(libs.plugins.android.library)
    id("maven-publish")
}

android {
    namespace = "com.appupdatekit"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    // Firebase Remote Config — re-exported so consumers don't need to declare separately
    api(platform(libs.firebase.bom))
    api(libs.firebase.config.ktx)

    // Play In-App Updates — re-exported for consumers
    api(libs.play.app.update.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Lifecycle
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.process)

    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    testImplementation(libs.junit)
}


// ─── Maven Publishing (required for JitPack) ──────────────────────────────────
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId    = project.findProperty("GROUP")?.toString()    ?: "com.github.jamam-dev"
                artifactId = project.findProperty("ARTIFACT_ID")?.toString() ?: "appupdatekit"
                version    = project.findProperty("VERSION_NAME")?.toString() ?: "1.0.0"

                pom {
                    name.set("AppUpdateKit")
                    description.set("Production-grade Android in-app update library controlled via Firebase Remote Config.")
                    url.set("https://github.com/jamam-dev/AppUpdateKit")
                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }
                }
            }
        }
    }
}
