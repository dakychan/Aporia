package so.aporia.module.settings
import so.aporia.utils.user.render.core.AporiaRenderer
import so.aporia.module.impl.render.clickgui.ThemeManagerModule.Theme
import java.util.function.Supplier
import com.chaos.annotation.ChaosNative
@ChaosNative
class TextSetting @JvmOverloads constructor(
    name: String,
    description: String,
    defaultValue: String,
    visible: Supplier<Boolean>? = null
) : Setting<String>(name, description, defaultValue, visible) {

    fun get(): String = value

    fun set(text: String) {
        value = text
    }

    override fun draw(r: AporiaRenderer, x: Float, y: Float, w: Float, theme: Theme, mouseX: Float, mouseY: Float, isDropOpen: Boolean) {
        val v = get()
        val txt = if (v.length > 10) v.substring(0, 8) + ".." else v
        val tw = r.getTextWidth("regular", txt, 8f)
        r.drawText("regular", name, x + 8f, y + (14f - 9f) / 2f - 1f, 9f, theme.guiSettingText)
        r.drawText("regular", txt, x + w - 8f - tw, y + (14f - 8f) / 2f - 1f, 8f, theme.guiSettingValue)
    }
}