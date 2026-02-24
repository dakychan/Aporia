package ru.files

import aporia.cc.Logger
import aporia.cc.files.ModuleConfig
import aporia.cc.user.UserData
import aporia.cc.user.UserRole
import com.google.gson.Gson
import com.google.gson.JsonObject
import ru.manager.OsManager
import ru.manager.OsManager.DirectoryType
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.DosFileAttributeView
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Расширенный менеджер файлов для управления всеми файлами приложения.
 * Использует OsManager для кроссплатформенной работы с путями.
 *
 * Поддерживаемые файлы:
 * - Config.apr - конфигурация модулей
 * - Friends.apr - список друзей
 * - Stats.json - статистика пользователя
 * - Keybinds.json - привязки клавиш
 * - HudConfig.json - позиции HUD элементов
 * - Logs - логи приложения
 */
class FilesManager {

    private val fileLoader = FileLoader()
    private val gson: Gson = Gson()

    // Основные пути через OsManager
    private val configPath: Path = OsManager.getFile(OsManager.DirectoryType.CONFIG, "Config.apr")
    private val friendsPath: Path = OsManager.getFile(OsManager.DirectoryType.CONFIG, "Friends.apr")
    private val statsPath: Path = OsManager.getFile(OsManager.DirectoryType.CACHE, "Stats.json")
    private val keybindsPath: Path = OsManager.getFile(OsManager.DirectoryType.CONFIG, "Keybinds.json")
    private val hudConfigPath: Path = OsManager.getFile(OsManager.DirectoryType.CONFIG, "HudConfig.json")
    private val modulesConfigPath: Path = OsManager.getFile(OsManager.DirectoryType.CONFIG, "ModulesConfig.apr")

    /**
     * Инициализировать файловую систему.
     * Создаёт все необходимые директории и файлы.
     */
    fun initialize() {
        Logger.info("Initializing file system with OsManager...")
        Logger.info("Platform: ${OsManager.getPlatformName()} ${OsManager.osVersion}")

        createAllDirectories()
        createAllFiles()
        setHiddenAttributeForCacheFiles()

        Logger.info("✓ File system initialized successfully")
        Logger.info("  Main directory: ${OsManager.mainDirectory}")
        Logger.info("  Cache directory: ${OsManager.cacheDirectory}")
        Logger.info("  Data directory: ${OsManager.dataDirectory}")
        Logger.info("  Logs directory: ${OsManager.logsDirectory}")

        // Добавить shutdown hook для очистки temp
        Runtime.getRuntime().addShutdownHook(Thread {
            cleanup()
        })
    }

    /**
     * Создать все необходимые директории.
     */
    private fun createAllDirectories() {
        val directories = listOf(
            OsManager.mainDirectory,
            OsManager.cacheDirectory,
            OsManager.dataDirectory,
            OsManager.logsDirectory,
            OsManager.tempDirectory,
            OsManager.backupDirectory,
            OsManager.modulesDirectory,
            OsManager.themesDirectory,
            OsManager.scriptsDirectory
        )

        directories.forEach { path ->
            createDirectoryWithFallback(path)
        }
    }

    /**
     * Создать все необходимые файлы.
     */
    private fun createAllFiles() {
        val files = mapOf(
            configPath to "# Aporia Configuration\n# Generated: ${LocalDateTime.now()}\n\n",
            friendsPath to "# Friends List\n# Add friends here (one per line)\n\n",
            statsPath to "{}",
            keybindsPath to """{"keybinds":[]}""",
            hudConfigPath to "[]",
            modulesConfigPath to "# Modules Configuration\n\n"
        )

        files.forEach { (path, content) ->
            ensureFileExists(path, content)
        }
    }

