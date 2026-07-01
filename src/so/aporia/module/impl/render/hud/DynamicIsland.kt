package so.aporia.module.impl.render.hud

import so.aporia.utils.imports.*
import dev.redstones.mediaplayerinfo.IMediaSession
import dev.redstones.mediaplayerinfo.MediaPlayerInfo
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import org.lwjgl.opengl.GL11
import so.aporia.module.ModuleManager
import so.aporia.module.impl.misc.DiscordRPCModule
import so.aporia.module.impl.render.Beautifully
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.TickEvent
import so.aporia.utils.user.render.animation.TypeAnim
import so.aporia.utils.user.render.core.AporiaRenderer
import java.io.ByteArrayInputStream
import java.util.Arrays
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.min

object DynamicIsland {

    enum class State { IDLE, MEDIA, BOSSBAR }
    enum class Mode { AUTO, LOGO, AVATAR, SKIN }

    private var state = State.IDLE
    private var prevState = State.IDLE
    private var animProgress = 0f

    @Volatile private var bgSession: IMediaSession? = null
    @Volatile private var bgTitle: String? = null
    @Volatile private var bgPlaying = false
    @Volatile private var bgArtBytes: ByteArray? = null

    private var mArtId: Identifier? = null
    private var mPrevArtHash = 0
    private var prevAnimTitle: String? = null
    private val typeAnim = TypeAnim(80, 40)
    private var marqueeTitle: String? = null
    private var marqueeStart = 0L

