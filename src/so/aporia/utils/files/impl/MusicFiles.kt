package so.aporia.utils.files.impl

import aporia.cc.OsManager
import so.aporia.utils.files.FilesManager
import so.aporia.utils.user.logger.Logger
import java.io.File
import java.io.InputStream
import com.chaos.annotation.ChaosNative

/**
 * MusicFiles — scans ~/Music for audio files (mp3, ogg, wav).
 * Uses FilesManager for path existence checks and OsManager for platform detection.
 * Only callable from MusicControl.
 */
@ChaosNative
object MusicFiles {

    private val extensions = setOf("mp3", "ogg", "wav")
    private var cachedTracks: List<File>? = null

    fun getTracks(): List<File> {
        cachedTracks?.let { return it }
        val dir = getMusicDir()
        if (!FilesManager.exists(dir.toPath())) {
            Logger.warn("Music directory not found: $dir")
            cachedTracks = emptyList()
            return cachedTracks!!
        }
        val files = dir.listFiles()
            ?.filter { it.isFile && it.extension.lowercase() in extensions }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()
        cachedTracks = files
        Logger.info("Found ${files.size} music tracks in $dir")
        return files
    }

    fun getRandomTrack(): File? {
        val tracks = getTracks()
        return if (tracks.isNotEmpty()) tracks.random() else null
    }

    fun openStream(file: File): InputStream? {
        return try {
            if (file.exists() && file.isFile) file.inputStream() else null
        } catch (e: Exception) {
            Logger.error("Failed to open ${file.name}: ${e.message}")
            null
        }
    }

    fun refresh() { cachedTracks = null }

    private fun getMusicDir(): File {
        return FilesManager.resolveTilde("~/Music").toFile()
    }
}
