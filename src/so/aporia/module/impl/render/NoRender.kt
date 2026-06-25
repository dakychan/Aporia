package so.aporia.module.impl.render

import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.settings.BooleanSetting
import so.aporia.module.settings.MultiSelectSetting
import so.aporia.utils.events.EventBus
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.LivingEntityRenderEvent
import so.aporia.utils.user.logger.Logger

class NoRender : Module("NoRender", Category.VISUAL) {

    val entities = MultiSelectSetting("Entities", "Hide entity types")
        .options("Items", "Paintings", "ArmorStands", "ExperianceOrbs")

    val overlays = MultiSelectSetting("Overlays", "Hide overlays")
        .options("Fire", "Water", "Pumpkin", "BossBar", "Scoreboard", "HeldItem")

    val effects = MultiSelectSetting("Effects", "Hide potion effects")
        .options("PotionHud", "PotionParticles")

    val armor = BooleanSetting("Armor", "Hide armor on players", false) {
        overlays.isSelected("HeldItem")
    }

    val totemAnimation = BooleanSetting("Totem Animation", "Hide totem pop animation", false)

    val hurtAnimation = BooleanSetting("Hurt Animation", "Hide hurt flash on players", false)

    override fun onEnable() {
        EventBus.register(this)
        Logger.info("NoRender enabled")
        updateActive()
    }

    override fun onDisable() {
        EventBus.unregister(this)
        Logger.info("NoRender disabled")
        updateActive()
    }

    @EventHandler
    fun onEntityRender(e: LivingEntityRenderEvent) {
        if (entities.isSelected("Items") || entities.isSelected("ArmorStands")) {
            e.cancel()
        }
    }

    companion object {
        @JvmField var active = false
        @JvmField var hideFire = false
        @JvmField var hideWater = false
        @JvmField var hidePumpkin = false
        @JvmField var hideBossBar = false
        @JvmField var hideScoreboard = false
        @JvmField var hideHeldItem = false
        @JvmField var hideArmor = false
        @JvmField var hideTotem = false
        @JvmField var hideHurt = false
        @JvmField var hidePotionHud = false
        @JvmField var hidePotionParticles = false

        private fun updateActive() {
            val m = so.aporia.module.ModuleManager.get("NoRender") as? NoRender ?: return
            active = m.isEnabled
            hideFire = active && m.overlays.isSelected("Fire")
            hideWater = active && m.overlays.isSelected("Water")
            hidePumpkin = active && m.overlays.isSelected("Pumpkin")
            hideBossBar = active && m.overlays.isSelected("BossBar")
            hideScoreboard = active && m.overlays.isSelected("Scoreboard")
            hideHeldItem = active && m.overlays.isSelected("HeldItem")
            hideArmor = active && m.armor.isEnabled
            hideTotem = active && m.totemAnimation.isEnabled
            hideHurt = active && m.hurtAnimation.isEnabled
            hidePotionHud = active && m.effects.isSelected("PotionHud")
            hidePotionParticles = active && m.effects.isSelected("PotionParticles")
        }
    }
}
