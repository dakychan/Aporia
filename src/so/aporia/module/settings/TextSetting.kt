package so.aporia.module.settings

import java.util.function.Supplier

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
}
