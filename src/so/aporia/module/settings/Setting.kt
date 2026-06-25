package so.aporia.module.settings

import java.util.function.Supplier

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
}
