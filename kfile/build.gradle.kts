plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.symbol.processing)
    alias(libs.plugins.kotest)
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

    sourceSets {
        commonTest.dependencies {
            implementation(libs.kotest.framework)
            implementation(libs.kotest.assertions)
        }
    }
}

tasks.withType<Test>().configureEach {
    logger.lifecycle("UP-TO-DATE check for $name is disabled, forcing it to run.")
    outputs.upToDateWhen { false }
}