    /**
     * Создать директорию с fallback для разных платформ.
     */
    private fun createDirectoryWithFallback(path: Path) {
        try {
            Files.createDirectories(path)
            Logger.debug("✓ Created directory: $path")
        } catch (e: Exception) {
            Logger.warn("Failed to create directory, trying fallback...")

            val success = when (OsManager.platform) {
                OsManager.Platform.WINDOWS -> createDirectoryWindows(path)
                OsManager.Platform.LINUX, OsManager.Platform.MAC, OsManager.Platform.UNKNOWN -> createDirectoryUnix(path)
            }

            if (success) {
                Logger.debug("✓ Created directory via fallback: $path")
            } else {
                Logger.error("✗ Failed to create directory: $path", e)
            }
        }
    }

    /**
     * Создать директорию на Windows.
     */
    private fun createDirectoryWindows(path: Path): Boolean {
        return try {
            val command = "powershell -Command \"New-Item -ItemType Directory -Force -Path '${path.toAbsolutePath()}'\""
            val process = Runtime.getRuntime().exec(command)
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Создать директорию на Unix-системах.
     */
    private fun createDirectoryUnix(path: Path): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("mkdir", "-p", path.toString()))
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Убедиться, что файл существует.
     */
    private fun ensureFileExists(path: Path, defaultContent: String) {
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path.parent)
                Files.writeString(path, defaultContent, StandardOpenOption.CREATE_NEW)
                Logger.debug("✓ Created file: ${path.fileName}")
            } catch (e: Exception) {
                Logger.error("✗ Failed to create file: ${path.fileName}", e)
            }
        } else {
            Logger.debug("○ File exists: ${path.fileName}")
        }
    }

    /**
     * Установить скрытый атрибут для кэш файлов на Windows.
     */
    private fun setHiddenAttributeForCacheFiles() {
        if (OsManager.platform == OsManager.Platform.WINDOWS) {
            try {
                val dosView = Files.getFileAttributeView(statsPath, DosFileAttributeView::class.java)
                dosView?.setHidden(true)
                Logger.debug("✓ Set hidden attribute for cache files")
            } catch (e: Exception) {
                Logger.warn("Could not set hidden attribute: ${e.message}")
            }
        }
    }

    // ========================================================================
    // Конфигурация модулей
    // ========================================================================

    /**
     * Сохранить конфигурацию модулей.
     */
    fun saveConfig(modules: Map<String, ModuleConfig>) {
        val content = buildString {
            appendLine("# Aporia Module Configuration")
            appendLine("# Generated: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}")
            appendLine()
            modules.forEach { (name, config) ->
                appendLine("$name.enabled=${config.enabled}")
                config.settings.forEach { (key, value) ->
                    appendLine("$name.$key=$value")
                }
                appendLine()
            }
        }

        try {
            Files.createDirectories(configPath.parent)
            Files.writeString(configPath, content)
            Logger.debug("✓ Config saved")
        } catch (e: Exception) {
            Logger.error("Failed to save config", e)
        }
    }

    /**
     * Загрузить конфигурацию модулей.
     */
    fun loadConfig(): Map<String, ModuleConfig>? {
        return fileLoader.loadApr(configPath)?.let { parseConfig(it) }
    }

    /**
     * Парсинг конфигурации.
     */
    private fun parseConfig(data: Map<String, String>): Map<String, ModuleConfig> {
        val configs = mutableMapOf<String, MutableMap<String, String>>()

        data.forEach { (key, value) ->
            val parts = key.split(".", limit = 2)
            if (parts.size == 2) {
                val moduleName = parts[0]
                val settingKey = parts[1]
                configs.getOrPut(moduleName) { mutableMapOf() }[settingKey] = value
            }
        }

        return configs.mapValues { (_, settings) ->
            val enabled = settings["enabled"]?.toBoolean() ?: false
            val moduleSettings = settings.filterKeys { it != "enabled" }
            ModuleConfig(enabled, moduleSettings)
        }
    }

    // ========================================================================
    // Статистика пользователя
    // ========================================================================

    /**
     * Сохранить статистику пользователя.
     */
    fun saveStats(userData: UserData.UserDataClass) {
        try {
            val json = gson.toJson(userData)
            fileLoader.saveJson(statsPath, JsonObject().apply {
                addProperty("username", userData.username)
                addProperty("uuid", userData.uuid)
                addProperty("role", userData.role.name)
                addProperty("hardwareId", userData.hardwareId)
                addProperty("lastUpdate", LocalDateTime.now().toString())
            })
        } catch (e: Exception) {
            Logger.error("Failed to save stats", e)
        }
    }

    /**
     * Загрузить статистику пользователя.
     */
    fun loadStats(): UserData.UserDataClass? {
        return try {
            if (!Files.exists(statsPath)) return null

            fileLoader.loadJson(statsPath)?.let { json ->
                UserData.UserDataClass(
                    username = json.get("username")?.asString ?: "Unknown",
                    uuid = json.get("uuid")?.asString ?: "",
                    role = UserRole.valueOf(json.get("role")?.asString ?: "USER"),
                    hardwareId = json.get("hardwareId")?.asString ?: ""
                )
            }
        } catch (e: Exception) {
            Logger.error("Failed to load stats", e)
            null
        }
    }

    // ========================================================================
    // Друзья
    // ========================================================================

    /**
     * Сохранить список друзей.
     */
    fun saveFriends(friends: List<String>) {
        try {
            val content = buildString {
                appendLine("# Friends List")
                appendLine("# Last updated: ${LocalDateTime.now()}")
                appendLine()
                friends.forEach { appendLine(it) }
            }
            Files.writeString(friendsPath, content)
            Logger.debug("✓ Friends saved (${friends.size})")
        } catch (e: Exception) {
            Logger.error("Failed to save friends", e)
        }
    }

    /**
     * Загрузить список друзей.
     */
    fun loadFriends(): List<String> {
        return try {
            if (!Files.exists(friendsPath)) return emptyList()

            Files.readAllLines(friendsPath)
                .filter { it.isNotBlank() && !it.startsWith("#") }
        } catch (e: Exception) {
            Logger.error("Failed to load friends", e)
            emptyList()
        }
    }

    /**
     * Добавить друга.
     */
    fun addFriend(name: String): Boolean {
        return try {
            val friends = loadFriends().toMutableList()
            if (friends.contains(name)) return false

            friends.add(name)
            saveFriends(friends)
            Logger.info("✓ Friend added: $name")
            true
        } catch (e: Exception) {
            Logger.error("Failed to add friend", e)
            false
        }
    }

    /**
     * Удалить друга.
     */
    fun removeFriend(name: String): Boolean {
        return try {
            val friends = loadFriends().toMutableList()
            if (!friends.contains(name)) return false

            friends.remove(name)
            saveFriends(friends)
            Logger.info("✓ Friend removed: $name")
            true
        } catch (e: Exception) {
            Logger.error("Failed to remove friend", e)
            false
        }
    }

    // ========================================================================
    // Keybinds
    // ========================================================================

    /**
     * Сохранить привязки клавиш.
     */
    fun saveKeybinds(keybinds: List<Map<String, Any>>) {
        try {
            val json = JsonObject()
            val array = com.google.gson.JsonArray()

            keybinds.forEach { kb ->
                JsonObject().apply {
                    addProperty("id", kb["id"] as String)
                    addProperty("keyCode", kb["keyCode"] as Int)
                    addProperty("keyName", kb["keyName"] as String)
                    array.add(this)
                }
            }

            json.add("keybinds", array)
            fileLoader.saveJson(keybindsPath, json)
        } catch (e: Exception) {
            Logger.error("Failed to save keybinds", e)
        }
    }

    /**
     * Загрузить привязки клавиш.
     */
    fun loadKeybinds(): List<Map<String, Any>> {
        return try {
            if (!Files.exists(keybindsPath)) return emptyList()

            fileLoader.loadJson(keybindsPath)?.let { json ->
                json.getAsJsonArray("keybinds")?.map {
                    val obj = it.asJsonObject
                    mapOf(
                        "id" to (obj.get("id")?.asString ?: ""),
                        "keyCode" to (obj.get("keyCode")?.asInt ?: 0),
                        "keyName" to (obj.get("keyName")?.asString ?: "")
                    )
                } ?: emptyList()
            } ?: emptyList()
        } catch (e: Exception) {
            Logger.error("Failed to load keybinds", e)
            emptyList()
        }
    }

    // ========================================================================
    // HUD Config
    // ========================================================================

    /**
     * Сохранить конфигурацию HUD.
     */
    fun saveHudConfig(components: List<Map<String, Any>>) {
        try {
            val json = com.google.gson.JsonArray()

            components.forEach { comp ->
                JsonObject().apply {
                    addProperty("name", comp["name"] as String)
                    addProperty("x", (comp["x"] as Number).toFloat())
                    addProperty("y", (comp["y"] as Number).toFloat())
                    addProperty("zIndex", comp["zIndex"] as Int)
                    json.add(this)
                }
            }

            fileLoader.saveJson(hudConfigPath, JsonObject().apply { add("components", json) })
        } catch (e: Exception) {
            Logger.error("Failed to save HUD config", e)
        }
    }

    /**
     * Загрузить конфигурацию HUD.
     */
    fun loadHudConfig(): List<Map<String, Any>> {
        return try {
            if (!Files.exists(hudConfigPath)) return emptyList()

            fileLoader.loadJson(hudConfigPath)?.let { json ->
                json.getAsJsonArray("components")?.map {
                    val obj = it.asJsonObject
                    mapOf(
                        "name" to (obj.get("name")?.asString ?: ""),
                        "x" to (obj.get("x")?.asFloat ?: 0f),
                        "y" to (obj.get("y")?.asFloat ?: 0f),
                        "zIndex" to (obj.get("zIndex")?.asInt ?: 0)
                    )
                } ?: emptyList()
            } ?: emptyList()
        } catch (e: Exception) {
            Logger.error("Failed to load HUD config", e)
            emptyList()
        }
    }

    // ========================================================================
    // Утилиты
    // ========================================================================

    /**
     * Создать резервную копию конфига.
     */
    fun backupConfig(): Path? {
        return try {
            val backupPath = OsManager.backupDirectory.resolve("Config.${System.currentTimeMillis()}.bak")
            configPath.toFile().copyTo(backupPath.toFile(), overwrite = true)
            Logger.info("✓ Backup created: Config")
            backupPath
        } catch (e: Exception) {
            Logger.error("✗ Failed to create backup: ${e.message}", e)
            null
        }
    }

    /**
     * Очистить кэш.
     */
    fun clearCache() {
        try {
            OsManager.cacheDirectory.toFile().walkTopDown()
                .filter { it.isFile && it.name != "Stats.json" }
                .forEach { it.delete() }
            Logger.info("✓ Cache cleared")
        } catch (e: Exception) {
            Logger.error("Failed to clear cache", e)
        }
    }

    /**
     * Очистить temp директорию.
     */
    fun clearTemp() {
        try {
            if (!OsManager.tempDirectory.toFile().exists()) return
            OsManager.tempDirectory.toFile().walkTopDown()
                .sortedByDescending { it.length() }
                .forEach { it.delete() }
            Logger.info("✓ Temp directory cleared")
        } catch (e: Exception) {
            Logger.warn("Failed to clear temp directory: ${e.message}")
        }
    }

    /**
     * Cleanup при закрытии.
     */
    fun cleanup() {
        Logger.info("Cleaning up file system...")
        clearTemp()
        Logger.info("✓ File system cleanup complete")
    }

    /**
     * Получить информацию о файловой системе.
     */
    fun getFileSystemInfo(): String = buildString {
        appendLine("=== File System Info ===")
        appendLine("Platform: ${OsManager.getPlatformName()}")
        appendLine("Main: ${OsManager.mainDirectory}")
        appendLine("Cache: ${OsManager.cacheDirectory}")
        appendLine("Data: ${OsManager.dataDirectory}")
        appendLine("Logs: ${OsManager.logsDirectory}")
        appendLine()
        appendLine("Files:")
        appendLine("  Config: ${fileLoader.getFileSizeReadable(configPath)}")
        appendLine("  Stats: ${fileLoader.getFileSizeReadable(statsPath)}")
        appendLine("  Friends: ${fileLoader.getFileSizeReadable(friendsPath)}")
    }
}
