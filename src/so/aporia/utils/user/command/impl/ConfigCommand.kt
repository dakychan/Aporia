package so.aporia.utils.user.command.impl
import com.chaos.annotation.Obfuscate
import so.aporia.utils.files.impl.ConfigFile
import so.aporia.utils.user.command.Command
import so.aporia.utils.user.command.CommandManager
import com.chaos.annotation.ChaosNative
@Obfuscate
@ChaosNative
class ConfigCommand : Command {
    override fun name() = "config"
    override fun description() = "Manage configs — .config save/load"
    override fun execute(args: Array<String>) {
        if (args.size < 2) {
            CommandManager.chat("§eUsage: ${CommandManager.PREFIX}config save/load")
            return
        }
        when (args[1].lowercase()) {
            "save" -> {
                ConfigFile.save()
                CommandManager.chat("§aConfig saved")
            }
            "load" -> {
                ConfigFile.load()
                CommandManager.chat("§aConfig loaded")
            }
            else -> CommandManager.chat("§eUsage: ${CommandManager.PREFIX}config save/load")
        }
    }
}