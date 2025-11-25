package es.jvbabi.kfile

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.BooleanVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.homeDirectoryForCurrentUser
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.temporaryDirectory
import platform.Foundation.writeToFile

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

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal actual fun platformDelete(path: String, recursive: Boolean) {
    val fileManager = NSFileManager.defaultManager
    val url = NSURL.fileURLWithPath(path)
    val error = nativeHeap.alloc<ObjCObjectVar<NSError?>>()
    val success = fileManager.removeItemAtURL(url, error.ptr)
    if (!success) {
        throw IllegalStateException("Failed to delete '$path': ${error.value?.localizedDescription}")
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal actual fun platformMkdir(path: String, recursive: Boolean) {
    val fm = NSFileManager.defaultManager
    val url = NSURL.fileURLWithPath(path)
    val error = nativeHeap.alloc<ObjCObjectVar<NSError?>>()
    val success = fm.createDirectoryAtURL(url, withIntermediateDirectories = recursive, attributes = null, error = error.ptr)
    if (!success) {
        throw IllegalStateException("Failed to create directory '$path': ${error.value?.localizedDescription}")
    }
}

internal actual fun platformGetUserHome(): String {
    return NSFileManager.defaultManager.homeDirectoryForCurrentUser.path
        ?: throw Exception("Failed to get user home directory")
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal actual fun platformReadFileToString(path: String): String {
    memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()
        val data = NSString.stringWithContentsOfFile(path, encoding = NSUTF8StringEncoding, error = error.ptr)
            ?: throw Exception("Failed to read file $path: ${error.value?.localizedDescription}")
        return data
    }
}

internal actual fun platformGetTempDirectory(): String {
    return NSFileManager.defaultManager.temporaryDirectory.path
        ?: throw Exception("Failed to get temporary directory")
}

@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
internal actual fun platformWriteTextToFile(path: String, text: String) {
    val nsString = NSString.create(string = text)
    val success = nsString.writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null)
    if (!success) {
        throw IllegalStateException("Failed to write file: $path")
    }
}