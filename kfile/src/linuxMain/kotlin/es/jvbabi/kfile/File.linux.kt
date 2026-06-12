@file:OptIn(ExperimentalForeignApi::class)

package es.jvbabi.kfile

import kotlinx.cinterop.*
import kotlinx.io.*
import platform.posix.*

internal actual fun platformIsPathAbsolute(path: String): Boolean = path.startsWith('/')

internal actual fun platformGetWorkingDirectory(): String = memScoped {
    val size = 4096
    val buf = allocArray<ByteVar>(size)
    val result = getcwd(buf, size.convert())
    if (result == null) {
        perror("getcwd failed")
        ""
    } else {
        buf.toKString()
    }
}

internal actual fun platformFileExists(path: String): Boolean {
    return access(path, F_OK) == 0
}

internal actual fun platformFileIsDirectory(path: String): Boolean = memScoped {
    val statBuf = alloc<stat>()
    if (stat(path, statBuf.ptr) != 0) {
        throw Exception("Failed to get attributes of file $path")
    }
    return (statBuf.st_mode and S_IFMT.toUInt()) == S_IFDIR.toUInt()
}

internal actual fun platformIsPathRoot(path: String): Boolean = path == "/"
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

internal actual fun platformGetUserHome(): String {
    memScoped {
        getenv("HOME")?.toKString()?.let { return it }
        throw IllegalStateException("HOME environment variable not set")
    }
}

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

internal actual fun platformReadBytes(path: String): ByteArray {
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

        return buffer
    } finally {
        fclose(file)
    }
}

internal actual fun platformWriteBytes(path: String, bytes: ByteArray) {
    val file = fopen(path, "wb") ?: throw IllegalArgumentException("Cannot open file: $path")
    try {
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

internal actual fun platformForEachLine(path: String, action: (String) -> Unit) {
    val file = fopen(path, "rb") ?: throw IllegalArgumentException("File not found: $path")
    try {
        val chunkSize = 8192
        val buffer = ByteArray(chunkSize)
        val lineBuilder = StringBuilder()

        memScoped {
            val cBuffer = buffer.refTo(0)
            while (true) {
                val bytesRead = fread(cBuffer, 1.convert(), chunkSize.convert(), file).toInt()
                if (bytesRead == 0 && lineBuilder.isEmpty()) break
                if (bytesRead == 0) {
                    if (lineBuilder.isNotEmpty()) {
                        action(lineBuilder.toString())
                        lineBuilder.clear()
                    }
                    break
                }

                for (i in 0 until bytesRead) {
                    val c = buffer[i].toInt().toChar()
                    if (c == '\n') {
                        action(lineBuilder.toString())
                        lineBuilder.clear()
                    } else if (c != '\r') {
                        lineBuilder.append(c)
                    }
                }
            }
        }

        if (lineBuilder.isNotEmpty()) {
            action(lineBuilder.toString())
        }
    } finally {
        fclose(file)
    }
}

private class FileRawSource(path: String) : RawSource {
    private val file = fopen(path, "rb") ?: throw IllegalArgumentException("File not found: $path")

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        val count = byteCount.toInt().coerceAtMost(8192)
        val buffer = ByteArray(count)
        memScoped {
            val bytesRead = fread(buffer.refTo(0), 1u, count.toULong(), file).toInt()
            if (bytesRead <= 0) return -1L
            sink.write(buffer, 0, bytesRead)
            return bytesRead.toLong()
        }
    }

    override fun close() {
        fclose(file)
    }
}

private class FileRawSink(path: String) : RawSink {
    private val file = fopen(path, "wb") ?: throw IllegalArgumentException("Cannot open file: $path")

    override fun write(source: Buffer, byteCount: Long) {
        val count = byteCount.toInt()
        val buffer = ByteArray(count)
        var offset = 0
        while (offset < count) {
            val bytesRead = source.readAtMostTo(buffer, offset, count)
            if (bytesRead == -1) throw EOFException("Unexpected end of source in write")
            offset += bytesRead
        }
        memScoped {
            fwrite(buffer.refTo(0), 1u, count.toULong(), file)
        }
    }

    override fun flush() {
        fflush(file)
    }

    override fun close() {
        fclose(file)
    }
}

internal actual fun platformFileSource(path: String): Source = FileRawSource(path).buffered()

internal actual fun platformFileSink(path: String): Sink = FileRawSink(path).buffered()

internal actual fun platformCopyFile(source: String, destination: String) {
    val bufferSize = 8192
    val buffer = ByteArray(bufferSize)

    val src = fopen(source, "rb")
        ?: throw IllegalStateException("Quelldatei konnte nicht geöffnet werden: $source (errno=$errno)")

    try {
        val dst = fopen(destination, "wb")
            ?: throw IllegalStateException("Zieldatei konnte nicht erstellt werden: $destination (errno=$errno)")

        try {
            buffer.usePinned { pinned ->
                while (true) {
                    val bytesRead = fread(pinned.addressOf(0), 1u, bufferSize.toULong(), src).toInt()
                    if (bytesRead == 0) break
                    val bytesWritten = fwrite(pinned.addressOf(0), 1u, bytesRead.toULong(), dst).toInt()
                    if (bytesWritten != bytesRead) {
                        throw IllegalStateException("Schreibfehler: $bytesWritten von $bytesRead Bytes geschrieben")
                    }
                }
            }
        } finally {
            fclose(dst)
        }
    } finally {
        fclose(src)
    }
}