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

    /**
     * Get the segments of the absolute path. This will remove relative names like "." and resolve ".."
     * @returns A list of path segments.
     */
    val cleanSegments: List<String>
        get() = absolutePath
            .split('/')
            .filterNot { it == "." }
            .filterNot { it.isEmpty() }
            .toMutableList()
            .let { segments ->
                var i = 0
                while (i < segments.size - 1) {
                    if (i == segments.size - 1) break
                    if (segments[i+1] == "..") {
                        segments.removeAt(i+1)
                        segments.removeAt(i)
                        if (i > 0) {
                            i--
                            continue
                        }
                    }

                    i++
                }

                segments
            }
            .toList()

    fun exists(): Boolean = platformFileExists(this.absolutePath)
    fun isDirectory(): Boolean = exists() && platformFileIsDirectory(this.absolutePath)

    val parent: File?
        get() =
            if (platformIsPathRoot(this.absolutePath)) null
            else File(
                this.cleanSegments
                    .dropLast(1)
                    .let { segments ->
                        // Add a leading slash since it gets removed in cleanSegments, on POSIX platforms, it must be added back again
                        if (platformIsPathRoot(segments.first())) segments
                        else listOf("") + segments
                    }
                    .joinToString("/")
            )
}

internal expect fun platformIsPathRoot(path: String): Boolean
internal expect fun platformIsPathAbsolute(path: String): Boolean
internal expect fun platformGetWorkingDirectory(): String
internal expect fun platformFileExists(path: String): Boolean
internal expect fun platformFileIsDirectory(path: String): Boolean