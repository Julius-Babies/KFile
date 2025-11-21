package es.jvbabi.kfile

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.windows.FILE_ATTRIBUTE_DIRECTORY
import platform.windows.GetCurrentDirectoryW
import platform.windows.GetFileAttributesW
import platform.windows.INVALID_FILE_ATTRIBUTES
import platform.windows.WCHARVar

/**
 * Check if the path starts with a Drive Letter, followed by a colon.
 */
internal actual fun platformIsPathAbsolute(path: String): Boolean {
    return regex.matchEntire(path.lowercase())?.groupValues?.get(1) != null
}

private val regex = Regex("""[a-z]:""")

@OptIn(ExperimentalForeignApi::class)
internal actual fun platformGetWorkingDirectory(): String {
    memScoped {
        val size = 4096u
        val buffer = allocArray<WCHARVar>(size.toInt())

        val len = GetCurrentDirectoryW(size, buffer)
        if (len == 0u) {
            throw Exception("Failed to get current directory.")
        } else {
            return buffer.toKString()
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun platformFileExists(path: String): Boolean = memScoped {
    val attrs = GetFileAttributesW(path)
    attrs != INVALID_FILE_ATTRIBUTES && (attrs and FILE_ATTRIBUTE_DIRECTORY.toUInt()) == 0u
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun platformFileIsDirectory(path: String): Boolean {
    memScoped {
        val attrs = GetFileAttributesW(path)
        if (attrs == INVALID_FILE_ATTRIBUTES) {
            throw Exception("Failed to get attributes of file $path")
        }
        return (attrs and FILE_ATTRIBUTE_DIRECTORY.toUInt()) != 0u
    }
}