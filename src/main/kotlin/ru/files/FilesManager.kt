package ru.files

import aporia.cc.user.UserData
import com.google.gson.Gson
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.DosFileAttributeView

class FilesManager {
    
    private val fileLoader = FileLoader()
    private val gson = Gson()
    
    private val configPath: Path = PathResolver.mainDirectory.resolve("Config.apr")
    private val friendsPath: Path = PathResolver.mainDirectory.resolve("Friends.apr")
    private val statsPath: Path = PathResolver.cacheDirectory.resolve("Stats.json")
    
    fun initialize() {
        Logger.info("Initializing file system...")
        
        createDirectories()
        
        ensureFileExists(configPath, "# Aporia Config\n")
        ensureFileExists(friendsPath, "# Friends List\n")
        ensureFileExists(statsPath, "{}")
        
        setHiddenAttributeForCacheFiles()
        
        Logger.info("File system initialized successfully")
        Logger.info("Main directory: ${PathResolver.mainDirectory}")
        Logger.info("Cache directory: ${PathResolver.cacheDirectory}")
    }
    
    private fun createDirectories() {
        createDirectoryWithFallback(PathResolver.mainDirectory)
        createDirectoryWithFallback(PathResolver.cacheDirectory)
    }
    
    private fun createDirectoryWithFallback(path: Path) {
        try {
            Files.createDirectories(path)
            Logger.info("✓ Created directory: $path")
        } catch (e: Exception) {
            Logger.warn("Failed to create directory with standard method, trying fallback...")
            
            val success = when (PathResolver.platform) {
                PathResolver.Platform.WINDOWS -> createDirectoryWindows(path)
                else -> createDirectoryUnix(path)
            }
            
            if (success) {
                Logger.info("✓ Created directory via fallback: $path")
            } else {
                Logger.error("✗ Failed to create directory: $path", e)
            }
        }
    }
    
    private fun createDirectoryWindows(path: Path): Boolean {
        return try {
            val command = "powershell -Command \"New-Item -ItemType Directory -Force -Path '${path}'\""
            val process = Runtime.getRuntime().exec(command)
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }
    
    private fun createDirectoryUnix(path: Path): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("mkdir", "-p", path.toString()))
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }
    
    private fun ensureFileExists(path: Path, defaultContent: String) {
        if (!Files.exists(path)) {
            try {
                Files.writeString(path, defaultContent, StandardOpenOption.CREATE_NEW)
                Logger.info("✓ Created file: ${path.fileName}")
            } catch (e: Exception) {
                Logger.error("✗ Failed to create file: ${path.fileName}", e)
            }
        } else {
            Logger.info("○ File exists: ${path.fileName}")
        }
    }
    
    private fun setHiddenAttributeForCacheFiles() {
        if (PathResolver.platform == PathResolver.Platform.WINDOWS) {
            try {
                val dosView = Files.getFileAttributeView(statsPath, DosFileAttributeView::class.java)
                dosView?.setHidden(true)
                Logger.info("✓ Set hidden attribute for cache files")
            } catch (e: Exception) {
                Logger.warn("Could not set hidden attribute for cache files: ${e.message}")
            }
        }
    }
    
    fun saveConfig(modules: Map<String, ModuleConfig>) {
        val content = buildString {
            appendLine("# Aporia Module Configuration")
            modules.forEach { (name, config) ->
                appendLine("$name.enabled=${config.enabled}")
                config.settings.forEach { (key, value) ->
                    appendLine("$name.$key=$value")
                }
            }
        }
        
        try {
            Files.writeString(configPath, content)
            Logger.info("Config saved")
        } catch (e: Exception) {
            Logger.error("Failed to save config", e)
        }
    }
    
    fun loadConfig(): Map<String, ModuleConfig>? {
        return fileLoader.loadApr(configPath)?.let { parseConfig(it) }
    }
    
    private fun parseConfig(data: Map<String, Any>): Map<String, ModuleConfig> {
        val configs = mutableMapOf<String, MutableMap<String, Any>>()
        
        data.forEach { (key, value) ->
            val parts = key.split(".", limit = 2)
            if (parts.size == 2) {
                val moduleName = parts[0]
                val settingKey = parts[1]
                
                configs.getOrPut(moduleName) { mutableMapOf() }[settingKey] = value
            }
        }
        
        return configs.mapValues { (_, settings) ->
            val enabled = settings["enabled"]?.toString()?.toBoolean() ?: false
            val moduleSettings = settings.filterKeys { it != "enabled" }
                .mapValues { it.value.toString() }
            
            ModuleConfig(enabled, moduleSettings)
        }
    }
    
    fun saveStats(userData: UserData.UserDataClass) {
        try {
            val json = gson.toJson(userData)
            Files.writeString(statsPath, json)
            Logger.info("Stats saved")
        } catch (e: Exception) {
            Logger.error("Failed to save stats", e)
        }
    }
    
    fun loadStats(): UserData.UserDataClass? {
        return try {
            fileLoader.loadJson(statsPath)?.let { 
                gson.fromJson(it, UserData.UserDataClass::class.java) 
            }
        } catch (e: Exception) {
            Logger.error("Failed to load stats", e)
            null
        }
    }
    
    fun saveFriends(friends: List<String>) {
        try {
            val content = friends.joinToString("\n")
            Files.writeString(friendsPath, content)
            Logger.info("Friends saved")
        } catch (e: Exception) {
            Logger.error("Failed to save friends", e)
        }
    }
    
    fun loadFriends(): List<String> {
        return try {
            if (Files.exists(friendsPath)) {
                Files.readAllLines(friendsPath).filter { it.isNotBlank() && !it.startsWith("#") }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Logger.error("Failed to load friends", e)
            emptyList()
        }
    }
}
