package so.aporia.module.impl.misc

import com.chaos.annotation.Obfuscate
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket
import net.minecraft.world.entity.Relative
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.settings.BooleanSetting
import so.aporia.utils.events.EventBus
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.PacketEvent

@Obfuscate
class Disabler : Module("Disabler", Category.MISC) {

    val noRotation = BooleanSetting("NoRotation", "Не сбрасывать ротацию/позицию при респавне", true)
    val noVelocityReset = BooleanSetting("NoVelocityReset", "Не сбрасывать скорость при смене геймода", false)
    val noSmallMove = BooleanSetting("NoSmallMove", "Отменять микро-телепорты игрока (<0.03 блока)", false)
    val noPlayerMove = BooleanSetting("NoPlayerMove", "Не давать серверу двигать игрока через пакеты энтити", false)
    val noEntityMove = BooleanSetting("NoEntityMove", "Блокировать любое движение энтити от сервера", false)

    override fun onEnable() { EventBus.register(this) }
    override fun onDisable() { EventBus.unregister(this) }

    @EventHandler
    fun onPacketReceive(event: PacketEvent) {
        val mc = Minecraft.getInstance()
        if (mc.player == null || mc.level == null) return
        if (event.direction != PacketEvent.Direction.INBOUND) return

        val packet = event.packet

        if (packet is ClientboundPlayerPositionPacket && noSmallMove.isEnabled) {
            val p = mc.player!!
            val oldX = p.x; val oldY = p.y; val oldZ = p.z
            val change = packet.change()
            val relatives = packet.relatives()
            var newX = change.position().x
            var newY = change.position().y
            var newZ = change.position().z
            if (relatives.contains(Relative.X)) newX += oldX
            if (relatives.contains(Relative.Y)) newY += oldY
            if (relatives.contains(Relative.Z)) newZ += oldZ
            val dx = newX - oldX; val dy = newY - oldY; val dz = newZ - oldZ
            if (Math.abs(dx) < 0.03125 && Math.abs(dy) < 0.015625 && Math.abs(dz) < 0.03125) {
                event.cancel()
                return
            }
        }

        if (noEntityMove.isEnabled) {
            if (packet is ClientboundMoveEntityPacket || packet is ClientboundTeleportEntityPacket || packet is ClientboundEntityPositionSyncPacket) {
                event.cancel()
                return
            }
        } else if (noPlayerMove.isEnabled) {
            val playerId = mc.player!!.id
            when (packet) {
                is ClientboundTeleportEntityPacket -> {
                    if (packet.id() == playerId) { event.cancel(); return }
                }
                is ClientboundEntityPositionSyncPacket -> {
                    if (packet.id() == playerId) { event.cancel(); return }
                }
                is ClientboundMoveEntityPacket -> {
                    val entity = packet.getEntity(mc.level!!)
                    if (entity != null && entity.id == playerId) { event.cancel(); return }
                }
            }
        }
    }

    companion object {
        private var instance: Disabler? = null

        @JvmStatic fun isNoRotationActive(): Boolean =
            instance != null && instance!!.isEnabled && instance!!.noRotation.isEnabled

        @JvmStatic fun isNoVelocityResetActive(): Boolean =
            instance != null && instance!!.isEnabled && instance!!.noVelocityReset.isEnabled

        @JvmStatic fun isNoPlayerMoveActive(): Boolean =
            instance != null && instance!!.isEnabled && instance!!.noPlayerMove.isEnabled
    }

    init { instance = this }
}
