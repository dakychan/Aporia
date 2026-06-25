package so.aporia.utils.user.command.impl

import com.chaos.annotation.Obfuscate
import so.aporia.utils.user.command.Command
import so.aporia.utils.user.command.CommandManager
import so.aporia.utils.user.friend.FriendManager

@Obfuscate
class FriendCommand : Command {
    override fun name() = "friend"
    override fun description() = "Manage friends — .friend add/remove/list"
    override fun execute(args: Array<String>) {
        if (args.size < 2) {
            CommandManager.chat("§eUsage: ${CommandManager.PREFIX}friend add/remove/list <name>")
            return
        }
        when (args[1].lowercase()) {
            "add" -> {
                if (args.size < 3) { CommandManager.chat("§cUsage: .friend add <name>"); return }
                val name = args[2]
                if (FriendManager.isFriend(name)) {
                    CommandManager.chat("§e$name is already a friend")
                } else {
                    FriendManager.add(name)
                    CommandManager.chat("§aAdded friend: $name")
                }
            }
            "remove" -> {
                if (args.size < 3) { CommandManager.chat("§cUsage: .friend remove <name>"); return }
                val name = args[2]
                if (FriendManager.isFriend(name)) {
                    FriendManager.remove(name)
                    CommandManager.chat("§aRemoved friend: $name")
                } else {
                    CommandManager.chat("§c$name is not a friend")
                }
            }
            "list" -> {
                val friends = FriendManager.getAll()
                if (friends.isEmpty()) {
                    CommandManager.chat("§eNo friends added")
                } else {
                    CommandManager.chat("§6--- Friends (${friends.size}) ---")
                    friends.sorted().forEach { f: String -> CommandManager.chat("§a- $f") }
                }
            }
            else -> CommandManager.chat("§eUsage: ${CommandManager.PREFIX}friend add/remove/list <name>")
        }
    }
}
