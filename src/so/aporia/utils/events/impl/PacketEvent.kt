package so.aporia.utils.events.impl
import net.minecraft.network.protocol.Packet
import com.chaos.annotation.ChaosNative
@ChaosNative
class PacketEvent(packet: Packet<*>, val direction: Direction) {

    enum class Direction { INBOUND, OUTBOUND }

    var packet: Packet<*> = packet
        private set

    var cancelled = false
        private set

    fun cancel() { cancelled = true }

    fun direction(): Direction = direction
    fun packet(): Packet<*> = packet
    fun isCancelled(): Boolean = cancelled

    fun setCancelled(cancelled: Boolean) { this.cancelled = cancelled }
    fun setPacket(packet: Packet<*>) { this.packet = packet }
}