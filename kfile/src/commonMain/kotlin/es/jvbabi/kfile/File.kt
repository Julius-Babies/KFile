package es.jvbabi.kfile

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

open class FileException(message: String) : Exception(message)
class FileOperationOnDirectoryException(message: String) : FileException("This operation is only allowed on a file, not a directory: $message")