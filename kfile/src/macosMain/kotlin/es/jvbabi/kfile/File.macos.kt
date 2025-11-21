package es.jvbabi.kfile

import platform.Foundation.NSFileManager

internal actual fun platformIsPathAbsolute(path: String): Boolean = path.startsWith("/")
internal actual fun platformGetWorkingDirectory(): String = NSFileManager.defaultManager.currentDirectoryPath