    private val mediaPoller = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "Aporia-MediaPoller").apply { isDaemon = true }
    }
    private var mediaPollStarted = false
    private var mediaPresent = false

    private var hoverPrev = false
    private var hoverPlay = false
    private var hoverNext = false
    private var pillRect = floatArrayOf(0f, 0f, 0f, 0f)

    private const val STATIC_PILL_W = 100f
    private const val PILL_H = 20f
    private const val PILL_RADIUS = 10f

    private var bossbarTitle: String? = null
    private var bossbarProgress = 0f
    private val bossbarTypeAnim = TypeAnim(60, 120)

    init {
        bus.register(this)
    }

    @EventHandler
    fun onTick(e: TickEvent) {
        if (mc.player == null) return
        bossbarTitle = null
        try {
            val overlay = mc.gui?.bossOverlay ?: return
            val eventsField = overlay.javaClass.getDeclaredField("events")
            eventsField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val events = eventsField.get(overlay) as? Map<*, *> ?: return
            val bb = events.values.firstOrNull() ?: return
            bossbarTitle = bb.javaClass.getMethod("getName").invoke(bb)?.toString()
            bossbarProgress = bb.javaClass.getMethod("getProgress").invoke(bb) as? Float ?: 0f
        } catch (_: Exception) {
            bossbarTitle = null
        }
    }

    @JvmStatic
    fun render(r: AporiaRenderer, mode: Mode) {
        if (mc.player == null) return

        val blur = Beautifully.isBlurEnabled()
        val sw = mc.window.guiScaledWidth.toFloat()
        val name = locale.get("watermark.name") ?: "Aporia.cc"
        val time = aporia.cc.OsManager.getTimeFormatted("HH:mm")
        val mx = mc.mouseHandler.getScaledXPos(mc.window).toFloat()
        val my = mc.mouseHandler.getScaledYPos(mc.window).toFloat()
        val y = 4f

        startPolling()
        mediaPresent = bgTitle != null
        val hasBossbar = bossbarTitle != null

        prevState = state
        state = when {
            mediaPresent -> State.MEDIA
            hasBossbar -> State.BOSSBAR
            else -> State.IDLE
        }
        animProgress = when {
            state == prevState && animProgress < 1f -> (animProgress + 0.08f).coerceAtMost(1f)
            state != prevState -> 0f
            else -> animProgress
        }

        val pillW = STATIC_PILL_W
        val cx = (sw - pillW) / 2f
        val bgColor = when (state) {
            State.MEDIA -> colorUtil.rgba(12, 18, 22, 220)
            State.BOSSBAR -> colorUtil.rgba(20, 28, 38, 220)
            State.IDLE -> colorUtil.rgba(12, 18, 22, 220)
        }
        pillRect = floatArrayOf(cx, y, pillW, PILL_H)

        val wTime = r.getTextWidth("regular", time, 9f) + 12
        val lx = cx - 4 - wTime
        if (blur) r.drawRectBlurred(lx, y, wTime, PILL_H, PILL_RADIUS, colorUtil.rgba(12, 18, 22, 220))
        else r.drawRect(lx, y, wTime, PILL_H, PILL_RADIUS, colorUtil.rgba(12, 18, 22, 220))
        r.drawText("regular", time, lx + 6, y + 5.5f, 9f, colorUtil.rgba(180, 180, 180, 255))

        if (blur) r.drawRectBlurred(cx, y, pillW, PILL_H, PILL_RADIUS, bgColor)
        else r.drawRect(cx, y, pillW, PILL_H, PILL_RADIUS, bgColor)

        val ax = cx + 6
        val ay = y + 3f
        renderAvatar(r, ax, ay, 14, mc, mode, blur)

        var textX = ax + 18f

        when (state) {
            State.IDLE -> {
                r.drawText("bold", name, textX, y + 5.5f, 9f, colorUtil.rgba(255, 255, 255, 255))
            }
            State.MEDIA -> {
                updateArtTexture()
                if (mArtId != null) {
                    r.drawImage(textX, y + 3f, 14f, 14f, mArtId!!, 7f)
                } else {
                    r.drawRect(textX, y + 3f, 14f, 14f, 7f, colorUtil.rgba(180, 180, 180, 255))
                }
                textX += 18f

                val title = bgTitle ?: ""
                if (title != prevAnimTitle) { typeAnim.setTarget(title); prevAnimTitle = title }
                val display = typeAnim.update()
                val typing = typeAnim.isRunning()

                if (typing) {
                    r.drawText("regular", display, textX, y + 5.5f, 9f, colorUtil.rgba(180, 180, 180, 255))
                } else {
                    val fullW = r.getTextWidth("regular", title, 9f)
                    val visibleW = (cx + pillW - 6 - textX).coerceAtLeast(0f)
                    var titleX = textX
                    if (fullW > visibleW) {
                        if (title != marqueeTitle) { marqueeTitle = title; marqueeStart = System.currentTimeMillis() }
                        val now = System.currentTimeMillis()
                        val scrollDist = fullW - visibleW + 8f
                        val scrollMs = (scrollDist / 22f * 1000f).toLong()
                        val pauseMs = 1500L; val cycleMs = pauseMs * 2 + scrollMs * 2
                        val phase = (now - marqueeStart) % cycleMs
                        titleX -= when {
                            phase < pauseMs -> 0f
                            phase < pauseMs + scrollMs -> (phase - pauseMs).toFloat() / scrollMs * scrollDist
                            phase < pauseMs * 2 + scrollMs -> scrollDist
                            else -> scrollDist - (phase - pauseMs * 2 - scrollMs).toFloat() / scrollMs * scrollDist
                        }
                        scissorClip(mc, textX, y, visibleW, PILL_H) {
                            r.drawText("regular", title, titleX, y + 5.5f, 9f, colorUtil.rgba(180, 180, 180, 255))
                        }
                    } else {
                        r.drawText("regular", title, titleX, y + 5.5f, 9f, colorUtil.rgba(180, 180, 180, 255))
                    }
                }

                val ctrlX = cx + pillW - 50f
                val ctrlY = y + 6f
                val prevX = ctrlX; val playX = ctrlX + 18f; val nextX = ctrlX + 36f

                hoverPrev = mx >= prevX && mx < prevX + 8f && my >= ctrlY && my < ctrlY + 8f
                hoverPlay = mx >= playX && mx < playX + 8f && my >= ctrlY && my < ctrlY + 8f
                hoverNext = mx >= nextX && mx < nextX + 8f && my >= ctrlY && my < ctrlY + 8f

                r.drawTriangle(prevX + 8f, ctrlY, prevX, ctrlY + 4f, prevX + 8f, ctrlY + 8f, if (hoverPrev) -1 else colorUtil.rgba(180, 180, 180, 255))
                if (bgPlaying) {
                    r.drawRect(playX, ctrlY, 3f, 8f, 0f, if (hoverPlay) -1 else colorUtil.rgba(180, 180, 180, 255))
                    r.drawRect(playX + 5f, ctrlY, 3f, 8f, 0f, if (hoverPlay) -1 else colorUtil.rgba(180, 180, 180, 255))
                } else {
                    r.drawTriangle(playX, ctrlY, playX, ctrlY + 8f, playX + 8f, ctrlY + 4f, if (hoverPlay) -1 else colorUtil.rgba(180, 180, 180, 255))
                }
                r.drawTriangle(nextX, ctrlY, nextX + 8f, ctrlY + 4f, nextX, ctrlY + 8f, if (hoverNext) -1 else colorUtil.rgba(180, 180, 180, 255))
            }
            State.BOSSBAR -> {
                val bbTitle = bossbarTitle ?: ""
                if (bbTitle != prevAnimTitle) { bossbarTypeAnim.setTarget(bbTitle); prevAnimTitle = bbTitle }
                val display = bossbarTypeAnim.update()
                val typing = bossbarTypeAnim.isRunning()

                val fullW = r.getTextWidth("regular", display, 9f)
                val visibleW = (cx + pillW - 6 - textX).coerceAtLeast(0f)
                var titleX = textX

                if (typing) {
                    r.drawText("regular", display, textX, y + 5.5f, 9f, colorUtil.rgba(200, 180, 100, 255))
                } else if (fullW > visibleW) {
                    if (bbTitle != marqueeTitle) { marqueeTitle = bbTitle; marqueeStart = System.currentTimeMillis() }
                    val now = System.currentTimeMillis()
                    val scrollDist = fullW - visibleW + 8f
                    val scrollMs = (scrollDist / 22f * 1000f).toLong()
                    val pauseMs = 1500L; val cycleMs = pauseMs * 2 + scrollMs * 2
                    val phase = (now - marqueeStart) % cycleMs
                    titleX -= when {
                        phase < pauseMs -> 0f
                        phase < pauseMs + scrollMs -> (phase - pauseMs).toFloat() / scrollMs * scrollDist
                        phase < pauseMs * 2 + scrollMs -> scrollDist
                        else -> scrollDist - (phase - pauseMs * 2 - scrollMs).toFloat() / scrollMs * scrollDist
                    }
                    scissorClip(mc, textX, y, visibleW, PILL_H) {
                        r.drawText("regular", bbTitle, titleX, y + 5.5f, 9f, colorUtil.rgba(200, 180, 100, 255))
                    }
                } else {
                    r.drawText("regular", bbTitle, textX, y + 5.5f, 9f, colorUtil.rgba(200, 180, 100, 255))
                }

                val barX = cx + 6f
                val barY = y + PILL_H - 5f
                val barW = pillW - 12f
                r.drawRect(barX, barY, barW, 2f, 1f, colorUtil.rgba(255, 255, 255, 30))
                r.drawRect(barX, barY, barW * bossbarProgress, 2f, 1f, colorUtil.rgba(200, 180, 100, 255))
            }
        }
    }

    @JvmStatic
    fun handleMediaClick(x: Double, y: Double, button: Int): Boolean {
        if (state != State.MEDIA) return false
        val cx = pillRect[0]; val cy = pillRect[1]; val cw = pillRect[2]
        val ctrlX = cx + cw - 50f
        val ctrlY = cy + 6f
        val relX = (x - ctrlX).toFloat()
        val relY = (y - ctrlY).toFloat()
        if (relY < 0 || relY > 8f || relX < 0 || relX > 44f) return false
        if (button != 0) return true
        try {
            when {
                relX < 8f -> bgSession?.previous()
                relX < 26f -> if (bgPlaying) bgSession?.pause() else bgSession?.play()
                else -> bgSession?.next()
            }
        } catch (_: Exception) {}
        return true
    }

    @JvmStatic
    fun isMediaPresent(): Boolean = mediaPresent

    @JvmStatic
    fun getPillRect(): FloatArray = pillRect

    fun resetMediaPoller() {
        mediaPollStarted = false
        bgSession = null; bgTitle = null; bgArtBytes = null
        mArtId = null; mPrevArtHash = 0
    }

    private fun startPolling() {
        if (mediaPollStarted) return
        mediaPollStarted = true
        mediaPoller.scheduleWithFixedDelay({
            try {
                val sessions = MediaPlayerInfo.INSTANCE.mediaSessions
                if (sessions == null || sessions.isEmpty()) {
                    if (bgSession != null) { bgSession = null; bgTitle = null }
                    return@scheduleWithFixedDelay
                }
                val s = sessions.firstOrNull { ses ->
                    val m = ses.media; m != null && !m.title.isNullOrEmpty() && m.isPlaying
                } ?: sessions.firstOrNull { ses ->
                    val m = ses.media; m != null && !m.title.isNullOrEmpty()
                }
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

    private fun updateArtTexture() {
        val bytes = bgArtBytes ?: return
        val h = Arrays.hashCode(bytes)
        if (h == mPrevArtHash) return
        mPrevArtHash = h
        try { mArtId = r.loadImage(ByteArrayInputStream(bytes)) }
        catch (_: Exception) { mArtId = null }
    }

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
        r.drawRect(x, y, size.toFloat(), size.toFloat(), 4f, colorUtil.rgba(255, 255, 255, 255))
    }

    private fun renderDiscord(r: AporiaRenderer, x: Float, y: Float, size: Int, blur: Boolean) {
        val id = getDiscordAvatarId() ?: return renderLogo(r, x, y, size)
        val radius = size / 2f
        if (blur) r.drawRectBlurred(x, y, size.toFloat(), size.toFloat(), radius, colorUtil.rgba(12, 18, 22, 220))
        else r.drawRect(x, y, size.toFloat(), size.toFloat(), radius, colorUtil.rgba(12, 18, 22, 220))
        r.drawImageCropped(x, y, size.toFloat(), size.toFloat(), id, radius, 0f, 0f, 1f, 1f)
    }

    private fun renderSkin(r: AporiaRenderer, x: Float, y: Float, size: Int, mc: Minecraft, blur: Boolean) {
        if (mc.player == null) return
        val skinId = mc.player!!.skin.body.texturePath() ?: return
        val radius = size / 2f
        if (blur) r.drawRectBlurred(x, y, size.toFloat(), size.toFloat(), radius, colorUtil.rgba(12, 18, 22, 220))
        else r.drawRect(x, y, size.toFloat(), size.toFloat(), radius, colorUtil.rgba(12, 18, 22, 220))
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

    private fun scissorClip(mc: Minecraft, x: Float, y: Float, w: Float, h: Float, block: () -> Unit) {
        val scale = mc.window.guiScale.toFloat()
        val sx = (x * scale).toInt(); val sy = ((mc.window.guiScaledHeight - (y + h)) * scale).toInt()
        val sw = (w * scale).toInt(); val sh = (h * scale).toInt()
        GL11.glEnable(GL11.GL_SCISSOR_TEST)
        GL11.glScissor(sx, sy, sw, sh)
        try { block() } finally { GL11.glDisable(GL11.GL_SCISSOR_TEST) }
    }
}
