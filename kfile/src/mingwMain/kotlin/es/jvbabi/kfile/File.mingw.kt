@file:OptIn(ExperimentalForeignApi::class)

package es.jvbabi.kfile

import kotlinx.cinterop.*
import kotlinx.io.Buffer
import kotlinx.io.EOFException
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.buffered
import platform.posix.fclose
import platform.posix.fflush
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fwrite
import platform.windows.*

/**
 * Check if the path starts with a Drive Letter, followed by a colon.
 */
internal actual fun platformIsPathAbsolute(path: String): Boolean {
    return regex.matchEntire(path.lowercase())?.groupValues?.get(1) != null
}

private val regex = Regex("""[a-z]:""")

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

internal actual fun platformFileExists(path: String): Boolean = memScoped {
    val attrs = GetFileAttributesW(path)
    attrs != INVALID_FILE_ATTRIBUTES && (attrs and FILE_ATTRIBUTE_DIRECTORY.toUInt()) == 0u
}

internal actual fun platformFileIsDirectory(path: String): Boolean {
    memScoped {
        val attrs = GetFileAttributesW(path)
        if (attrs == INVALID_FILE_ATTRIBUTES) {
            throw Exception("Failed to get attributes of file $path")
        }
        return (attrs and FILE_ATTRIBUTE_DIRECTORY.toUInt()) != 0u
    }
}

internal actual fun platformIsPathRoot(path: String): Boolean {
    return regex.matchEntire(path.lowercase()) != null
}

internal actual fun platformGetFileSize(path: String): Long {
    memScoped {
        val data = alloc<WIN32_FILE_ATTRIBUTE_DATA>()

        val ok = GetFileAttributesExW(
            path,
            GET_FILEEX_INFO_LEVELS.GetFileExInfoStandard,
            data.ptr
        )

        if (ok == 0) {
            return -1
        }

        val high = data.nFileSizeHigh.toLong() shl 32
        val low = data.nFileSizeLow.toLong() and 0xFFFFFFFF
        return high or low
    }
}

internal actual fun platformDelete(path: String, recursive: Boolean) {
    fun deleteRecursively(p: String) {
        val findData = nativeHeap.alloc<WIN32_FIND_DATAA>()
        val hFind = FindFirstFileA("$p\\*", findData.ptr)
        if (hFind == INVALID_HANDLE_VALUE) return
        try {
            do {
                val name = findData.cFileName.toKString()
                if (name != "." && name != "..") {
                    val fullPath = "$p\\$name"
                    if ((findData.dwFileAttributes and FILE_ATTRIBUTE_DIRECTORY.toUInt()) != 0u) {
                        deleteRecursively(fullPath)
                    } else {
                        DeleteFileA(fullPath)
                    }
                }
            } while (FindNextFileA(hFind, findData.ptr) != 0)
        } finally {
            FindClose(hFind)
        }
        RemoveDirectoryA(p)
    }

    if (recursive) deleteRecursively(path)
    else {
        val attrs = GetFileAttributesA(path)
        if (attrs == INVALID_FILE_ATTRIBUTES) return
        if ((attrs and FILE_ATTRIBUTE_DIRECTORY.toUInt()) != 0u) RemoveDirectoryA(path)
        else DeleteFileA(path)
    }
}

internal actual fun platformMkdir(path: String, recursive: Boolean) {
    fun createRecursively(p: String) {
        val segments = p.split("\\").filter { it.isNotEmpty() }
        var currentPath = if (p.startsWith("\\")) "\\" else ""
        for (segment in segments) {
            currentPath += if (currentPath.endsWith("\\")) segment else "\\$segment"
            val success: Int = CreateDirectoryA(path, null)
            if (success == 0) {
                val err = GetLastError()
                if (err != ERROR_ALREADY_EXISTS.toUInt()) {
                    throw IllegalStateException("Failed to create directory '$path', error $err")
                }
            }
        }
    }

    if (recursive) createRecursively(path)
    else {
        val success: Int = CreateDirectoryA(path, null)
        if (success == 0) {
            val err = GetLastError()
            if (err != ERROR_ALREADY_EXISTS.toUInt()) {
                throw IllegalStateException("Failed to create directory '$path', error $err")
            }
        }
    }
}

