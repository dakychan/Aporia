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
import so.aporia.module.impl.render.NoRender
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.TickEvent
import so.aporia.utils.user.render.animation.Animator
import so.aporia.utils.user.render.animation.Easing
import so.aporia.utils.user.render.animation.SpringSimulator
import so.aporia.utils.user.render.animation.TypeAnim
import so.aporia.utils.user.render.core.AporiaRenderer
import java.io.ByteArrayInputStream
import java.util.Arrays
import com.chaos.annotation.ChaosNative
@ChaosNative
object DynamicIsland {

    enum class State { IDLE, MEDIA, BOSSBAR }
    enum class Mode { AUTO, LOGO, AVATAR, SKIN }

    @JvmField var posX = 0f
    @JvmField var posY = 4f

    private var state = State.IDLE

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

    @Volatile private var pollerRunning = false
    private var pollerThread: Thread? = null
    private var mediaPresent = false
    private var lastLevel: Any? = null

    private var hoverPrev = false
    private var hoverPlay = false
    private var hoverNext = false
    private var pillRect = floatArrayOf(0f, 0f, 0f, 0f)

    private const val COLLAPSED_W = 100f
    private const val EXPANDED_W = 220f
    private const val PILL_H = 20f
    private const val MEDIA_EXPANDED_H = 52f
    private const val PILL_RADIUS = 10f

    private var bossbarTitle: String? = null
    private var bossbarProgress = 0f
    private val bossbarTypeAnim = TypeAnim(60, 120)

    private val expandSpring = SpringSimulator(180f, 16f, 0f)
    private var isExpanded = false
    private var expandStart = 0L

    init {
        bus.register(this)
    }

    @EventHandler
    fun onTick(e: TickEvent) {
        if (mc.player == null) return
        bossbarTitle = null
        try {
            val overlay = mc.gui.bossOverlay ?: return
            val eventsField = overlay.javaClass.getDeclaredField("events")
            eventsField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val events = eventsField.get(overlay) as? Map<*, *> ?: return
            val bb = events.values.firstOrNull() ?: return
            val nameField = bb.javaClass.getDeclaredField("name")
            nameField.isAccessible = true
            bossbarTitle = nameField.get(bb)?.toString()
            val progressField = bb.javaClass.getDeclaredField("progress")
            progressField.isAccessible = true
            bossbarProgress = progressField.get(bb) as? Float ?: 0f
        } catch (_: Exception) {
            bossbarTitle = null
        }
    }

