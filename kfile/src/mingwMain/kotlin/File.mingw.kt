/**
 * Check if the path starts with a Drive Letter, followed by a colon.
 */
internal actual fun platformIsPathAbsolute(path: String): Boolean {
    return regex.matchEntire(path.lowercase())?.groupValues?.get(1) != null
}

private val regex = Regex("""[a-z]:""")