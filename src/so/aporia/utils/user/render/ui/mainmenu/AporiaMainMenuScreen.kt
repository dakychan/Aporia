package so.aporia.utils.user.render.ui.mainmenu

import so.aporia.utils.imports.*
import com.chaos.annotation.Obfuscate
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.joml.Vector3f
import so.aporia.utils.user.render.animation.Animator
import so.aporia.utils.user.render.animation.Easing
import so.aporia.utils.user.render.core.AporiaRenderer
import so.aporia.utils.user.render.core.BlurRenderer
import so.aporia.utils.user.render.theme.ThemeManager.Theme
import java.io.File
import net.minecraft.client.renderer.texture.TextureAtlas
import kotlin.math.*
import com.chaos.annotation.ChaosNative
@Obfuscate
@OnlyIn(Dist.CLIENT)
@ChaosNative
class AporiaMainMenuScreen : Screen(Component.literal("Aporia")) {

    companion object {
        private const val LOCK_TIMEOUT_MS = 5L * 60L * 1000L
        private const val SCREENSHOT_PATH = "screenshots/_mainmenu_temp.png"
    }

    enum class Mode { SCREENSHOT, SCENE_3D }

    private var mode = Mode.SCREENSHOT

    /* === Lock screen === */
    private var locked = false
    private var lastActivity = 0L
    private var lockFadeAnim = Animator(500, Easing::cubicOut)

    /* === Layout cache === */
    private var sw = 0; private var sh = 0
    private var titleSize = 0f; private var subSize = 0f; private var btnTextSize = 0f
    private var clockSize = 0f; private var dateSize = 0f
    private var btnW = 0; private var btnH = 0; private var btnGap = 0
    private var logoY = 0f; private var subY = 0f; private var btnStartY = 0f

    /* === Buttons === */
    private val buttons = mutableListOf<MenuButton>()

    /* === Mode toggle === */
    private var toggleBtnX = 0; private var toggleBtnY = 0
    private val toggleBtnW = 130; private val toggleBtnH = 22

    /* === Animation === */
    private val loadAnim = Animator(800, Easing::sineInOut)

    /* === Screenshot === */
    private var screenshotId: Identifier? = null
    private var screenshotFailed = false

    /* === 3D Scene === */
    private var camPos = Vector3f(0f, 4f, -8f)
    private var camYaw = 25f
    private var camPitch = -15f
    private var lastMouseX = 0
    private var lastMouseY = 0
    private var mouseDown = false
    private val pressedKeys = mutableSetOf<Int>()
    private var sceneVariant = 0 // 0 = cross, 1 = checkerboard
    private var sceneBlocks = listOf<BlockDef>()

    /* ==== Block rendering data ==== */
    private enum class BlockType(val topTex: String, val sideTex: String, val bottomTex: String) {
        GRASS("grass_block_top", "grass_block_side", "dirt"),
        STONE("stone", "stone", "stone"),
        COBBLESTONE("cobblestone", "cobblestone", "cobblestone"),
        STONE_BRICKS("stone_bricks", "stone_bricks", "stone_bricks"),
        BEDROCK("bedrock", "bedrock", "bedrock"),
        GOLD_BLOCK("gold_block", "gold_block", "gold_block"),
        OAK_LOG("oak_log_top", "oak_log", "oak_log_top"),
        DIRT("dirt", "dirt", "dirt"),
        POPPY("poppy", "poppy", "poppy"),
    }

    private data class BlockDef(val x: Float, val y: Float, val z: Float, val type: BlockType)

    init {
        lastActivity = System.currentTimeMillis()
        initScreenshot()
        buildScene()
    }

    override fun init() {
        lastActivity = System.currentTimeMillis()
        locked = false
        try { calcLayout() } catch (_: Throwable) {}
        loadAnim.play()
        lockFadeAnim = Animator(500, Easing::cubicOut)
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        calcLayout()
    }

    // ============================================================
    // Layout
    // ============================================================

