@file:Suppress("UnstableApiUsage")

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Without a build stamp there is no way to tell which APK is on the device:
// two builds with different payloads look identical on screen. Shown on the
// About page so the version can be verified without adb.
val buildStamp: String =
    SimpleDateFormat("yyMMdd-HHmm", Locale.US).format(Date())

android {
    namespace = "online.usim.go2servicemode"

    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }

    defaultConfig {
        applicationId = "online.usim.go2servicemode"
        minSdk = 31
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
        buildConfigField("String", "BUILD_STAMP", "\"$buildStamp\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Self-signed, open-source friendly. Replace with the project
            // keystore once its details are decided.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    // The engine exec()s the payload through a real on-disk file, so the native
    // library must be extracted rather than page-mapped from the APK.
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

dependencies {
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.foundation:foundation:1.12.0")
    implementation("androidx.compose.ui:ui:1.12.0")
    implementation("androidx.compose.runtime:runtime:1.12.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.12.0")

    // Liquid glass: backdrop + shapes (Capsule, ...) from Kyant0.
    implementation("io.github.kyant0:backdrop:2.0.1")
    implementation("io.github.kyant0:shapes:1.2.1")
}
