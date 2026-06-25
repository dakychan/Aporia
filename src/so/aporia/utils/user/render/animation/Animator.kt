/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.utils.user.render.animation

import java.util.function.Function

/**
 * Time-based animator — drives a value from 0→1 over a duration using any easing.
 *
 * Usage:
 * ```kotlin
 * val anim = Animator(300, Easing::sineOut)
 * anim.play()
 * // each frame:
 * anim.update()
 * val v = anim.value // 0→1
 * ```
 */
class Animator(private val durationMs: Long, private val easing: Function<Float, Float>) {

    enum class Direction { FORWARD, BACKWARD }
    enum class State { IDLE, PLAYING, FINISHED }

    private var startMs = 0L
    private var startVal = 0f
    private var endVal = 1f
    private var direction = Direction.FORWARD
    private var state = State.IDLE
    private var currentValue = 0f

    fun play() {
        startMs = System.currentTimeMillis()
        startVal = currentValue
        endVal = 1f
        direction = Direction.FORWARD
        state = State.PLAYING
    }

    fun reverse() {
        startMs = System.currentTimeMillis()
        startVal = currentValue
        endVal = 0f
        direction = Direction.BACKWARD
        state = State.PLAYING
    }

    fun toggle() {
        if (direction == Direction.FORWARD && state == State.PLAYING) reverse()
        else play()
    }

    fun update(): Float {
        if (state != State.PLAYING) return currentValue
        val t = (System.currentTimeMillis() - startMs).toFloat() / durationMs
        if (t >= 1f) {
            currentValue = endVal
            state = State.FINISHED
            return currentValue
        }
        val eased = easing.apply(t)
        currentValue = startVal + (endVal - startVal) * eased
        return currentValue
    }

    fun value(): Float = currentValue
    fun state(): State = state
    fun isPlaying(): Boolean = state == State.PLAYING
    fun isFinished(): Boolean = state == State.FINISHED

    fun reset() { state = State.IDLE; currentValue = 0f }

    fun snapTo(v: Float) { currentValue = v; state = State.IDLE }

    companion object {
        @JvmStatic fun fadeIn(ms: Long): Animator = Animator(ms, Easing::sineOut)
        @JvmStatic fun fadeOut(ms: Long): Animator = Animator(ms, Easing::sineIn)
        @JvmStatic fun pop(ms: Long): Animator = Animator(ms, Easing::elasticOut)
        @JvmStatic fun slide(ms: Long): Animator = Animator(ms, Easing::cubicOut)
        @JvmStatic fun bounce(ms: Long): Animator = Animator(ms, Easing::bounceOut)
        @JvmStatic fun spring(ms: Long): Animator = Animator(ms, Function { t -> Easing.springCritical(t, 8f) })
    }
}
