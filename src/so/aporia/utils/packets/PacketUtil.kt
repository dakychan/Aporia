package so.aporia.utils.packets

import com.chaos.annotation.Obfuscate
import net.minecraft.client.Minecraft

@Obfuscate
object PacketUtil {

    private val mc = Minecraft.getInstance()

    @JvmStatic
    fun isMovePacket(className: String): Boolean {
        return className.contains("MovePlayer") || className.contains("MoveEntity")
    }

    @JvmStatic
    fun isChatPacket(className: String): Boolean {
        return className.contains("Chat") || className.contains("SystemChat")
    }
}
