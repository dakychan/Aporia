package so.aporia.utils.user.render.animation

import com.chaos.annotation.ChaosNative
import kotlin.math.*

/**
 * Easing functions — all take [t] in [0,1] and return a value in ~[0,1].
 */
@ChaosNative
object Easing {

    private const val PI = Math.PI.toFloat()
    private const val TAU = PI * 2f

    private fun clamp(t: Float) = t.coerceIn(0f, 1f)

    @JvmStatic fun linear(t: Float) = clamp(t)

    @JvmStatic fun quadIn(t: Float) = clamp(t).let { it * it }
    @JvmStatic fun quadOut(t: Float) = clamp(t).let { t2 -> 1 - (1 - t2) * (1 - t2) }
    @JvmStatic fun quadInOut(t: Float) = clamp(t).let { t2 -> if (t2 < 0.5f) 2 * t2 * t2 else 1 - 2 * (1 - t2) * (1 - t2) }

    @JvmStatic fun cubicIn(t: Float) = clamp(t).let { it * it * it }
    @JvmStatic fun cubicOut(t: Float) = clamp(t).let { t2 -> val u = 1 - t2; 1 - u * u * u }
    @JvmStatic fun cubicInOut(t: Float) = clamp(t).let { t2 -> if (t2 < 0.5f) 4 * t2 * t2 * t2 else 1 - 4 * (1 - t2) * (1 - t2) * (1 - t2) }

    @JvmStatic fun quartIn(t: Float) = clamp(t).let { it * it * it * it }
    @JvmStatic fun quartOut(t: Float) = clamp(t).let { t2 -> val u = 1 - t2; 1 - u * u * u * u }
    @JvmStatic fun quartInOut(t: Float) = clamp(t).let { t2 -> if (t2 < 0.5f) 8 * t2 * t2 * t2 * t2 else 1 - 8 * (1 - t2) * (1 - t2) * (1 - t2) * (1 - t2) }

    @JvmStatic fun quintIn(t: Float) = clamp(t).let { it * it * it * it * it }
    @JvmStatic fun quintOut(t: Float) = clamp(t).let { t2 -> val u = 1 - t2; 1 - u * u * u * u * u }
    @JvmStatic fun quintInOut(t: Float) = clamp(t).let { t2 -> if (t2 < 0.5f) 16 * t2 * t2 * t2 * t2 * t2 else 1 - 16 * (1 - t2) * (1 - t2) * (1 - t2) * (1 - t2) * (1 - t2) }

    @JvmStatic fun sineIn(t: Float) = clamp(t).let { t2 -> 1 - cos(t2 * PI / 2).toFloat() }
    @JvmStatic fun sineOut(t: Float) = clamp(t).let { t2 -> sin(t2 * PI / 2).toFloat() }
    @JvmStatic fun sineInOut(t: Float) = clamp(t).let { t2 -> 0.5f - 0.5f * cos(t2 * PI).toFloat() }

    @JvmStatic fun expoIn(t: Float) = clamp(t).let { t2 -> if (t2 == 0f) 0f else 2f.pow(10 * t2 - 10) }
    @JvmStatic fun expoOut(t: Float) = clamp(t).let { t2 -> if (t2 == 1f) 1f else 1 - 2f.pow(-10 * t2) }
    @JvmStatic fun expoInOut(t: Float) = clamp(t).let { t2 ->
        if (t2 == 0f) 0f else if (t2 == 1f) 1f
        else if (t2 < 0.5f) 2f.pow(20 * t2 - 10) / 2
        else (2 - 2f.pow(-20 * t2 + 10)) / 2
    }

    @JvmStatic fun circIn(t: Float) = clamp(t).let { t2 -> 1 - sqrt(1 - t2 * t2).toFloat() }
    @JvmStatic fun circOut(t: Float) = clamp(t).let { t2 -> sqrt(1 - (t2 - 1) * (t2 - 1)).toFloat() }
    @JvmStatic fun circInOut(t: Float) = clamp(t).let { t2 ->
        if (t2 < 0.5f) (1 - sqrt(1 - 4 * t2 * t2).toFloat()) / 2
        else (sqrt(1 - (2 * t2 - 2) * (2 * t2 - 2)).toFloat() + 1) / 2
    }

