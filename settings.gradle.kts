pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.jetbrains.kotlin.multiplatform") version "2.2.21" apply false // Remember to change the version in libs.version.toml as well
    id("com.google.devtools.ksp") version "2.3.3" apply false
    id("io.kotest") version "6.0.4" apply false
}

rootProject.name = "KFile"
include(":kfile")
include("kfile-samples")