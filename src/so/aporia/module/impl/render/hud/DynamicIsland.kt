package so.aporia.module.impl.render.hud

import so.aporia.utils.imports.*
import dev.redstones.mediaplayerinfo.IMediaSession
import dev.redstones.mediaplayerinfo.MediaPlayerInfo
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import so.aporia.module.ModuleManager
import so.aporia.module.impl.misc.DiscordRPCModule
import so.aporia.module.impl.render.Beautifully
import so.aporia.module.impl.render.NoRender
import so.aporia.utils.assets.AssetManager
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.TickEvent
import so.aporia.utils.user.render.core.AporiaRenderer
import so.aporia.utils.user.render.font.Fonts
import java.io.ByteArrayInputStream
import java.util.Arrays
import java.util.HashMap
import com.chaos.annotation.ChaosNative

@ChaosNative
object DynamicIsland {

    enum class Mode { AUTO, LOGO, AVATAR, SKIN }

    @JvmField var posX = 0f
    @JvmField var posY = 4f

    @Volatile private var bgSession: IMediaSession? = null
    @Volatile private var bgTitle: String? = null
    @Volatile private var bgPlaying = false
    @Volatile private var bgArtBytes: ByteArray? = null

    private var mArtId: Identifier? = null
    private var mPrevArtHash = 0

    @Volatile private var pollerRunning = false
    private var pollerThread: Thread? = null
    private var lastLevel: Any? = null

    private var pillRect = floatArrayOf(0f, 0f, 0f, 0f)

    private const val PILL_W = 173f
    private const val PILL_H = 30f
    private const val PILL_RADIUS = 15f
    private const val AVATAR_SIZE = 26f
    private const val AVATAR_RADIUS = 15f
    private const val TEXT_SIZE = 15f
    private const val PAD = 8f

    init { bus.register(this) }

    @EventHandler
    fun onTick(e: TickEvent) {
        if (mc.player == null) return
        val level = mc.level
        if (level !== lastLevel) { lastLevel = level; resetMediaPoller() }
        ensurePoller()
    }