    private fun calcLayout() {
        sw = width; sh = height
        val scale = mc.window.guiScale
        val baseUnit = min(sw, sh) / 100f

        btnW = maxOf(90, minOf(150, (sw * 0.17f).toInt()))
        btnH = maxOf(24, minOf(36, (baseUnit * 3.5f).toInt()))
        btnGap = maxOf(4, (baseUnit * 0.8f).toInt())

        titleSize = maxOf(18f, minOf(42f, sw * 0.045f))
        if (scale >= 3f) titleSize *= 0.75f else if (scale >= 2f) titleSize *= 0.85f

        subSize = maxOf(9f, minOf(16f, sw * 0.016f))
        if (scale >= 3f) subSize *= 0.75f else if (scale >= 2f) subSize *= 0.85f

        btnTextSize = maxOf(9f, minOf(13f, sw * 0.013f))
        if (scale >= 3f) btnTextSize *= 0.8f else if (scale >= 2f) btnTextSize *= 0.9f

        clockSize = maxOf(32f, minOf(72f, sw * 0.07f))
        dateSize = maxOf(11f, minOf(20f, sw * 0.02f))

        val centerY = sh / 2f
        logoY = centerY - sh * 0.38f
        subY = centerY - sh * 0.28f

        val keys = listOf("menu.singleplayer", "menu.multiplayer", "menu.settings", "menu.exit")
        val totalBtnH = keys.size * btnH + (keys.size - 1) * btnGap
        btnStartY = centerY + sh * 0.02f - totalBtnH / 2f

        buttons.clear()
        for ((i, key) in keys.withIndex()) {
            val bx = (sw - btnW) / 2
            val by = (btnStartY + i * (btnH + btnGap)).toInt()
            buttons.add(MenuButton(bx, by, btnW, btnH, key, i, i * 60))
        }

        toggleBtnX = (sw - toggleBtnW) / 2
        toggleBtnY = 8
    }

    // ============================================================
    // Screenshot management
    // ============================================================

    private fun initScreenshot() {
        val path = mc.gameDirectory.toPath().resolve(SCREENSHOT_PATH)
        val file = path.toFile()
        if (file.exists()) {
            val id = r.loadImage(path)
            if (id != null) {
                screenshotId = id
                return
            }
        }
        screenshotFailed = true
    }

    // ============================================================
    // 3D Scene building
    // ============================================================

    private fun buildScene() {
        sceneBlocks = if (sceneVariant == 0) buildCross() else buildCheckerboard()
    }

    private fun buildCross(): List<BlockDef> {
        val list = mutableListOf<BlockDef>()
        // 6x6 grass floor at y=0
        for (x in -3 until 3) {
            for (z in -3 until 3) {
                list.add(BlockDef(x.toFloat(), 0f, z.toFloat(), BlockType.GRASS))
            }
        }
        // Stone cross pillars at y=1 and y=2
        val place = { x: Int, z: Int ->
            list.add(BlockDef(x.toFloat(), 1f, z.toFloat(), BlockType.STONE))
            list.add(BlockDef(x.toFloat(), 2f, z.toFloat(), BlockType.STONE))
        }
        place(0, 0); place(1, 0); place(-1, 0); place(0, 1); place(0, -1)
        // Bedrock base under center
        list.add(BlockDef(0f, -1f, 0f, BlockType.BEDROCK))
        // Gold block on top
        list.add(BlockDef(0f, 3f, 0f, BlockType.GOLD_BLOCK))
        return list
    }

    private fun buildCheckerboard(): List<BlockDef> {
        val list = mutableListOf<BlockDef>()
        // 6x6 checkerboard floor
        for (x in -3 until 3) {
            for (z in -3 until 3) {
                val type = if ((x + z) % 2 == 0) BlockType.STONE else BlockType.COBBLESTONE
                list.add(BlockDef(x.toFloat(), 0f, z.toFloat(), type))
            }
        }
        // Stone brick pillar center
        list.add(BlockDef(0f, 1f, 0f, BlockType.STONE_BRICKS))
        list.add(BlockDef(0f, 2f, 0f, BlockType.STONE_BRICKS))
        // Gold block on top
        list.add(BlockDef(0f, 3f, 0f, BlockType.GOLD_BLOCK))
        return list
    }

