package es.jvbabi.kfile

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.windows.GetCurrentDirectoryW
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