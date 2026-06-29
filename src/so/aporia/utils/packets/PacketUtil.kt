package so.aporia.utils.packets

import com.chaos.annotation.Obfuscate
import so.aporia.utils.imports.*

@Obfuscate
object PacketUtil {

    @JvmStatic
    fun isMovePacket(className: String): Boolean {
        return className.contains("MovePlayer") || className.contains("MoveEntity")
    }

    @JvmStatic
    fun isChatPacket(className: String): Boolean {
        return className.contains("Chat") || className.contains("SystemChat")
    }
}
