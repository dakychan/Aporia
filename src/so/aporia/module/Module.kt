package so.aporia.module

abstract class Module @JvmOverloads constructor(
    val name: String,
    val category: Category,
    var keybind: Int = -1
) {
    var isEnabled: Boolean = false
        private set

    protected open fun onEnable() {}
    protected open fun onDisable() {}

    fun enable() {
        if (isEnabled) return
        isEnabled = true
        so.aporia.utils.files.impl.ConfigFile.markModuleActivated(this)
        so.aporia.utils.files.impl.ConfigFile.markDirty()
        onEnable()
    }

    fun disable() {
        if (!isEnabled) return
        isEnabled = false
        so.aporia.utils.files.impl.ConfigFile.markDirty()
        onDisable()
    }

    fun toggle() {
        if (isEnabled) disable() else enable()
    }

    fun name(): String = name
    fun keybind(): Int = keybind
}
