package aporia.cc
import com.chaos.annotation.Obfuscate
import net.minecraft.network.chat.Component
import so.aporia.utils.imports.*
import com.chaos.annotation.ChaosNative
@Obfuscate
@ChaosNative
class PanicSystem private constructor() {

    var isPanicked: Boolean = false
        private set

    private var restoreKey: String? = null

    fun panic() {
        if (isPanicked) return
        isPanicked = true

        restoreKey = mc.user.name

        mc.gui.chat.clearMessages(false)

        mc.player?.displayClientMessage(
            Component.literal("§7Bye bye! To restore the client, type \"§f$restoreKey§7\""),
            false
        )
    }

    fun restore() {
        isPanicked = false
        restoreKey = null
        mc.player?.displayClientMessage(
            Component.literal("§aAporia restored."),
            false
        )
    }

    fun handleChat(message: String): Boolean {
        if (!isPanicked) return false

        if (restoreKey != null && message == restoreKey) {
            restore()
            return true
        }

        return false
    }

    companion object {
        @JvmField
        val INSTANCE = PanicSystem()
    }
}