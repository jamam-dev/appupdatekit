/**
 * AppUpdateKit
 * build.gradle.kts (appupdatekit module)
 * Purpose: Library build configuration for AppUpdateKit
 */
plugins {
    alias(libs.plugins.android.library)
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