    // ============================================================
    // 3D projection
    // ============================================================

    private fun renderScene3D(r: AporiaRenderer) {
        val atlas: net.minecraft.client.renderer.texture.TextureAtlas
        try {
            @Suppress("DEPRECATION")
            val tex = mc.textureManager.getTexture(TextureAtlas.LOCATION_BLOCKS)
            atlas = tex as net.minecraft.client.renderer.texture.TextureAtlas
        } catch (_: Exception) {
            return // atlas not ready yet
        }
        @Suppress("DEPRECATION")
        val atlasId = TextureAtlas.LOCATION_BLOCKS

        val yawRad = Math.toRadians(camYaw.toDouble()).toFloat()
        val pitchRad = Math.toRadians(camPitch.toDouble()).toFloat()
        val sinY = sin(yawRad); val cosY = cos(yawRad)
        val sinP = sin(pitchRad); val cosP = cos(pitchRad)

        data class FaceDraw(
            val minX: Float, val minY: Float, val maxX: Float, val maxY: Float,
            val u0: Float, val v0: Float, val u1: Float, val v1: Float,
            val depth: Float
        )

        val drawList = mutableListOf<FaceDraw>()

        for (b in sceneBlocks) {
            val bx = b.x; val by = b.y; val bz = b.z
            val topSprite = atlas.getSprite(Identifier.withDefaultNamespace("block/${b.type.topTex}"))
            val sideSprite = atlas.getSprite(Identifier.withDefaultNamespace("block/${b.type.sideTex}"))
            val bottomSprite = atlas.getSprite(Identifier.withDefaultNamespace("block/${b.type.bottomTex}"))

            data class Def(val sprite: net.minecraft.client.renderer.texture.TextureAtlasSprite,
                           val corners: List<Triple<Float,Float,Float>>,
                           val nx: Float, val ny: Float, val nz: Float)

            val faceDefs = listOf(
                Def(topSprite,    listOf(Triple(bx,by+1,bz), Triple(bx+1,by+1,bz), Triple(bx+1,by+1,bz+1), Triple(bx,by+1,bz+1)), 0f, 1f, 0f),
                Def(bottomSprite, listOf(Triple(bx,by,bz),   Triple(bx+1,by,bz),   Triple(bx+1,by,bz+1),   Triple(bx,by,bz+1)),   0f,-1f, 0f),
                Def(sideSprite,   listOf(Triple(bx,by,bz+1), Triple(bx+1,by,bz+1), Triple(bx+1,by+1,bz+1), Triple(bx,by+1,bz+1)), 0f, 0f, 1f),
                Def(sideSprite,   listOf(Triple(bx+1,by,bz), Triple(bx,by,bz),     Triple(bx,by+1,bz),     Triple(bx+1,by+1,bz)), 0f, 0f,-1f),
                Def(sideSprite,   listOf(Triple(bx+1,by,bz), Triple(bx+1,by,bz+1), Triple(bx+1,by+1,bz+1), Triple(bx+1,by+1,bz)), 1f, 0f, 0f),
                Def(sideSprite,   listOf(Triple(bx,by,bz+1), Triple(bx,by,bz),     Triple(bx,by+1,bz),     Triple(bx,by+1,bz+1)),-1f, 0f, 0f),
            )

            for (def in faceDefs) {
                val cx = def.corners.map { it.first }.average().toFloat()
                val cy = def.corners.map { it.second }.average().toFloat()
                val cz = def.corners.map { it.third }.average().toFloat()
                val vdx = cx - camPos.x; val vdy = cy - camPos.y; val vdz = cz - camPos.z
                if (def.nx * vdx + def.ny * vdy + def.nz * vdz <= 0f) continue

                val projected = def.corners.map { project(it.first, it.second, it.third) }
                if (projected.any { it == null }) continue
                val pts = projected.map { it!! }

                var area = 0f
                for (i in 0..3) {
                    val j = (i + 1) % 4
                    area += pts[i].first * pts[j].second - pts[j].first * pts[i].second
                }
                if (area <= 0f) continue

                val minX = pts.minOf { it.first }
                val minY = pts.minOf { it.second }
                val maxX = pts.maxOf { it.first }
                val maxY = pts.maxOf { it.second }
                if (maxX <= minX || maxY <= minY) continue

                val rz = vdx * sinY + vdz * cosY
                val depth = vdy * sinP + rz * cosP

                drawList.add(FaceDraw(minX, minY, maxX, maxY,
                    def.sprite.getU0(), def.sprite.getV0(), def.sprite.getU1(), def.sprite.getV1(),
                    depth))
            }
        }

        drawList.sortByDescending { it.depth }

        for (face in drawList) {
            r.drawImageCropped(
                face.minX, face.minY,
                face.maxX - face.minX, face.maxY - face.minY,
                atlasId, 0f,
                face.u0, face.v0, face.u1, face.v1
            )
        }
    }

