package ru.files

import java.nio.file.Path
import java.nio.file.Paths

object PathResolver {
    
    enum class Platform {
        WINDOWS, LINUX, MAC
    }
    
    val platform: Platform = detectPlatform()
    
    val mainDirectory: Path = when (platform) {
        Platform.WINDOWS -> Paths.get(System.getenv("APPDATA"), ".apr")
        Platform.LINUX, Platform.MAC -> Paths.get(System.getProperty("user.home"), ".config", ".apr")
    }
    
    val cacheDirectory: Path = when (platform) {
        Platform.WINDOWS -> Paths.get(System.getenv("APPDATA"), "cached", "Aporia.cc")
        Platform.LINUX, Platform.MAC -> Paths.get(System.getProperty("user.home"), ".config", "cached", "Aporia.cc")
    }
    
    private fun detectPlatform(): Platform {
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("win") -> Platform.WINDOWS
            os.contains("mac") -> Platform.MAC
            else -> Platform.LINUX
        }
    }
}
