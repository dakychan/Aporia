package so.aporia.utils.events.impl

import net.minecraft.world.entity.LivingEntity

class LivingEntityRenderEvent(val entity: LivingEntity) {

    var cancelled = false
        private set

    fun cancel() { cancelled = true }
}
