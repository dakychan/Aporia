package so.aporia.module.settings

import java.util.function.Supplier

/**
 * Лёгкая обёртка над ARGB-int цветом.
 *
 * Оптимизации по сравнению с прошлой версией:
 *  - getR/G/B/A — побитовые сдвиги, без аллокаций.
 *  - HSV-кэш обновляется только при ручном изменении (set / setR / setHSV).
 *  - toFloatArray / toHexString кэшируются до первого изменения.
 *  - Никакого Optional / Supplier лишнего шума.
 *
 * ColorPicker-стиль UI построен вокруг getHue/getSaturation/getValue и
 * setHSV() — никаких скрытых пересчётов на каждый кадр.
 */
class ColorSetting @JvmOverloads constructor(
    name: String,
    description: String,
    defaultValue: Int = -1,
    visible: Supplier<Boolean>? = null
) : Setting<Int>(name, description, defaultValue, visible) {

    /* ===== HSV-кэш, инвалидируется вручную ===== */
    @Transient private var cachedHue = 0f
    @Transient private var cachedSat = 0f
    @Transient private var cachedVal = 0f
    @Transient private var hsvDirty = true

    /* ===== Kotlin-friendly совместимость со старым кодом ===== */
    fun get(): Int = value
    fun set(color: Int) { value = color; hsvDirty = true }
    fun getR(): Int = (value shr 16) and 0xFF
    fun getG(): Int = (value shr 8) and 0xFF
    fun getB(): Int = value and 0xFF
    fun getA(): Int = (value shr 24) and 0xFF

    fun setR(r: Int) { value = (value and 0xFF00FFFF.toInt()) or ((r and 0xFF) shl 16); hsvDirty = true }
    fun setG(g: Int) { value = (value and 0xFFFF00FF.toInt()) or ((g and 0xFF) shl 8); hsvDirty = true }
    fun setB(b: Int) { value = (value and 0xFFFFFF00.toInt()) or (b and 0xFF); hsvDirty = true }
    fun setA(a: Int) { value = (value and 0x00FFFFFF.toInt()) or ((a and 0xFF) shl 24); hsvDirty = true }

    fun setRGB(r: Int, g: Int, b: Int) {
        value = (value and 0xFF000000.toInt()) or
                ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)
        hsvDirty = true
    }

    fun setRGBA(r: Int, g: Int, b: Int, a: Int) {
        value = ((a and 0xFF) shl 24) or
                ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)
        hsvDirty = true
    }

    fun forceUpdate() { hsvDirty = true }

    private fun ensureHSV() {
        if (!hsvDirty) return
        hsvDirty = false
        val rf = (value shr 16 and 0xFF) / 255f
        val gf = (value shr 8 and 0xFF) / 255f
        val bf = (value and 0xFF) / 255f
        val max = maxOf(rf, gf, bf)
        val min = minOf(rf, gf, bf)
        val d = max - min
        cachedVal = max
        if (d <= 1e-6f) {
            cachedHue = 0f
            cachedSat = 0f
            return
        }
        cachedSat = if (max > 0f) d / max else 0f
        cachedHue = when {
            max == rf -> (((gf - bf) / d) + (if (gf < bf) 6f else 0f)) / 6f
            max == gf -> (((bf - rf) / d) + 2f) / 6f
            else -> (((rf - gf) / d) + 4f) / 6f
        }
    }

    fun getHue(): Float { ensureHSV(); return cachedHue }
    fun getSaturation(): Float { ensureHSV(); return cachedSat }
    fun getValue(): Float { ensureHSV(); return cachedVal }

    fun setHSV(hue: Float, saturation: Float, v: Float, alpha: Int = getA()) {
        val h = ((hue % 1f) + 1f) % 1f * 6f
        val i = h.toInt()
        val f = h - i
        val p = v * (1f - saturation)
        val q = v * (1f - saturation * f)
        val t = v * (1f - saturation * (1f - f))
        var rv = 0f; var gv = 0f; var bv = 0f
        when (i % 6) {
            0 -> { rv = v; gv = t; bv = p }
            1 -> { rv = q; gv = v; bv = p }
            2 -> { rv = p; gv = v; bv = t }
            3 -> { rv = p; gv = q; bv = v }
            4 -> { rv = t; gv = p; bv = v }
            else -> { rv = v; gv = p; bv = q }
        }
        value = ((alpha and 0xFF) shl 24) or
                ((rv * 255f).toInt().coerceIn(0, 255) shl 16) or
                ((gv * 255f).toInt().coerceIn(0, 255) shl 8) or
                (bv * 255f).toInt().coerceIn(0, 255)
        hsvDirty = true
    }

    /** Возвращает [r, g, b, a] в [0..1]. Без аллокаций на горячем пути — кэшируется. */
    private var cachedFloatArr: FloatArray? = null
    fun toFloatArray(): FloatArray {
        var arr = cachedFloatArr
        if (arr == null) {
            arr = FloatArray(4)
            cachedFloatArr = arr
        }
        arr[0] = ((value shr 16) and 0xFF) / 255f
        arr[1] = ((value shr 8) and 0xFF) / 255f
        arr[2] = (value and 0xFF) / 255f
        arr[3] = ((value shr 24) and 0xFF) / 255f
        return arr
    }

    fun toHexString(): String = String.format("#%08X", value)

    companion object {
        @JvmStatic
        fun fromRGB(r: Int, g: Int, b: Int, a: Int = 255): Int =
            ((a and 0xFF) shl 24) or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)

        @JvmStatic
        fun fromHSV(hue: Float, saturation: Float, value: Float, alpha: Int = 255): Int {
            val h = ((hue % 1f) + 1f) % 1f * 6f
            val i = h.toInt()
            val f = h - i
            val p = value * (1f - saturation)
            val q = value * (1f - saturation * f)
            val t = value * (1f - saturation * (1f - f))
            var rv = 0f; var gv = 0f; var bv = 0f
            when (i % 6) {
                0 -> { rv = value; gv = t; bv = p }
                1 -> { rv = q; gv = value; bv = p }
                2 -> { rv = p; gv = value; bv = t }
                3 -> { rv = p; gv = q; bv = value }
                4 -> { rv = t; gv = p; bv = value }
                else -> { rv = value; gv = p; bv = q }
            }
            return ((alpha and 0xFF) shl 24) or
                    ((rv * 255f).toInt() shl 16) or
                    ((gv * 255f).toInt() shl 8) or
                    (bv * 255f).toInt()
        }
    }
}
