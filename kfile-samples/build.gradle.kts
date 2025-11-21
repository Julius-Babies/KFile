plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

group = "es.jvbabi"
version = "unspecified"

repositories {
    mavenCentral()
}

kotlin {
    val targets = listOf(
        linuxArm64(),
        linuxX64(),
        macosArm64(),
        macosX64(),
    )
    applyDefaultHierarchyTemplate()

    targets.forEach { target ->
        target.apply {
            binaries {
                executable {
                    entryPoint = "main"
                }
            }
        }
    }

    sourceSets {
        nativeMain.dependencies {
            implementation(project(":kfile"))
        }
    }
}