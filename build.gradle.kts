// Single-module project.
// AGP 9 has built-in Kotlin support, so only the Compose compiler plugin is
// applied explicitly here.
plugins {
    id("com.android.application") version "9.4.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" apply false
}
