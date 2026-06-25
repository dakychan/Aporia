package so.aporia.module.settings

import java.util.function.Supplier

class RangeSetting @JvmOverloads constructor(
    name: String,
    description: String,
    defaultValue: Float,
    val min: Float,
    val max: Float,
    val step: Float,
    visible: Supplier<Boolean>? = null
) : Setting<Float>(name, description, defaultValue, visible) {

    fun get(): Float = value

    fun setValue(v: Float) {
        value = v.coerceIn(min, max)
    }
}
