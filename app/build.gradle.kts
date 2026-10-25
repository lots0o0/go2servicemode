@file:Suppress("UnstableApiUsage")

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties

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

/**
 * Release signing material. Never committed: read from keystore.properties in
 * the repository root (git-ignored) or from environment variables, so CI can
 * inject the same key through secrets without a file in the tree.
 *
 * Losing this key means the app can never be updated on an existing install:
 * Android identifies an application by its signing certificate.
 */
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun signingValue(property: String, env: String): String? =
    (keystoreProps.getProperty(property) ?: System.getenv(env))?.takeIf { it.isNotBlank() }

val releaseStorePath = signingValue("storeFile", "GO2SM_KEYSTORE")
val hasReleaseSigning = releaseStorePath != null

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

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = rootProject.file(releaseStorePath!!)
                storePassword = signingValue("storePassword", "GO2SM_STORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "GO2SM_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "GO2SM_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Without local signing material a contributor can still build a
            // release APK; it is then debug-signed and must not be published.
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                logger.warn(
                    "go2servicemode: no release signing material found " +
                        "(keystore.properties or GO2SM_* env vars); release APK will be debug-signed."
                )
                signingConfigs.getByName("debug")
            }
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
