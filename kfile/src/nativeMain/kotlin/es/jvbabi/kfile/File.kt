package es.jvbabi.kfile

class File(private val path: String) {
    companion object {
        fun isPathAbsolute(path: String) = platformIsPathAbsolute(path)
        fun getWorkingDirectory(): File = File(platformGetWorkingDirectory())
    }

    /**
     * Get the current path of this file.
     * @returns A path that may be relative to the current working directory.
     */
    val currentPath: String
        get() = this.path

    /**
     * Get the absolute path of this file. On POSIX systems, this is relative to the root directory.
     * On Windows hosts, the path will start with a drive letter.
     * @returns A path that is guaranteed to be relative to the root directory.
     */
    val absolutePath: String
        get() =
            if (isPathAbsolute(this.path)) this.path
            else getWorkingDirectory().currentPath + "/${this.path}"

    fun exists(): Boolean = platformFileExists(this.absolutePath)
    fun isDirectory(): Boolean = exists() && platformFileIsDirectory(this.absolutePath)
}

internal expect fun platformIsPathAbsolute(path: String): Boolean
internal expect fun platformGetWorkingDirectory(): String
internal expect fun platformFileExists(path: String): Boolean
internal expect fun platformFileIsDirectory(path: String): Boolean