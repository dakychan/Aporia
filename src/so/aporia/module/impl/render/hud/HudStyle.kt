package so.aporia.module.impl.render.hud

import so.aporia.utils.imports.*
import so.aporia.utils.user.render.core.AporiaRenderer
import so.aporia.utils.user.render.font.Fonts
import com.chaos.annotation.ChaosNative

/**
 * Shared visual language for the Aporia HUD panels (ported from the Aporia HUD mockup):
 * translucent "glass" backgrounds over Kawase blur, a hairline border, theme-driven accent,
 * and small rounded chips. Keeps DynamicIsland / TargetHud / KeyBinds / ScoreBoard / Potions
 * looking like one system.
 */
@ChaosNative
object HudStyle {

    const val RADIUS = 12f
    const val PILL_RADIUS = 100f
    const val PAD = 8f

    // ── palette (follows the active theme) ──
    fun accent(): Int = theme.guiEnabledDot
    fun header(): Int = colorUtil.withAlpha(theme.guiTitleText, 190)
    fun text(): Int = theme.guiModuleText
    fun textDim(): Int = colorUtil.withAlpha(theme.guiModuleText, 170)
    fun dimDot(): Int = theme.guiDisabledDot
    fun rowActiveBg(): Int = colorUtil.rgba(255, 255, 255, 26)
    fun chipBg(): Int = colorUtil.rgba(255, 255, 255, 30)
    fun divider(): Int = colorUtil.rgba(255, 255, 255, 30)

    fun glassBg(blur: Boolean): Int = if (blur) colorUtil.rgba(0, 0, 0, 92) else colorUtil.rgba(12, 18, 22, 210)
    private fun borderColor(): Int = colorUtil.rgba(255, 255, 255, 18)

    /** Glass panel background + hairline border. */
    fun panel(r: AporiaRenderer, x: Float, y: Float, w: Float, h: Float, radius: Float, blur: Boolean) {
        if (blur) r.drawRectBlurred(x, y, w, h, radius, glassBg(true))
        else r.drawRect(x, y, w, h, radius, glassBg(false))
        r.drawStroke(x, y, w, h, radius, 1f, 0, 0f, borderColor())
    }

    /** Small rounded chip (key labels, tags). Returns its width. */
    fun chip(r: AporiaRenderer, text: String, x: Float, y: Float, fs: Float, fg: Int, bg: Int): Float {
        val tw = r.getTextWidth(Fonts.BOLD, text, fs)
        val w = tw + 10f
        val h = fs + 6f
        r.drawRect(x, y, w, h, 5f, bg)
        r.drawText(Fonts.BOLD, text, x + 5f, y + 3f, fs, fg)
        return w
    }

    /** Roman numeral suffix for a 0-based amplifier (0 -> "", 1 -> "II" ...). */
    fun roman(amplifier: Int): String = when (amplifier) {
        0 -> ""
        1 -> "II"; 2 -> "III"; 3 -> "IV"; 4 -> "V"; 5 -> "VI"
        6 -> "VII"; 7 -> "VIII"; 8 -> "IX"; 9 -> "X"
        else -> (amplifier + 1).toString()
    }

    /** mm:ss for a tick duration; empty for infinite. */
    fun duration(ticks: Int): String {
        if (ticks < 0) return ""
        val s = ticks / 20
        return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
    }
}