    @JvmStatic
    fun render(r: AporiaRenderer, mode: Mode) {
        if (mc.player == null) return

        val blur = Beautifully.isBlurEnabled() && Beautifully.isFeatureEnabled("Dynamic Island Blur")
        val sw = mc.window.guiScaledWidth.toFloat()
        val cx = (sw - PILL_W) / 2f
        val y = posY

        pillRect = floatArrayOf(cx, y, PILL_W, PILL_H)

        // Background
        val bgColor = colorUtil.rgba(12, 18, 22, 200)
        if (blur) r.drawRectBlurred(cx, y, PILL_W, PILL_H, PILL_RADIUS, bgColor)
        else r.drawRect(cx, y, PILL_W, PILL_H, PILL_RADIUS, bgColor)

        // Avatar (left side, centered vertically)
        val avatarX = cx + PAD
        val avatarY = y + (PILL_H - AVATAR_SIZE) / 2f
        renderAvatar(r, avatarX, avatarY, AVATAR_SIZE, AVATAR_RADIUS, mc, mode, blur)

        // Text (right of avatar)
        val textX = avatarX + AVATAR_SIZE + PAD
        val textY = y + (PILL_H - TEXT_SIZE) / 2f

        val title = bgTitle
        if (title != null && title.isNotEmpty()) {
            // Music playing — show track info
            val maxTextW = PILL_H - PAD * 2 - AVATAR_SIZE - PAD
            val displayText = truncateText(r, title, maxTextW, TEXT_SIZE)
            r.drawText("regular", displayText, textX, textY, TEXT_SIZE, colorUtil.rgba(255, 255, 255, 255))
        } else {
            // No music — show watermark name
            val name = locale.get("watermark.name")
            r.drawText("regular", name, textX, textY, TEXT_SIZE, colorUtil.rgba(255, 255, 255, 200))
        }
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

    // ── Avatar rendering ──

    private fun renderAvatar(r: AporiaRenderer, x: Float, y: Float, size: Float, radius: Float,
                             mc: Minecraft, mode: Mode, blur: Boolean) {
        when (mode) {
            Mode.AUTO -> {
                val discordId = getDiscordAvatarId()
                if (discordId != null) { renderCircleImage(r, x, y, size, radius, discordId, blur); return }
                if (mc.player != null) { renderPlayerHead(r, x, y, size, radius, mc, blur); return }
                renderLogo(r, x, y, size, radius)
            }
            Mode.LOGO -> renderLogo(r, x, y, size, radius)
            Mode.AVATAR -> {
                val discordId = getDiscordAvatarId()
                if (discordId != null) renderCircleImage(r, x, y, size, radius, discordId, blur)
                else renderLogo(r, x, y, size, radius)
            }
            Mode.SKIN -> renderPlayerHead(r, x, y, size, radius, mc, blur)
        }
    }

    private fun renderLogo(r: AporiaRenderer, x: Float, y: Float, size: Float, radius: Float) {
        r.drawRect(x, y, size, size, radius, colorUtil.rgba(60, 60, 80, 255))
        r.drawText("bold", "A", x + size / 2f - 5f, y + size / 2f - 6f, 12f, colorUtil.rgba(200, 200, 255, 255))
    }

    private fun renderCircleImage(r: AporiaRenderer, x: Float, y: Float, size: Float, radius: Float,
                                  textureId: Identifier, blur: Boolean) {
        if (blur) r.drawRectBlurred(x, y, size, size, radius, colorUtil.rgba(30, 30, 40, 200))
        else r.drawRect(x, y, size, size, radius, colorUtil.rgba(30, 30, 40, 200))
        r.drawImageCropped(x, y, size, size, textureId, radius, 0f, 0f, 1f, 1f)
    }

    private fun renderPlayerHead(r: AporiaRenderer, x: Float, y: Float, size: Float, radius: Float,
                                 mc: Minecraft, blur: Boolean) {
        val skinId = mc.player!!.skin.body.texturePath()
        if (blur) r.drawRectBlurred(x, y, size, size, radius, colorUtil.rgba(30, 30, 40, 200))
        else r.drawRect(x, y, size, size, radius, colorUtil.rgba(30, 30, 40, 200))
        r.drawImageCropped(x, y, size, size, skinId, radius, 8f / 64f, 8f / 64f, 16f / 64f, 16f / 64f)
        r.drawImageCropped(x, y, size, size, skinId, radius, 40f / 64f, 8f / 64f, 48f / 64f, 16f / 64f)
    }

    private fun getDiscordAvatarId(): Identifier? {
        val discordRPC = ModuleManager.get("Discord RPC") ?: return null
        if (!discordRPC.isEnabled) return null
        val discordModule = discordRPC as DiscordRPCModule
        discordModule.loadAvatarOnRenderThread()
        return discordModule.avatarId
    }

    // ── Media poller ──

    fun resetMediaPoller() {
        pollerRunning = false
        pollerThread?.interrupt()
        pollerThread = null
        bgSession = null; bgTitle = null; bgArtBytes = null
        mArtId = null; mPrevArtHash = 0
    }

    private fun ensurePoller() {
        if (pollerRunning) return
        pollerRunning = true
        pollerThread = Thread({
            while (pollerRunning) {
                try {
                    val sessions = MediaPlayerInfo.INSTANCE.mediaSessions
                    if (sessions.isNullOrEmpty()) {
                        if (bgSession != null) { bgSession = null; bgTitle = null }
                    } else {
                        val s = sessions.firstOrNull { ses ->
                            val m = ses.media; m != null && !m.title.isNullOrEmpty() && m.isPlaying
                        } ?: sessions.firstOrNull { ses ->
                            val m = ses.media; m != null && !m.title.isNullOrEmpty()
                        }
                        if (s != null) {
                            val info = s.media
                            if (info != null) {
                                bgSession = s
                                bgTitle = if (!info.title.isNullOrBlank()) info.title else info.artist
                                bgPlaying = info.isPlaying
                                bgArtBytes = info.artworkPng
                            }
                        } else {
                            bgSession = null; bgTitle = null
                        }
                    }
                } catch (_: Exception) {
                    bgSession = null; bgTitle = null
                }
                try { Thread.sleep(1000) } catch (_: InterruptedException) { break }
            }
        }, "Aporia-MediaPoller").apply { isDaemon = true }
        pollerThread!!.start()
    }

    // ── Click handling ──

    @JvmStatic
    fun handleMediaClick(x: Double, y: Double, button: Int): Boolean {
        if (bgTitle == null || button != 0) return false
        val cx = pillRect[0]; val cy = pillRect[1]; val cw = pillRect[2]; val ch = pillRect[3]
        if (x < cx || x > cx + cw || y < cy || y > cy + ch) return false
        try {
            if (bgPlaying) bgSession?.pause() else bgSession?.play()
        } catch (_: Exception) {}
        return true
    }

    @JvmStatic
    fun getPillRect(): FloatArray = pillRect
}
