package so.aporia.utils.music

import so.aporia.utils.events.StateMachineEngine
import so.aporia.utils.files.impl.MusicFiles
import so.aporia.utils.imports.*
import so.aporia.utils.user.logger.Logger
import dev.redstones.mediaplayerinfo.IMediaSession
import dev.redstones.mediaplayerinfo.MediaPlayerInfo
import net.minecraft.resources.Identifier
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl
import java.io.ByteArrayInputStream
import java.io.File
import com.chaos.annotation.ChaosNative

/**
 * MusicControl — plays local music files from ~/Music.
 * Uses StateMachine for state tracking.
 */
@ChaosNative
object MusicControl {

    enum class MusicState { IDLE, PLAYING, PAUSED }

    private val sm = StateMachineEngine(this, MusicState::class, MusicState.IDLE)

    private var clip: Clip? = null
    private var currentFile: File? = null
    private var volume = 0.5f

    fun getState(): MusicState = sm.state()
    fun isPlaying(): Boolean = sm.state() == MusicState.PLAYING && clip?.isActive == true
    fun isPaused(): Boolean = sm.state() == MusicState.PAUSED

    fun getVolume(): Float = volume

    fun setVolume(v: Float) {
        volume = v.coerceIn(0f, 1f)
        clip?.let {
            if (it.isOpen) {
                val gain = it.getControl(FloatControl.Type.MASTER_GAIN) as? FloatControl ?: return
                val min = gain.minimum.toDouble()
                val max = gain.maximum.toDouble()
                gain.value = (min + (max - min) * volume).toFloat()
            }
        }
    }

    fun play(file: File? = null) {
        stop()
        val target = file ?: MusicFiles.getRandomTrack() ?: run {
            Logger.warn("No music files found")
            return
        }
        try {
            val stream = AudioSystem.getAudioInputStream(target)
            clip = AudioSystem.getClip()
            clip!!.open(stream)
            stream.close()
            setVolume(volume)
            clip!!.start()
            clip!!.addLineListener { e ->
                if (e.type == javax.sound.sampled.LineEvent.Type.STOP) {
                    sm.currentState = MusicState.IDLE
                }
            }
            currentFile = target
            sm.currentState = MusicState.PLAYING
            Logger.info("Now playing: ${target.name}")
        } catch (e: Exception) {
            Logger.error("Failed to play ${target.name}: ${e.message}")
            sm.currentState = MusicState.IDLE
        }
    }

    fun pause() {
        clip?.let { if (it.isActive) it.stop() }
        sm.currentState = MusicState.PAUSED
    }

    fun resume() {
        clip?.let { if (it.isOpen && !it.isActive) it.start() }
        sm.currentState = MusicState.PLAYING
    }

    fun stop() {
        clip?.let { if (it.isOpen) { it.stop(); it.close() } }
        clip = null
        currentFile = null
        sm.currentState = MusicState.IDLE
    }

    fun next() {
        val files = MusicFiles.getTracks()
        if (files.isEmpty()) return
        val current = currentFile
        val idx = if (current != null) files.indexOf(current) else -1
        val next = if (idx in 0 until files.size - 1) files[idx + 1] else files.firstOrNull()
        play(next)
    }

    fun previous() {
        val files = MusicFiles.getTracks()
        if (files.isEmpty()) return
        val current = currentFile
        val idx = if (current != null) files.indexOf(current) else -1
        val prev = if (idx > 0) files[idx - 1] else files.lastOrNull()
        play(prev)
    }

    fun getCurrentTrack(): String? = currentFile?.nameWithoutExtension
    fun getPosition(): Long = clip?.microsecondPosition ?: 0
    fun getDuration(): Long = clip?.microsecondLength ?: 0

    fun seek(pos: Long) {
        clip?.let { if (it.isOpen) it.microsecondPosition = pos.coerceIn(0, it.microsecondLength) }
    }

