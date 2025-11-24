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
        mkdir(absolutePath, recursive)
    }
}

/**
 * @return true if the path represents the root of the file system, "/" in POSIX or a drive letter in Windows.
 */
internal expect fun platformIsPathRoot(path: String): Boolean
internal expect fun platformIsPathAbsolute(path: String): Boolean
internal expect fun platformGetWorkingDirectory(): String
internal expect fun platformFileExists(path: String): Boolean
internal expect fun platformFileIsDirectory(path: String): Boolean
internal expect fun platformGetFileSize(path: String): Long
internal expect fun platformDelete(path: String, recursive: Boolean)
internal expect fun mkdir(path: String, recursive: Boolean)