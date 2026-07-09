package so.aporia.module.impl.misc
import com.chaos.annotation.Obfuscate
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundCommandsPacket
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket
import net.minecraft.network.protocol.common.custom.BrandPayload
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.settings.BooleanSetting
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.PacketEvent
import so.aporia.utils.files.FilesManager
import so.aporia.utils.imports.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.chaos.annotation.ChaosNative
@Obfuscate
@ChaosNative
class PacketDebug : Module("PacketDebug", Category.MISC) {

    val logInbound = BooleanSetting("Log Inbound", "Входящие пакеты", true)
    val logOutbound = BooleanSetting("Log Outbound", "Исходящие пакеты", true)

    private var sessionFile: java.nio.file.Path? = null
    private var packetCount = 0L

    override fun onEnable() {
        packetCount = 0
        val ts = LocalDateTime.now().format(FILE_FMT)
        sessionFile = FilesManager.ROOT.resolve("packets/parse_$ts.log")
        try {
            FilesManager.append(sessionFile!!, "=== PacketDebug Session: $ts ===\n\n")
            logger.success("[PacketDebug] ENABLED → ${sessionFile!!.fileName}")
        } catch (e: Exception) {
            logger.error("[PacketDebug] init failed: ${e.message}")
            sessionFile = null
            return
        }
        bus.register(this)
    }

    override fun onDisable() {
        bus.unregister(this)
        try {
            val sf = sessionFile ?: return
            FilesManager.append(sf, "\n=== Session ended ($packetCount packets) ===\n")
            logger.success("[PacketDebug] DISABLED — logged $packetCount packets")
        } catch (e: Exception) {
            logger.error("[PacketDebug] disable failed: ${e.message}")
        }
        sessionFile = null
    }

    private fun dumpCommands(pkt: ClientboundCommandsPacket): String {
        try {
            val field = ClientboundCommandsPacket::class.java.getDeclaredField("entries")
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val entries = field.get(pkt) as List<*>
            val names = mutableListOf<String>()

            for (entry in entries) {
                val e = entry ?: continue
                val stubField = e::class.java.getDeclaredField("stub")
                stubField.isAccessible = true
                val stub = stubField.get(e)
                if (stub != null) {
                    val idField = stub::class.java.getDeclaredField("id")
                    idField.isAccessible = true
                    val id = idField.get(stub) as? String
                    if (id != null) names.add(id)
                }
            }

            val sorted = names.sorted().distinct()
            return "Commands[${sorted.size}]: ${sorted.joinToString(", ")}"
        } catch (e: Exception) {
            return "CommandsPacket (parse error: ${e.message})"
        }
    }

    private fun packetData(pkt: Packet<*>): String {
        return when (pkt) {
            is ClientboundCommandsPacket -> dumpCommands(pkt)
            is ServerboundCommandSuggestionPacket -> {
                "cmd=\"${pkt.command}\""
            }
            is ClientboundSystemChatPacket -> {
                "msg=\"${pkt.content.getString()}\""
            }
            is ClientboundCustomPayloadPacket -> {
                val pl = pkt.payload()
                val chan = pl.type().id()
                if (pl is BrandPayload) "BrandPayload: \"${pl.brand}\"" else "CustomPayload: $chan"
            }
            else -> pkt.toString()
        }
    }

    @EventHandler
    fun onPacket(event: PacketEvent) {
        val sf = sessionFile ?: return
        if (event.direction == PacketEvent.Direction.INBOUND && !logInbound.isEnabled) return
        if (event.direction == PacketEvent.Direction.OUTBOUND && !logOutbound.isEnabled) return
        val pkt = event.packet
        val dir = if (event.direction == PacketEvent.Direction.INBOUND) "IN " else "OUT"
        val time = LocalDateTime.now().format(TIME_FMT)
        val data = packetData(pkt)
        val entry = "[$time] [$dir] ${pkt.javaClass.simpleName}\n  class: ${pkt.javaClass.name}\n  data : $data\n──────────────────────────────────────────\n"
        try {
            FilesManager.append(sf, entry)
            packetCount++
        } catch (e: Exception) {
            logger.error("[PacketDebug] write failed: ${e.message}")
        }
    }

    companion object {
        private val TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
        private val FILE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
    }

    override val settings = listOf(logInbound, logOutbound)
}