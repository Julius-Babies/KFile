package es.jvbabi.kfile

import kotlinx.io.Sink
import kotlinx.io.Source

class File(path: String) {

    private val normalizedAbsolutePath: String = normalize(path)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is File) return false
        return normalizedAbsolutePath == other.normalizedAbsolutePath
    }

    override fun hashCode(): Int {
        return normalizedAbsolutePath.hashCode()
    }

    companion object {
        fun isPathAbsolute(path: String) = platformIsPathAbsolute(path)
        fun getWorkingDirectory(): File = File(platformGetWorkingDirectory())
        fun getUserHomeDirectory(): File = File(platformGetUserHome())
        fun getTempDirectory(): File = File(platformGetTempDirectory())

        private fun normalize(input: String): String {
            val absolute = if (isPathAbsolute(input)) {
                input
            } else {
                val wd = platformGetWorkingDirectory()
                "$wd/$input"
            }

            val segments = absolute.split('/').filter { it.isNotEmpty() && it != "." }
            val stack = ArrayDeque<String>()

            for (seg in segments) {
                if (seg == "..") {
                    if (stack.isNotEmpty() && !platformIsPathRoot(stack.first())) {
                        stack.removeLast()
                    }
                } else {
                    stack.add(seg)
                }
            }

            return when {
                platformIsPathRoot(segments.firstOrNull() ?: "") ->
                    segments.joinToString("/")
                else ->
                    "/" + stack.joinToString("/")
            }
        }
    }

    val absolutePath: String
        get() = normalizedAbsolutePath

    val name: String
        get() = absolutePath.split('/').last()

    /**
     * Returns the extension of the file name, including the dot.
     * If the file name has no extension, the filename is returned.
     */
    val extension: String
        get() = name.substringAfterLast(".")

    /**
     * Returns the file name without the extension. If the file name has no extension, the filename is returned.
     */
    val nameWithoutExtension: String
        get() = name.substringBeforeLast(".")

    fun exists(): Boolean = platformFileExists(absolutePath)
    fun isDirectory(): Boolean = exists() && platformFileIsDirectory(absolutePath)

    val parent: File?
        get() =
            if (platformIsPathRoot(absolutePath)) null
            else {
                val segments = absolutePath.split('/').filter { it.isNotEmpty() }
                if (segments.size <= 1) null
                else {
                    val parentPath = "/" + segments.dropLast(1).joinToString("/")
                    File(parentPath)
                }
            }

    fun resolve(path: String): File = File("$absolutePath/$path")

    val size: Long
        get() = platformGetFileSize(absolutePath)

    fun delete(recursive: Boolean = false) {
        platformDelete(absolutePath, recursive)
    }

    fun mkdir(recursive: Boolean = false) {
        platformMkdir(absolutePath, recursive)
    }

    /**
     * Reads the entire contents of this file as a String.
     * If the file is very large, this may cause an OutOfMemoryError as all the content is loaded into memory.
     * If you're running this on a directory, you'll receive an exception.
     * @throws FileOperationOnDirectoryException
     */
    fun readText(): String {
        if (isDirectory()) throw FileOperationOnDirectoryException("Cannot read text from a directory")
        return platformReadFileToString(absolutePath)
    }

    fun writeText(text: String) {
        if (isDirectory()) throw FileOperationOnDirectoryException("Cannot write text to a directory")
        platformWriteTextToFile(absolutePath, text)
    }

    /**
     * Reads the entire contents of this file as a ByteArray.
     * If the file is very large, this may cause an OutOfMemoryError as all the content is loaded into memory.
     * @throws FileOperationOnDirectoryException
     */
    fun readBytes(): ByteArray {
        if (isDirectory()) throw FileOperationOnDirectoryException("Cannot read bytes from a directory")
        return platformReadBytes(absolutePath)
    }

    /**
     * Writes the given ByteArray to this file, replacing any existing content.
     * @throws FileOperationOnDirectoryException
     */
    fun writeBytes(bytes: ByteArray) {
        if (isDirectory()) throw FileOperationOnDirectoryException("Cannot write bytes to a directory")
        platformWriteBytes(absolutePath, bytes)
    }

    /**
     * Reads the file line by line and returns all lines as a list.
     * Unlike [readText], this does not split by lines using text splitting internally
     * but rather reads lines one by one.
     */
    fun readLines(): List<String> {
        if (isDirectory()) throw FileOperationOnDirectoryException("Cannot read lines from a directory")
        val lines = mutableListOf<String>()
        platformForEachLine(absolutePath) { lines.add(it) }
        return lines
    }

    /**
     * Reads the file line by line, calling [action] for each line.
     * This is a streaming operation and does not load the entire file into memory.
     * @throws FileOperationOnDirectoryException
     */
    fun forEachLine(action: (String) -> Unit) {
        if (isDirectory()) throw FileOperationOnDirectoryException("Cannot read lines from a directory")
        platformForEachLine(absolutePath, action)
    }

    /**
     * Opens a streaming [Source] for reading bytes from this file.
     * The source is backed by a file handle; close it when done to release resources.
     * @throws FileOperationOnDirectoryException
     */
    fun source(): Source {
        if (isDirectory()) throw FileOperationOnDirectoryException("Cannot open source from a directory")
        return platformFileSource(absolutePath)
    }

    /**
     * Opens a streaming [Sink] for writing bytes to this file.
     * The sink is backed by a file handle; close it when done to flush and release resources.
     * @throws FileOperationOnDirectoryException
     */
    fun sink(): Sink {
        if (isDirectory()) throw FileOperationOnDirectoryException("Cannot open sink to a directory")
        return platformFileSink(absolutePath)
    }

    fun listFiles(): List<File> {
        if (!isDirectory()) throw DirectoryOperationOnFileException("Cannot list files from a file")
        return platformGetFileNamesInDirectory(absolutePath).map { this.resolve(it) }
    }
    
    fun copy(to: File) {
        platformCopyFile(this.normalizedAbsolutePath, to.normalizedAbsolutePath)
    }
}

/**
 * @return true if the path represents the root of the file system, "/" in POSIX or a drive letter in Windows.
 */
internal expect fun platformIsPathRoot(path: String): Boolean
internal expect fun platformIsPathAbsolute(path: String): Boolean
internal expect fun platformGetWorkingDirectory(): String
internal expect fun platformGetUserHome(): String
internal expect fun platformGetTempDirectory(): String
internal expect fun platformFileExists(path: String): Boolean
internal expect fun platformFileIsDirectory(path: String): Boolean
internal expect fun platformGetFileSize(path: String): Long
internal expect fun platformDelete(path: String, recursive: Boolean)
internal expect fun platformMkdir(path: String, recursive: Boolean)
internal expect fun platformReadFileToString(path: String): String
internal expect fun platformWriteTextToFile(path: String, text: String)
internal expect fun platformReadBytes(path: String): ByteArray
internal expect fun platformWriteBytes(path: String, bytes: ByteArray)
internal expect fun platformForEachLine(path: String, action: (String) -> Unit)
internal expect fun platformFileSource(path: String): Source
internal expect fun platformFileSink(path: String): Sink
internal expect fun platformGetFileNamesInDirectory(path: String): List<String>
internal expect fun platformCopyFile(source: String, destination: String)

open class FileException(message: String) : Exception(message)
class FileOperationOnDirectoryException(message: String) : FileException("This operation is only allowed on a file, not a directory: $message")
class DirectoryOperationOnFileException(message: String) : FileException("This operation is only allowed on a directory, not a file: $message")