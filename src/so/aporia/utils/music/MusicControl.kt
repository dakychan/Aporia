package so.aporia.utils.music

import so.aporia.utils.events.StateMachineEngine
import so.aporia.utils.files.impl.MusicFiles
import so.aporia.utils.imports.*
import so.aporia.utils.user.logger.Logger
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl
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
}
