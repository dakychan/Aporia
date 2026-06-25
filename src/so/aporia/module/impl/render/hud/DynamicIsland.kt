package so.aporia.module.impl.render.hud

import dev.redstones.mediaplayerinfo.IMediaSession
import dev.redstones.mediaplayerinfo.MediaPlayerInfo
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import org.lwjgl.opengl.GL11
import so.aporia.module.ModuleManager
import so.aporia.module.impl.misc.DiscordRPCModule
import so.aporia.module.impl.render.Beautifully
import so.aporia.utils.user.locale.LocaleManager
import so.aporia.utils.user.render.color.ColorUtil
import so.aporia.utils.user.render.core.AporiaRenderer
import java.io.ByteArrayInputStream
import java.nio.file.Path
import java.util.Arrays
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object DynamicIsland {

    private const val PILL_H = 20
    private const val AVATAR_SZ = 14
    private const val PAD = 6
    private const val GAP = 4
    private const val MARGIN = 4
    private const val FS = 9f
    private val C_BG = ColorUtil.rgba(12, 18, 22, 220)
    private val C_ACCENT = ColorUtil.rgba(80, 200, 200, 255)
    private val C_DIM = ColorUtil.rgba(160, 160, 160, 255)

    // ── Media bubble constants ──
    private const val MEDIA_H = 20
    private const val MEDIA_ART_SZ = 14
    private const val MEDIA_TITLE_SZ = 9f
    private const val MAX_TITLE_W = 130f

    // ── Background-thread media state ──
    @Volatile private var bgSession: IMediaSession? = null
    @Volatile private var bgTitle: String? = null
    @Volatile private var bgPlaying = false
    @Volatile private var bgArtBytes: ByteArray? = null

    // ── Render-thread fields ──
    private var mArtId: Identifier? = null
    private var mPrevArtHash = 0
    private var mLeftId: Identifier? = null
    private var mRightId: Identifier? = null
    private var mPlayId: Identifier? = null
    private var mPauseId: Identifier? = null
    private var mIconsLoaded = false
    private var marqueeTitle: String? = null
    private var marqueeStart = 0L

    private val mediaPoller = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "Aporia-MediaPoller").apply { isDaemon = true }
    }
    private var mediaPollStarted = false

    enum class Mode { AUTO, LOGO, AVATAR, SKIN }

    @JvmStatic
    fun render(r: AporiaRenderer, mode: Mode) {
        val mc = Minecraft.getInstance()
        if (mc.player == null) return

        val blur = Beautifully.isBlurEnabled()
        val sw = mc.window.guiScaledWidth.toFloat()

        val name = LocaleManager.INSTANCE.get("watermark.name")
        val time = aporia.cc.OsManager.getTimeFormatted("HH:mm")

        val radius = PILL_H / 2f
        val y = MARGIN.toFloat()

        val wLabel = r.getTextWidth("bold", name, FS)
        val wCenter = PAD + AVATAR_SZ + GAP + wLabel + PAD
        val cx = (sw - wCenter) / 2f

        if (blur) r.drawRectBlurred(cx, y, wCenter, PILL_H.toFloat(), radius, C_BG)
        else r.drawRect(cx, y, wCenter, PILL_H.toFloat(), radius, C_BG)

        val ax = cx + PAD
        val ay = y + (PILL_H - AVATAR_SZ) / 2f
        renderAvatar(r, ax, ay, AVATAR_SZ, mc, mode, blur)
        r.drawText("bold", name, ax + AVATAR_SZ + GAP, y + (PILL_H - FS) / 2f, FS, C_ACCENT)

        val wTime = r.getTextWidth("regular", time, FS) + PAD * 2
        val lx = cx - GAP - wTime
        if (blur) r.drawRectBlurred(lx, y, wTime, PILL_H.toFloat(), radius, C_BG)
        else r.drawRect(lx, y, wTime, PILL_H.toFloat(), radius, C_BG)
        r.drawText("regular", time, lx + PAD, y + (PILL_H - FS) / 2f, FS, C_DIM)

        if (mc.level != null) renderMedia(r)
    }

    // ── Media bubble ──

    private fun renderMedia(r: AporiaRenderer) {
        startPolling()
        val title = bgTitle ?: return
        val mc = Minecraft.getInstance()
        val sw = mc.window.guiScaledWidth.toFloat()

        val artBytes = bgArtBytes
        if (artBytes != null) {
            val h = Arrays.hashCode(artBytes)
            if (h != mPrevArtHash) {
                mPrevArtHash = h
                try { mArtId = AporiaRenderer.INSTANCE.loadImage(ByteArrayInputStream(artBytes)) }
                catch (_: Exception) { mArtId = null }
            }
        }

        val fullW = r.getTextWidth("regular", title, MEDIA_TITLE_SZ)
        val titleW = minOf(fullW, MAX_TITLE_W)
        val contentW = (PAD + MEDIA_ART_SZ + GAP + titleW + PAD).toFloat()
        val blur = Beautifully.isBlurEnabled()
        val radius = MEDIA_H / 2f
        val bx = (sw - contentW) / 2f
        val by = (MARGIN + PILL_H + GAP).toFloat()

        if (blur) r.drawRectBlurred(bx, by, contentW, MEDIA_H.toFloat(), radius, C_BG)
        else r.drawRect(bx, by, contentW, MEDIA_H.toFloat(), radius, C_BG)

        val artX = bx + PAD
        val artY = by + (MEDIA_H - MEDIA_ART_SZ) / 2f
        if (mArtId != null) {
            r.drawImage(artX, artY, MEDIA_ART_SZ.toFloat(), MEDIA_ART_SZ.toFloat(), mArtId!!, MEDIA_ART_SZ / 2f)
        } else {
            r.drawRect(artX, artY, MEDIA_ART_SZ.toFloat(), MEDIA_ART_SZ.toFloat(), MEDIA_ART_SZ / 2f, C_ACCENT)
        }

        var textX = artX + MEDIA_ART_SZ + GAP
        val visibleW = contentW - PAD - MEDIA_ART_SZ - GAP - PAD
        if (fullW > visibleW) {
            if (title != marqueeTitle) { marqueeTitle = title; marqueeStart = System.currentTimeMillis() }
            val now = System.currentTimeMillis()
            val scrollDist = fullW - visibleW + 8f
            val speed = 22f
            val scrollMs = (scrollDist / speed * 1000f).toLong()
            val pauseMs = 1500L
            val cycleMs = pauseMs * 2 + scrollMs * 2
            val phase = (now - marqueeStart) % cycleMs
            textX -= when {
                phase < pauseMs -> 0f
                phase < pauseMs + scrollMs -> (phase - pauseMs).toFloat() / scrollMs * scrollDist
                phase < pauseMs * 2 + scrollMs -> scrollDist
                else -> scrollDist - (phase - pauseMs * 2 - scrollMs).toFloat() / scrollMs * scrollDist
            }
        }

        // scissor clip to bubble text area
        val scaleFactor = mc.window.guiScale.toFloat()
        val scX = ((bx + PAD + MEDIA_ART_SZ + GAP) * scaleFactor).toInt()
        val scY = ((mc.window.guiScaledHeight - (by + MEDIA_H)) * scaleFactor).toInt()
        val scW = ((contentW - PAD - PAD - MEDIA_ART_SZ - GAP) * scaleFactor).toInt()
        val scH = (MEDIA_H * scaleFactor).toInt()
        GL11.glEnable(GL11.GL_SCISSOR_TEST)
        GL11.glScissor(scX, scY, scW, scH)
        try {
            r.drawText("regular", title, textX, by + (MEDIA_H - MEDIA_TITLE_SZ) / 2f, MEDIA_TITLE_SZ, 0xFFFFFFFF.toInt())
        } finally {
            GL11.glDisable(GL11.GL_SCISSOR_TEST)
        }
    }

    @JvmStatic
    fun handleMediaClick(x: Double, y: Double, button: Int): Boolean = false

    // ── Background polling ──

    private fun startPolling() {
        if (mediaPollStarted) return
        mediaPollStarted = true
        mediaPoller.scheduleWithFixedDelay({
            try {
                val sessions = MediaPlayerInfo.INSTANCE.mediaSessions ?: return@scheduleWithFixedDelay
                val s = sessions.firstOrNull { ses ->
                    val m = ses.media; m != null && !m.title.isNullOrEmpty() && m.isPlaying
                } ?: sessions.firstOrNull { ses ->
                    val m = ses.media; m != null && !m.title.isNullOrEmpty()
                } ?: sessions.firstOrNull()
                if (s == null) { bgSession = null; bgTitle = null; return@scheduleWithFixedDelay }
                val info = s.media ?: run { bgSession = null; bgTitle = null; return@scheduleWithFixedDelay }

                bgSession = s
                bgTitle = if (!info.title.isNullOrBlank()) info.title else info.artist
                bgPlaying = info.isPlaying
                bgArtBytes = info.artworkPng
            } catch (_: Exception) {
                bgSession = null; bgTitle = null
            }
        }, 2, 1, TimeUnit.SECONDS)
    }

    // ── Icons (render thread) ──

    private fun ensureIcons() {
        if (mIconsLoaded) return
        try {
            val home = System.getProperty("user.home")
            val dir = "$home/.apr/aporia/texture/dynamic island"
            mLeftId = AporiaRenderer.INSTANCE.loadImage(Path.of(dir, "left.png"))
            mRightId = AporiaRenderer.INSTANCE.loadImage(Path.of(dir, "right.png"))
            mPlayId = AporiaRenderer.INSTANCE.loadImage(Path.of(dir, "play.png"))
            mPauseId = AporiaRenderer.INSTANCE.loadImage(Path.of(dir, "pause.png"))
        } catch (_: Throwable) {}
        mIconsLoaded = true
    }

    // ── Avatar rendering ──

    private fun renderAvatar(r: AporiaRenderer, x: Float, y: Float, size: Int, mc: Minecraft, mode: Mode, blur: Boolean) {
        when (mode) {
            Mode.AUTO -> {
                val id = getDiscordAvatarId()
                if (id != null) { renderDiscord(r, x, y, size, blur); return }
                if (mc.player != null) { renderSkin(r, x, y, size, mc, blur); return }
                renderLogo(r, x, y, size)
            }
            Mode.LOGO -> renderLogo(r, x, y, size)
            Mode.AVATAR -> renderDiscord(r, x, y, size, blur)
            Mode.SKIN -> renderSkin(r, x, y, size, mc, blur)
        }
    }

    private fun renderLogo(r: AporiaRenderer, x: Float, y: Float, size: Int) {
        r.drawRect(x, y, size.toFloat(), size.toFloat(), 4f, C_ACCENT)
    }

    private fun renderDiscord(r: AporiaRenderer, x: Float, y: Float, size: Int, blur: Boolean) {
        val id = getDiscordAvatarId() ?: return renderLogo(r, x, y, size)
        val radius = size / 2f
        if (blur) r.drawRectBlurred(x, y, size.toFloat(), size.toFloat(), radius, C_BG)
        else r.drawRect(x, y, size.toFloat(), size.toFloat(), radius, C_BG)
        r.drawImageCropped(x, y, size.toFloat(), size.toFloat(), id, radius, 0f, 0f, 1f, 1f)
    }

    private fun renderSkin(r: AporiaRenderer, x: Float, y: Float, size: Int, mc: Minecraft, blur: Boolean) {
        if (mc.player == null) return
        val skinId = mc.player!!.skin.body.texturePath() ?: return
        val radius = size / 2f
        if (blur) r.drawRectBlurred(x, y, size.toFloat(), size.toFloat(), radius, C_BG)
        else r.drawRect(x, y, size.toFloat(), size.toFloat(), radius, C_BG)
        r.drawImageCropped(x, y, size.toFloat(), size.toFloat(), skinId, radius, 8f/64f, 8f/64f, 16f/64f, 16f/64f)
        r.drawImageCropped(x, y, size.toFloat(), size.toFloat(), skinId, radius, 40f/64f, 8f/64f, 48f/64f, 16f/64f)
    }

    private fun getDiscordAvatarId(): Identifier? {
        val discordRPC = ModuleManager.get("Discord RPC") ?: return null
        if (!discordRPC.isEnabled) return null
        val discordModule = discordRPC as DiscordRPCModule
        discordModule.loadAvatarOnRenderThread()
        return discordModule.avatarId
    }
}
