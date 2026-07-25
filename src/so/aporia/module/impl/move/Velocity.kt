package so.aporia.module.impl.move
import so.aporia.utils.imports.*
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket
import net.minecraft.world.phys.Vec3
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.settings.BooleanSetting
import so.aporia.module.settings.SliderSetting
import so.aporia.module.settings.SelectSetting
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.PacketEvent
import com.chaos.annotation.ChaosNative
@ChaosNative
class Velocity : Module("Velocity", Category.MOVE) {
    companion object {
        @JvmField val mc = Minecraft.getInstance()
    }


    val mode: SelectSetting
    val factor: SliderSetting
    val fishingRod: BooleanSetting

    init {
        mode = SelectSetting("Mode", "Velocity mode")
            .value("Cancel", "Reduce", "Vertical")
            .selected("Cancel")

        factor = SliderSetting(
            "Factor", "Knockback reduction factor (0-100%)",
            0.0, 0.0, 100.0, 1.0,
            { mode.isSelected("Reduce") })

        fishingRod = BooleanSetting("Fishing Rod", "Also apply velocity to fishing rod knockback", false)
    }

    override val settings = listOf(mode, factor, fishingRod)

    override fun onEnable() {
        bus.register(this)
    }

    override fun onDisable() {
        bus.unregister(this)
    }

    @EventHandler
    fun onPacketReceive(event: PacketEvent) {
        if (mc.player == null) return
        if (event.direction != PacketEvent.Direction.INBOUND) return

        val packet = event.packet
        if (packet is ClientboundSetEntityMotionPacket) {
            if (packet.id != mc.player!!.id) return

            // Check if the knockback is from a fishing rod (small horizontal, no Y change)
            val vel = packet.movement
            val isFishingRod = vel.y == 0.0 && vel.length() > 0.0 && vel.length() < 0.5

            if (fishingRod.isEnabled && !isFishingRod) return
            if (!fishingRod.isEnabled && isFishingRod) return

            if (mode.isSelected("Cancel")) {
                event.cancel()
                return
            }

            if (mode.isSelected("Vertical")) {
                val old = packet.movement
                event.setPacket(ClientboundSetEntityMotionPacket(
                    packet.id, Vec3(old.x, 0.0, old.z)
                ))
            }

            if (mode.isSelected("Reduce")) {
                val f = factor.get() / 100.0
                val old = packet.movement
                event.setPacket(ClientboundSetEntityMotionPacket(
                    packet.id, old.multiply(f, f, f)
                ))
            }
        }
    }
}