    private fun project(vx: Float, vy: Float, vz: Float): Pair<Float, Float>? {
        val dx = vx - camPos.x
        val dy = vy - camPos.y
        val dz = vz - camPos.z
        val yawRad = Math.toRadians(camYaw.toDouble()).toFloat()
        val pitchRad = Math.toRadians(camPitch.toDouble()).toFloat()
        val cosY = cos(yawRad); val sinY = sin(yawRad)
        val cosP = cos(pitchRad); val sinP = sin(pitchRad)
        val rx = dx * cosY - dz * sinY
        val ry2 = dy
        val rz = dx * sinY + dz * cosY
        val px = rx
        val py = ry2 * cosP - rz * sinP
        val pz = ry2 * sinP + rz * cosP
        if (pz < 0.1f) return null
        val fov = 65f * (PI.toFloat() / 180f)
        val aspect = sw.toFloat() / sh.toFloat().coerceAtLeast(1f)
        val f = 1f / tan(fov / 2f)
        val sx = px / pz * f / aspect
        val sy = py / pz * f
        val screenX = (sx * 0.5f + 0.5f) * sw.toFloat()
        val screenY = (-sy * 0.5f + 0.5f) * sh.toFloat()
        return Pair(screenX, screenY)
    }

    // ============================================================
    // Render
    // ============================================================

    override fun render(gfx: GuiGraphics, mx: Int, my: Int, delta: Float) {
        val now = System.currentTimeMillis()
        var safe = true

        try {
            BlurRenderer.prepareFrameBlur(mc, 30f, 0.5f)
            renderBackground(r)
        } catch (_: Throwable) {
            safe = false
            // GL resources disposed (world unloaded) — draw plain black bg via vanilla API
            if (sw > 0 && sh > 0) {
                gfx.fill(0, 0, sw, sh, -16777216)
            }
        }

        updateSceneCamera()

        if (!locked && now - lastActivity > LOCK_TIMEOUT_MS) {
            locked = true
            lockFadeAnim = Animator(500, Easing::cubicOut).also { it.play() }
        }

        if (locked) {
            try { renderLockScreen(r, gfx, now) } catch (_: Throwable) {}
            return
        }

        if (!safe) return

        val th = theme
        loadAnim.update()
        val prog = loadAnim.value()
        val lift = (1f - prog) * sh * 0.03f
        val logoAlpha = maxOf(0f, minOf(1f, (prog - 0.1f) / 0.4f))
        val subAlpha = maxOf(0f, minOf(1f, (prog - 0.3f) / 0.4f))
        val btnAlpha = maxOf(0f, minOf(1f, (prog - 0.4f) / 0.5f))

        try {
            renderModeToggle(r, th, mx, my, prog)

            if (logoAlpha > 0.01f) {
                val title = locale.get("menu.title")
                val tw = r.getTextWidth("bold", title, titleSize)
                val tx = (sw - tw) / 2f
                val tc = colorUtil.rgba(255, 255, 255, (logoAlpha * 255).toInt())
                r.drawText("bold", title, tx, logoY - lift, titleSize, tc)
            }

            if (subAlpha > 0.01f) {
                val sub = locale.get("menu.subtitle")
                val sw2 = r.getTextWidth("regular", sub, subSize)
                val sx = (sw - sw2) / 2f
                val sc = colorUtil.rgba(255, 255, 255, (subAlpha * 255).toInt())
                r.drawText("regular", sub, sx, subY - lift, subSize, sc)
            }

            if (btnAlpha > 0.01f) {
                for (btn in buttons) {
                    btn.render(r, gfx, mx, my, btnAlpha, th)
                }
            }

            val ver = "Aporia v1.0"
            val vw = r.getTextWidth("regular", ver, 8f)
            r.drawText("regular", ver, sw - vw - 8, sh - 14f, 8f, colorUtil.rgba(255, 255, 255, (80 * prog).toInt()))
        } catch (_: Throwable) {}
    }

