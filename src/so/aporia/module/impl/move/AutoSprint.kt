package so.aporia.module.impl.move

import com.chaos.annotation.Obfuscate
import net.minecraft.client.Minecraft
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.utils.events.EventBus
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.TickEvent

@Obfuscate
class AutoSprint : Module("AutoSprint", Category.MOVE) {
    companion object {
        @JvmField val mc = Minecraft.getInstance()
    }


    override fun onEnable() {
        EventBus.register(this)
    }

    override fun onDisable() {
        EventBus.unregister(this)
        if (mc.player != null) {
            mc.player!!.setSprinting(false)
        }
    }

    @EventHandler
    fun onTick(event: TickEvent) {
        if (mc.player == null) return

        if (isMovingForward()
            && !mc.player!!.isUsingItem()
            && mc.player!!.foodData.foodLevel > 6
            && !mc.player!!.isSwimming
            && !mc.player!!.isPassenger) {

            mc.player!!.setSprinting(true)
        }
    }

    private fun isMovingForward(): Boolean {
        return mc.player!!.zza > 0
    }
}
