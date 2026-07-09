/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.utils.user.render.animation
import kotlin.math.max
import kotlin.math.min
import com.chaos.annotation.ChaosNative
/**
 * Per-message physics animation: alpha fade + horizontal slide-in.
 * Both driven by real spring oscillators — call [tick] each frame.
 */
@ChaosNative
class MessageAnim(initialAlpha: Float) {

    val alpha: SpringSimulator
    val slideX: SpringSimulator
    val slideY: SpringSimulator

    private var lastMs = System.currentTimeMillis()

    init {
        alpha = SpringSimulator(ALPHA_K, ALPHA_D, initialAlpha)
        slideX = SpringSimulator(SLIDE_K, SLIDE_D, if (initialAlpha > 0.5f) 0f else SLIDE_INITIAL)
        slideY = SpringSimulator(FLOAT_K, FLOAT_D, if (initialAlpha > 0.5f) 0f else FLOAT_INITIAL)
        alpha.setTarget(initialAlpha)
        slideX.setTarget(0f)
        slideY.setTarget(0f)
    }

    fun tick() {
        val now = System.currentTimeMillis()
        val dt = min((now - lastMs) / 1000f, 0.05f)
        lastMs = now
        alpha.update(dt)
        slideX.update(dt)
        slideY.update(dt)
    }

    fun setAlphaTarget(t: Float) { alpha.setTarget(t) }

    fun alpha(): Float = max(0f, min(1f, alpha.value()))
    fun slideX(): Float = slideX.value()
    fun slideY(): Float = slideY.value()

    fun isDead(): Boolean = alpha.getTarget() < 0.01f && alpha.isSettled()

    companion object {
        private const val ALPHA_K = 160f
        private const val ALPHA_D = 24f
        private const val SLIDE_K = 340f
        private const val SLIDE_D = 22f

        @JvmField
        val SLIDE_INITIAL = 12f

        private const val FLOAT_K = 120f
        private const val FLOAT_D = 20f

        @JvmField
        val FLOAT_INITIAL = 8f
    }
}