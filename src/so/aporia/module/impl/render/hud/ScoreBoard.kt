package so.aporia.module.impl.render.hud

import so.aporia.utils.imports.*
import net.minecraft.network.chat.numbers.StyledFormat
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.Objective
import net.minecraft.world.scores.PlayerScoreEntry
import net.minecraft.world.scores.PlayerTeam
import net.minecraft.world.scores.Scoreboard
import so.aporia.module.impl.render.Beautifully
import so.aporia.utils.user.render.core.AporiaRenderer
import so.aporia.utils.user.render.font.Fonts
import com.chaos.annotation.ChaosNative

/**
 * Scoreboard (right side) — real server sidebar objective, restyled to the Aporia glass look.
 * Replicates vanilla Hud.extractScoreboardSidebar data (team-override -> SIDEBAR slot, top 15,
 * value desc / owner asc). While drawn, [active] suppresses the vanilla sidebar.
 */
@ChaosNative
object ScoreBoard {

    /** True while we render our sidebar — read by vanilla Hud.java to hide its own. */
    @JvmField var active = false

    @JvmField var posX = 0f
    @JvmField var posY = 40f
    @JvmField var userMoved = false
    private var placed = false

    @JvmField var lastW = 0f
    @JvmField var lastH = 0f

    // ── settings ──
    @JvmField val showNumbers = so.aporia.module.settings.BooleanSetting("Show Numbers", "Show score values on the right", true)
    @JvmField val settings: List<so.aporia.module.settings.Setting<*>> = listOf(showNumbers)

    private const val TITLE_FS = 11f
    private const val ROW_FS = 10f
    private const val ROW_H = 13f
    private const val GAP = 14f

    private data class Row(val name: String, val score: String)

    private fun resolveObjective(scoreboard: Scoreboard): Objective? {
        try {
            val team = scoreboard.getPlayersTeam(mc.player!!.scoreboardName)
            if (team != null) {
                val color = team.color
                if (color.isPresent) {
                    val teamObjective = scoreboard.getDisplayObjective(color.get().displaySlot())
                    if (teamObjective != null) return teamObjective
                }
            }
        } catch (_: Exception) {}
        return scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR)
    }

    @JvmStatic
    fun render(r: AporiaRenderer) {
        if (mc.player == null || mc.level == null) { active = false; return }

        val scoreboard = mc.level!!.scoreboard
        val objective = resolveObjective(scoreboard)
        if (objective == null) { active = false; lastW = 0f; lastH = 0f; return }

        val fmt = objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT)
        val rows = scoreboard.listPlayerScores(objective)
            .filter { !it.isHidden }
            .sortedWith(compareByDescending<PlayerScoreEntry> { it.value() }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.owner() })
            .take(15)
            .map { score ->
                val team = scoreboard.getPlayersTeam(score.owner())
                Row(PlayerTeam.formatNameForTeam(team, score.ownerName()).string,
                    score.formatValue(fmt).string)
            }

        active = true

        val title = objective.displayName.string
        val blur = Beautifully.isBlurEnabled() && Beautifully.isFeatureEnabled("Scoreboard Blur")

        // ── measure ──
        var contentW = r.getTextWidth(Fonts.BOLD, title, TITLE_FS)
        for (row in rows) {
            val nameW = r.getTextWidth(Fonts.REGULAR, row.name, ROW_FS)
            val scoreW = r.getTextWidth(Fonts.BOLD, row.score, ROW_FS)
            val rowW = nameW + GAP + scoreW
            if (rowW > contentW) contentW = rowW
        }
        val w = (contentW + HudStyle.PAD * 2).coerceAtLeast(120f)
        val headerH = TITLE_FS + 12f
        val h = headerH + rows.size * ROW_H + HudStyle.PAD

        val sw = mc.window.guiScaledWidth.toFloat()
        val sh = mc.window.guiScaledHeight.toFloat()
        if (!placed || !userMoved) { posX = sw - w - 6f; placed = true }
        posX = posX.coerceIn(0f, (sw - w).coerceAtLeast(0f))
        posY = posY.coerceIn(0f, (sh - h).coerceAtLeast(0f))
        val x = posX
        val y = posY
        lastW = w; lastH = h

        // ── panel ──
        HudStyle.panel(r, x, y, w, h, HudStyle.RADIUS, blur)

        // header (centered) + divider
        val titleW = r.getTextWidth(Fonts.BOLD, title, TITLE_FS)
        r.drawText(Fonts.BOLD, title, x + (w - titleW) / 2f, y + HudStyle.PAD, TITLE_FS, HudStyle.text())
        r.drawRect(x + HudStyle.PAD, y + headerH - 3f, w - HudStyle.PAD * 2, 1f, 0f, HudStyle.divider())

        var rowY = y + headerH
        for (row in rows) {
            r.drawText(Fonts.REGULAR, row.name, x + HudStyle.PAD, rowY + (ROW_H - ROW_FS) / 2f, ROW_FS, HudStyle.textDim())
            if (showNumbers.isEnabled) {
                val scoreW = r.getTextWidth(Fonts.BOLD, row.score, ROW_FS)
                r.drawText(Fonts.BOLD, row.score, x + w - HudStyle.PAD - scoreW, rowY + (ROW_H - ROW_FS) / 2f, ROW_FS, HudStyle.accent())
            }
            rowY += ROW_H
        }
    }
}
