package so.aporia.module.impl.render
import so.aporia.utils.imports.*
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.entity.player.Player
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.settings.BooleanSetting
import so.aporia.module.settings.MultiSelectSetting
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.TickEvent
import so.aporia.utils.user.render.core.RenderFilter
import java.util.function.Predicate
import com.chaos.annotation.ChaosNative
@ChaosNative
class NoRender : Module("NoRender", Category.VISUAL) {

    val entities = MultiSelectSetting("Entities", "Hide entity types")
        .options("Items", "Paintings", "ArmorStands", "ExperienceOrbs")

    val overlays = MultiSelectSetting("Overlays", "Hide overlays")
        .options("Fire", "Water", "Pumpkin", "BossBar", "Scoreboard", "HeldItem")

    val effects = MultiSelectSetting("Effects", "Hide effects")
        .options("PotionHud", "PotionParticles", "PortalOverlay", "BlindnessOverlay")

    val armor = BooleanSetting("Armor", "Hide armor on players", false)

    val totemAnimation = BooleanSetting("Totem Animation", "Hide totem pop animation", false)

    val hurtAnimation = BooleanSetting("Hurt Animation", "Hide hurt flash on players", false)

    val hurtCam = BooleanSetting("Hurt Cam", "Hide hurt camera tilt", false)

    val deathScreen = BooleanSetting("Death Screen", "Hide death screen overlay", false)

    override val settings = listOf(entities, overlays, effects, armor, totemAnimation, hurtAnimation, hurtCam, deathScreen)

    private val entityFilter = Predicate<Entity> { entity ->
        shouldSkipEntity(entity)
    }

    override fun onEnable() {
        bus.register(this)
        pushState()
        RenderFilter.addFilter(entityFilter)
        logger.info("NoRender enabled")
    }

    override fun onDisable() {
        bus.unregister(this)
        RenderFilter.removeFilter(entityFilter)
        pushState()
        logger.info("NoRender disabled")
    }

    @EventHandler
    fun onTick(e: TickEvent) {
        pushState()
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
        @JvmField var hidePortal = false
        @JvmField var hideBlindness = false
        @JvmField var hideHurtCam = false
        @JvmField var hideDeathScreen = false

        @JvmField var hideItems = false
        @JvmField var hideExperienceOrbs = false

        fun shouldSkipEntity(entity: Entity): Boolean {
            if (!active) return false
            if (hideItems && entity is ItemEntity) return true
            if (hideExperienceOrbs && entity is ExperienceOrb) return true
            if (hideArmor && entity is ArmorStand) return true
            return false
        }

        private fun pushState() {
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
            hidePortal = active && m.effects.isSelected("PortalOverlay")
            hideBlindness = active && m.effects.isSelected("BlindnessOverlay")
            hideHurtCam = active && m.hurtCam.isEnabled
            hideDeathScreen = active && m.deathScreen.isEnabled
            hideItems = active && m.entities.isSelected("Items")
            hideExperienceOrbs = active && m.entities.isSelected("ExperienceOrbs")
        }
    }
}