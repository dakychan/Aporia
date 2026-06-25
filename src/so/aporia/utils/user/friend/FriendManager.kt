package so.aporia.utils.user.friend

import com.chaos.annotation.Obfuscate
import so.aporia.utils.files.FilesManager
import so.aporia.utils.user.logger.Logger
import java.util.concurrent.ConcurrentHashMap

@Obfuscate
object FriendManager {

    private val FILE = FilesManager.ROOT.resolve("friends.apr")
    private val friends = ConcurrentHashMap.newKeySet<String>()

    @JvmStatic
    fun init() {
        load()
        Logger.info("[FriendManager] Loaded ${friends.size} friends")
    }

    @JvmStatic
    fun add(name: String) {
        friends.add(name)
        save()
    }

    @JvmStatic
    fun remove(name: String) {
        friends.remove(name)
        save()
    }

    @JvmStatic
    fun isFriend(name: String): Boolean = friends.contains(name)

    @JvmStatic
    fun getAll(): Set<String> = HashSet(friends)

    @JvmStatic
    fun count(): Int = friends.size

    private fun load() {
        if (!FilesManager.exists(FILE)) return
        try {
            val content = FilesManager.readApr(FILE)
            if (content.isNullOrBlank()) return
            for (line in content.split("\n")) {
                val name = line.trim()
                if (name.isNotEmpty()) friends.add(name)
            }
        } catch (e: Exception) {
            Logger.error("[FriendManager] Failed to load: ${e.message}")
        }
    }

    private fun save() {
        try {
            val content = friends.joinToString("\n")
            FilesManager.writeApr(FILE, content)
        } catch (e: Exception) {
            Logger.error("[FriendManager] Failed to save: ${e.message}")
        }
    }
}