    private fun renderBackground(r: AporiaRenderer) {
        when (mode) {
            Mode.SCREENSHOT -> renderScreenshotBg(r)
            Mode.SCENE_3D -> try { renderScene3D(r) } catch (_: Throwable) {}
        }
    }

    // ============================================================
    // Screenshot background
    // ============================================================

    private fun renderScreenshotBg(r: AporiaRenderer) {
        val id = screenshotId
        if (id != null) {
            r.drawImage(0f, 0f, sw.toFloat(), sh.toFloat(), id)
        } else {
            r.drawRect(0f, 0f, sw.toFloat(), sh.toFloat(), 0f, -16777216)
        }
    }



    // ============================================================
    // Mode toggle
    // ============================================================

    private fun renderModeToggle(r: AporiaRenderer, th: Theme, mx: Int, my: Int, prog: Float) {
        val alpha = ((th.mmButtonBg shr 24) and 0xFF) * prog
        val bgColor = colorUtil.rgba(
            (th.mmButtonBg shr 16) and 0xFF, (th.mmButtonBg shr 8) and 0xFF, th.mmButtonBg and 0xFF,
            alpha.toInt())
        r.drawRectBlurred(toggleBtnX.toFloat(), toggleBtnY.toFloat(), toggleBtnW.toFloat(), toggleBtnH.toFloat(),
            toggleBtnH / 2f, bgColor, 10f)

        val labels = listOf("Screenshot", "3D Scene")
        val itemW = toggleBtnW / labels.size
        for (i in labels.indices) {
            val sel = (mode == Mode.SCREENSHOT && i == 0) || (mode == Mode.SCENE_3D && i == 1)
            val lx = toggleBtnX + i * itemW
            val ly = toggleBtnY
            if (sel) {
                r.drawRect(lx.toFloat(), ly.toFloat(), itemW.toFloat(), toggleBtnH.toFloat(), 0f,
                    colorUtil.rgba(255, 255, 255, 40))
            }
            val fgColor = if (sel) -1 else colorUtil.rgba(255, 255, 255, 150)
            val txtSize = 9f
            val tw = r.getTextWidth("regular", labels[i], txtSize)
            r.drawText("regular", labels[i],
                lx + (itemW - tw) / 2f,
                ly + (toggleBtnH - txtSize) / 2f - 1f,
                txtSize, fgColor)
        }
    }

    // ============================================================
    // Lock screen
    // ============================================================

    private fun renderLockScreen(r: AporiaRenderer, gfx: GuiGraphics, now: Long) {
        lockFadeAnim.update()
        val alpha = lockFadeAnim.value()
        val cal = java.util.Calendar.getInstance()
        val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = cal.get(java.util.Calendar.MINUTE)
        val dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK) - 1
        val month = cal.get(java.util.Calendar.MONTH) + 1
        val year = cal.get(java.util.Calendar.YEAR)

        val timeStr = String.format(locale.get("lock.time_format"), hour, minute)
        val dayName = locale.get("lock.day_$dayOfWeek")
        val dateStr = "$dayName, ${String.format("%02d", month)}.$year"

        val floatOffset = (sin(now / 1500.0) * 6f).toFloat()

        val timeW = r.getTextWidth("bold", timeStr, clockSize)
        val timeX = (sw - timeW) / 2f
        val timeY = sh / 2f - clockSize * 0.6f + floatOffset
        r.drawText("bold", timeStr, timeX, timeY, clockSize, colorUtil.rgba(255, 255, 255, (alpha * 255).toInt()))

