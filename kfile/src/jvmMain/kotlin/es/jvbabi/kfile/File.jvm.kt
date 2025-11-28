package es.jvbabi.kfile

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