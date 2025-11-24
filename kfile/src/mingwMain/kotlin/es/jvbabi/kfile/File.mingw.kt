package es.jvbabi.kfile

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import platform.windows.CreateDirectoryA
import platform.windows.DeleteFileA
import platform.windows.ERROR_ALREADY_EXISTS
import platform.windows.FILE_ATTRIBUTE_DIRECTORY
import platform.windows.FindClose
import platform.windows.FindFirstFileA
import platform.windows.FindNextFileA
import platform.windows.GET_FILEEX_INFO_LEVELS
import platform.windows.GetCurrentDirectoryW
import platform.windows.GetFileAttributesA
import platform.windows.GetFileAttributesExW
import platform.windows.GetFileAttributesW
import platform.windows.GetLastError
import platform.windows.INVALID_FILE_ATTRIBUTES
import platform.windows.INVALID_HANDLE_VALUE
import platform.windows.RemoveDirectoryA
import platform.windows.WCHARVar
import platform.windows.WIN32_FILE_ATTRIBUTE_DATA
import platform.windows.WIN32_FIND_DATAA

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

internal actual fun mkdir(path: String, recursive: Boolean) {
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