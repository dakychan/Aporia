package so.aporia.utils.assets

import aporia.cc.OsManager
import com.chaos.annotation.Obfuscate
import net.minecraft.resources.Identifier
import so.aporia.utils.files.FilesManager
import so.aporia.utils.user.logger.Logger
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

@Obfuscate
object AssetManager {

    private const val ASSETS_URL = "https://gitlab.com/protect3ed/files-for-aporia/-/raw/main/aporia.apr"
    private const val ASSETS_DIR = ".assets"

    @JvmStatic
    fun downloadAssets() {
        val assetsDir = FilesManager.ROOT.resolve(ASSETS_DIR)

        if (Files.exists(assetsDir) && Files.isDirectory(assetsDir)) {
            Logger.info("[AssetManager] Local assets found. Skipping download.")
            return
        }

        try {
            Logger.info("[AssetManager] Assets folder missing. Synchronizing core assets...")

            val aprData = downloadFile(ASSETS_URL)
            if (aprData == null || aprData.isEmpty()) {
                Logger.error("[AssetManager] Error: Downloaded data is empty!")
                return
            }

            Files.createDirectories(assetsDir)
            extractAprArchive(aprData, assetsDir)

            if (OsManager.platform == OsManager.Platform.WINDOWS) {
                hideWindowsDirectory(assetsDir)
            }
            Logger.info("[AssetManager] Assets successfully synchronized.")
        } catch (e: Exception) {
            Logger.error("[AssetManager] Failed to download assets: ${e.message}")
        }
    }

    @Throws(Exception::class)
    private fun extractAprArchive(aprData: ByteArray, targetDir: Path) {
        ByteArrayInputStream(aprData).use { bais ->
            ZipInputStream(bais).use { zis ->
                var entry: ZipEntry?
                while (zis.nextEntry.also { entry = it } != null) {
                    // НЕ ТРОГАЙ ИМЯ! Оно уже нормальное!
                    val entryPath = targetDir.resolve(entry!!.name)

                    if (entry!!.isDirectory) {
                        Files.createDirectories(entryPath)
                    } else {
                        Files.createDirectories(entryPath.parent)
                        Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING)
                    }
                    zis.closeEntry()
                }
            }
        }
    }

    @Throws(Exception::class)
    private fun downloadFile(urlStr: String): ByteArray? {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
        conn.setRequestProperty("Accept-Encoding", "identity")

        val status = conn.responseCode
        if (status != 200) {
            throw IOException("Server returned HTTP $status")
        }

        return try {
            conn.inputStream.use { input ->
                ByteArrayOutputStream().use { out ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        out.write(buffer, 0, bytesRead)
                    }
                    out.toByteArray()
                }
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun hideWindowsDirectory(dir: Path) {
        try {
            val pb = ProcessBuilder("attrib", "+s", "+h", dir.toAbsolutePath().toString())
            val process = pb.start()
            process.waitFor()
        } catch (_: Exception) {}
    }

    private fun deleteDirectory(dir: Path) {
        Files.walk(dir)
            .sorted(Comparator.reverseOrder())
            .forEach { path ->
                try { Files.delete(path) } catch (_: IOException) {}
            }
    }

    @JvmStatic
    fun getResourcePath(id: Identifier): Path? {
        if (id.namespace != "aporia" || id.path.startsWith("/")) {
            return null
        }
        val aprPath = FilesManager.ROOT.resolve(".assets").resolve(id.namespace).resolve(id.path)
        if (Files.exists(aprPath)) return aprPath
        val ccPath = FilesManager.ROOT.resolve(ASSETS_DIR).resolve(id.namespace).resolve(id.path)
        if (Files.exists(ccPath)) return ccPath
        return aprPath
    }

    @JvmStatic
    fun getResourceString(id: Identifier): String {
        val path = getResourcePath(id)
        if (path != null && Files.exists(path)) {
            return try {
                Files.readString(path)
            } catch (e: IOException) {
                throw RuntimeException("[AssetManager] Failed to read asset string: $id", e)
            }
        }
        throw RuntimeException("[AssetManager] Asset file missing: $id")
    }

    @JvmStatic
    fun getResourceBytes(id: Identifier): ByteArray {
        val path = getResourcePath(id)
        if (path != null && Files.exists(path)) {
            return try {
                Files.readAllBytes(path)
            } catch (e: IOException) {
                throw RuntimeException("[AssetManager] Failed to read asset bytes: $id", e)
            }
        }
        throw RuntimeException("[AssetManager] Asset bytes missing: $id")
    }
}
