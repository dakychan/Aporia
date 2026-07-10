package so.aporia.module.settings
import so.aporia.utils.user.render.core.AporiaRenderer
import so.aporia.module.impl.render.clickgui.ThemeManagerModule.Theme
import java.util.function.Supplier
import com.chaos.annotation.ChaosNative
@ChaosNative
class ButtonSetting @JvmOverloads constructor(
    name: String,
    description: String,
    private val action: Runnable,
    visible: Supplier<Boolean>? = null
) : Setting<Unit>(name, description, Unit, visible) {

    fun click() {
        action.run()
    }

    override fun draw(r: AporiaRenderer, x: Float, y: Float, w: Float, theme: Theme, mouseX: Float, mouseY: Float, isDropOpen: Boolean) {
        val bw = r.getTextWidth("regular", name, 8f) + 12f
        val bx = x + w - 8f - bw
        r.drawRect(bx, y + 1f, bw, 14f - 3f, 2f, 0x4DFFFFFF.toInt())
        r.drawText("regular", name, bx + 4f, y + (14f - 8f) / 2f - 1f, 8f, theme.guiSettingValue)
    }
}