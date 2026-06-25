package so.aporia.utils.user.command.impl

import com.chaos.annotation.Obfuscate
import so.aporia.module.ModuleManager
import so.aporia.module.impl.misc.PacketDebug
import so.aporia.utils.files.FilesManager
import so.aporia.utils.user.command.Command
import so.aporia.utils.user.command.CommandManager
import java.nio.file.Files
import java.util.*
import java.util.stream.Collectors

@Obfuscate
class PacketCommand : Command {
    override fun name() = "packet"
    override fun description() = "Packet debug: .packet log | .packet save"

    override fun execute(args: Array<String>) {
        if (args.size < 2) {
            CommandManager.chat("§cUsage: .packet log | .packet save")
            return
        }
        when (args[1].lowercase()) {
            "log" -> handleLog()
            "save" -> handleSave()
            else -> CommandManager.chat("§cUnknown subcommand: ${args[1]}")
        }
    }

    private fun handleLog() {
        val packetsDir = FilesManager.ROOT.resolve("packets")
        if (!Files.exists(packetsDir)) {
            CommandManager.chat("§cNo packet logs found")
            return
        }
        try {
            val logs = Files.list(packetsDir)
                .filter { p -> p.fileName.toString().startsWith("parse_") && p.fileName.toString().endsWith(".log") }
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList())

            if (logs.isEmpty()) {
                CommandManager.chat("§cNo packet logs found")
                return
            }

            CommandManager.chat("§6--- Packet Logs ---")
            for (i in 0 until minOf(logs.size, 10)) {
                val log = logs[i]
                val size = Files.size(log)
                CommandManager.chat("§e${i + 1}. §f${log.fileName} §7(${formatSize(size)})")
            }
            if (logs.size > 10) {
                CommandManager.chat("§7... and ${logs.size - 10} more")
            }
        } catch (e: Exception) {
            CommandManager.chat("§cError: ${e.message}")
        }
    }

    private fun handleSave() {
        val module = ModuleManager.get("PacketDebug") as? PacketDebug
        if (module == null || !module.isEnabled) {
            CommandManager.chat("§cPacketDebug module not enabled")
            return
        }

        val packetsDir = FilesManager.ROOT.resolve("packets")
        if (!Files.exists(packetsDir)) {
            CommandManager.chat("§cNo packet logs found")
            return
        }

        try {
            val latest = Files.list(packetsDir)
                .filter { p -> p.fileName.toString().startsWith("parse_") && p.fileName.toString().endsWith(".log") }
                .max(Comparator.comparing { it.fileName.toString() })
                .orElse(null)

            if (latest == null) {
                CommandManager.chat("§cNo packet logs found")
                return
            }

            val content = FilesManager.readText(latest)
            CommandManager.chat("§6--- Latest Packet Log ---")
            CommandManager.chat("§eFile: §f${latest.fileName}")
            CommandManager.chat("§eSize: §f${formatSize(Files.size(latest))}")
            CommandManager.chat("§eLines: §f${content.split("\n").size}")
            CommandManager.chat("§eLocation: §f${latest.toAbsolutePath()}")
        } catch (e: Exception) {
            CommandManager.chat("§cError: ${e.message}")
        }
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
}
