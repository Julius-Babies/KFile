package es.jvbabi.kfile

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.BooleanVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSNumber

internal actual fun platformIsPathAbsolute(path: String): Boolean = path.startsWith("/")
internal actual fun platformGetWorkingDirectory(): String = NSFileManager.defaultManager.currentDirectoryPath
internal actual fun platformFileExists(path: String): Boolean = NSFileManager.defaultManager.fileExistsAtPath(path)

@OptIn(ExperimentalForeignApi::class)
internal actual fun platformFileIsDirectory(path: String): Boolean {
    memScoped {
        val boolVar = alloc<BooleanVar>()
        NSFileManager.defaultManager.fileExistsAtPath(path, boolVar.ptr)
        return boolVar.value
    }
}

internal actual fun platformIsPathRoot(path: String): Boolean = path == "/"
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal actual fun platformGetFileSize(path: String): Long {
    val fileManager = NSFileManager.defaultManager
    memScoped {
        val errPtr = alloc<ObjCObjectVar<NSError?>>()
        val attrs = fileManager.attributesOfItemAtPath(path, error = errPtr.ptr)

        val err = errPtr.value
        if (err != null) {
            throw Exception("Failed to get attributes of file $path: ${err.localizedDescription}")
        }

        val sizeNumber = attrs?.get(NSFileSize) as? NSNumber
        return sizeNumber?.longLongValue ?: -1
    }
}