    // ══════════════════════════════════════════════════════════════════
    //  Unified media facade (local Clip first, then OS media)
    //  All time values are normalized to SECONDS. OS media (redstones
    //  MediaPlayerInfo) already reports seconds; the local Clip reports
    //  microseconds and is converted here.
    // ══════════════════════════════════════════════════════════════════

    @Volatile private var osTitle: String? = null
    @Volatile private var osArtist: String? = null
    @Volatile private var osPlaying = false
    @Volatile private var osPosSec = 0L
    @Volatile private var osDurSec = 0L
    @Volatile private var osPosSampleMs = 0L
    @Volatile private var osArt: ByteArray? = null
    @Volatile private var osSession: IMediaSession? = null

    @Volatile private var pollerRunning = false
    private var pollerThread: Thread? = null

    private var artId: Identifier? = null
    private var artHash = 0

    private fun isLocalActive(): Boolean = isPlaying() || isPaused()

    /** Any media (local or OS) currently present. Also lazily starts the OS-media poller. */
    fun hasMedia(): Boolean {
        ensureMediaPoller()
        return isLocalActive() || !osTitle.isNullOrEmpty()
    }

    fun mediaTitle(): String? = if (isLocalActive()) getCurrentTrack() else osTitle
    fun mediaArtist(): String? = if (isLocalActive()) null else osArtist
    fun mediaPlaying(): Boolean = if (isLocalActive()) isPlaying() else osPlaying
    fun mediaDurationSeconds(): Long = if (isLocalActive()) getDuration() / 1_000_000L else osDurSec

    fun mediaPositionSeconds(): Long {
        if (isLocalActive()) return getPosition() / 1_000_000L
        // OS media is polled ~1s; extrapolate while playing so the bar moves smoothly.
        if (osPlaying) {
            val elapsed = (System.currentTimeMillis() - osPosSampleMs) / 1000L
            val p = osPosSec + elapsed.coerceAtLeast(0L)
            return if (osDurSec > 0L) p.coerceAtMost(osDurSec) else p
        }
        return osPosSec
    }

    /** Play/pause whichever source is active. */
    fun mediaTogglePlayPause() {
        if (isLocalActive()) { if (isPlaying()) pause() else resume() }
        else osSession?.let { try { it.playPause() } catch (_: Exception) {} }
    }

    /** Current OS-media cover art as a cached texture (reloaded only when it changes). Null for local files. */
    fun mediaArtworkId(): Identifier? {
        if (isLocalActive()) return null
        val bytes = osArt
        if (bytes == null || bytes.isEmpty()) return null
        val h = bytes.contentHashCode()
        if (artId != null && h == artHash) return artId
        val id = try { r.loadImage(ByteArrayInputStream(bytes)) } catch (_: Exception) { null } ?: return artId
        artId = id; artHash = h
        return id
    }

    private fun ensureMediaPoller() {
        if (pollerRunning) return
        pollerRunning = true
        pollerThread = Thread({
            while (pollerRunning) {
                try {
                    val sessions = MediaPlayerInfo.INSTANCE.mediaSessions
                    val s = sessions?.firstOrNull { it.media?.let { m -> !m.title.isNullOrEmpty() && m.isPlaying } == true }
                        ?: sessions?.firstOrNull { it.media?.let { m -> !m.title.isNullOrEmpty() } == true }
                    val info = s?.media
                    if (info != null) {
                        osSession = s
                        osTitle = if (!info.title.isNullOrBlank()) info.title else info.artist
                        osArtist = info.artist
                        osPlaying = info.isPlaying
                        osPosSec = info.position
                        osDurSec = info.duration
                        osPosSampleMs = System.currentTimeMillis()
                        osArt = info.artworkPng
                    } else {
                        osSession = null; osTitle = null; osArtist = null; osPlaying = false; osArt = null
                    }
                } catch (_: Exception) {
                    osSession = null; osTitle = null; osArtist = null; osPlaying = false
                }
                try { Thread.sleep(1000) } catch (_: InterruptedException) { break }
            }
        }, "Aporia-MediaPoller").apply { isDaemon = true }
        pollerThread!!.start()
    }
}
