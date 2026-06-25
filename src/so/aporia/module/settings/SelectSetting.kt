package so.aporia.module.settings

import java.util.function.Supplier

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
}
