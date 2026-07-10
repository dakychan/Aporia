package so.aporia.utils.user.whois

import com.chaos.annotation.ChaosNative
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket
import net.minecraft.network.protocol.common.custom.BrandPayload
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.PacketEvent
import so.aporia.utils.imports.*
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@ChaosNative
object WhoIs {

    private const val APORIA_BRAND = "aporia"
    private val known = ConcurrentHashMap<UUID, String>()

    var ourUUID: UUID? = null
        private set

    fun init() {
        bus.register(this)
    }

    fun shutdown() {
        bus.unregister(this)
        known.clear()
    }

    fun isAperiaUser(uuid: UUID): Boolean = known.containsKey(uuid)

    fun addKnown(uuid: UUID, username: String) {
        known[uuid] = username
    }

    fun removeKnown(uuid: UUID) {
        known.remove(uuid)
    }

    @EventHandler
    fun onPacket(event: PacketEvent) {
        if (event.direction != PacketEvent.Direction.INBOUND) return
        val pkt = event.packet
        if (pkt !is ClientboundCustomPayloadPacket) return
        val payload = pkt.payload
        if (payload is BrandPayload && payload.brand.equals(APORIA_BRAND, true)) {
            // server brand detected as Aporia
        }
    }
}
