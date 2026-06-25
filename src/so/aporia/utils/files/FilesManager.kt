package so.aporia.utils.files

import aporia.cc.OsManager
import com.chaos.annotation.Obfuscate
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import so.aporia.utils.files.impl.ChatFile
import so.aporia.utils.files.impl.ConfigFile
import so.aporia.utils.user.logger.Logger
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.*
import java.util.*
import java.util.stream.Collectors
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@Obfuscate
object FilesManager {

    enum class FileType {
        APR, ZIP, JAVA, LUA, CBM, CFG, UNKNOWN;

        companion object {
            @JvmStatic
            fun fromPath(path: Path): FileType {
                val name = path.fileName.toString().lowercase()
                return when {
                    name.endsWith(".apr") -> APR
                    name.endsWith(".zip") -> ZIP
                    name.endsWith(".java") -> JAVA
                    name.endsWith(".lua") -> LUA
                    name.endsWith(".cbm") -> CBM
                    name.endsWith(".cfg") -> CFG
                    else -> UNKNOWN
                }
            }
        }
    }

    @JvmField
    val GSON: Gson = GsonBuilder().setPrettyPrinting().create()

    @JvmField
    val ROOT: Path = OsManager.mainDirectory

    @JvmField
    val CONFIG_FILE: Path = ROOT.resolve("config.apr")

    @JvmField
    val ACCOUNTS_FILE: Path = ROOT.resolve("accounts.apr")

    @JvmStatic
    @Throws(IOException::class)
    fun init() {
        if (!Files.exists(ROOT)) {
            Files.createDirectories(ROOT)
            Logger.success("Created root directory: $ROOT")
            if (OsManager.platform == OsManager.Platform.WINDOWS) {
                hideWindowsDirectory(ROOT)
            }
        }
        ChatFile.load()
        ConfigFile.load()
    }

    private fun hideWindowsDirectory(path: Path) {
        try {
            Runtime.getRuntime().exec(arrayOf("attrib", "+s", "+h", path.toAbsolutePath().toString()))
            Logger.debug("Hidden directory: $path")
        } catch (e: IOException) {
            Logger.warn("Failed to hide directory: $path")
        }
    }

    @JvmStatic
    fun resolve(pathStr: String): Path {
        val p = Paths.get(pathStr)
        return if (p.isAbsolute) p else ROOT.resolve(p)
    }

