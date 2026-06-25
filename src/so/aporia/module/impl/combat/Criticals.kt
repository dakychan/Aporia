package so.aporia.module.impl.combat

import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.settings.SelectSetting
import so.aporia.utils.events.EventBus
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.PacketEvent
import so.aporia.utils.events.impl.TickEvent

class Criticals : Module("Criticals", Category.COMBAT) {
    companion object {
        @JvmField val mc = Minecraft.getInstance()
    }


    val mode: SelectSetting

    private var critTicks = 0

    init {
        mode = SelectSetting("Mode", "Criticals mode")
            .value("Packet", "Jump")
            .selected("Packet")
    }

    override fun onEnable() {
        EventBus.register(this)
        critTicks = 0
    }

    override fun onDisable() {
        EventBus.unregister(this)
        critTicks = 0
    }

    @EventHandler
    fun onTick(event: TickEvent) {
        if (mc.player == null || mc.level == null) return

        if (critTicks > 0) {
            critTicks--
        }
    }

    @EventHandler
    fun onPacketSend(event: PacketEvent) {
        if (mc.player == null) return
        if (event.direction != PacketEvent.Direction.OUTBOUND) return

        if (mode.isSelected("Packet") && event.packet is ServerboundInteractPacket) {
            if (critTicks <= 0) {
                critTicks = 3
                val x = mc.player!!.x
                val y = mc.player!!.y
                val z = mc.player!!.z
                val yr = mc.player!!.yRot
                val xr = mc.player!!.xRot

                mc.player!!.connection.send(ServerboundMovePlayerPacket.PosRot(
                    x, y + 0.015625, z, yr, xr, false, false
                ))
                mc.player!!.connection.send(ServerboundMovePlayerPacket.PosRot(
                    x, y, z, yr, xr, false, false
                ))
            }
        }
    }
}
