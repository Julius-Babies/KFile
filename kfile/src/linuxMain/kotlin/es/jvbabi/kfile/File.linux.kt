package es.jvbabi.kfile

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.getcwd
import platform.posix.perror

internal actual fun platformIsPathAbsolute(path: String): Boolean = path.startsWith('/')

@OptIn(ExperimentalForeignApi::class)
internal actual fun platformGetWorkingDirectory(): String = memScoped {
    val size = 4096
    val buf = allocArray<ByteVar>(size)
    val result = getcwd(buf, size.convert())
    if (result == null) {
        perror("getcwd failed")
        ""  // oder Exception je nach Design
    } else {
        buf.toKString()
    }
}