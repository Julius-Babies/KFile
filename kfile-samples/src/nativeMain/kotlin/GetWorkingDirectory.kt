import es.jvbabi.kfile.File

fun main() {
    println("Running in ${File.getWorkingDirectory().absolutePath}")
    //val file = File("/Users/../Users/julius/Library/../../julius/..")
    val file = File("./Data/../Data1/Data2/../../")
    val currentSegments = File.getWorkingDirectory().cleanSegments
    file.cleanSegments.forEachIndexed { i, s ->
        println("$s ${currentSegments[i]}")
    }
    println("${file.cleanSegments.size} ${currentSegments.size}")
}