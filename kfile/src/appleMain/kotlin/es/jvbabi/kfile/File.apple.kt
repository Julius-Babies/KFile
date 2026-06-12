@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package es.jvbabi.kfile

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.BooleanVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import kotlinx.cinterop.refTo
import kotlinx.cinterop.value
import kotlinx.io.Buffer
import kotlinx.io.EOFException
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.buffered
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
import platform.posix.*

internal actual fun platformIsPathAbsolute(path: String): Boolean = path.startsWith("/")
internal actual fun platformGetWorkingDirectory(): String = NSFileManager.defaultManager.currentDirectoryPath
internal actual fun platformFileExists(path: String): Boolean = NSFileManager.defaultManager.fileExistsAtPath(path)

internal actual fun platformFileIsDirectory(path: String): Boolean {
    memScoped {
        val boolVar = alloc<BooleanVar>()
        NSFileManager.defaultManager.fileExistsAtPath(path, boolVar.ptr)
        return boolVar.value
    }
}

internal actual fun platformIsPathRoot(path: String): Boolean = path == "/"
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

internal actual fun platformDelete(path: String, recursive: Boolean) {
    val fileManager = NSFileManager.defaultManager
    val url = NSURL.fileURLWithPath(path)
    val error = nativeHeap.alloc<ObjCObjectVar<NSError?>>()
    val success = fileManager.removeItemAtURL(url, error.ptr)
    if (!success) {
        throw IllegalStateException("Failed to delete '$path': ${error.value?.localizedDescription}")
    }
}

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

internal actual fun platformWriteTextToFile(path: String, text: String) {
    val nsString = NSString.create(string = text)
    val success = nsString.writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null)
    if (!success) {
        throw IllegalStateException("Failed to write file: $path")
    }
}

internal actual fun platformGetFileNamesInDirectory(path: String): List<String> {
    memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()
        val result = NSFileManager.defaultManager.contentsOfDirectoryAtPath(path = path, error = error.ptr)
            ?: throw Exception("Failed to get file names in directory $path: ${error.value?.localizedDescription}")

        return result.map { it.toString() }
    }
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
    val fileManager = NSFileManager.defaultManager
    val sourceUrl = NSURL.fileURLWithPath(source)
    val destinationUrl = NSURL.fileURLWithPath(destination)

    // NSFileManager cannot overwrite directly, so remove destination first if it exists
    if (fileManager.fileExistsAtPath(destination)) {
        fileManager.removeItemAtURL(destinationUrl, null)
    }

    memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()
        val success = fileManager.copyItemAtURL(
            srcURL = sourceUrl,
            toURL = destinationUrl,
            error = error.ptr
        )

        if (!success) {
            val errorDescription = error.value?.localizedDescription ?: "Unknown error"
            throw IllegalStateException("Datei konnte nicht kopiert werden: $source -> $destination ($errorDescription)")
        }
    }
}