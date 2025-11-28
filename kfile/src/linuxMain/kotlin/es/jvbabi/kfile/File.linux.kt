package es.jvbabi.kfile

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.refTo
import kotlinx.cinterop.toKString
import platform.posix.EEXIST
import platform.posix.F_OK
import platform.posix.SEEK_END
import platform.posix.S_IFDIR
import platform.posix.S_IFMT
import platform.posix.access
import platform.posix.closedir
import platform.posix.errno
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fseek
import platform.posix.ftell
import platform.posix.fwrite
import platform.posix.getcwd
import platform.posix.getenv
import platform.posix.mkdir
import platform.posix.opendir
import platform.posix.perror
import platform.posix.readdir
import platform.posix.rewind
import platform.posix.rmdir
import platform.posix.stat
import platform.posix.unlink

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

internal actual fun platformIsPathRoot(path: String): Boolean = path == "/"
@OptIn(ExperimentalForeignApi::class)
internal actual fun platformGetFileSize(path: String): Long {
    memScoped {
        val statBuf = alloc<stat>()
        if (stat(path, statBuf.ptr) == 0) {
            return statBuf.st_size
        } else {
            throw Exception("Failed to get attributes of file $path")
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun platformDelete(path: String, recursive: Boolean) {
    fun deleteRecursively(p: String) {
        val statBuf = nativeHeap.alloc<stat>()
        if (stat(p, statBuf.ptr) != 0) return
        if ((statBuf.st_mode and S_IFDIR.toUInt()).toInt() != 0) {
            val dir = opendir(p) ?: return
            try {
                var entry = readdir(dir)
                while (entry != null) {
                    val name = entry.pointed.d_name.toKString()
                    if (name != "." && name != "..") {
                        deleteRecursively("$p/$name")
                    }
                    entry = readdir(dir)
                }
            } finally {
                closedir(dir)
            }
            rmdir(p)
        } else {
            unlink(p)
        }
    }

    if (recursive) deleteRecursively(path)
    else {
        val statBuf = nativeHeap.alloc<stat>()
        if (stat(path, statBuf.ptr) == 0) {
            if ((statBuf.st_mode and S_IFDIR.toUInt()).toInt() != 0) rmdir(path)
            else unlink(path)
        }
    }
}

internal actual fun platformMkdir(path: String, recursive: Boolean) {
    if (!recursive) {
        if (mkdir(path, 0x1FF.toUInt()) != 0) { // 0x1FF = 0777
            perror("mkdir")
        }
        return
    }

    val segments = path.split('/').filter { it.isNotEmpty() }
    var currentPath = if (path.startsWith("/")) "/" else ""
    for (segment in segments) {
        currentPath += if (currentPath.endsWith("/")) segment else "/$segment"
        if (mkdir(currentPath, 0x1FF.toUInt()) != 0) {
            if (errno != EEXIST) perror("mkdir")
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun platformGetUserHome(): String {
    memScoped {
        getenv("HOME")?.toKString()?.let { return it }
        throw IllegalStateException("HOME environment variable not set")
    }
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun platformReadFileToString(path: String): String {
    val file = fopen(path, "rb") ?: throw IllegalArgumentException("File not found: $path")
    try {
        fseek(file, 0, SEEK_END)
        val size = ftell(file)
        rewind(file)

        val buffer = ByteArray(size.toInt())
        memScoped {
            val cBuffer = buffer.refTo(0)
            fread(cBuffer, 1.convert(), size.convert(), file)
        }

        return buffer.decodeToString()
    } finally {
        fclose(file)
    }
}

internal actual fun platformGetTempDirectory(): String {
    return "/tmp"
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun platformWriteTextToFile(path: String, text: String) {
    val file = fopen(path, "wb") ?: throw IllegalArgumentException("Cannot open file: $path")
    try {
        val bytes = text.encodeToByteArray()
        memScoped {
            val written = fwrite(bytes.refTo(0), 1.convert(), bytes.size.convert(), file)
            if (written.toInt() != bytes.size) {
                throw IllegalStateException("Failed to write all bytes to file: $path")
            }
        }
    } finally {
        fclose(file)
    }
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun platformGetFileNamesInDirectory(path: String): List<String> {
    val dir = opendir(path)
    if (dir == null) {
        perror("opendir")
        throw Exception("Failed to open directory $path")
    }

    val files = mutableListOf<String>()
    memScoped {
        while (true) {
            val entry = readdir(dir)?.pointed ?: break
            val name = entry.d_name.toKString()
            if (name != "." && name != "..") {
                files += name
            }
        }
    }

    closedir(dir)
    return files
}