package so.aporia.module.settings
import so.aporia.utils.user.render.core.AporiaRenderer
import so.aporia.module.impl.render.clickgui.ThemeManagerModule.Theme
import java.util.function.Supplier
import com.chaos.annotation.ChaosNative
@ChaosNative
class MultiSelectSetting @JvmOverloads constructor(
    name: String,
    description: String,
    visible: Supplier<Boolean>? = null
) : Setting<String>(name, description, "", visible) {

    private val options = mutableListOf<String>()
    private val selected = mutableSetOf<String>()

    fun options(vararg opts: String): MultiSelectSetting {
        options.clear()
        options.addAll(opts)
        return this
    }

    fun getOptions(): List<String> = options.toList()

    fun getSelected(): List<String> = selected.toList()

    fun setSelected(items: List<String>) {
        selected.clear()
        selected.addAll(items.filter { it in options })
        value = selected.joinToString(", ")
    }

    fun isSelected(option: String): Boolean = selected.contains(option)

    fun toggle(option: String) {
        if (selected.contains(option)) {
            selected.remove(option)
        } else {
            selected.add(option)
        }
        value = selected.joinToString(", ")
    }

    override fun displayHeight(dropOpen: Boolean): Float {
        if (!dropOpen) return 14f
        return 14f + 4f + options.size * 9.8f + 3f
    }

    override fun draw(r: AporiaRenderer, x: Float, y: Float, w: Float, theme: Theme, mouseX: Float, mouseY: Float, isDropOpen: Boolean) {
        val pw = w - 14f
        r.drawText("regular", name, x + 8f, y + (14f - 9f) / 2f - 1f, 9f, theme.guiSettingText)
        val sum = getSelected().joinToString(", ").ifEmpty { "-" }
        val disp = sum + " \u25BC"
        val dw = r.getTextWidth("regular", disp, 8f)
        r.drawText("regular", disp, x + w - 8f - dw, y + (14f - 8f) / 2f - 1f, 8f, theme.guiSettingValue)
        if (isDropOpen) {
            val dy = y + 14f + 2f
            for (oi in options.indices) {
                val oy = dy + oi * 9.8f
                val sel = isSelected(options[oi])
                val oh = mouseX >= x + 8f && mouseX < x + w - 8f && mouseY >= oy && mouseY < oy + 9.8f
                if (oh) r.drawRect(x + 8f, oy, pw, 9.8f, 0f, 0x33FFFFFF.toInt())
                r.drawText("regular", options[oi], x + 12f, oy + (9.8f - 8f) / 2f - 1f, 8f, if (sel) -0x1 else theme.guiSettingValue)
                val bx = x + w - 18f
                val by2 = oy + 9.8f / 2f - 2.5f
                r.drawRect(bx, by2, 6f, 6f, 1f, if (sel) theme.guiSettingValue else 0x99FFFFFF.toInt())
                if (sel) {
                    r.drawLine(bx, by2 + 2.5f, bx + 1.5f, by2 + 4f, 1f, -0x1)
                    r.drawLine(bx + 1.5f, by2 + 4f, bx + 4f, by2 + 1f, 1f, -0x1)
                }
            }
        }
    }
}