    @JvmStatic
    fun resolveTilde(pathStr: String): Path {
        var s = pathStr
        if (s.startsWith("~")) {
            s = OsManager.userHome.toString() + s.substring(1)
        }
        return Paths.get(s)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun writeApr(aprPath: Path, content: ByteArray) {
        ensureParentDirs(aprPath)
        val now = OsManager.getCurrentDateTimeIso()
        var created = now
        if (Files.exists(aprPath)) {
            try {
                val old = readAprTimeJson(aprPath).getOrDefault("created", now)
                if (old.isNotEmpty()) created = old
            } catch (_: Exception) {}
        }
        ZipOutputStream(FileOutputStream(aprPath.toFile())).use { zos ->
            zos.putNextEntry(ZipEntry("content.dat"))
            zos.write(content)
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("time.json"))
            zos.write(buildTimeJson(created, now).toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun writeApr(aprPath: Path, content: String) {
        writeApr(aprPath, content.toByteArray(StandardCharsets.UTF_8))
    }

    @JvmStatic
    @Throws(IOException::class)
    fun readAprBytes(aprPath: Path): ByteArray {
        ZipInputStream(FileInputStream(aprPath.toFile())).use { zis ->
            var entry: ZipEntry?
            while (zis.nextEntry.also { entry = it } != null) {
                if (entry!!.name == "content.dat") {
                    return zis.readAllBytes()
                }
            }
        }
        throw IOException("content.dat not found in $aprPath")
    }

    @JvmStatic
    @Throws(IOException::class)
    fun readApr(aprPath: Path): String {
        return String(readAprBytes(aprPath), StandardCharsets.UTF_8)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun readAprTimeJson(aprPath: Path): Map<String, String> {
        val result = mutableMapOf<String, String>()
        ZipInputStream(FileInputStream(aprPath.toFile())).use { zis ->
            var entry: ZipEntry?
            while (zis.nextEntry.also { entry = it } != null) {
                if (entry!!.name == "time.json") {
                    val json = String(zis.readAllBytes(), StandardCharsets.UTF_8)
                    result["created"] = extractJsonValue(json, "created")
                    result["modified"] = extractJsonValue(json, "modified")
                    return result
                }
            }
        }
        return result
    }

    private fun buildTimeJson(created: String, modified: String): String {
        return "{\n  \"created\": \"$created\",\n  \"modified\": \"$modified\"\n}"
    }

    @JvmStatic
    @Throws(IOException::class)
    fun readBytes(path: Path): ByteArray = Files.readAllBytes(path)

    @JvmStatic
    @Throws(IOException::class)
    fun readText(path: Path): String = String(Files.readAllBytes(path), StandardCharsets.UTF_8)

    @JvmStatic
    @Throws(IOException::class)
    fun writeBytes(path: Path, data: ByteArray) {
        ensureParentDirs(path)
        Files.write(path, data)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun writeText(path: Path, text: String) {
        writeBytes(path, text.toByteArray(StandardCharsets.UTF_8))
    }

    @JvmStatic
    fun delete(path: Path): Boolean {
        return try {
            Files.deleteIfExists(path)
        } catch (_: IOException) {
            false
        }
    }

    @JvmStatic
    fun exists(path: Path): Boolean = Files.exists(path)

    @JvmStatic
    fun size(path: Path): Long {
        return try {
            Files.size(path)
        } catch (_: IOException) {
            -1
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun copy(src: Path, dst: Path) {
        ensureParentDirs(dst)
        Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun move(src: Path, dst: Path) {
        ensureParentDirs(dst)
        Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun readCfg(path: Path): Map<String, String> {
        val map = mutableMapOf<String, String>()
        for (line in Files.readAllLines(path, StandardCharsets.UTF_8)) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue
            val eq = trimmed.indexOf('=')
            if (eq > 0) {
                map[trimmed.substring(0, eq).trim()] = trimmed.substring(eq + 1).trim()
            }
        }
        return map
    }

    @JvmStatic
    @Throws(IOException::class)
    fun writeCfg(path: Path, data: Map<String, String>) {
        val sb = StringBuilder()
        sb.appendLine("# Aporia config")
        for ((key, value) in data) {
            sb.append(key).append("=").append(value).appendLine()
        }
        writeText(path, sb.toString())
    }

    @JvmStatic
    @Throws(IOException::class)
    fun listFiles(dir: Path, extension: String?): List<Path> {
        Files.list(dir).use { stream ->
            return stream
                .filter { Files.isRegularFile(it) }
                .filter { extension == null || it.fileName.toString().endsWith(extension) }
                .collect(Collectors.toList())
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun append(path: Path, text: String) {
        ensureParentDirs(path)
        Files.write(path, text.toByteArray(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun writeJson(path: Path, obj: Any) {
        writeText(path, GSON.toJson(obj))
    }

    @JvmStatic
    @Throws(IOException::class)
    fun <T> readJson(path: Path, type: Class<T>): T {
        return GSON.fromJson(readText(path), type)
    }

    @Throws(IOException::class)
    private fun ensureParentDirs(path: Path) {
        val parent = path.parent
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent)
        }
    }

    private fun extractJsonValue(json: String, key: String): String {
        val search = "\"$key\""
        val idx = json.indexOf(search)
        if (idx < 0) return ""
        val colon = json.indexOf(':', idx + search.length)
        if (colon < 0) return ""
        val start = json.indexOf('"', colon + 1)
        if (start < 0) return ""
        val end = json.indexOf('"', start + 1)
        if (end < 0) return ""
        return json.substring(start + 1, end)
    }

    @JvmStatic
    fun getFileType(path: Path): FileType = FileType.fromPath(path)
}
