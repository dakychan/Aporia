package so.aporia.utils.user.command.impl
import com.chaos.annotation.Obfuscate
import so.aporia.utils.user.command.Command
import so.aporia.utils.user.command.CommandManager
import so.aporia.utils.imports.*
import com.chaos.annotation.ChaosNative
@Obfuscate
@ChaosNative
class InfoCommand : Command {
    override fun name() = "info"
    override fun description() = "Shows mod and player info"
    override fun execute(args: Array<String>) {
        CommandManager.chat("§6--- Aporia Info ---")
        CommandManager.chat("§eVersion: §f1.0.0")
        CommandManager.chat("§eMinecraft: §f${mc.launchedVersion}")
        mc.player?.let { player ->
            CommandManager.chat("§ePlayer: §f${player.name.string}")
            CommandManager.chat("§ePosition: §f${String.format("%.1f, %.1f, %.1f", player.x, player.y, player.z)}")
        }
    }
}