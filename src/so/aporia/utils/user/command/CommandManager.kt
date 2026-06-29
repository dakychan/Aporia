package so.aporia.utils.user.command

import aporia.cc.PanicSystem
import com.chaos.annotation.Obfuscate
import net.minecraft.network.chat.Component
import so.aporia.utils.imports.*
import so.aporia.utils.user.command.impl.*

@Obfuscate
object CommandManager {

    @JvmField
    var PREFIX = "."

    private val commands = mutableMapOf<String, Command>()

    init {
        register(ConfigCommand())
        register(FriendCommand())
        register(HelpCommand())
        register(InfoCommand())
        register(PanicCommand())
        register(PrefixCommand())
        register(PacketCommand())
    }

    @JvmStatic
    fun register(cmd: Command) {
        commands[cmd.name().lowercase()] = cmd
    }

    @JvmStatic
    fun getAll(): Collection<Command> = commands.values

    @JvmStatic
    fun handle(message: String): Boolean {
        if (!message.startsWith(PREFIX)) return false
        if (PanicSystem.INSTANCE.isPanicked) return false

        val parts = message.substring(PREFIX.length).trim().split("\\s+".toRegex()).toTypedArray()
        if (parts.isEmpty() || parts[0].isEmpty()) return true

        val cmd = commands[parts[0].lowercase()]
        if (cmd == null) {
            chat("§cUnknown command: ${parts[0]}. Use ${PREFIX}help")
        } else {
            cmd.execute(parts)
        }
        return true
    }

    @JvmStatic
    fun chat(text: String) {
        if (PanicSystem.INSTANCE.isPanicked) return
        mc.player?.displayClientMessage(Component.literal(text), false)
    }
}
