package es.jvbabi.kfile

import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import org.apache.commons.lang3.SystemUtils

private val driveLetterRegex = Regex("""[a-z]:""")
internal actual fun platformIsPathRoot(path: String): Boolean {
    if (SystemUtils.IS_OS_WINDOWS) {
        if (driveLetterRegex.matchEntire(path) != null) return true
    } else {
        if (path == "/") return true
    }
    return false
}

internal actual fun platformIsPathAbsolute(path: String): Boolean {
    if (SystemUtils.IS_OS_WINDOWS) {
        return driveLetterRegex.matchesAt(path, 0)
    }

    return path.startsWith('/')
}

internal actual fun platformGetWorkingDirectory(): String {
    return java.io.File(".").absolutePath
}

internal actual fun platformFileExists(path: String): Boolean {
    return java.io.File(path).exists()
}

internal actual fun platformFileIsDirectory(path: String): Boolean {
    return java.io.File(path).isDirectory()
}

internal actual fun platformGetFileSize(path: String): Long {
    return java.io.File(path).length()
}

internal actual fun platformDelete(path: String, recursive: Boolean) {
    if (recursive) java.io.File(path).deleteRecursively()
    else java.io.File(path).delete()
}

internal actual fun platformMkdir(path: String, recursive: Boolean) {
    if (recursive) java.io.File(path).mkdirs()
    else java.io.File(path).mkdir()
}

internal actual fun platformGetUserHome(): String {
    return System.getProperty("user.home")
}

internal actual fun platformReadFileToString(path: String): String {
    return with(java.io.File(path)) {
        readText()
    }
}

internal actual fun platformGetTempDirectory(): String {
    return System.getProperty("java.io.tmpdir")
}

internal actual fun platformWriteTextToFile(path: String, text: String) {
    with (java.io.File(path)) {
        writeText(text)
    }
}

internal actual fun platformGetFileNamesInDirectory(path: String): List<String> {
    return java.io.File(path).list()?.toList() ?: emptyList()
}

internal actual fun platformReadBytes(path: String): ByteArray {
    return java.io.File(path).readBytes()
}

internal actual fun platformWriteBytes(path: String, bytes: ByteArray) {
    java.io.File(path).writeBytes(bytes)
}

internal actual fun platformForEachLine(path: String, action: (String) -> Unit) {
    java.io.File(path).bufferedReader().use { reader ->
        var line = reader.readLine()
        while (line != null) {
            action(line)
            line = reader.readLine()
        }
    }
}

internal actual fun platformFileSource(path: String): Source {
    return java.io.FileInputStream(path).asSource().buffered()
}

internal actual fun platformFileSink(path: String): Sink {
    return java.io.FileOutputStream(path).asSink().buffered()
}

internal actual fun platformCopyFile(source: String, destination: String) {
    java.io.File(source).copyTo(java.io.File(destination), overwrite = true)
}