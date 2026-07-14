package so.aporia.module.impl.render.hud

import so.aporia.utils.imports.*
import dev.redstones.mediaplayerinfo.IMediaSession
import dev.redstones.mediaplayerinfo.MediaPlayerInfo
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import org.lwjgl.opengl.GL11
import so.aporia.module.Module
import so.aporia.module.ModuleManager
import so.aporia.module.impl.misc.DiscordRPCModule
import so.aporia.module.impl.render.Beautifully
import so.aporia.module.impl.render.NoRender
import so.aporia.utils.assets.AssetManager
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.TickEvent
import so.aporia.utils.user.render.animation.Animator
import so.aporia.utils.user.render.animation.Easing
import so.aporia.utils.user.render.animation.SpringSimulator
import so.aporia.utils.user.render.animation.TypeAnim
import so.aporia.utils.user.render.core.AporiaRenderer
import java.io.ByteArrayInputStream
import java.util.Arrays
import java.util.HashMap
import com.chaos.annotation.ChaosNative
@ChaosNative
object DynamicIsland {

    enum class State { IDLE, MEDIA, BOSSBAR, MODULE_INFO }
    enum class Mode { AUTO, LOGO, AVATAR, SKIN }

    @JvmField var posX = 0f
    @JvmField var posY = 4f

    private var state = State.IDLE

    @Volatile private var bgSession: IMediaSession? = null
    @Volatile private var bgTitle: String? = null
    @Volatile private var bgPlaying = false
    @Volatile private var bgArtBytes: ByteArray? = null
    @Volatile private var bgPosition = 0L
    @Volatile private var bgDuration = 0L

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
    private const val EXPANDED_W = 240f
    private const val PILL_H = 20f
    private const val MEDIA_EXPANDED_H = 52f
    private const val PILL_RADIUS = 10f
    private const val EXPANDED_RADIUS = 14f

    private var bossbarTitle: String? = null
    private var bossbarProgress = 0f
    private val bossbarTypeAnim = TypeAnim(60, 120)

    private val expandSpring = SpringSimulator(180f, 16f, 0f)
    private var isExpanded = false
    private var expandStart = 0L

    // Module info tracking
    private val cachedModuleStates = HashMap<String, Boolean>()
    private var lastModuleName: String? = null
    private var lastModuleEnabled = false
    private var lastModuleTime = 0L
    private var moduleEnabledId: Identifier? = null
    private var moduleDisabledId: Identifier? = null
    private var moduleIconsLoaded = false

    init {
        bus.register(this)
    }

