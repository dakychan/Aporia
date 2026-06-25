package so.aporia.module.settings

import java.util.function.Supplier

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
}
