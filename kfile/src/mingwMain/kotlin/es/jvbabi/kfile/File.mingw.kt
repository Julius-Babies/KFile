package es.jvbabi.kfile

import kotlinx.cinterop.*
import platform.windows.*

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

internal actual fun platformIsPathRoot(path: String): Boolean {
    return regex.matchEntire(path.lowercase()) != null
}

@OptIn(ExperimentalForeignApi::class)
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

@OptIn(ExperimentalForeignApi::class)
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

@OptIn(ExperimentalForeignApi::class)
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

@OptIn(ExperimentalForeignApi::class)
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

@OptIn(ExperimentalForeignApi::class)
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

@OptIn(ExperimentalForeignApi::class)
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