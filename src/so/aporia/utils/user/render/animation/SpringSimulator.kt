/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.utils.user.render.animation

import kotlin.math.abs
import com.chaos.annotation.ChaosNative
/**
 * Real-time spring physics simulator.
 *
 * Models a damped harmonic oscillator: `F = -k*x - d*v`.
 * Call [update] every frame with delta-time in seconds,
 * then read [value] and [velocity].
 *
 * Usage:
 * ```kotlin
 * val spring = SpringSimulator(200f, 20f, 0f)
 * spring.setTarget(1f)
 * // each frame:
 * spring.update(deltaSeconds)
 * val x = spring.value
 * ```
 */
@ChaosNative
class SpringSimulator(
    private var stiffness: Float,
    private var damping: Float,
    initial: Float
) {

    private var mass = 1f
    private var position = initial
    private var velocity = 0f
    private var target = initial

    fun update(dt: Float) {
        if (dt <= 0) return
        val force = -stiffness * (position - target) - damping * velocity
        velocity += (force / mass) * dt
        position += velocity * dt
    }

    fun updateMs(ms: Long) { update(ms / 1000f) }

    fun value(): Float = position
    fun velocity(): Float = velocity

    fun setTarget(t: Float) { target = t }
    fun setPosition(p: Float) { position = p; velocity = 0f }
    fun snap(p: Float) { position = p; target = p; velocity = 0f }

    fun getTarget(): Float = target
    fun isSettled(): Boolean = abs(position - target) < 0.001f && abs(velocity) < 0.001f

    companion object {
        @JvmStatic fun snappy(initial: Float): SpringSimulator = SpringSimulator(300f, 22f, initial)
        @JvmStatic fun bouncy(initial: Float): SpringSimulator = SpringSimulator(200f, 10f, initial)
        @JvmStatic fun smooth(initial: Float): SpringSimulator = SpringSimulator(120f, 24f, initial)
        @JvmStatic fun stiff(initial: Float): SpringSimulator = SpringSimulator(500f, 35f, initial)
        @JvmStatic fun wobbly(initial: Float): SpringSimulator = SpringSimulator(180f, 8f, initial)
    }
}
