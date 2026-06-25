package so.aporia.module.impl.misc

import com.chaos.annotation.Obfuscate
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.settings.BooleanSetting
import so.aporia.utils.events.EventBus
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.ChatMessageEvent
import so.aporia.utils.events.impl.PlayerDeathEvent
import so.aporia.utils.events.impl.TickEvent
import net.minecraft.client.Minecraft
import java.util.regex.Pattern

@Obfuscate
class AutoEZ : Module("AutoEZ", Category.MISC) {

    val chatMode = BooleanSetting("Chat Detect", "", true)
    val eventMode = BooleanSetting("Event Detect", "", true)
    val message = so.aporia.module.settings.TextSetting("Message", "", "ez")

    private val killedPlayers = mutableSetOf<String>()
    private var lastKilledName: String? = null
    private var delayTicks = 0

    override fun onEnable() {
        EventBus.register(this)
        killedPlayers.clear()
        lastKilledName = null
        delayTicks = 0
    }

    override fun onDisable() {
        EventBus.unregister(this)
        killedPlayers.clear()
        lastKilledName = null
    }

    @EventHandler
    fun onChat(event: ChatMessageEvent) {
        if (!chatMode.isEnabled) return
        val mc = Minecraft.getInstance()
        if (mc.player == null) return
        val text = event.plainText
        var m = KILL_CHAT_RU.matcher(text)
        if (m.find()) { onKill(m.group(1)); return }
        m = KILL_CHAT_EN.matcher(text)
        if (m.find()) onKill(m.group(1))
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        if (!eventMode.isEnabled) return
        val mc = Minecraft.getInstance()
        if (mc.player == null) return
        if (event.player == null || event.player == mc.player) return
        onKill(event.playerName)
    }

    @EventHandler
    fun onTick(event: TickEvent) {
        if (lastKilledName == null) return
        if (delayTicks > 0) { delayTicks--; return }
        sendEz(lastKilledName!!)
        lastKilledName = null
    }

    private fun onKill(playerName: String) {
        val name = playerName.replace("[^a-zA-Z0-9_]".toRegex(), "")
        if (name.isEmpty() || killedPlayers.contains(name)) return
        killedPlayers.add(name)
        lastKilledName = name
        delayTicks = (message.get().length * 20).toInt()
    }

    private fun sendEz(target: String) {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        val msg = message.get().replace("{player}", target)
        player.connection?.sendChat(if (msg.isEmpty()) "ez" else msg)
    }

    companion object {
        private val KILL_CHAT_RU = Pattern.compile("Вы\\s+убили\\s+([^\\s.!?]+)", Pattern.CASE_INSENSITIVE)
        private val KILL_CHAT_EN = Pattern.compile("You\\s+killed\\s+([^\\s.!?]+)", Pattern.CASE_INSENSITIVE)
    }
}
