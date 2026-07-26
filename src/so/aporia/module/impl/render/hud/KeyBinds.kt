package so.aporia.module.impl.render.hud

import so.aporia.utils.imports.*
import so.aporia.module.ModuleManager
import so.aporia.module.impl.render.Beautifully
import so.aporia.module.settings.BooleanSetting
import so.aporia.module.settings.Setting
import so.aporia.utils.user.input.KeyCodeMap
import so.aporia.utils.user.render.core.AporiaRenderer
import so.aporia.utils.user.render.font.Fonts
import com.chaos.annotation.ChaosNative

/**
 * Keybinds panel (left side) — ported from the Aporia HUD mockup.
 * Lists modules that have a keybind: status dot, name, key chip. Real data from ModuleManager.
 */
@ChaosNative
object KeyBinds {

    @JvmField var posX = 6f
    @JvmField var posY = 40f
    @JvmField var userMoved = false

    @JvmField var lastW = 0f
    @JvmField var lastH = 0f

    // ── settings (surfaced via the HUD module + the right-click popup) ──
    @JvmField val showAllBinds = BooleanSetting("Show All Binds", "Show bound modules that are currently off too", true)
    @JvmField val settings: List<Setting<*>> = listOf(showAllBinds)

    private const val HEADER_FS = 8f
    private const val NAME_FS = 10f
    private const val KEY_FS = 8.5f
    private const val DOT = 5f
    private const val DOT_GAP = 7f
    private const val ROW_H = 15f
    private const val ROW_GAP = 2f
    private const val GAP_NAME_KEY = 12f

    private fun keyLabel(code: Int): String = when {
        code >= 500 -> when (code) { 500 -> "LMB"; 501 -> "RMB"; 502 -> "MMB"; else -> "M${code - 497}" }
        else -> KeyCodeMap.getName(code)
    }

    @JvmStatic
    fun render(r: AporiaRenderer) {
        if (mc.player == null) return

        val binds = ModuleManager.getAll().filter {
            it.keybind != -1 && (showAllBinds.isEnabled || it.isEnabled)
        }
        if (binds.isEmpty()) { lastW = 0f; lastH = 0f; return }

        val blur = Beautifully.isBlurEnabled() && Beautifully.isFeatureEnabled("Keybinds Blur")

        // ── measure ──
        val header = "KEYBINDS"
        var contentW = r.getTextWidth(Fonts.BOLD, header, HEADER_FS)
        for (m in binds) {
            val keyW = r.getTextWidth(Fonts.BOLD, keyLabel(m.keybind), KEY_FS) + 10f
            val rowW = DOT + DOT_GAP + r.getTextWidth(Fonts.REGULAR, m.name, NAME_FS) + GAP_NAME_KEY + keyW
            if (rowW > contentW) contentW = rowW
        }
        val w = contentW + HudStyle.PAD * 2
        val headerH = HudStyle.PAD + HEADER_FS + 8f   // extra breathing room under the header
        val h = headerH + binds.size * (ROW_H + ROW_GAP) - ROW_GAP + HudStyle.PAD

        clampToScreen(w, h)
        val x = posX
        val y = posY
        lastW = w; lastH = h

        // ── panel ──
        HudStyle.panel(r, x, y, w, h, HudStyle.RADIUS, blur)
        r.drawText(Fonts.BOLD, header, x + HudStyle.PAD, y + HudStyle.PAD, HEADER_FS, HudStyle.header())

        var rowY = y + headerH
        val rowX = x + 4f
        val rowW = w - 8f
        for (m in binds) {
            if (m.isEnabled) r.drawRect(rowX, rowY, rowW, ROW_H, 6f, HudStyle.rowActiveBg())

            val dotY = rowY + (ROW_H - DOT) / 2f
            r.drawRect(rowX + 5f, dotY, DOT, DOT, DOT / 2f, if (m.isEnabled) HudStyle.accent() else HudStyle.dimDot())

            val nameX = rowX + 5f + DOT + DOT_GAP
            val nameY = rowY + (ROW_H - NAME_FS) / 2f
            r.drawText(Fonts.REGULAR, m.name, nameX, nameY, NAME_FS,
                if (m.isEnabled) HudStyle.text() else HudStyle.textDim())

            val keyStr = keyLabel(m.keybind)
            val keyW = r.getTextWidth(Fonts.BOLD, keyStr, KEY_FS) + 10f
            val keyX = x + w - HudStyle.PAD - keyW
            val keyY = rowY + (ROW_H - (KEY_FS + 6f)) / 2f
            HudStyle.chip(r, keyStr, keyX, keyY, KEY_FS, HudStyle.textDim(), HudStyle.chipBg())

            rowY += ROW_H + ROW_GAP
        }
    }

    /** Keep the panel on screen after a resize / drag. Left-anchored, so only clamp. */
    private fun clampToScreen(w: Float, h: Float) {
        val sw = mc.window.guiScaledWidth.toFloat()
        val sh = mc.window.guiScaledHeight.toFloat()
        posX = posX.coerceIn(0f, (sw - w).coerceAtLeast(0f))
        posY = posY.coerceIn(0f, (sh - h).coerceAtLeast(0f))
    }
}
