package so.aporia.module.settings

import java.util.function.Supplier

class ButtonSetting @JvmOverloads constructor(
    name: String,
    description: String,
    private val action: Runnable,
    visible: Supplier<Boolean>? = null
) : Setting<Unit>(name, description, Unit, visible) {

    fun click() {
        action.run()
    }
}
