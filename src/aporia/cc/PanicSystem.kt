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

        mc.gui.hud.getChat().clearMessages(false)

        mc.player?.sendSystemMessage(
            Component.literal("§7Bye bye! To restore the client, type \"§f$restoreKey§7\"")
        )
    }

    fun restore() {
        isPanicked = false
        restoreKey = null
        mc.player?.sendSystemMessage(
            Component.literal("§aAporia restored.")
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