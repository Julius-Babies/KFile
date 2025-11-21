package es.jvbabi.kfile

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import platform.posix.F_OK
import platform.posix.S_IFDIR
import platform.posix.S_IFMT
import platform.posix.access
import platform.posix.getcwd
import platform.posix.perror
import platform.posix.stat

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

internal actual fun platformFileExists(path: String): Boolean {
    return access(path, F_OK) == 0
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun platformFileIsDirectory(path: String): Boolean = memScoped {
    val statBuf = alloc<stat>()
    if (stat(path, statBuf.ptr) != 0) {
        throw Exception("Failed to get attributes of file $path")
    }
    return (statBuf.st_mode and S_IFMT.toUInt()) == S_IFDIR.toUInt()
}