internal actual fun platformGetUserHome(): String {
    memScoped {
        val outPtr = alloc<CPointerVar<WCHARVar>>()
        val result = SHGetKnownFolderPath(null, CSIDL_PROFILE.toUInt(), NULL, outPtr.ptr)
        if (result != 0) {
            throw Exception("Failed to get user home directory")
        }
        return outPtr.value!!.toKString()
    }
}

internal actual fun platformReadFileToString(path: String): String {
    memScoped {
        val handle = CreateFileW(
            path,
            GENERIC_READ,
            FILE_SHARE_READ.toUInt(),
            null,
            OPEN_EXISTING.toUInt(),
            FILE_ATTRIBUTE_NORMAL.toUInt(),
            null
        )
        if (handle == INVALID_HANDLE_VALUE) {
            throw IllegalArgumentException("Cannot open file: $path")
        }

        try {
            val fileSizeHigh = alloc<DWORDVar>()
            val fileSizeLow = GetFileSize(handle, fileSizeHigh.ptr)
            if (fileSizeLow == INVALID_FILE_SIZE && GetLastError() != 0u) {
                throw IllegalStateException("Cannot get file size for: $path")
            }

            val size = (fileSizeHigh.value.toULong() shl 32) or fileSizeLow.toULong()
            if (size > Int.MAX_VALUE.toULong()) {
                throw IllegalStateException("File too large to read into memory")
            }

            val buffer = ByteArray(size.toInt())
            val bytesRead = alloc<DWORDVar>()

            val success = buffer.usePinned { pinned ->
                ReadFile(
                    handle,
                    pinned.addressOf(0),
                    size.toUInt(),
                    bytesRead.ptr,
                    null
                )
            }
            if (success != 0 || bytesRead.value.toULong() != size) {
                throw IllegalStateException("Failed to read file: $path")
            }

            return buffer.decodeToString()
        } finally {
            CloseHandle(handle)
        }
    }
}

internal actual fun platformGetTempDirectory(): String {
    val bufferLength = MAX_PATH + 1
    return memScoped {
        val buffer = allocArray<ByteVar>(bufferLength)
        val result = GetTempPathA(bufferLength.toUInt(), buffer)
        if (result == 0u) {
            val err = GetLastError()
            throw RuntimeException("GetTempPath2A failed, error: $err")
        }
        val str = buffer.toKString()
        str
    }
}

internal actual fun platformWriteTextToFile(path: String, text: String) {
    memScoped {
        val handle = CreateFileW(
            path,
            GENERIC_WRITE.toUInt(),
            0u,
            null,
            CREATE_ALWAYS.toUInt(),
            FILE_ATTRIBUTE_NORMAL.toUInt(),
            null
        )
        if (handle == INVALID_HANDLE_VALUE) {
            throw IllegalStateException("Cannot open file: $path")
        }

        try {
            val bytes = text.encodeToByteArray()
            val bytesWritten = alloc<DWORDVar>()
            val success: Boolean = bytes.usePinned { pinned ->
                WriteFile(
                    handle,
                    pinned.addressOf(0),  // CPointer<out CPointed>
                    bytes.size.toUInt(),
                    bytesWritten.ptr,
                    null
                ) == 0
            }

            if (!success || bytesWritten.value.toInt() != bytes.size) {
                throw IllegalStateException("Failed to write file: $path")
            }
        } finally {
            CloseHandle(handle)
        }
    }
}

internal actual fun platformGetFileNamesInDirectory(path: String): List<String> {
    val files = mutableListOf<String>()
    memScoped {
        val findData = alloc<WIN32_FIND_DATAW>()
        val handle = FindFirstFileW("$path/*", findData.ptr)

        if (handle == INVALID_HANDLE_VALUE) {
            throw Exception("Failed to find files in directory $path")
        }

        do {
            val name = findData.cFileName.toKString()
            if (name != "." && name != "..") {
                files += name
            }
        } while (FindNextFileW(handle, findData.ptr) == 0)

        FindClose(handle)
    }

    return files
}

