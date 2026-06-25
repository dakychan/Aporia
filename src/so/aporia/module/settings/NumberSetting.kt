package so.aporia.module.settings

import java.util.function.Supplier

class NumberSetting @JvmOverloads constructor(
    name: String,
    description: String,
    defaultValue: Double,
    val min: Double,
    val max: Double,
    val step: Double,
    visible: Supplier<Boolean>? = null
) : Setting<Double>(name, description, defaultValue, visible) {

    fun get(): Double = value
    fun getFloat(): Float = value.toFloat()
    fun getInt(): Int = value.toInt()

    fun increase() { value = (value + step).coerceAtMost(max) }
    fun decrease() { value = (value - step).coerceAtLeast(min) }

    fun setValue(v: Double) {
        value = v.coerceIn(min, max)
    }
}
