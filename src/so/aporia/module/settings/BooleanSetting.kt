package so.aporia.module.settings
import so.aporia.utils.user.render.animation.SpringSimulator
import so.aporia.utils.user.render.core.AporiaRenderer
import so.aporia.module.impl.render.clickgui.ThemeManagerModule.Theme
import java.util.function.Supplier
import com.chaos.annotation.ChaosNative
@ChaosNative
class BooleanSetting @JvmOverloads constructor(
    name: String,
    description: String,
    defaultValue: Boolean,
    visible: Supplier<Boolean>? = null
) : Setting<Boolean>(name, description, defaultValue, visible) {

    private val toggleSpring = SpringSimulator(340f, 24f, if (defaultValue) 1f else 0f)
    private var lastTick = System.currentTimeMillis()

    val isEnabled: Boolean get() = value

    fun toggle() {
        value = !value
        toggleSpring.setTarget(if (value) 1f else 0f)
    }

    fun set(enabled: Boolean) {
        value = enabled
        toggleSpring.setTarget(if (value) 1f else 0f)
    }

    override fun draw(r: AporiaRenderer, x: Float, y: Float, w: Float, theme: Theme, mouseX: Float, mouseY: Float, isDropOpen: Boolean) {
        val now = System.currentTimeMillis()
        toggleSpring.update((now - lastTick) / 1000f)
        lastTick = now

        r.drawText("regular", name, x + 8f, y + (14f - 9f) / 2f - 1f, 9f, theme.guiSettingText)
        val tx = x + w - 26f
        val ty = y + (14f - 10f) / 2f
        val t = toggleSpring.value()
        r.drawRect(tx, ty, 18f, 10f, 5f,
            if (isEnabled) theme.guiEnabledDot else 0x66FFFFFF.toInt())
        val knobOff = t * 8f
        r.drawCircle(tx + knobOff + 5f, ty + 5f, 5f, -0x1)
        r.drawCircle(tx + knobOff + 5f, ty + 5f, 3f,
            if (isEnabled) 0xFF66DD66.toInt() else 0xFF888888.toInt())
    }
}