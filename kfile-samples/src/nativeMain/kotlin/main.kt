import es.jvbabi.kfile.File

fun main() {
    println("Running in ${File.getWorkingDirectory().absolutePath}")
    println("User home: ${File.getUserHomeDirectory().absolutePath}")

    val file = File.getWorkingDirectory().parent!!.resolve("settings.gradle.kts")
    require(file.exists())
    println("File: ${file.size}")
}