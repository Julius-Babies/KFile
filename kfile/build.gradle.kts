plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.symbol.processing)
    alias(libs.plugins.kotest)

    alias(libs.plugins.maven.publish)
}

group = "io.github.julius-babies"
version = System.getenv("VERSION")?.ifBlank { null } ?: "unspecified"

repositories {
    mavenCentral()
}


kotlin {
    macosArm64()
    macosX64()
    mingwX64()
    linuxX64()
    linuxArm64()
    jvm()

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonTest.dependencies {
            implementation(libs.kotest.framework)
            implementation(libs.kotest.assertions)
        }

        jvmMain.dependencies {
            implementation(libs.apache.commons.lang)
        }
    }
}

tasks.withType<Test>().configureEach {
    logger.lifecycle("UP-TO-DATE check for $name is disabled, forcing it to run.")
    outputs.upToDateWhen { false }
}

tasks.withType<PublishToMavenRepository> {
    dependsOn(tasks.withType<Test>())
}

mavenPublishing {
    publishToMavenCentral()
    if (!gradle.startParameter.taskNames.any { it.contains("publishToMavenLocal") }) {
        signAllPublications()
    }
    coordinates(project.group.toString(), project.name, project.version.toString())

    pom {
        name = "kfile"
        description = "A Kotlin/Native + JVM Library for file operations"
        url = "https://github.com/Julius-Babies/KFile"

        developers {
            developer {
                id = "julius-vincent-babies"
                name = "Julius Vincent Babies"
                email = "julvin.babies@gmail.com"
                url = "https://github.com/Julius-Babies"
            }
        }

        scm {
            url = "https://github.com/Julius-Babies/KFile"
        }

        licenses {
            license {
                name = "The MIT License (MIT)"
                url = "https://opensource.org/license/MIT"
            }
        }
    }
}