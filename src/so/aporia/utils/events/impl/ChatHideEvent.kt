package so.aporia.utils.events.impl
import net.minecraft.network.chat.Component
import com.chaos.annotation.ChaosNative
@ChaosNative
class ChatHideEvent(val message: Component) {
    val plainText: String = message.string

    var cancelled = false
        private set

    fun cancel() { cancelled = true }
    fun isCancelled(): Boolean = cancelled

    fun message(): Component = message
    fun plainText(): String = plainText
}