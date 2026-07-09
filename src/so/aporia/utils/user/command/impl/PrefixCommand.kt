package so.aporia.utils.user.command.impl
import com.chaos.annotation.Obfuscate
import so.aporia.utils.user.command.Command
import so.aporia.utils.user.command.CommandManager
import com.chaos.annotation.ChaosNative
@Obfuscate
@ChaosNative
class PrefixCommand : Command {
    override fun name() = "prefix"
    override fun description() = "Changes the command prefix. Usage: prefix <new>"
    override fun execute(args: Array<String>) {
        if (args.size < 2) {
            CommandManager.chat("§eCurrent prefix: §f${CommandManager.PREFIX}")
            return
        }
        CommandManager.PREFIX = args[1]
        CommandManager.chat("§aPrefix changed to: §f${CommandManager.PREFIX}")
    }
}