    @JvmStatic
    fun render(r: AporiaRenderer, mode: Mode) {
        if (mc.player == null) return

        val blur = Beautifully.isBlurEnabled() && Beautifully.isFeatureEnabled("Dynamic Island Blur")
        val sw = mc.window.guiScaledWidth.toFloat()
        val name = locale.get("watermark.name")
        val time = aporia.cc.OsManager.getTimeFormatted("HH:mm")
        val mx = mc.mouseHandler.getScaledXPos(mc.window).toFloat()
        val my = mc.mouseHandler.getScaledYPos(mc.window).toFloat()
        val y = posY

        val level = mc.level
        if (level !== lastLevel) {
            lastLevel = level
            resetMediaPoller()
        }
        ensurePoller()
        mediaPresent = bgTitle != null
        val hasBossbar = bossbarTitle != null

        state = when {
            mediaPresent -> State.MEDIA
            hasBossbar && !NoRender.hideBossBar -> State.BOSSBAR
            else -> State.IDLE
        }

        val isMedia = state == State.MEDIA
        val overPill = if (isMedia) {
            mx >= (sw - COLLAPSED_W) / 2f && mx < (sw + COLLAPSED_W) / 2f && my >= y && my < y + MEDIA_EXPANDED_H
        } else {
            mx >= (sw - COLLAPSED_W) / 2f && mx < (sw + COLLAPSED_W) / 2f && my >= y && my < y + PILL_H
        }
        if (overPill && !isExpanded) {
            isExpanded = true
            expandStart = System.currentTimeMillis()
        } else if (!overPill && isExpanded && System.currentTimeMillis() - expandStart > 500) {
            isExpanded = false
        }

        expandSpring.setTarget(if (isExpanded) 1f else 0f)
        val now = System.currentTimeMillis()
        expandSpring.update(0.016f)
        val expandFrac = expandSpring.value()
        val ctrlReserved = expandFrac * 50f

        val (pillW, pillH, cx) = if (isMedia) {
            val h = PILL_H + (MEDIA_EXPANDED_H - PILL_H) * expandFrac
            Triple(COLLAPSED_W, h, (sw - COLLAPSED_W) / 2f)
        } else {
            val w = COLLAPSED_W + (EXPANDED_W - COLLAPSED_W) * expandFrac
            Triple(w, PILL_H, (sw - w) / 2f)
        }
        val bgColor = when (state) {
            State.MEDIA -> colorUtil.rgba(12, 18, 22, 220)
            State.BOSSBAR -> colorUtil.rgba(20, 28, 38, 220)
            State.IDLE -> colorUtil.rgba(12, 18, 22, 220)
        }
        pillRect = floatArrayOf(cx, y, pillW, pillH)

        // Time pill - slides left when expanded (only for non-media)
        val timeOpacity = if (isMedia) 0f else (1f - expandFrac).coerceIn(0f, 1f)
        val timeSlide = -expandFrac * 60f
        val wTime = r.getTextWidth("regular", time, 9f) + 12
        val lx = cx - 4 - wTime + timeSlide
        if (timeOpacity > 0.01f) {
            val timeColor = colorUtil.rgba(12, 18, 22, (220 * timeOpacity).toInt())
            if (blur) r.drawRectBlurred(lx, y, wTime, PILL_H, PILL_RADIUS, timeColor)
            else r.drawRect(lx, y, wTime, PILL_H, PILL_RADIUS, timeColor)
            val textColor = colorUtil.rgba(180, 180, 180, (255 * timeOpacity).toInt())
            r.drawText("regular", time, lx + 6, y + 5.5f, 9f, textColor)
        }

        // Main pill
        if (blur) r.drawRectBlurred(cx, y, pillW, pillH, PILL_RADIUS, bgColor)
        else r.drawRect(cx, y, pillW, pillH, PILL_RADIUS, bgColor)

        val ax = cx + 6
        val ay = y + 3f
        var textX = ax + 18f

        // Controls on right when collapsed/partial, move to bottom when fully expanded vertical
        if (state == State.MEDIA && expandFrac > 0.01f && expandFrac < 0.5f) {
            textX += ctrlReserved
        }

        when (state) {
            State.IDLE -> {
                renderAvatar(r, ax, ay, 14, mc, mode, blur)
                r.drawText("bold", name, textX, y + 5.5f, 9f, colorUtil.rgba(255, 255, 255, 255))
            }
            State.MEDIA -> {
                updateArtTexture()
                if (mArtId != null) {
                    r.drawImage(ax, ay, 14f, 14f, mArtId!!, 7f)
                } else {
                    renderAvatar(r, ax, ay, 14, mc, mode, blur)
                }

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

                // Media controls
                val ctrlAlpha = if (isMedia) {
                    (if (expandFrac > 0.01f) expandFrac else 0f)
                } else {
                    (if (expandFrac > 0.01f) expandFrac else 0f)
                }
                if (ctrlAlpha > 0.01f) {
                    val aMul = (ctrlAlpha * 255).toInt().coerceIn(0, 255)
                    val white = colorUtil.rgba(255, 255, 255, aMul)
                    val gray = colorUtil.rgba(180, 180, 180, aMul)

                    val (ctrlX, ctrlY) = if (isMedia && expandFrac > 0.5f) {
                        Pair(cx + 6f, y + 14f + (pillH - 14f - 8f) * ((expandFrac - 0.5f) / 0.5f))
                    } else {
                        Pair(cx + pillW - 50f, y + 6f)
                    }
                    val prevX = ctrlX; val playX = ctrlX + 18f; val nextX = ctrlX + 36f

                    hoverPrev = mx >= prevX && mx < prevX + 8f && my >= ctrlY && my < ctrlY + 8f
                    hoverPlay = mx >= playX && mx < playX + 8f && my >= ctrlY && my < ctrlY + 8f
                    hoverNext = mx >= nextX && mx < nextX + 8f && my >= ctrlY && my < ctrlY + 8f

                    r.drawTriangle(prevX + 8f, ctrlY, prevX, ctrlY + 4f, prevX + 8f, ctrlY + 8f, if (hoverPrev) white else gray)
                    if (bgPlaying) {
                        r.drawRect(playX, ctrlY, 3f, 8f, 0f, if (hoverPlay) white else gray)
                        r.drawRect(playX + 5f, ctrlY, 3f, 8f, 0f, if (hoverPlay) white else gray)
                    } else {
                        r.drawTriangle(playX, ctrlY, playX, ctrlY + 8f, playX + 8f, ctrlY + 4f, if (hoverPlay) white else gray)
                    }
                    r.drawTriangle(nextX, ctrlY, nextX + 8f, ctrlY + 4f, nextX, ctrlY + 8f, if (hoverNext) white else gray)
                }
            }
            State.BOSSBAR -> {
                renderAvatar(r, ax, ay, 14, mc, mode, blur)

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
        if (button != 0) return false
        val cx = pillRect[0]; val cy = pillRect[1]; val cw = pillRect[2]; val ch = pillRect[3]
        val isExpandedLocal = ch > PILL_H + 2f
        val (ctrlX, ctrlY) = if (isExpandedLocal) {
            Pair(cx + 6f, cy + 14f + (ch - 14f - 8f))
        } else {
            Pair(cx + cw - 50f, cy + 6f)
        }
        val relX = (x - ctrlX).toFloat()
        val relY = (y - ctrlY).toFloat()
        if (relY < 0 || relY > 8f || relX < 0 || relX > 44f) return false
        try {
            when {
                relX < 8f -> bgSession?.previous()
                relX < 18f -> return false
                relX < 26f -> if (bgPlaying) bgSession?.pause() else bgSession?.play()
                relX < 36f -> return false
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
                    if (sessions == null || sessions.isEmpty()) {
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
        val skinId = mc.player!!.skin.body.texturePath()
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
