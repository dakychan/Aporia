package so.aporia.module.settings

import java.util.function.Supplier

class BooleanSetting @JvmOverloads constructor(
    name: String,
    description: String,
    defaultValue: Boolean,
    visible: Supplier<Boolean>? = null
) : Setting<Boolean>(name, description, defaultValue, visible) {

    val isEnabled: Boolean get() = value

    fun toggle() { value = !value }

    fun set(enabled: Boolean) {
        value = enabled
    }
}
