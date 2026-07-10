package so.aporia.module.settings
import so.aporia.utils.user.render.core.AporiaRenderer
import so.aporia.module.impl.render.clickgui.ThemeManagerModule.Theme
import java.util.function.Supplier
import com.chaos.annotation.ChaosNative
@ChaosNative
class SelectSetting @JvmOverloads constructor(
    name: String,
    description: String,
    visible: Supplier<Boolean>? = null
) : Setting<String>(name, description, "", visible) {

    private val options = mutableListOf<String>()
    private var selectedIndex = 0

    fun value(vararg values: String): SelectSetting {
        options.clear()
        options.addAll(values)
        if (options.isNotEmpty()) {
            value = options[0]
            selectedIndex = 0
        }
        return this
    }

    fun selected(value: String): SelectSetting {
        val idx = options.indexOf(value)
        if (idx >= 0) {
            selectedIndex = idx
            this.value = value
        }
        return this
    }

    fun get(): String = value

    fun getOptions(): List<String> = options.toList()

    fun getSelectedIndex(): Int = selectedIndex

    fun setSelectedIndex(index: Int) {
        if (index in options.indices) {
            selectedIndex = index
            value = options[index]
        }
    }

    fun isSelected(option: String): Boolean = value == option

    override fun displayHeight(dropOpen: Boolean): Float {
        if (!dropOpen) return 14f
        return 14f + 4f + options.size * 9.8f + 3f
    }

    override fun draw(r: AporiaRenderer, x: Float, y: Float, w: Float, theme: Theme, mouseX: Float, mouseY: Float, isDropOpen: Boolean) {
        val pw = w - 14f
        r.drawText("regular", name, x + 8f, y + (14f - 9f) / 2f - 1f, 9f, theme.guiSettingText)
        val disp = get() + " \u25BC"
        val dw = r.getTextWidth("regular", disp, 8f)
        r.drawText("regular", disp, x + w - 8f - dw, y + (14f - 8f) / 2f - 1f, 8f, theme.guiSettingValue)
        if (isDropOpen) {
            val dy = y + 14f + 2f
            for (oi in options.indices) {
                val oy = dy + oi * 9.8f
                val sel = selectedIndex == oi
                val oh = mouseX >= x + 8f && mouseX < x + w - 8f && mouseY >= oy && mouseY < oy + 9.8f
                if (oh) r.drawRect(x + 8f, oy, pw, 9.8f, 0f, 0x33FFFFFF.toInt())
                r.drawText("regular", options[oi], x + 12f, oy + (9.8f - 8f) / 2f - 1f, 8f, if (sel) -0x1 else theme.guiSettingValue)
                val dx = x + w - 16f
                val dy2 = oy + 9.8f / 2f
                r.drawCircle(dx, dy2, 3f, if (sel) theme.guiSettingValue else 0x99FFFFFF.toInt())
                if (sel) r.drawCircle(dx, dy2, 1.5f, -0x1)
            }
        }
    }
}