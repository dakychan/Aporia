package so.aporia.module.settings

import java.util.function.Supplier

class ColorSetting @JvmOverloads constructor(
    name: String,
    description: String,
    defaultValue: Int = -1,
    visible: Supplier<Boolean>? = null
) : Setting<Int>(name, description, defaultValue, visible) {

    @Transient private var cachedHue = 0f
    @Transient private var cachedSat = 0f
    @Transient private var cachedVal = 0f
    @Transient private var hsvDirty = true

    fun get(): Int = value

    fun set(color: Int) {
        value = color
        hsvDirty = true
    }

    fun getR(): Int = (value shr 16) and 0xFF
    fun getG(): Int = (value shr 8) and 0xFF
    fun getB(): Int = value and 0xFF
    fun getA(): Int = (value shr 24) and 0xFF

    fun setR(r: Int) { value = (value and 0xFF00FFFF.toInt()) or ((r and 0xFF) shl 16); hsvDirty = true }
    fun setG(g: Int) { value = (value and 0xFFFF00FF.toInt()) or ((g and 0xFF) shl 8); hsvDirty = true }
    fun setB(b: Int) { value = (value and 0xFFFFFF00.toInt()) or (b and 0xFF); hsvDirty = true }
    fun setA(a: Int) { value = (value and 0x00FFFFFF.toInt()) or ((a and 0xFF) shl 24) }

    private fun ensureHSV() {
        if (!hsvDirty) return
        hsvDirty = false
        val r = getR() / 255f; val g = getG() / 255f; val b = getB() / 255f
        val max = maxOf(r, g, b); val min = minOf(r, g, b); val d = max - min
        cachedVal = max
        if (d == 0f) {
            cachedHue = 0f; cachedSat = 0f; return
        }
        cachedSat = d / max
        cachedHue = when (max) {
            r -> ((g - b) / d + if (g < b) 6f else 0f) / 6f
            g -> ((b - r) / d + 2f) / 6f
            else -> ((r - g) / d + 4f) / 6f
        }
    }

    fun getHue(): Float { ensureHSV(); return cachedHue }
    fun getSaturation(): Float { ensureHSV(); return cachedSat }
    fun getValue(): Float { ensureHSV(); return cachedVal }

    fun setHSV(hue: Float, saturation: Float, v: Float, alpha: Int = getA()) {
        val h = (hue % 1f) * 6f
        val i = h.toInt(); val f = h - i
        val p = v * (1f - saturation)
        val q = v * (1f - saturation * f)
        val t = v * (1f - saturation * (1f - f))
        val (r, g, b) = when (i % 6) {
            0 -> Triple(v, t, p)
            1 -> Triple(q, v, p)
            2 -> Triple(p, v, t)
            3 -> Triple(p, q, v)
            4 -> Triple(t, p, v)
            else -> Triple(v, p, q)
        }
        set(((alpha and 0xFF) shl 24) or ((r * 255).toInt().coerceIn(0, 255) shl 16) or
                ((g * 255).toInt().coerceIn(0, 255) shl 8) or ((b * 255).toInt().coerceIn(0, 255)))
    }

    fun toFloatArray(): FloatArray = floatArrayOf(
        getR() / 255f, getG() / 255f, getB() / 255f, getA() / 255f
    )

    fun toHexString(): String = String.format("#%08X", value)

    companion object {
        fun fromRGB(r: Int, g: Int, b: Int, a: Int = 255): Int =
            ((a and 0xFF) shl 24) or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)

        fun fromHSV(hue: Float, saturation: Float, value: Float, alpha: Int = 255): Int {
            val h = (hue % 1f) * 6f
            val i = h.toInt(); val f = h - i
            val p = value * (1f - saturation)
            val q = value * (1f - saturation * f)
            val t = value * (1f - saturation * (1f - f))
            val (r, g, b) = when (i % 6) {
                0 -> Triple(value, t, p)
                1 -> Triple(q, value, p)
                2 -> Triple(p, value, t)
                3 -> Triple(p, q, value)
                4 -> Triple(t, p, value)
                else -> Triple(value, p, q)
            }
            return ((alpha and 0xFF) shl 24) or ((r * 255).toInt() shl 16) or
                    ((g * 255).toInt() shl 8) or (b * 255).toInt()
        }
    }
}
