@file:Suppress("UnstableApiUsage")

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Build identity. Two builds can behave differently while looking identical, so
// the About page shows the build date; the APK's own SHA-256 is computed at
// runtime from the installed file.
val buildStamp: String =
    SimpleDateFormat("yyMMdd-HHmm", Locale.US).format(Date())
val buildDate: String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())

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
        versionName = "0.0.1"
        buildConfigField("String", "BUILD_STAMP", "\"$buildStamp\"")
        buildConfigField("String", "BUILD_DATE", "\"$buildDate\"")
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