    private fun ensureModuleIcons() {
        if (moduleIconsLoaded) return
        moduleIconsLoaded = true
        try {
            val enPath = AssetManager.getResourcePath(
                Identifier.fromNamespaceAndPath("aporia", "texture/modules/enabled.png"))
            if (enPath != null) moduleEnabledId = r.loadImage(enPath)
        } catch (_: Exception) {}
        try {
            val disPath = AssetManager.getResourcePath(
                Identifier.fromNamespaceAndPath("aporia", "texture/modules/disabled.png"))
            if (disPath != null) moduleDisabledId = r.loadImage(disPath)
        } catch (_: Exception) {}
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

        val now = System.currentTimeMillis()
        for (mod in ModuleManager.getAll()) {
            val prev = cachedModuleStates[mod.name]
            val curr = mod.isEnabled
            if (prev != null && prev != curr) {
                lastModuleName = mod.name
                lastModuleEnabled = curr
                lastModuleTime = now
            }
            cachedModuleStates[mod.name] = curr
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
        val now = System.currentTimeMillis()
        val hasRecentToggle = lastModuleName != null && now - lastModuleTime < 2000

        state = when {
            mediaPresent -> State.MEDIA
            hasBossbar && !NoRender.hideBossBar -> State.BOSSBAR
            hasRecentToggle -> State.MODULE_INFO
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
            expandStart = now
        } else if (!overPill && isExpanded && now - expandStart > 500) {
            isExpanded = false
        }

        expandSpring.setTarget(if (isExpanded) 1f else 0f)
        expandSpring.update(0.016f)
        val expandFrac = expandSpring.value()
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
            else -> colorUtil.rgba(12, 18, 22, 220)
        }
        val radius = PILL_RADIUS + (if (isMedia) (EXPANDED_RADIUS - PILL_RADIUS) * expandFrac else 0f)
        pillRect = floatArrayOf(cx, y, pillW, pillH)

        // Time pill — visible in collapsed state even during media
        val showClock = !isMedia || expandFrac < 0.3f
        val timeOpacity = (if (showClock) 1f - (expandFrac / 0.3f).coerceIn(0f, 1f) else 0f)
        val timeSlide = -expandFrac * 60f
        val wTime = r.getTextWidth("regular", time, 9f) + 12
        val lx = cx - 4 - wTime + timeSlide
        if (timeOpacity > 0.01f) {
            val timeColor = colorUtil.rgba(12, 18, 22, (220 * timeOpacity).toInt())
            if (blur) r.drawRectBlurred(lx, y, wTime, PILL_H, radius, timeColor)
            else r.drawRect(lx, y, wTime, PILL_H, radius, timeColor)
            r.drawText("regular", time, lx + 6, y + 5.5f, 9f,
                colorUtil.rgba(255, 255, 255, (255 * timeOpacity).toInt()))
        }

        // Main pill
        if (blur) r.drawRectBlurred(cx, y, pillW, pillH, radius, bgColor)
        else r.drawRect(cx, y, pillW, pillH, radius, bgColor)

        val ax = cx + 6
        val ay = y + 3f
        var textX = ax + 18f

        val white = colorUtil.rgba(255, 255, 255, 255)

        when (state) {
            State.IDLE -> {
                renderAvatar(r, ax, ay, 14, mc, mode, blur)
                r.drawText("bold", name, textX, y + 5.5f, 9f, white)
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

                val availableW = (cx + pillW - 6 - textX).coerceAtLeast(0f)
                val fullW = r.getTextWidth("regular", if (typing) display else title, 9f)

                if (fullW > availableW || typing) {
                    val visibleW = availableW
                    var titleX = textX
                    if (fullW > visibleW && !typing) {
                        if (title != marqueeTitle) { marqueeTitle = title; marqueeStart = now }
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
                    }
                    scissorClip(mc, textX, y, visibleW, if (isMedia && expandFrac > 0.3f) pillH else PILL_H) {
                        r.drawText("regular", if (typing) display else title, titleX, y + 5.5f, 9f, white)
                    }
                } else {
                    r.drawText("regular", title, textX, y + 5.5f, 9f, white)
                }

                // Media controls — centered on X, slightly below center Y
                val ctrlAlpha = expandFrac.coerceIn(0f, 1f)
                if (ctrlAlpha > 0.01f) {
                    val aMul = (ctrlAlpha * 255).toInt().coerceIn(0, 255)
                    val cWhite = colorUtil.rgba(255, 255, 255, aMul)
                    val cHover = colorUtil.rgba(255, 255, 255, 255)

                    val ctrlFrac = expandFrac.coerceIn(0f, 1f)
                    val ctrlXFrom = cx + pillW - 50f
                    val ctrlXTo = cx + pillW / 2f - 22f
                    val ctrlX = ctrlXFrom + (ctrlXTo - ctrlXFrom) * ctrlFrac
                    val ctrlYFrom = y + 9f
                    val ctrlYTo = y + pillH / 2f + 5f
                    val ctrlY = ctrlYFrom + (ctrlYTo - ctrlYFrom) * ctrlFrac
                    val prevX = ctrlX; val playX = ctrlX + 18f; val nextX = ctrlX + 36f

                    hoverPrev = mx >= prevX && mx < prevX + 8f && my >= ctrlY && my < ctrlY + 8f
                    hoverPlay = mx >= playX && mx < playX + 8f && my >= ctrlY && my < ctrlY + 8f
                    hoverNext = mx >= nextX && mx < nextX + 8f && my >= ctrlY && my < ctrlY + 8f

                    r.drawTriangle(prevX + 8f, ctrlY, prevX, ctrlY + 4f, prevX + 8f, ctrlY + 8f, if (hoverPrev) cHover else cWhite)
                    if (bgPlaying) {
                        r.drawRect(playX, ctrlY, 3f, 8f, 0f, if (hoverPlay) cHover else cWhite)
                        r.drawRect(playX + 5f, ctrlY, 3f, 8f, 0f, if (hoverPlay) cHover else cWhite)
                    } else {
                        r.drawTriangle(playX, ctrlY, playX, ctrlY + 8f, playX + 8f, ctrlY + 4f, if (hoverPlay) cHover else cWhite)
                    }
                    r.drawTriangle(nextX, ctrlY, nextX + 8f, ctrlY + 4f, nextX, ctrlY + 8f, if (hoverNext) cHover else cWhite)
                }

                // Seekbar above media controls
                if (expandFrac > 0.5f && bgDuration > 0) {
                    val seekFrac = (bgPosition.toFloat() / bgDuration.toFloat()).coerceIn(0f, 1f)
                    val seekBarX = cx + 6f
                    val seekBarW = pillW - 12f
                    val seekBarY = y + pillH / 2f - 3f
                    r.drawRect(seekBarX, seekBarY, seekBarW, 2f, 1f, colorUtil.rgba(255, 255, 255, 40))
                    r.drawRect(seekBarX, seekBarY, seekBarW * seekFrac, 2f, 1f, colorUtil.rgba(255, 255, 255, 200))

                    val timeStr = "${formatTime(bgPosition)}/${formatTime(bgDuration)}"
                    val timeW = r.getTextWidth("regular", timeStr, 6f)
                    r.drawText("regular", timeStr, cx + pillW - timeW - 6f, seekBarY - 8f, 6f, colorUtil.rgba(200, 200, 200, 255))
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

                if (fullW > visibleW || typing) {
                    if (fullW > visibleW && !typing) {
                        if (bbTitle != marqueeTitle) { marqueeTitle = bbTitle; marqueeStart = now }
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
                    }
                    scissorClip(mc, textX, y, visibleW, PILL_H) {
                        r.drawText("regular", if (typing) display else bbTitle, titleX, y + 5.5f, 9f, white)
                    }
                } else {
                    r.drawText("regular", bbTitle, textX, y + 5.5f, 9f, white)
                }

                val barX = cx + 6f
                val barY = y + PILL_H - 5f
                val barW = pillW - 12f
                r.drawRect(barX, barY, barW, 2f, 1f, colorUtil.rgba(255, 255, 255, 30))
                r.drawRect(barX, barY, barW * bossbarProgress, 2f, 1f, colorUtil.rgba(255, 255, 255, 200))
            }
            State.MODULE_INFO -> {
                ensureModuleIcons()
                renderAvatar(r, ax, ay, 14, mc, mode, blur)

                val modName = lastModuleName ?: "Module"
                if (lastModuleEnabled) {
                    if (moduleEnabledId != null) {
                        r.drawImage(cx + pillW - 22f, y + 3f, 14f, 14f, moduleEnabledId, 3f)
                    }
                } else {
                    if (moduleDisabledId != null) {
                        r.drawImage(cx + pillW - 22f, y + 3f, 14f, 14f, moduleDisabledId, 3f)
                    }
                }

                r.drawText("regular", modName, textX, y + 5.5f, 9f, white)
            }
        }
    }

    private fun formatTime(t: Long): String {
        if (t <= 0) return "0:00"
        val sec = when {
            t > 100000000L -> t / 10000000   // 100ns ticks
            else -> t / 1000                  // millis
        }
        val min = sec / 60
        val s = sec % 60
        return "${min}:${s.toString().padStart(2, '0')}"
    }

    @JvmStatic
    fun handleMediaClick(x: Double, y: Double, button: Int): Boolean {
        if (state != State.MEDIA) return false
        if (button != 0) return false
        val cx = pillRect[0]; val cy = pillRect[1]; val cw = pillRect[2]; val ch = pillRect[3]
        val frac = ((ch - PILL_H) / (MEDIA_EXPANDED_H - PILL_H)).coerceIn(0f, 1f)
        val ctrlX = cx + cw - 50f + (cw / 2f - 22f - (cw - 50f)) * frac
        val ctrlY = cy + 6f + (ch / 2f + 2f - 6f) * frac
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
        bgPosition = 0; bgDuration = 0
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
                                bgPosition = info.position
                                bgDuration = info.duration
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