internal actual fun platformReadBytes(path: String): ByteArray = memScoped {
    val handle = CreateFileW(
        path,
        GENERIC_READ,
        FILE_SHARE_READ.toUInt(),
        null,
        OPEN_EXISTING.toUInt(),
        FILE_ATTRIBUTE_NORMAL.toUInt(),
        null
    )
    if (handle == INVALID_HANDLE_VALUE) {
        throw IllegalArgumentException("Cannot open file: $path")
    }

    try {
        val fileSizeHigh = alloc<DWORDVar>()
        val fileSizeLow = GetFileSize(handle, fileSizeHigh.ptr)
        if (fileSizeLow == INVALID_FILE_SIZE && GetLastError() != 0u) {
            throw IllegalStateException("Cannot get file size for: $path")
        }

        val size = (fileSizeHigh.value.toULong() shl 32) or fileSizeLow.toULong()
        if (size > Int.MAX_VALUE.toULong()) {
            throw IllegalStateException("File too large to read into memory")
        }

        val buffer = ByteArray(size.toInt())
        val bytesRead = alloc<DWORDVar>()

        val success = buffer.usePinned { pinned ->
            ReadFile(
                handle,
                pinned.addressOf(0),
                size.toUInt(),
                bytesRead.ptr,
                null
            )
        }
        if (success == 0 || bytesRead.value.toULong() != size) {
            throw IllegalStateException("Failed to read file: $path")
        }

        buffer
    } finally {
        CloseHandle(handle)
    }
}

internal actual fun platformWriteBytes(path: String, bytes: ByteArray) = memScoped {
    val handle = CreateFileW(
        path,
        GENERIC_WRITE.toUInt(),
        0u,
        null,
        CREATE_ALWAYS.toUInt(),
        FILE_ATTRIBUTE_NORMAL.toUInt(),
        null
    )
    if (handle == INVALID_HANDLE_VALUE) {
        throw IllegalStateException("Cannot open file: $path")
    }

    try {
        val bytesWritten = alloc<DWORDVar>()
        val success = bytes.usePinned { pinned ->
            WriteFile(
                handle,
                pinned.addressOf(0),
                bytes.size.toUInt(),
                bytesWritten.ptr,
                null
            )
        }

        if (success == 0 || bytesWritten.value.toInt() != bytes.size) {
            throw IllegalStateException("Failed to write file: $path")
        }
    } finally {
        CloseHandle(handle)
    }
}

internal actual fun platformForEachLine(path: String, action: (String) -> Unit) = memScoped {
    val handle = CreateFileW(
        path,
        GENERIC_READ,
        FILE_SHARE_READ.toUInt(),
        null,
        OPEN_EXISTING.toUInt(),
        FILE_ATTRIBUTE_NORMAL.toUInt(),
        null
    )
    if (handle == INVALID_HANDLE_VALUE) {
        throw IllegalArgumentException("Cannot open file: $path")
    }

    try {
        val chunkSize = 8192
        val buffer = ByteArray(chunkSize)
        val bytesRead = alloc<DWORDVar>()
        val lineBuilder = StringBuilder()

        while (true) {
            val success = buffer.usePinned { pinned ->
                ReadFile(
                    handle,
                    pinned.addressOf(0),
                    chunkSize.toUInt(),
                    bytesRead.ptr,
                    null
                )
            }

            val count = bytesRead.value.toInt()
            if (success == 0 || count == 0) {
                if (lineBuilder.isNotEmpty()) {
                    action(lineBuilder.toString())
                    lineBuilder.clear()
                }
                break
            }

            for (i in 0 until count) {
                val c = buffer[i].toInt().toChar()
                if (c == '\n') {
                    action(lineBuilder.toString())
                    lineBuilder.clear()
                } else if (c != '\r') {
                    lineBuilder.append(c)
                }
            }
        }

        if (lineBuilder.isNotEmpty()) {
            action(lineBuilder.toString())
        }
    } finally {
        CloseHandle(handle)
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
    val success = CopyFileW(source, destination, 0)
    if (success == 0) {
        val error = GetLastError()
        throw Exception("Failed to copy file: $error")
    }
}