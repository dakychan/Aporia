package so.aporia.utils.events.impl
import com.chaos.annotation.ChaosNative
@ChaosNative
class MouseClickEvent(val x: Double, val y: Double, val button: Int, val action: Action) {

    enum class Action { PRESS, RELEASE }

    var cancelled = false
        private set

    fun cancel() { cancelled = true }

    fun action(): Action = action
    fun button(): Int = button
    fun x(): Double = x
    fun y(): Double = y
}