package ru.files

import java.nio.file.Path
import java.nio.file.Paths

/**
 * Path resolver for platform-specific directories.
 */
object PathResolver {
    
    /**
     * Supported platforms.
     */
    enum class Platform {
        WINDOWS, LINUX, MAC
    }
    
    /** Current platform */
    val platform: Platform = detectPlatform()
    
    /**
     * Main directory for config files (friends, settings, etc.)
     */
    val mainDirectory: Path = when (platform) {
        Platform.WINDOWS -> Paths.get(System.getProperty("user.home"), ".apr")
        Platform.LINUX, Platform.MAC -> Paths.get(System.getProperty("user.home"), ".config", ".apr")
    }
    
    /**
     * Cache directory for stats, info.json, important data.
     */
    val cacheDirectory: Path = when (platform) {
        Platform.WINDOWS -> Paths.get(System.getenv("APPDATA"), "Aporia.cc")
        Platform.LINUX -> Paths.get(System.getProperty("user.home"), ".cache", "Aporia")
        Platform.MAC -> Paths.get(System.getProperty("user.home"), ".cache", "Aporia")
    }
    
    /**
     * Detect current platform.
     * 
     * @return Detected platform
     */
    private fun detectPlatform(): Platform {
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("win") -> Platform.WINDOWS
            os.contains("mac") -> Platform.MAC
            else -> Platform.LINUX
        }
    }
}
