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
    return File(".").absolutePath
}

internal actual fun platformFileExists(path: String): Boolean {
    return File(path).exists()
}

internal actual fun platformFileIsDirectory(path: String): Boolean {
    return File(path).isDirectory()
}

internal actual fun platformGetFileSize(path: String): Long {
    return File(path).size
}

internal actual fun platformDelete(path: String, recursive: Boolean) {
    File(path).delete(recursive)
}

internal actual fun platformMkdir(path: String, recursive: Boolean) {
    File(path).mkdir(recursive)
}

internal actual fun platformGetUserHome(): String {
    return System.getProperty("user.home")
}