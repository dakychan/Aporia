package so.aporia.module.impl.render.hud

import so.aporia.utils.imports.*
import so.aporia.module.impl.render.Beautifully
import so.aporia.utils.events.StateMachineEngine
import so.aporia.utils.music.MusicControl
import so.aporia.utils.user.render.avatar.AvatarRenderer
import so.aporia.utils.user.render.core.AporiaRenderer
import so.aporia.utils.user.render.font.Fonts
import so.aporia.utils.user.render.animation.SpringSimulator
import com.chaos.annotation.ChaosNative

@ChaosNative
object DynamicIsland {

    /** Visual state of the island, driven each frame and held by our StateMachine. */
    enum class IslandState { IDLE, MUSIC, EXPANDED }
    private val sm = StateMachineEngine(this, IslandState::class, IslandState.IDLE)

    @JvmField var posX = 0f
    @JvmField var posY = 4f

    private var pillRect = floatArrayOf(0f, 0f, 0f, 0f)

    private const val PILL_W = 173f
    private const val PILL_H = 24f
    private const val PILL_RADIUS = 12f
    private const val AVATAR_SIZE = 18f
    private const val AVATAR_RADIUS = 9f
    private const val TITLE_FS = 10f
    private const val SUB_FS = 7.5f
    private const val PROGRESS_W = 106f
    private const val PAD = 6f
    private const val MAX_TITLE_W = 120f

    private val widthSpring = SpringSimulator(300f, 26f, PILL_W)
    private val heightSpring = SpringSimulator(300f, 26f, PILL_H)
    private var lastAnimTick = System.currentTimeMillis()
    private const val EXPAND_H = 6f

    @JvmStatic
    fun render(r: AporiaRenderer, mode: AvatarRenderer.Mode) {
        if (mc.player == null) return

        val blur = Beautifully.isBlurEnabled() && Beautifully.isFeatureEnabled("Dynamic Island Blur")
        val sw = mc.window.guiScaledWidth.toFloat()

        // Hover test against the previous frame's pill rect (only meaningful when a cursor is free).
        val mmx = mc.mouseHandler.getScaledXPos(mc.window).toFloat()
        val mmy = mc.mouseHandler.getScaledYPos(mc.window).toFloat()
        val hover = pillRect[2] > 0f && mmx >= pillRect[0] && mmx < pillRect[0] + pillRect[2] &&
            mmy >= pillRect[1] && mmy < pillRect[1] + pillRect[3]

        // ── media via the unified MusicControl facade (times in SECONDS; local or OS) ──
        val hasTrack = MusicControl.hasMedia()
        var song = ""; var artist = ""
        if (hasTrack) {
            val t = MusicControl.mediaTitle() ?: ""
            val a = MusicControl.mediaArtist()
            when {
                !a.isNullOrEmpty() -> { song = t; artist = a }
                t.contains(" - ") -> { val i = t.indexOf(" - "); artist = t.substring(0, i); song = t.substring(i + 3) }
                else -> song = t
            }
        }
        val posU = MusicControl.mediaPositionSeconds()
        val durU = MusicControl.mediaDurationSeconds()
        val playing = MusicControl.mediaPlaying()
        val showTimes = durU > 0L

        // Drive the island state machine: no track -> IDLE, hovered -> EXPANDED, else -> MUSIC.
        sm.currentState = when {
            !hasTrack -> IslandState.IDLE
            hover -> IslandState.EXPANDED
            else -> IslandState.MUSIC
        }
        val expanded = sm.isIn(IslandState.EXPANDED)

        val titleStr = if (hasTrack) truncateText(r, song, MAX_TITLE_W, TITLE_FS) else locale.get("watermark.name")
        val subStr = if (hasTrack && artist.isNotEmpty()) truncateText(r, artist, MAX_TITLE_W, SUB_FS) else ""
        val titleW = r.getTextWidth("regular", titleStr, TITLE_FS)
        val subW = if (subStr.isEmpty()) 0f else r.getTextWidth("regular", subStr, SUB_FS)
        val midW = maxOf(titleW, subW)

        // ── morph width (compact fits content, hover expands to show the progress detail) ──
        val leftW = PAD + AVATAR_SIZE + PAD
        val targetW = if (expanded) leftW + midW + 12f + PROGRESS_W + PAD
                      else (leftW + midW + PAD).coerceAtLeast(96f)

        val now = System.currentTimeMillis()
        val dt = (now - lastAnimTick).coerceIn(1L, 50L) / 1000f
        lastAnimTick = now
        widthSpring.setTarget(targetW); widthSpring.update(dt)
        heightSpring.setTarget(if (expanded) PILL_H + EXPAND_H else PILL_H); heightSpring.update(dt)
        val animW = widthSpring.value().coerceAtLeast(leftW + 12f)
        val animH = heightSpring.value().coerceAtLeast(PILL_H)

        val cx = (sw - animW) / 2f
        val y = posY
        pillRect = floatArrayOf(cx, y, animW, animH)

        // Background (shared glass style)
        HudStyle.panel(r, cx, y, animW, animH, PILL_RADIUS, blur)

        // Avatar: song cover art (OS media) takes priority, otherwise the resolved user avatar.
        val avatarX = cx + PAD
        val avatarY = y + (animH - AVATAR_SIZE) / 2f
        val artId = MusicControl.mediaArtworkId()
        if (artId != null) AvatarRenderer.drawCircle(r, avatarX, avatarY, AVATAR_SIZE, AVATAR_RADIUS, artId, blur)
        else AvatarRenderer.draw(r, avatarX, avatarY, AVATAR_SIZE, AVATAR_RADIUS, blur, mode)

        // Title (+ artist below when we have one) — vertically centered within the animated height
        val textX = avatarX + AVATAR_SIZE + PAD
        val titleColor = if (hasTrack) colorUtil.rgba(255, 255, 255, 255) else colorUtil.rgba(255, 255, 255, 200)
        if (subStr.isEmpty()) {
            r.drawText("regular", titleStr, textX, y + (animH - TITLE_FS) / 2f, TITLE_FS, titleColor)
        } else {
            val blockH = TITLE_FS + 2f + SUB_FS
            val topY = y + (animH - blockH) / 2f
            r.drawText("regular", titleStr, textX, topY, TITLE_FS, titleColor)
            r.drawText("regular", subStr, textX, topY + TITLE_FS + 2f, SUB_FS, colorUtil.rgba(255, 255, 255, 185))
        }

        // Hover-only expanded detail: progress bar + pos/len + equalizer
        if (expanded) {
            val progX = textX + midW + 12f
            val progAvail = cx + animW - PAD - progX
            if (progAvail > 44f) renderProgress(r, progX, y + animH / 2f, progAvail, now, posU, durU, playing, showTimes)
        }
    }