    private const val C1 = 1.70158f
    private const val C2 = C1 * 1.525f
    private const val C3 = C1 + 1f

    @JvmStatic fun backIn(t: Float) = clamp(t).let { t2 -> C3 * t2 * t2 * t2 - C1 * t2 * t2 }
    @JvmStatic fun backOut(t: Float) = clamp(t).let { t2 -> val u = t2 - 1; 1 + C3 * u * u * u + C1 * u * u }
    @JvmStatic fun backInOut(t: Float) = clamp(t).let { t2 ->
        if (t2 < 0.5f) (4 * t2 * t2 * ((C2 + 1) * 2 * t2 - C2)) / 2
        else (4 * (t2 - 1) * (t2 - 1) * ((C2 + 1) * (2 * t2 - 2) + C2) + 2) / 2
    }

    @JvmStatic fun backOut(t: Float, overshoot: Float) = clamp(t).let { t2 ->
        val u = t2 - 1; val c = overshoot + 1; 1 + c * u * u * u + overshoot * u * u
    }

    private const val E1 = TAU / 3f
    private const val E2 = TAU / 4.5f

    @JvmStatic fun elasticIn(t: Float) = clamp(t).let { t2 ->
        if (t2 == 0f) 0f else if (t2 == 1f) 1f
        else -(2f.pow(10 * t2 - 10) * sin((t2 * 10 - 10.75) * E1)).toFloat()
    }
    @JvmStatic fun elasticOut(t: Float) = clamp(t).let { t2 ->
        if (t2 == 0f) 0f else if (t2 == 1f) 1f
        else (2f.pow(-10 * t2) * sin((t2 * 10 - 0.75) * E1)).toFloat() + 1
    }
    @JvmStatic fun elasticInOut(t: Float) = clamp(t).let { t2 ->
        if (t2 == 0f) 0f else if (t2 == 1f) 1f
        else if (t2 < 0.5f) -(2f.pow(20 * t2 - 10) * sin((20 * t2 - 11.125) * E2)).toFloat() / 2
        else (2f.pow(-20 * t2 + 10) * sin((20 * t2 - 11.125) * E2)).toFloat() / 2 + 1
    }

    @JvmStatic fun elasticOut(t: Float, amplitude: Float, period: Float) = clamp(t).let { t2 ->
        if (t2 == 0f) 0f else if (t2 == 1f) 1f
        else {
            val s = (period / TAU * asin(1 / amplitude)).toFloat()
            (amplitude * 2f.pow(-10 * t2) * sin((t2 - s) * TAU / period)).toFloat() + 1
        }
    }

    @JvmStatic fun bounceOut(t: Float): Float {
        val t2 = clamp(t); val n1 = 7.5625f; val d1 = 2.75f
        return when {
            t2 < 1 / d1 -> n1 * t2 * t2
            t2 < 2 / d1 -> { val t3 = t2 - 1.5f / d1; n1 * t3 * t3 + 0.75f }
            t2 < 2.5f / d1 -> { val t3 = t2 - 2.25f / d1; n1 * t3 * t3 + 0.9375f }
            else -> { val t3 = t2 - 2.625f / d1; n1 * t3 * t3 + 0.984375f }
        }
    }
    @JvmStatic fun bounceIn(t: Float) = 1 - bounceOut(1 - t)
    @JvmStatic fun bounceInOut(t: Float) = if (t < 0.5f) (1 - bounceOut(1 - 2 * t)) / 2 else (1 + bounceOut(2 * t - 1)) / 2

    @JvmStatic fun smoothstep(t: Float) = clamp(t).let { t2 -> t2 * t2 * (3 - 2 * t2) }
    @JvmStatic fun smootherstep(t: Float) = clamp(t).let { t2 -> t2 * t2 * t2 * (t2 * (t2 * 6 - 15) + 10) }
    @JvmStatic fun smootheststep(t: Float) = clamp(t).let { t2 -> t2 * t2 * t2 * t2 * (t2 * (t2 * (t2 * (-20) + 70) - 84) + 35) }

