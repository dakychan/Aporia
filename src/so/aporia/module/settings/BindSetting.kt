package so.aporia.module.settings

import so.aporia.utils.user.render.core.AporiaRenderer
import so.aporia.utils.user.input.KeyCodeMap
import so.aporia.utils.user.render.color.ColorUtil
import java.util.function.Supplier

class BindSetting @JvmOverloads constructor(
    name: String,
    description: String,
    defaultValue: Int = -1,
    visible: Supplier<Boolean>? = null
) : Setting<Int>(name, description, defaultValue, visible) {

    fun getKey(): Int = value
    fun isBound(): Boolean = value != -1

    fun setKey(key: Int) {
        value = key
    }

    fun render(r: AporiaRenderer, x: Int, y: Int, w: Int, active: Boolean) {
        val txt = when {
            active -> "Bind.."
            isBound() -> { val n = KeyCodeMap.getName(value); if (n == "UNKNOWN") "Btn$value" else n }
            else -> "-"
        }
        val color = if (active) 0xFFFFAA00.toInt() else 0xFFAAAAAA.toInt()
        val fs = 10f
        val tw = r.getTextWidth("regular", txt, fs)
        r.drawText("regular", txt, (x + w - 8 - tw).toFloat(), (y + (14 - fs) / 2f - 1f).toFloat(), fs, color)
    }
}
