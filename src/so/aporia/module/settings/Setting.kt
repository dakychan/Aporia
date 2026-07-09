package so.aporia.module.settings

import so.aporia.utils.user.render.core.AporiaRenderer
import so.aporia.utils.user.render.theme.ThemeManager.Theme
import java.util.function.Supplier
import com.chaos.annotation.ChaosNative
@ChaosNative
abstract class Setting<T>(
    val name: String,
    val description: String,
    value: T,
    private val visible: Supplier<Boolean>? = null,
    var category: String = "",
    var hierarchy: Int = 0
) {
    init {
        val cur = SettingCategoryManager.current()
        if (cur != null && category.isEmpty()) {
            category = cur.first; hierarchy = cur.second
        }
    }

    var value: T = value
        protected set(v) {
            field = v
            so.aporia.utils.files.impl.ConfigFile.markDirty()
        }

    open fun isVisible(): Boolean = visible?.get() ?: true
    fun name(): String = name

    open fun displayHeight(dropOpen: Boolean): Float = 14f

    open fun draw(r: AporiaRenderer, x: Float, y: Float, w: Float, theme: Theme, mouseX: Float, mouseY: Float, isDropOpen: Boolean) {}
}
