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
import so.aporia.utils.user.render.avatar.AvatarRenderer
import so.aporia.utils.user.render.font.Fonts
import com.chaos.annotation.ChaosNative
@ChaosNative
object TargetHud {

    // Compact horizontal pill (ported from the Aporia HUD mockup): [skin] name [hp bar] hp
    const val H = 26f
    const val PAD = 5f
    const val GAP = 7f
    const val NAME_FS = 11f
    const val HP_FS = 10.5f
    const val BAR_W = 62f
    const val BAR_H = 4f

    @JvmField var W = 120f            // updated each render for the drag hit-test
    @JvmField var posX = 4f
    @JvmField var posY = 56f

    private val C_NAME = colorUtil.rgba(255, 255, 255, 255)
    private val C_GREEN = colorUtil.rgba(85, 255, 85, 255)
    private val C_YELLOW = colorUtil.rgba(255, 255, 85, 255)
    private val C_RED = colorUtil.rgba(255, 85, 85, 255)
    private val C_BAR_BG = colorUtil.rgba(0, 0, 0, 110)

    @JvmStatic
    fun render(gfx: GuiGraphicsExtractor) {
        if (mc.player == null || mc.level == null) return
        val target = resolveTarget() ?: return
        if (target.isDeadOrDying) return

        val blur = Beautifully.isBlurEnabled() && Beautifully.isFeatureEnabled("Target HUD Blur")

        val name = target.name.string
        val health = (target.health + target.absorptionAmount).coerceAtLeast(0f)
        val maxHealth = target.maxHealth.coerceAtLeast(1f)
        val healthPct = (health / maxHealth).coerceIn(0f, 1f)
        val healthColor = when { health > 10f -> C_GREEN; health > 6f -> C_YELLOW; else -> C_RED }
        val hpText = String.format("%.1f", health)

        val avatar = H - PAD * 2
        val nameW = r.getTextWidth(Fonts.BOLD, name, NAME_FS)
        val hpW = r.getTextWidth(Fonts.BOLD, hpText, HP_FS)
        val w = PAD + avatar + GAP + nameW + GAP + BAR_W + GAP + hpW + PAD
        W = w

        val x = posX
        val y = posY
        HudStyle.panel(r, x, y, w, H, H / 2f, blur)

        // avatar — the TARGET's skin (not ours)
        val ax = x + PAD
        val ay = y + PAD
        val skinId = (target as? Player)?.let { AvatarRenderer.getSkinTargetFromMinecraft(it) }
        if (skinId != null) AvatarRenderer.drawSkin(r, ax, ay, avatar, avatar / 2f, skinId, blur)
        else r.drawRect(ax, ay, avatar, avatar, avatar / 2f, colorUtil.rgba(30, 34, 40, 255))

        // name
        val nameX = ax + avatar + GAP
        r.drawText(Fonts.BOLD, name, nameX, y + (H - NAME_FS) / 2f, NAME_FS, C_NAME)

        // inline hp bar
        val barX = nameX + nameW + GAP
        val barY = y + (H - BAR_H) / 2f
        r.drawRect(barX, barY, BAR_W, BAR_H, BAR_H / 2f, C_BAR_BG)
        r.drawRect(barX, barY, BAR_W * healthPct, BAR_H, BAR_H / 2f, healthColor)

        // hp number
        val hpX = barX + BAR_W + GAP
        r.drawText(Fonts.BOLD, hpText, hpX, y + (H - HP_FS) / 2f, HP_FS, healthColor)
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
