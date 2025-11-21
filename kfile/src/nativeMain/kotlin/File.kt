class File(path: String) {
    companion object {
        fun isPathAbsolute(path: String) = platformIsPathAbsolute(path)
    }
}

internal expect fun platformIsPathAbsolute(path: String): Boolean