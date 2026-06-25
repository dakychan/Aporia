package so.aporia.utils.events.impl

class KeyInputEvent(val key: Int, val scancode: Int, val modifiers: Int, val action: Action) {

    enum class Action { PRESS, RELEASE, REPEAT }

    var cancelled = false
        private set

    val isCtrl get() = (modifiers and 2) != 0
    val isShift get() = (modifiers and 1) != 0
    val isAlt get() = (modifiers and 4) != 0

    fun cancel() { cancelled = true }

    fun action(): Action = action
    fun scancode(): Int = scancode
    fun key(): Int = key
    fun modifiers(): Int = modifiers
}