    private fun renderProgress(r: AporiaRenderer, x: Float, cy: Float, availW: Float, now: Long,
                               pos: Long, dur: Long, playing: Boolean, showTimes: Boolean) {
        // cy is the vertical CENTER of the pill (already passed in) — do not offset again.
        val pct = if (dur > 0L) (pos.toDouble() / dur).toFloat().coerceIn(0f, 1f) else 0f
        val fs = 7f
        val eqW = 12f
        val gap = 5f
        val inset = 3f                 // keep the detail off the pill's rounded edge
        val right = x + availW - inset

        // Timestamps (seconds) — shown for both sources now that units are normalized.
        var barX = x
        var barEnd = right - eqW - gap
        if (showTimes) {
            val posStr = fmtSec(pos)
            val lenStr = fmtSec(dur)
            val posW = r.getTextWidth(Fonts.REGULAR, posStr, fs)
            val lenW = r.getTextWidth(Fonts.REGULAR, lenStr, fs)
            r.drawText(Fonts.REGULAR, posStr, x, cy - fs / 2f, fs, HudStyle.textDim())
            barX = x + posW + gap
            barEnd = right - eqW - gap - lenW - gap
            r.drawText(Fonts.REGULAR, lenStr, barEnd + gap, cy - fs / 2f, fs, HudStyle.textDim())
        }

        val barW = (barEnd - barX).coerceAtLeast(8f)
        r.drawRect(barX, cy - 1.5f, barW, 3f, 1.5f, colorUtil.rgba(255, 255, 255, 50))
        r.drawRect(barX, cy - 1.5f, barW * pct, 3f, 1.5f, colorUtil.rgba(255, 255, 255, 235))

        // equalizer bars — white to match the bar (animate only while playing)
        val t = (now % 100000L) / 1000.0
        val speeds = doubleArrayOf(6.9, 8.9, 5.7)
        val phase = doubleArrayOf(0.0, 1.3, 2.1)
        val eqColor = colorUtil.rgba(255, 255, 255, 220)
        val eqBaseX = right - eqW
        for (i in 0 until 3) {
            val h = if (playing) (3.5 + (kotlin.math.sin(t * speeds[i] + phase[i]) * 0.5 + 0.5) * 7.0).toFloat() else 3.5f
            r.drawRect(eqBaseX + i * 4.5f, cy + 5f - h, 2.5f, h, 1.25f, eqColor)
        }
    }

    private fun fmtSec(sec: Long): String {
        val s = sec.coerceAtLeast(0L)
        return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
    }

    private fun truncateText(r: AporiaRenderer, text: String, maxW: Float, size: Float): String {
        val fullW = r.getTextWidth("regular", text, size)
        if (fullW <= maxW) return text
        // Simple truncation with ellipsis
        for (i in text.length downTo 1) {
            val truncated = text.substring(0, i) + "…"
            if (r.getTextWidth("regular", truncated, size) <= maxW) return truncated
        }
        return "…"
    }

    // ── Click handling ──

    @JvmStatic
    fun handleMediaClick(x: Double, y: Double, button: Int): Boolean {
        if (button != 0 || !MusicControl.hasMedia()) return false
        val cx = pillRect[0]; val cy = pillRect[1]; val cw = pillRect[2]; val ch = pillRect[3]
        if (x < cx || x > cx + cw || y < cy || y > cy + ch) return false
        MusicControl.mediaTogglePlayPause()
        return true
    }

    @JvmStatic
    fun getPillRect(): FloatArray = pillRect
}
