package so.aporia.module.impl.combat

import com.chaos.annotation.Obfuscate
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Items
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.settings.BooleanSetting
import so.aporia.module.settings.NumberSetting
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.TickEvent
import net.minecraft.client.Minecraft
import so.aporia.utils.imports.*
import so.aporia.utils.user.player.inventory.InventoryUtil

@Obfuscate
class AutoTotem : Module("AutoTotem", Category.COMBAT) {
    companion object {
        @JvmField val mc = Minecraft.getInstance()
    }


    val fallCheck: BooleanSetting
    val fallDistance: NumberSetting
    val crystalCheck: BooleanSetting
    val crystalRange: NumberSetting
    val hpCheck: BooleanSetting
    val hpThreshold: NumberSetting

    init {
        fallCheck = BooleanSetting("Fall Check", "Swap totem when falling from lethal height", true)
        fallDistance = NumberSetting("Fall Distance", "Minimum fall distance to trigger", 10.0, 3.0, 50.0, 1.0,
            { fallCheck.isEnabled })

        crystalCheck = BooleanSetting("Crystal Check", "Swap totem when near end crystal", true)
        crystalRange = NumberSetting("Crystal Range", "Max distance to crystal", 6.0, 1.0, 12.0, 1.0,
            { crystalCheck.isEnabled })

        hpCheck = BooleanSetting("HP Check", "Swap totem when health is low", true)
        hpThreshold = NumberSetting("HP Threshold", "Health threshold to trigger", 6.0, 1.0, 20.0, 1.0,
            { hpCheck.isEnabled })
    }

    override fun onEnable() {
        bus.register(this)
    }

    override fun onDisable() {
        bus.unregister(this)
    }

    @EventHandler
    fun onTick(event: TickEvent) {
        if (mc.player == null || mc.level == null) return

        if (InventoryUtil.offhandHas(Items.TOTEM_OF_UNDYING)) return

        if (!needsTotem()) return

        val totemSlot = InventoryUtil.findItemInInventory(Items.TOTEM_OF_UNDYING)
        if (totemSlot == -1) return

        InventoryUtil.swapToOffhand(totemSlot)
    }

    private fun needsTotem(): Boolean {
        if (hpCheck.isEnabled) {
            val hp = mc.player!!.health + mc.player!!.absorptionAmount
            if (hp <= hpThreshold.getFloat()) return true
        }

        if (fallCheck.isEnabled) {
            if (mc.player!!.fallDistance >= fallDistance.getFloat()) return true
        }

        if (crystalCheck.isEnabled) {
            val range = crystalRange.getFloat()
            for (entity in mc.level!!.entitiesForRendering()) {
                if (entity.type == EntityType.END_CRYSTAL
                    && mc.player!!.distanceTo(entity) <= range) {
                    return true
                }
            }
        }

        return false
    }
}
