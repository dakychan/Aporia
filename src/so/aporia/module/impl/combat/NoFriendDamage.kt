package so.aporia.module.impl.combat

import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.world.entity.player.Player
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.utils.events.EventBus
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.PacketEvent
import so.aporia.utils.user.friend.FriendManager

class NoFriendDamage : Module("NoFriendDamage", Category.COMBAT) {
    companion object {
        @JvmField val mc = Minecraft.getInstance()
        private val entityIdField by lazy {
            val f = ServerboundInteractPacket::class.java.getDeclaredField("entityId")
            f.isAccessible = true
            f
        }
    }

    override fun onEnable() {
        EventBus.register(this)
    }

    override fun onDisable() {
        EventBus.unregister(this)
    }

    @EventHandler
    fun onPacket(event: PacketEvent) {
        if (event.direction() != PacketEvent.Direction.OUTBOUND) return
        if (mc.player == null || mc.level == null) return

        val packet = event.packet()
        if (packet is ServerboundInteractPacket) {
            val entityId = entityIdField.getInt(packet)
            val entity = mc.level!!.getEntity(entityId)
            if (entity is Player && FriendManager.isFriend(entity.name.string)) {
                event.cancel()
            }
        }
    }
}
