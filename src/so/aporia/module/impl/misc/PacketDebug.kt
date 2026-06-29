package so.aporia.module.impl.misc

import com.chaos.annotation.Obfuscate
import net.minecraft.network.protocol.Packet
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.settings.BooleanSetting
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.PacketEvent
import so.aporia.utils.files.FilesManager
import so.aporia.utils.imports.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Obfuscate
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

    @EventHandler
    fun onPacket(event: PacketEvent) {
        val sf = sessionFile ?: return
        if (event.direction == PacketEvent.Direction.INBOUND && !logInbound.isEnabled) return
        if (event.direction == PacketEvent.Direction.OUTBOUND && !logOutbound.isEnabled) return
        val pkt = event.packet
        val dir = if (event.direction == PacketEvent.Direction.INBOUND) "IN " else "OUT"
        val time = LocalDateTime.now().format(TIME_FMT)
        val entry = "[$time] [$dir] ${pkt.javaClass.simpleName}\n  class: ${pkt.javaClass.name}\n  data : $pkt\n──────────────────────────────────────────\n"
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
}
