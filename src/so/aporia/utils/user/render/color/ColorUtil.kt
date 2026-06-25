package so.aporia.utils.user.render.color

object ColorUtil {

    private val LEGACY = intArrayOf(
        -0x1000000, -0xffff56, -0xff0001, -0xffff56,
        -0x560001, -0x560056, -0x100, -0x565656,
        -0xaaaaab, -0x5656ff, -0x56ff56, -0x56ffff,
        -0xff5656, -0xff56ff, -0x10000, -0x1,
    )

    @JvmStatic
    fun rgb(r: Int, g: Int, b: Int): Int {
        return -0x1000000 or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)
    }

    @JvmStatic
    fun rgba(r: Int, g: Int, b: Int, a: Int): Int {
        return ((a and 0xFF) shl 24) or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)
    }

    @JvmStatic
    fun rgbf(r: Float, g: Float, b: Float): Int {
        return rgb((r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt())
    }

    @JvmStatic
    fun rgbaf(r: Float, g: Float, b: Float, a: Float): Int {
        return rgba((r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt(), (a * 255).toInt())
    }

    @JvmStatic
    fun fromHex(hex: String?, fallback: Int): Int {
        if (hex == null) return fallback
        val s = if (hex.startsWith("#")) hex.substring(1) else hex
        return try {
            when (s.length) {
                3 -> {
                    val r = s.substring(0, 1).toInt(16)
                    val g = s.substring(1, 2).toInt(16)
                    val b = s.substring(2, 3).toInt(16)
                    rgb(r * 17, g * 17, b * 17)
                }
                6 -> -0x1000000 or s.toInt(16)
                8 -> s.toLong(16).toInt()
                else -> fallback
            }
        } catch (_: NumberFormatException) {
            fallback
        }
    }

    @JvmStatic
    fun fromLegacyCode(code: Char): Int {
        val idx = "0123456789abcdef".indexOf(code.lowercaseChar())
        return if (idx >= 0) LEGACY[idx] else -1
    }

    @JvmStatic
    fun alpha(argb: Int): Int = (argb shr 24) and 0xFF

    @JvmStatic
    fun red(argb: Int): Int = (argb shr 16) and 0xFF

    @JvmStatic
    fun green(argb: Int): Int = (argb shr 8) and 0xFF

    @JvmStatic
    fun blue(argb: Int): Int = argb and 0xFF

    @JvmStatic
    fun withAlpha(argb: Int, a: Int): Int {
        return (argb and 0x00FFFFFF) or ((a and 0xFF) shl 24)
    }

    @JvmStatic
    fun multiplyAlpha(argb: Int, factor: Float): Int {
        val a = (alpha(argb) * factor).toInt()
        return withAlpha(argb, a)
    }

    @JvmStatic
    fun lerp(a: Int, b: Int, t: Float): Int {
        val it = 1f - t
        val ar = (red(a) * it + red(b) * t).toInt()
        val ag = (green(a) * it + green(b) * t).toInt()
        val ab = (blue(a) * it + blue(b) * t).toInt()
        val aa = (alpha(a) * it + alpha(b) * t).toInt()
        return rgba(ar, ag, ab, aa)
    }

    @JvmStatic
    fun gradient(start: Int, end: Int, steps: Int): IntArray {
        if (steps <= 1) return intArrayOf(start)
        return IntArray(steps) { lerp(start, end, it.toFloat() / (steps - 1)) }
    }

    @JvmStatic
    fun rainbow(steps: Int, alpha: Int): IntArray {
        return IntArray(steps) { withAlpha(fromHSV(it.toFloat() / steps, 1f, 1f), alpha) }
    }

    @JvmStatic
    fun rainbowAt(t: Float, saturation: Float, value: Float, alpha: Int): Int {
        return withAlpha(fromHSV(t % 1f, saturation, value), alpha)
    }

    @JvmStatic
    fun fromHSV(h: Float, s: Float, v: Float): Int {
        if (s == 0f) {
            val c = (v * 255).toInt()
            return rgb(c, c, c)
        }
        val hh = (h % 1f) * 6f
        val i = hh.toInt()
        val f = hh - i
        val p = v * (1f - s)
        val q = v * (1f - s * f)
        val t = v * (1f - s * (1f - f))
        return when (i) {
            0 -> rgbf(v, t, p)
            1 -> rgbf(q, v, p)
            2 -> rgbf(p, v, t)
            3 -> rgbf(p, q, v)
            4 -> rgbf(t, p, v)
            else -> rgbf(v, p, q)
        }
    }

    @JvmStatic
    fun toHSV(argb: Int): FloatArray {
        val r = red(argb) / 255f
        val g = green(argb) / 255f
        val b = blue(argb) / 255f
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val d = max - min
        var h = 0f
        val s = if (max == 0f) 0f else d / max
        val v = max
        if (d != 0f) {
            h = when (max) {
                r -> (g - b) / d + (if (g < b) 6f else 0f)
                g -> (b - r) / d + 2f
                else -> (r - g) / d + 4f
            }
            h /= 6f
        }
        return floatArrayOf(h, s, v)
    }
}
