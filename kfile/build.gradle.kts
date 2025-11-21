plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

group = "es.jvbabi"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}


kotlin {
    macosArm64()
    macosX64()
    mingwX64()
    linuxX64()
    linuxArm64()

    applyDefaultHierarchyTemplate()
}