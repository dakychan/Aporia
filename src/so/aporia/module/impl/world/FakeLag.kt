package so.aporia.module.impl.world

import com.chaos.annotation.Obfuscate
import com.chaos.annotation.ChaosNative
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.settings.SelectSetting
import so.aporia.module.settings.SliderSetting
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.PacketEvent
import so.aporia.utils.events.impl.TickEvent
import so.aporia.utils.imports.*

/**
 * FakeLag — artificially freezes our outbound position on the server.
 *
 * Freeze cycle:
 *   1. FREEZING phase — cancel & queue position packets for [time] ms
 *   2. WAITING phase  — release packets (burst or sharp update), then wait [delay] ms
 *   3. Repeat
 *
 * Ghost position (serverPos*) is exposed for PlayerESP outline rendering.
 */
@Obfuscate
@ChaosNative
class FakeLag : Module("FakeLag", Category.WORLD) {

    val mode = SelectSetting("Mode", "Lagging = freeze then burst-release; FullyFreeze = freeze then one sharp update")
        .value("Lagging", "FullyFreeze").selected("Lagging")
    val time = SliderSetting("Time", "How long to freeze on the server (ms)", 500.0, 50.0, 3000.0, 10.0)
    val delay = SliderSetting("Delay", "Cooldown between freeze cycles (ms)", 0.0, 0.0, 1000.0, 10.0)

    override val settings = listOf(mode, time, delay)

    // ── Ghost position (server-side position) ──
    companion object {
        @JvmStatic var serverPosX = 0.0
        @JvmStatic var serverPosY = 0.0
        @JvmStatic var serverPosZ = 0.0
    }

    private val cached = ArrayList<ServerboundMovePlayerPacket>()
    private var windowStart = 0L
    private var flushing = false

    private enum class Phase { FREEZING, WAITING }
    private var phase = Phase.FREEZING

    override fun onEnable() {
        bus.register(this)
        cached.clear()
        windowStart = System.currentTimeMillis()
        phase = Phase.FREEZING
        val p = mc.player ?: return
        serverPosX = p.x; serverPosY = p.y; serverPosZ = p.z
    }

    override fun onDisable() {
        bus.unregister(this)
        if (phase == Phase.FREEZING) releasePackets()
        cached.clear()
    }

    @EventHandler
    fun onPacketSend(e: PacketEvent) {
        if (flushing) return
        if (e.direction() != PacketEvent.Direction.OUTBOUND) return
        if (mc.player == null) return
        val pkt = e.packet()
        if (pkt !is ServerboundMovePlayerPacket) return

        // Only freeze position-carrying packets (Pos / PosRot), never Rot or StatusOnly.
        if (!pkt.hasPosition()) return

        // Track server position for ghost rendering.
        serverPosX = pkt.getX(mc.player!!.x)
        serverPosY = pkt.getY(mc.player!!.y)
        serverPosZ = pkt.getZ(mc.player!!.z)

        if (phase == Phase.WAITING) return  // let packets flow during cooldown

        cached.add(pkt)
        e.cancel()
    }

    @EventHandler
    fun onTick(e: TickEvent) {
        if (mc.player == null) return
        val elapsed = System.currentTimeMillis() - windowStart
        when (phase) {
            Phase.FREEZING -> {
                if (elapsed >= time.get().toLong()) {
                    releasePackets()
                    cached.clear()
                    windowStart = System.currentTimeMillis()
                    phase = Phase.WAITING
                }
            }
            Phase.WAITING -> {
                if (elapsed >= delay.get().toLong()) {
                    windowStart = System.currentTimeMillis()
                    phase = Phase.FREEZING
                }
            }
        }
    }

    private fun releasePackets() {
        val p = mc.player ?: run { flushing = false; return }
        flushing = true
        try {
            if (mode.isSelected("Lagging")) {
                for (pkt in cached) p.connection.send(pkt)
            } else { // FullyFreeze
                p.connection.send(
                    ServerboundMovePlayerPacket.PosRot(p.x, p.y, p.z, p.yRot, p.xRot, p.onGround(), p.horizontalCollision)
                )
                serverPosX = p.x; serverPosY = p.y; serverPosZ = p.z
            }
        } catch (_: Exception) {}
        flushing = false
    }
}
