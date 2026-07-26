package so.aporia.module.impl.render.hud

import so.aporia.utils.imports.*
import net.minecraft.resources.Identifier
import net.minecraft.world.effect.MobEffectInstance
import so.aporia.module.impl.render.Beautifully
import so.aporia.module.settings.BooleanSetting
import so.aporia.module.settings.Setting
import so.aporia.utils.user.render.core.AporiaRenderer
import so.aporia.utils.user.render.font.Fonts
import com.chaos.annotation.ChaosNative

/**
 * Active potion effects (right side) — ported from the Aporia HUD mockup.
 * One glass pill per effect: the real Minecraft effect icon, name + level, remaining time.
 * While drawn, [active] suppresses the vanilla effect icons.
 */
@ChaosNative
object Potions {

    /** True while we render our effect pills — read by vanilla Hud.java to hide its icons. */
    @JvmField var active = false

    @JvmField var posX = 0f
    @JvmField var posY = 200f
    @JvmField var userMoved = false
    private var placed = false

    @JvmField var lastW = 0f
    @JvmField var lastH = 0f

    // ── settings ──
    @JvmField val showDuration = BooleanSetting("Show Duration", "Show the remaining time on each effect", true)
    @JvmField val settings: List<Setting<*>> = listOf(showDuration)

    private const val NAME_FS = 10.5f
    private const val TIME_FS = 9f
    private const val ICON = 15f
    private const val ICON_GAP = 8f
    private const val GAP_NAME_TIME = 10f
    private const val PILL_H = 22f
    private const val PILL_GAP = 6f
    private const val HPAD = 8f

    private data class Pot(val name: String, val time: String, val icon: Identifier?)

    private fun build(inst: MobEffectInstance): Pot {
        val effect = inst.effect.value()
        val level = HudStyle.roman(inst.amplifier)
        val name = effect.displayName.string + if (level.isNotEmpty()) " $level" else ""
        val time = if (!showDuration.isEnabled || inst.isInfiniteDuration) "" else HudStyle.duration(inst.duration)
        // Real MC effect icon: textures/mob_effect/<id>.png
        val icon = inst.effect.unwrapKey().map { it.identifier() }.orElse(null)?.let {
            Identifier.fromNamespaceAndPath(it.namespace, "textures/mob_effect/${it.path}.png")
        }
        return Pot(name, time, icon)
    }

    @JvmStatic
    fun render(r: AporiaRenderer) {
        if (mc.player == null) { active = false; return }

        val effects = mc.player!!.activeEffects
        if (effects.isEmpty()) { active = false; lastW = 0f; lastH = 0f; return }

        val pots = effects.sortedByDescending { it.duration }.map { build(it) }

        active = true
        val blur = Beautifully.isBlurEnabled() && Beautifully.isFeatureEnabled("Potions Blur")

        // ── measure widest pill ──
        var maxW = 0f
        val widths = FloatArray(pots.size)
        for (i in pots.indices) {
            val p = pots[i]
            val nameW = r.getTextWidth(Fonts.REGULAR, p.name, NAME_FS)
            val timeW = if (p.time.isEmpty()) 0f else GAP_NAME_TIME + r.getTextWidth(Fonts.BOLD, p.time, TIME_FS)
            val pw = HPAD + ICON + ICON_GAP + nameW + timeW + HPAD
            widths[i] = pw
            if (pw > maxW) maxW = pw
        }
        val h = pots.size * PILL_H + (pots.size - 1) * PILL_GAP

        placeAndClamp(maxW, h)
        val x = posX
        val y = posY
        lastW = maxW; lastH = h

        // ── pills (right-aligned within the drag box) ──
        val rightEdge = x + maxW
        var pillY = y
        for (i in pots.indices) {
            val p = pots[i]
            val pw = widths[i]
            val px = rightEdge - pw

            HudStyle.panel(r, px, pillY, pw, PILL_H, PILL_H / 2f, blur)

            val iconY = pillY + (PILL_H - ICON) / 2f
            if (p.icon != null) r.drawImage(px + HPAD, iconY, ICON, ICON, p.icon, 0f)

            val nameX = px + HPAD + ICON + ICON_GAP
            r.drawText(Fonts.REGULAR, p.name, nameX, pillY + (PILL_H - NAME_FS) / 2f, NAME_FS, HudStyle.text())

            if (p.time.isNotEmpty()) {
                val timeW = r.getTextWidth(Fonts.BOLD, p.time, TIME_FS)
                r.drawText(Fonts.BOLD, p.time, px + pw - HPAD - timeW, pillY + (PILL_H - TIME_FS) / 2f, TIME_FS, HudStyle.textDim())
            }
            pillY += PILL_H + PILL_GAP
        }
    }

    /** Right-anchored until the user drags it; always kept on screen after a resize. */
    private fun placeAndClamp(w: Float, h: Float) {
        val sw = mc.window.guiScaledWidth.toFloat()
        val sh = mc.window.guiScaledHeight.toFloat()
        if (!placed || !userMoved) { posX = sw - w - 6f; placed = true }
        posX = posX.coerceIn(0f, (sw - w).coerceAtLeast(0f))
        posY = posY.coerceIn(0f, (sh - h).coerceAtLeast(0f))
    }
}
