package so.aporia.module.impl.misc

import com.chaos.annotation.ChaosNative
import com.chaos.annotation.Obfuscate
import com.ferra13671.discordipc.DiscordIPC
import com.ferra13671.discordipc.IPCUser
import com.ferra13671.discordipc.activity.ActivityInfo
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.settings.Setting
import so.aporia.utils.user.logger.Logger
import so.aporia.utils.user.render.core.AporiaRenderer

@Obfuscate
@ChaosNative
class DiscordRPCModule : Module("Discord RPC", Category.MISC) {

    private var ipc: DiscordIPC? = null
    var discordUser: IPCUser? = null
        private set
    var avatarId: Identifier? = null
        private set

    override val settings: List<Setting<*>> = emptyList()

    override fun onEnable() {
        try {
            val newIpc = DiscordIPC()
            // Use reflection to set private Lombok fields
            val onReadyField = DiscordIPC::class.java.getDeclaredField("onReady")
            onReadyField.isAccessible = true
            onReadyField.set(newIpc, Runnable {
                val userField = DiscordIPC::class.java.getDeclaredField("user")
                userField.isAccessible = true
                discordUser = userField.get(newIpc) as? IPCUser
                updateActivity()
            })
            val onErrorField = DiscordIPC::class.java.getDeclaredField("onError")
            onErrorField.isAccessible = true
            onErrorField.set(newIpc, java.util.function.BiConsumer<Int, String> { code, message ->
                Logger.error("Discord RPC error: $message")
            })
            newIpc.start(APP_ID)
            ipc = newIpc
        } catch (e: Exception) {
            Logger.error("Discord RPC init failed: ${e.message}")
            ipc = null
        }
    }

    fun loadAvatarOnRenderThread() {
        if (avatarId != null || discordUser == null) return
        try {
            val userAvatar = discordUser!!.avatarImage ?: return
            val stream = userAvatar.inputStream() ?: return
            avatarId = AporiaRenderer.loadImage(stream)
        } catch (e: Exception) {
            Logger.warn("Failed to load Discord avatar: ${e.message}")
        }
    }

    override fun onDisable() {
        ipc?.stop()
        ipc = null
        discordUser = null
        avatarId = null
    }

    private fun updateActivity() {
        val p = ipc ?: return
        if (!p.isConnected) return
        val mc = Minecraft.getInstance()
        val state = if (mc.connection != null) "Playing on server" else "In main menu"
        p.updateActivity {
            ActivityInfo("Aporia Cheat", state, null, null, null, null,
                System.currentTimeMillis() / 1000, null, null, null)
        }
    }

    companion object {
        private const val APP_ID = 1471901603287142421L
    }
}
