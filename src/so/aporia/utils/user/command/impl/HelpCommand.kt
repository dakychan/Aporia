package so.aporia.utils.user.command.impl

import com.chaos.annotation.Obfuscate
import so.aporia.utils.user.command.Command
import so.aporia.utils.user.command.CommandManager

@Obfuscate
class HelpCommand : Command {
    override fun name() = "help"
    override fun description() = "Lists all available commands"
    override fun execute(args: Array<String>) {
        CommandManager.chat("§6--- Aporia Commands ---")
        CommandManager.getAll().sortedBy { it.name() }.forEach { cmd ->
            CommandManager.chat("§e${CommandManager.PREFIX}${cmd.name()} §7- ${cmd.description()}")
        }
    }
}