        val dateW = r.getTextWidth("regular", dateStr, dateSize)
        val dateX = (sw - dateW) / 2f
        val dateY = timeY + clockSize * 0.55f + floatOffset * 0.5f
        r.drawText("regular", dateStr, dateX, dateY, dateSize, colorUtil.rgba(255, 255, 255, (alpha * 0.6f * 255).toInt()))

        val hint = locale.get("lock.click_hint")
        val hintW = r.getTextWidth("regular", hint, 10f)
        val hintX = (sw - hintW) / 2f
        val hintY = sh / 2f + sh * 0.12f
        val pulse = (sin(now / 800.0) * 0.3 + 0.7).toFloat()
        r.drawText("regular", hint, hintX, hintY, 10f, colorUtil.rgba(255, 255, 255, (80 * alpha * pulse).toInt()))
    }

    // ============================================================
    // Input handlers
    // ============================================================

    override fun mouseClicked(e: MouseButtonEvent, b: Boolean): Boolean {
        lastActivity = System.currentTimeMillis()
        if (locked) { locked = false; return true }
        val mx = e.x().toInt(); val my = e.y().toInt()
        // Mode toggle
        if (my in toggleBtnY..toggleBtnY + toggleBtnH && mx in toggleBtnX..toggleBtnX + toggleBtnW) {
            val idx = (mx - toggleBtnX) / (toggleBtnW / 2)
            if (idx != 0 && idx != 1) return true
            val newMode = if (idx == 0) Mode.SCREENSHOT else Mode.SCENE_3D
            if (newMode != mode) {
                mode = newMode
                if (mode == Mode.SCENE_3D) {
                    buildScene()
                }
            }
            return true
        }
        // Menu buttons
        for (btn in buttons) {
            if (btn.clicked(mx, my, e.button())) {
                onButtonAction(btn.id)
                return true
            }
        }
        // 3D scene mouse look (start drag)
        if (mode == Mode.SCENE_3D && e.button() == 0) {
            mouseDown = true
            lastMouseX = mx; lastMouseY = my
        }
        return super.mouseClicked(e, b)
    }

    override fun mouseReleased(e: MouseButtonEvent): Boolean {
        if (e.button() == 0) mouseDown = false
        return super.mouseReleased(e)
    }

    override fun mouseDragged(e: MouseButtonEvent, dx: Double, dy: Double): Boolean {
        lastActivity = System.currentTimeMillis()
        if (mode == Mode.SCENE_3D && mouseDown) {
            camYaw += dx.toFloat() * 0.3f
            camPitch = (camPitch + dy.toFloat() * 0.3f).coerceIn(-89f, 89f)
        }
        return super.mouseDragged(e, dx, dy)
    }

    override fun keyPressed(e: KeyEvent): Boolean {
        lastActivity = System.currentTimeMillis()
        if (locked) { locked = false; return true }
        if (mode == Mode.SCENE_3D) {
            pressedKeys.add(e.scancode())
        }
        if (e.isEscape) { onClose(); return true }
        return super.keyPressed(e)
    }

    override fun keyReleased(e: KeyEvent): Boolean {
        pressedKeys.remove(e.scancode())
        return super.keyReleased(e)
    }

    // Update camera on tick (called from render)
    // We handle it in the main render since there's no tick in Screen

    private fun updateSceneCamera() {
        if (mode != Mode.SCENE_3D) return
        val speed = 0.15f
        val yawRad = Math.toRadians(camYaw.toDouble()).toFloat()
        val fwd = Vector3f(-sin(yawRad), 0f, -cos(yawRad))
        val right = Vector3f(cos(yawRad), 0f, -sin(yawRad))
        for (key in pressedKeys) {
            when (key) {
                17 -> { camPos.add(fwd.x * speed, 0f, fwd.z * speed) } // W
                31 -> { camPos.add(-fwd.x * speed, 0f, -fwd.z * speed) } // S
                30 -> { camPos.add(-right.x * speed, 0f, -right.z * speed) } // A
                32 -> { camPos.add(right.x * speed, 0f, right.z * speed) } // D
                57 -> camPos.y += speed // Space
                42, 54 -> camPos.y -= speed // Shift (L/R)
            }
        }
    }

    override fun charTyped(e: CharacterEvent): Boolean {
        lastActivity = System.currentTimeMillis()
        return super.charTyped(e)
    }

    private fun onButtonAction(id: Int) {
        when (id) {
            0 -> mc.setScreen(net.minecraft.client.gui.screens.worldselection.SelectWorldScreen(this))
            1 -> mc.setScreen(net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen(this))
            2 -> mc.setScreen(net.minecraft.client.gui.screens.options.OptionsScreen(this, mc.options))
            3 -> mc.execute { mc.stop() }
        }
    }

    override fun isPauseScreen() = false

    override fun renderBackground(gfx: GuiGraphics, mx: Int, my: Int, delta: Float) {}
    override fun removed() {
        try { r.flush() } catch (_: Throwable) {}
    }

    // ============================================================
    // MenuButton
    // ============================================================

    private inner class MenuButton(
        val x: Int, val y: Int, val w: Int, val h: Int,
        val localeKey: String, val id: Int, private val appearDelay: Int
    ) {
        private val hoverAnim = Animator(200, Easing::cubicOut)
        private val appearAnim = Animator(400, Easing::cubicOut)

        fun render(r: AporiaRenderer, gfx: GuiGraphics, mx: Int, my: Int, globalAlpha: Float, th: Theme) {
            val hov = mx in x until x + w && my in y until y + h
            if (hov && !hoverAnim.isPlaying() && hoverAnim.value() < 0.99f) hoverAnim.play()
            if (!hov && !hoverAnim.isPlaying() && hoverAnim.value() > 0.01f) hoverAnim.reverse()
            hoverAnim.update()
            if (!appearAnim.isPlaying() && appearAnim.value() < 0.99f) appearAnim.play()
            appearAnim.update()

            val hp = hoverAnim.value()
            val ap = appearAnim.value()
            val alpha = globalAlpha * ap
            val slideY = (1f - ap) * h * 0.5f
            val drawY = y + slideY.toInt()

            if (alpha > 0.01f) {
                val bgA = ((th.mmButtonBg shr 24) and 0xFF) * alpha.toInt()
                val bgR = (th.mmButtonBg shr 16) and 0xFF
                val bgG = (th.mmButtonBg shr 8) and 0xFF
                val bgB = th.mmButtonBg and 0xFF
                val glassTint = colorUtil.rgba(bgR, bgG, bgB, bgA)
                r.drawRectBlurred(x.toFloat(), drawY.toFloat(), w.toFloat(), h.toFloat(), h * 0.5f, glassTint, 10f)
            }

            val rVal = h * 0.5f

            val bgAlpha = ((10 + 20 * hp) * alpha).toInt()
            r.drawRect(x.toFloat(), drawY.toFloat(), w.toFloat(), h.toFloat(), rVal,
                colorUtil.rgba(255, 255, 255, bgAlpha))

            val borderAlpha = ((30 + 50 * hp) * alpha).toInt()
            r.drawStroke(x.toFloat(), drawY.toFloat(), w.toFloat(), h.toFloat(), rVal, 1f, 1, 0f,
                colorUtil.rgba(255, 255, 255, borderAlpha))

            val txtSize = maxOf(9f, minOf(13f, h * 0.38f))
            val text = locale.get(localeKey)
            val textW = r.getTextWidth("regular", text, txtSize)
            val textX = x + (w - textW) / 2f
            val textY = drawY + (h - txtSize) / 2f - 1f
            r.drawText("regular", text, textX, textY, txtSize,
                colorUtil.rgba(255, 255, 255, ((180 + 75 * hp) * alpha).toInt()))
        }

        fun clicked(mx: Int, my: Int, button: Int): Boolean {
            if (button != 0) return false
            return mx in x until x + w && my in y until y + h
        }
    }
}