    @JvmStatic fun springCritical(t: Float, stiffness: Float) = clamp(t).let { t2 ->
        val x = stiffness * t2; 1f - (1f + x) * exp(-x).toFloat()
    }

    @JvmStatic fun springUnderdamped(t: Float, damping: Float, frequency: Float) = clamp(t).let { t2 ->
        if (t2 == 0f) 0f else {
            val w = frequency * TAU
            val wd = w * sqrt(1 - damping * damping).toFloat()
            1f - (exp(-damping * w * t2) * cos(wd * t2)).toFloat()
        }
    }

    @JvmStatic fun gravity(t: Float, bounces: Int): Float {
        val t2 = clamp(t)
        if (bounces <= 0) return t2 * t2
        var result = 0f; var h = 1f; var tLeft = t2
        for (i in 0..bounces) {
            val segLen = sqrt(h).toFloat() * 0.4f
            if (tLeft <= segLen) {
                val u = tLeft / segLen; return h * (1 - (2 * u - 1) * (2 * u - 1))
            }
            tLeft -= segLen; h *= 0.5f
        }
        return 1f
    }

    @JvmStatic fun pendulum(t: Float) = clamp(t).let { t2 -> (1 - cos(t2 * PI * 0.5) * exp(-t2 * 2.5)).toFloat() }
    @JvmStatic fun rubberBand(t: Float) = clamp(t).let { t2 -> 1f + (2f.pow(-10 * t2) * sin((t2 - 0.1) * TAU / 0.4)).toFloat() }

    @JvmStatic fun anticipate(t: Float, pullback: Float) = clamp(t).let { t2 ->
        if (t2 < 0.2f) { val u = t2 / 0.2f; -pullback * u * u }
        else { val u = (t2 - 0.2f) / 0.8f; backOut(u, 1.5f) * (1 + pullback) - pullback }
    }

    @JvmStatic fun overshoot(t: Float, overshoot: Float) = clamp(t).let { t2 ->
        1f + overshoot * (sin(t2 * PI) * exp(-t2 * 4)).toFloat()
    }

    @JvmStatic fun cubicBezier(t: Float, x1: Float, y1: Float, x2: Float, y2: Float): Float {
        var u = clamp(t)
        repeat(8) {
            val bx = bezierCoord(u, x1, x2) - u
            val dx = bezierDeriv(u, x1, x2)
            if (abs(dx) < 1e-6f) return@repeat
            u -= bx / dx
        }
        return bezierCoord(u, y1, y2)
    }

    private fun bezierCoord(t: Float, p1: Float, p2: Float) = 3 * (1 - t) * (1 - t) * t * p1 + 3 * (1 - t) * t * t * p2 + t * t * t
    private fun bezierDeriv(t: Float, p1: Float, p2: Float) = 3 * (1 - t) * (1 - t) * p1 + 6 * (1 - t) * t * (p2 - p1) + 3 * t * t * (1 - p2)

    @JvmStatic fun cssEase(t: Float) = cubicBezier(t, 0.25f, 0.1f, 0.25f, 1.0f)
    @JvmStatic fun cssEaseIn(t: Float) = cubicBezier(t, 0.42f, 0f, 1.0f, 1.0f)
    @JvmStatic fun cssEaseOut(t: Float) = cubicBezier(t, 0f, 0f, 0.58f, 1.0f)
    @JvmStatic fun cssEaseInOut(t: Float) = cubicBezier(t, 0.42f, 0f, 0.58f, 1.0f)

    @JvmStatic fun stepped(t: Float, steps: Int) = floor(clamp(t) * steps) / steps

    @JvmStatic fun mirror(t: Float, fn: (Float) -> Float) = if (t < 0.5f) fn(t * 2) / 2 else 1 - fn((1 - t) * 2) / 2
    @JvmStatic fun squared(t: Float, fn: (Float) -> Float) = fn(t).let { it * it }
}
