package so.aporia.module.impl.move

import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket
import net.minecraft.world.phys.Vec3
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.settings.NumberSetting
import so.aporia.module.settings.SelectSetting
import so.aporia.utils.events.EventBus
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.PacketEvent

class Velocity : Module("Velocity", Category.MOVE) {
    companion object {
        @JvmField val mc = Minecraft.getInstance()
    }


    val mode: SelectSetting
    val factor: NumberSetting

    init {
        mode = SelectSetting("Mode", "Velocity mode")
            .value("Cancel", "Reduce", "Vertical")
            .selected("Cancel")

        factor = NumberSetting(
            "Factor", "Knockback reduction factor (0-100%)",
            0.0, 0.0, 100.0, 1.0,
            { mode.isSelected("Reduce") })
    }

    override fun onEnable() {
        EventBus.register(this)
    }

    override fun onDisable() {
        EventBus.unregister(this)
    }

    @EventHandler
    fun onPacketReceive(event: PacketEvent) {
        if (mc.player == null) return
        if (event.direction != PacketEvent.Direction.INBOUND) return

        val packet = event.packet
        if (packet is ClientboundSetEntityMotionPacket) {
            if (packet.id != mc.player!!.id) return

            if (mode.isSelected("Cancel")) {
                event.cancel()
                return
            }

            if (mode.isSelected("Vertical")) {
                val old = packet.getMovement()
                event.setPacket(ClientboundSetEntityMotionPacket(
                    packet.id, Vec3(old.x, 0.0, old.z)
                ))
                return
            }

            if (mode.isSelected("Reduce")) {
                val f = factor.get() / 100.0
                val old = packet.getMovement()
                event.setPacket(ClientboundSetEntityMotionPacket(
                    packet.id, old.multiply(f, f, f)
                ))
            }
        }
    }
}
