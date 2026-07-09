package so.aporia.utils.events.impl
import com.chaos.annotation.ChaosNative
@ChaosNative
class MouseScrollEvent(val x: Double, val y: Double, val deltaX: Double, val deltaY: Double) {

    var cancelled = false
        private set

    fun cancel() { cancelled = true }

    fun x(): Double = x
    fun y(): Double = y
    fun deltaX(): Double = deltaX
    fun deltaY(): Double = deltaY
}