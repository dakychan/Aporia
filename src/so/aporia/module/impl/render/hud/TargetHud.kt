package so.aporia.module.impl.render.hud
import so.aporia.utils.imports.*
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import so.aporia.module.ModuleManager
import so.aporia.module.impl.combat.Aura
import so.aporia.module.impl.combat.TPAura
import so.aporia.module.impl.render.Beautifully
import com.chaos.annotation.ChaosNative
@ChaosNative
object TargetHud {

    const val W = 160f
    const val H = 48f
    const val PAD = 8f
    const val MARGIN = 4f
    const val FS = 12f
    const val HEALTH_FS = 16f
    const val RADIUS = 8f
    @JvmField var posX = MARGIN
    @JvmField var posY = 56f
    val C_BG = colorUtil.rgba(12, 18, 22, 200)
    val C_ACCENT = colorUtil.rgba(80, 200, 200, 255)
    val C_NAME = colorUtil.rgba(255, 255, 255, 255)
    val C_DIST = colorUtil.rgba(180, 180, 190, 200)
    val C_HEALTH_GREEN = colorUtil.rgba(85, 255, 85, 255)
    val C_HEALTH_YELLOW = colorUtil.rgba(255, 255, 85, 255)
    val C_HEALTH_RED = colorUtil.rgba(255, 85, 85, 255)
    val C_BAR_BG = colorUtil.rgba(0, 0, 0, 100)

    @JvmStatic
    fun render(gfx: GuiGraphicsExtractor) {
        if (mc.player == null || mc.level == null) return

        val target = resolveTarget() ?: return
        if (target.isDeadOrDying) return
        val blur = Beautifully.isBlurEnabled() && Beautifully.isFeatureEnabled("Target HUD Blur")

        val x = posX
        val y = posY

        if (blur) r.drawRectBlurred(x, y, W, H, RADIUS, C_BG)
        else r.drawRect(x, y, W, H, RADIUS, C_BG)

        val name = target.name.string
        val health = (target.health + target.absorptionAmount).coerceAtLeast(0f)
        val maxHealth = target.maxHealth.coerceAtLeast(1f)
        val healthPct = (health / maxHealth).coerceIn(0f, 1f)

        val healthColor = when {
            health > 10 -> C_HEALTH_GREEN
            health > 5 -> C_HEALTH_YELLOW
            else -> C_HEALTH_RED
        }

        val avatarSize = H - PAD * 2
        val avatarX = x + PAD
        val avatarY = y + PAD
        r.drawRect(avatarX, avatarY, avatarSize, avatarSize, avatarSize / 2f, colorUtil.rgba(30, 34, 40, 255))
        val skinId = (target as? Player)?.let { mc.getSkinManager().createLookup(it.gameProfile, false).get().body().texturePath() }
        if (skinId != null) {
            r.drawImageCropped(avatarX, avatarY, avatarSize, avatarSize, skinId, avatarSize / 2f, 8f/64f, 8f/64f, 16f/64f, 16f/64f)
        }

        val textX = avatarX + avatarSize + PAD
        val rightArea = x + W - PAD

        r.drawText("bold", name, textX, y + PAD, FS, C_NAME)

        val dist = mc.player!!.distanceTo(target).toInt()
        val distText = "${dist}m"
        val distW = r.getTextWidth("regular", distText, 10f)
        r.drawText("regular", distText, rightArea - distW, y + PAD, 10f, C_DIST)

        val hpText = String.format("%.1f", health)
        val hpW = r.getTextWidth("bold", hpText, HEALTH_FS)
        r.drawText("bold", hpText, rightArea - hpW, y + H - PAD - HEALTH_FS, HEALTH_FS, healthColor)

        val barY = y + H - PAD - 4f
        val barW = rightArea - textX
        val barH = 4f
        r.drawRect(textX, barY, barW, barH, 2f, C_BAR_BG)
        r.drawRect(textX, barY, barW * healthPct, barH, 2f, healthColor)
    }

    private fun resolveTarget(): LivingEntity? {
        try {
            val aura = ModuleManager.get("Aura") as? Aura
            if (aura != null && aura.isEnabled) {
                val f = aura.javaClass.getDeclaredField("lockedTarget")
                f.isAccessible = true
                val target = f.get(aura) as? Entity
                if (target is LivingEntity) return target
            }
        } catch (_: Exception) {}
        try {
            val tpAura = ModuleManager.get("TPAura") as? TPAura
            if (tpAura != null && tpAura.isEnabled) {
                val f = tpAura.javaClass.getDeclaredField("lockedTarget")
                f.isAccessible = true
                val target = f.get(tpAura) as? Entity
                if (target is LivingEntity) return target
            }
        } catch (_: Exception) {}
        var closest: Player? = null
        var closestDist = Double.MAX_VALUE
        for (entity in mc.level!!.entitiesForRendering()) {
            if (entity !is Player) continue
            if (entity == mc.player || entity == mc.cameraEntity) continue
            val dist = mc.player!!.distanceTo(entity)
            if (dist < closestDist) {
                closestDist = dist.toDouble()
                closest = entity
            }
        }
        return closest
    }
}