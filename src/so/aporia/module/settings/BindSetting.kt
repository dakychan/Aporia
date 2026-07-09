package so.aporia.module.settings

import so.aporia.utils.user.render.core.AporiaRenderer
import so.aporia.utils.user.input.KeyCodeMap
import so.aporia.utils.user.render.theme.ThemeManager.Theme
import java.util.function.Supplier
import com.chaos.annotation.ChaosNative
@ChaosNative
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

    override fun draw(r: AporiaRenderer, x: Float, y: Float, w: Float, theme: Theme, mouseX: Float, mouseY: Float, isDropOpen: Boolean) {
        val txt = when {
            isDropOpen -> "Bind.."
            isBound() -> { val n = KeyCodeMap.getName(value); if (n == "UNKNOWN") "Btn$value" else n }
            else -> "-"
        }
        val color = if (isDropOpen) 0xFFFFAA00.toInt() else 0xFFAAAAAA.toInt()
        val fs = 10f
        val tw = r.getTextWidth("regular", txt, fs)
        r.drawText("regular", txt, (x + w - 8f - tw), y + (14f - fs) / 2f - 1f, fs, color)
    }
}
