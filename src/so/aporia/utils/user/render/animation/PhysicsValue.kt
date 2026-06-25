/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.utils.user.render.animation

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow

/**
 * A single animated float value driven by configurable physics.
 *
 * Combines a [SpringSimulator] with optional easing on top.
 * Tracks its own time so you just call [get] each frame.
 *
 * Usage:
 * ```kotlin
 * val alpha = PhysicsValue.spring(0f)
 * alpha.setTarget(1f)
 * // each frame:
 * val a = alpha.get()
 * ```
 */
class PhysicsValue private constructor(
    private val mode: Mode,
    initial: Float,
    private val spring: SpringSimulator?,
    private val expSpeed: Float
) {

    enum class Mode {
        SPRING,
        EXPONENTIAL,
        INSTANT
    }

    private var value = initial
    private var target = initial
    private var lastMs = System.currentTimeMillis()

    fun get(): Float {
        val now = System.currentTimeMillis()
        val dt = now - lastMs
        lastMs = now
        return advance(dt)
    }

    private fun advance(dtMs: Long): Float {
        when (mode) {
            Mode.SPRING -> {
                spring!!.updateMs(dtMs)
                value = spring.value()
            }
            Mode.EXPONENTIAL -> {
                val factor = 1f - (1f - expSpeed).pow(dtMs.toFloat())
                value += (target - value) * min(1f, factor)
            }
            Mode.INSTANT -> value = target
        }
        return value
    }

    fun setTarget(t: Float) {
        target = t
        if (mode == Mode.SPRING) spring!!.setTarget(t)
    }

    fun snap(v: Float) {
        value = v
        target = v
        if (mode == Mode.SPRING) spring!!.snap(v)
    }

    fun getTarget(): Float = target
    fun peek(): Float = value
    fun isSettled(): Boolean =
        if (mode == Mode.SPRING) spring!!.isSettled()
        else abs(value - target) < 0.001f

    companion object {
        @JvmStatic fun spring(initial: Float): PhysicsValue {
            return PhysicsValue(Mode.SPRING, initial, SpringSimulator.snappy(initial), 0f)
        }
        @JvmStatic fun spring(initial: Float, sim: SpringSimulator): PhysicsValue {
            return PhysicsValue(Mode.SPRING, initial, sim, 0f)
        }
        @JvmStatic fun exponential(initial: Float, speed: Float): PhysicsValue {
            return PhysicsValue(Mode.EXPONENTIAL, initial, null, speed)
        }
        @JvmStatic fun instant(initial: Float): PhysicsValue {
            return PhysicsValue(Mode.INSTANT, initial, null, 0f)
        }
    }
}
