package so.aporia.module.settings

import so.aporia.utils.user.render.core.AporiaRenderer
import so.aporia.module.impl.render.clickgui.ThemeManagerModule.Theme
import java.util.function.Supplier
import com.chaos.annotation.ChaosNative
@ChaosNative
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

    private val hueBarCache = IntArray(64)
    private var hueCacheKey = -1

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

    private var cachedFloatArr: FloatArray? = null
    fun toFloatArray(): FloatArray {
        var arr = cachedFloatArr
        if (arr == null) { arr = FloatArray(4); cachedFloatArr = arr }
        arr[0] = ((value shr 16) and 0xFF) / 255f
        arr[1] = ((value shr 8) and 0xFF) / 255f
        arr[2] = (value and 0xFF) / 255f
        arr[3] = ((value shr 24) and 0xFF) / 255f
        return arr
    }

    fun toHexString(): String = String.format("#%08X", value)

    override fun displayHeight(dropOpen: Boolean): Float {
        if (!dropOpen) return 14f
        return 14f + 4f + 60f + 6f + 12f + 6f
    }

    override fun draw(r: AporiaRenderer, x: Float, y: Float, w: Float, theme: Theme, mouseX: Float, mouseY: Float, isDropOpen: Boolean) {
        val color = get()
        val previewSize = 14f - 4f
        val previewX = x + w - 8f - previewSize
        val previewY = y + (14f - previewSize) / 2f
        r.drawRect(previewX, previewY, previewSize, previewSize, 2f, color)
        r.drawStroke(previewX, previewY, previewSize, previewSize, 2f, 1f, 0, 0f, 0x99FFFFFF.toInt())
        r.drawText("regular", name, x + 8f, y + (14f - 9f) / 2f - 1f, 9f, theme.guiSettingText)
        val hex = String.format("#%06X", color and 0xFFFFFF)
        val hw = r.getTextWidth("regular", hex, 7f)
        r.drawText("regular", hex, previewX - 4f - hw, y + (14f - 7f) / 2f, 7f, theme.guiSettingValue)
        if (isDropOpen) {
            drawCompactPicker(r, x, w, y, theme)
        }
    }

    private fun drawCompactPicker(r: AporiaRenderer, sx: Float, sw: Float, y: Float, theme: Theme) {
        val padX = 8f
        val pickerX = sx + padX
        val pickerY = y + 14f + 4f
        val pickerW = (sw - padX * 2f).coerceIn(100f, 160f)

        val hue = getHue()
        val sat = getSaturation()
        val v = getValue()
        val barH = 12f

        // Saturation-Value square (compact)
        val svSize = 60f
        val hueKey = hue.toRawBits()
        if (hueKey != hueCacheKey) {
            hueCacheKey = hueKey
            for (i in 0 until 64) {
                val fi = i.toFloat() / 64f
                hueBarCache[i] = fromHSV(fi, 1f, 1f)
            }
        }

        for (sy in 0 until 30) {
            for (sx2 in 0 until 30) {
                val st = sx2.toFloat() / 30f
                val sv = 1f - sy.toFloat() / 30f
                val idx = (sy * 30 + sx2).coerceIn(0, 899)
                r.drawRect(pickerX + sx2 * (svSize / 30f), pickerY + sy * (svSize / 30f),
                    svSize / 30f, svSize / 30f, 0f, fromHSV(hue, st, sv))
            }
        }
        r.drawStroke(pickerX, pickerY, svSize, svSize, 1f, 1f, 0, 0f, 0x99FFFFFF.toInt())

        // Marker
        val mkX = pickerX + svSize * sat
        val mkY = pickerY + svSize * (1f - v)
        r.drawStroke(mkX - 3f, mkY - 3f, 6f, 6f, 0.5f, 1f, 0, 0f, -0x1)
        r.drawStroke(mkX - 2.5f, mkY - 2.5f, 5f, 5f, 0.5f, 1f, 0, 0f, 0xFF000000.toInt())

        // Hue bar (vertical, right of SV)
        val gap = 4f
        val hueX = pickerX + svSize + gap
        for (hi in 0 until 64) {
            r.drawRect(hueX, pickerY + hi * (svSize / 64f), 8f, svSize / 64f + 1f, 0f, hueBarCache[hi])
        }
        r.drawStroke(hueX, pickerY, 8f, svSize, 1f, 1f, 0, 0f, 0x99FFFFFF.toInt())
        r.drawStroke(hueX - 1f, pickerY + svSize * hue - 1f, 10f, 3f, 0.5f, 1f, 0, 0f, -0x1)

        // Alpha bar (full width)
        val alphaY = pickerY + svSize + gap
        val alphaW = pickerX + pickerW - pickerX
        val baseColor = get() and 0x00FFFFFF

        // Checkerboard background
        for (cx in 0 until alphaW.toInt() step 4) {
            for (cy in 0 until barH.toInt() step 4) {
                val c = if (((cx / 4) + (cy / 4)) % 2 == 0) 0xFFCCCCCC.toInt() else 0xFF777777.toInt()
                r.drawRect(pickerX + cx, alphaY + cy, 4f, 4f, 0f, c)
            }
        }
        // Gradient overlay
        for (ai in 0 until alphaW.toInt()) {
            val aFrac = ai.toFloat() / alphaW
            val a = (aFrac * 255f).toInt().coerceIn(0, 255)
            r.drawRect(pickerX + ai, alphaY, 1f, barH, 0f, (a shl 24) or baseColor)
        }
        r.drawStroke(pickerX, alphaY, alphaW, barH, 1f, 1f, 0, 0f, 0x99FFFFFF.toInt())

        // Alpha marker
        val alphaFrac = getA() / 255f
        r.drawStroke(pickerX + alphaW * alphaFrac - 1.5f, alphaY - 1f, 3f, barH + 2f, 0.5f, 1f, 0, 0f, -0x1)
    }

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
