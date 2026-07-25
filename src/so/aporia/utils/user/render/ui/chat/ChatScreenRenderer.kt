package so.aporia.utils.user.render.ui.chat

import com.chaos.annotation.ChaosNative
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.ChatComponent
import net.minecraft.client.multiplayer.chat.GuiMessage
import so.aporia.module.impl.render.Beautifully
import so.aporia.utils.user.render.animation.Easing
import so.aporia.utils.user.render.animation.MessageAnim
import so.aporia.utils.user.render.color.ColorUtil
import so.aporia.utils.user.render.core.AporiaRenderer

/**
 * HUD-side chat renderer. Draws the main chat window (WinCfg index 0)
 * while no chat screen is open. Messages fade out using vanilla timing and slide in via
 * physics animations tracked in [anims].
 */
@ChaosNative
object ChatScreenRenderer {

    private const val FADE_START = 160
    private const val FADE_TICKS = 40
    private const val ANIM_CLEANUP_INTERVAL = 20

    val anims = mutableMapOf<Long, MessageAnim>()
    private var cleanupTimer = 0

    /** Called every frame from the HUD render hook. */
    @JvmStatic
    fun render(gfx: GuiGraphicsExtractor, font: Font, chat: ChatComponent, guiTicks: Int) {
        if (!Beautifully.isCustomChatEnabled()) return

        gfx.nextStratum()

        val c = ChatScreenBackendApi.WinMgr.wins[0]
        if (c.lines.isEmpty()) return

        val screenH = gfx.guiHeight()
        val boxH = c.h
        val boxY = screenH - c.bottomY - boxH
        val maxL = c.maxLines()
        val count = minOf(c.lines.size, maxL)

        var maxAlpha = 0f
        val alphas = FloatArray(count)
        val offsetsX = FloatArray(count)
        val offsetsY = FloatArray(count)

        var i = 0
        for (line in c.lines) {
            if (i >= count) break
            val key = ChatScreenBackendApi.lineKey(line)
            val anim = anims.getOrPut(key) { MessageAnim(0f) }
            anim.setAlphaTarget(vanillaAlpha(line, guiTicks))
            anim.tick()
            alphas[i] = anim.alpha()
            offsetsX[i] = anim.slideX()
            offsetsY[i] = anim.slideY()
            if (alphas[i] > maxAlpha) maxAlpha = alphas[i]
            i++
        }

        if (++cleanupTimer >= ANIM_CLEANUP_INTERVAL) {
            cleanupTimer = 0
            anims.entries.removeAll { it.value.isDead() }
        }

        if (maxAlpha < 0.01f) return

        val actualH = minOf(boxH, count * ChatScreenBackendApi.LINE_H + ChatScreenBackendApi.BOX_PAD * 2)
        val actualBoxY = boxY + boxH - actualH
        val bgColor = ColorUtil.rgba(255, 255, 255, (18 * maxAlpha).toInt())
        if (Beautifully.isBlurEnabled()) {
            AporiaRenderer.drawRectBlurred(c.x.toFloat(), actualBoxY.toFloat(), c.w.toFloat(), actualH.toFloat(), ChatScreenBackendApi.RADIUS.toFloat(), bgColor)
        } else {
            AporiaRenderer.drawRect(c.x.toFloat(), actualBoxY.toFloat(), c.w.toFloat(), actualH.toFloat(), ChatScreenBackendApi.RADIUS.toFloat(), bgColor)
        }

        val textX = c.x + ChatScreenBackendApi.BOX_PAD
        for (j in 0 until count) {
            if (alphas[j] < 0.01f) continue
            val lineY = actualBoxY + ChatScreenBackendApi.BOX_PAD + (count - 1 - j) * ChatScreenBackendApi.LINE_H + offsetsY[j].toInt()
            val tx = textX + offsetsX[j].toInt()
            val col = ColorUtil.rgba(255, 255, 255, (255 * alphas[j]).toInt())
            val line = getLine(c, j) ?: continue
            gfx.text(font, line.content(), tx, lineY, col, false)
        }
    }

    private fun getLine(c: ChatScreenBackendApi.WinCfg, idx: Int): GuiMessage.Line? {
        var k = 0
        for (l in c.lines) {
            if (k++ == idx) return l
        }
        return null
    }

    /** Computes vanilla-style fade alpha based on message age in ticks. */
    @JvmStatic
    fun vanillaAlpha(line: GuiMessage.Line, guiTicks: Int): Float {
        val age = guiTicks - line.addedTime()
        if (age > FADE_START + FADE_TICKS) return 0f
        if (age < FADE_START) return 1f
        return 1f - Easing.cubicIn((age - FADE_START).toFloat() / FADE_TICKS)
    }
}
