package so.aporia.module.impl.render

import so.aporia.utils.imports.*
import com.chaos.annotation.Obfuscate
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.ChatScreen
import org.lwjgl.glfw.GLFW
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.impl.render.hud.DynamicIsland
import so.aporia.module.impl.render.hud.TargetHud
import so.aporia.module.settings.BooleanSetting
import so.aporia.module.settings.SelectSetting
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.MouseClickEvent
import so.aporia.utils.user.render.core.AporiaRenderer
import so.aporia.utils.user.render.font.Fonts
import java.text.DecimalFormat
import kotlin.math.roundToInt
import com.chaos.annotation.ChaosNative
@Obfuscate
@ChaosNative
class Hud : Module("HUD", Category.VISUAL) {

    val watermarkMode = SelectSetting("Dynamic Island", "Avatar display mode")
        .value("Auto", "Logo", "Avatar", "Skin")
        .selected("Auto")

    val showFps = BooleanSetting("Show FPS", "Display FPS counter", true)
    val showCoords = BooleanSetting("Show Coords", "Display coordinates", true)
    val showPing = BooleanSetting("Show Ping", "Display latency", true)
    val showTargetHud = BooleanSetting("Target HUD", "Display target information", true)
    val showPlayerCount = BooleanSetting("Player Count", "Display online player count", true)
    val showSpeed = BooleanSetting("Show Speed", "Display movement speed", false)
    val showDirection = BooleanSetting("Show Direction", "Display cardinal direction", false)

    private val panelPos = ElementRect(0f, 0f, 0f, 0f)
    private var isDragging = false
    private var dragTarget = ""
    private var dragOffX = 0f
    private var dragOffY = 0f
    private var isChatOpen = false
    private var mouseX = 0f
    private var mouseY = 0f

    init { enable() }

    override val settings = listOf(watermarkMode, showFps, showCoords, showPing, showTargetHud, showPlayerCount, showSpeed, showDirection)

    override fun onEnable() { bus.register(this) }
    override fun onDisable() { bus.unregister(this) }

    @EventHandler
    fun onMouseClick(e: MouseClickEvent) {
        if (e.action() != MouseClickEvent.Action.PRESS) return
        if (DynamicIsland.handleMediaClick(e.x(), e.y(), e.button())) { e.cancel(); return }
        val x = e.x().toFloat()
        val y = e.y().toFloat()

        // Check island (pill) hit
        val pill = DynamicIsland.getPillRect()
        if (pill[2] > 0f && x >= pill[0] && x < pill[0] + pill[2] && y >= pill[1] && y < pill[1] + pill[3]) {
            isDragging = true; dragTarget = "island"
            dragOffX = x - pill[0]; dragOffY = y - pill[1]
            e.cancel(); return
        }
        // Check target hud hit
        val tw = TargetHud.W; val th = TargetHud.H
        val tx = TargetHud.posX; val ty = TargetHud.posY
        if (showTargetHud.isEnabled && tw > 0f && x >= tx && x < tx + tw && y >= ty && y < ty + th) {
            isDragging = true; dragTarget = "target"
            dragOffX = x - tx; dragOffY = y - ty
            e.cancel(); return
        }
        // Check info panel hit
        if (panelPos.w > 0f && x >= panelPos.x && x < panelPos.x + panelPos.w && y >= panelPos.y && y < panelPos.y + panelPos.h) {
            isDragging = true; dragTarget = "panel"
            dragOffX = x - panelPos.x; dragOffY = y - panelPos.y
            e.cancel(); return
        }
    }

    fun render(gfx: GuiGraphicsExtractor, mx: Int, my: Int, delta: Float) {
        if (mc.player == null) return

        val sw = mc.window.guiScaledWidth
        val sh = mc.window.guiScaledHeight

        mouseX = mc.mouseHandler.getScaledXPos(mc.window).toFloat()
        mouseY = mc.mouseHandler.getScaledYPos(mc.window).toFloat()
        isChatOpen = mc.gui.screen() is ChatScreen

        if (isDragging &&
            GLFW.glfwGetMouseButton(mc.window.handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) != 1) {
            isDragging = false
        }

        val altDown = GLFW.glfwGetKey(mc.window.handle(), GLFW.GLFW_KEY_LEFT_ALT) == 1

        // Update drag positions
        if (isDragging) {
            val nx = mouseX - dragOffX
            val ny = mouseY - dragOffY
            val sx = if (altDown) snap(nx) else nx
            val sy = if (altDown) snap(ny) else ny
            when (dragTarget) {
                "island" -> { DynamicIsland.posX = sx; DynamicIsland.posY = sy }
                "target" -> { TargetHud.posX = sx; TargetHud.posY = sy }
                "panel" -> { panelPos.x = sx; panelPos.y = sy }
            }
        }

        DynamicIsland.render(r, parseMode(watermarkMode.getSelectedIndex()))

        if (showTargetHud.isEnabled) TargetHud.render(gfx)

        renderInfoPanel(r, mc, sw, sh)

        r.flush()

        if (isDragging && altDown) {
            r.drawRect(0f, 0f, sw.toFloat(), sh.toFloat(), 0f, colorUtil.rgba(0, 0, 0, 80))
            for (x in 0 until sw step 16) r.drawLine(x.toFloat(), 0f, x.toFloat(), sh.toFloat(), 0.5f, colorUtil.rgba(255, 255, 255, 30))
            for (y in 0 until sh step 16) r.drawLine(0f, y.toFloat(), sw.toFloat(), y.toFloat(), 0.5f, colorUtil.rgba(255, 255, 255, 30))
        }
    }

    private fun renderInfoPanel(r: AporiaRenderer, mc: Minecraft, sw: Int, sh: Int) {
        val lines = buildList {
            if (showFps.isEnabled) {
                val fps = mc.fps
                add("FPS: $fps" to when {
                    fps >= 60 -> colorUtil.rgba(85, 255, 85, 255)
                    fps >= 30 -> colorUtil.rgba(255, 255, 85, 255)
                    else -> colorUtil.rgba(255, 85, 85, 255)
                })
            }
            if (showPing.isEnabled) {
                val ping = mc.player?.connection?.getPlayerInfo(mc.player!!.uuid)?.latency ?: -1
                add("Ping: ${ping}ms" to when {
                    ping <= 0 -> colorUtil.rgba(160, 160, 160, 255)
                    ping <= 50 -> colorUtil.rgba(85, 255, 85, 255)
                    ping <= 150 -> colorUtil.rgba(255, 255, 85, 255)
                    else -> colorUtil.rgba(255, 85, 85, 255)
                })
            }
            if (showPlayerCount.isEnabled) {
                add("Players: ${mc.level?.players()?.size ?: 0}" to colorUtil.rgba(160, 200, 255, 255))
            }
            if (showCoords.isEnabled) {
                val pos = mc.player!!.position()
                add("XYZ: ${DecimalFormat("0.0").format(pos.x)} ${DecimalFormat("0.0").format(pos.y)} ${DecimalFormat("0.0").format(pos.z)}" to colorUtil.rgba(180, 180, 200, 255))
            }
            if (showDirection.isEnabled) {
                val yaw = ((mc.player!!.yRot % 360) + 360) % 360
                add(when {
                    yaw < 45 || yaw >= 315 -> "South"
                    yaw < 135 -> "West"
                    yaw < 225 -> "North"
                    else -> "East"
                } to colorUtil.rgba(200, 180, 140, 255))
            }
            if (showSpeed.isEnabled) {
                val vel = mc.player!!.deltaMovement
                val speed = Math.sqrt(vel.x * vel.x + vel.z * vel.z) * 20.0
                add("Speed: ${DecimalFormat("0.0").format(speed)} bps" to
                    if (speed > 10) colorUtil.rgba(85, 255, 85, 255) else colorUtil.rgba(200, 200, 200, 255))
            }
        }
        if (lines.isEmpty()) return

        val lineH = 11f; val pad = 5f; val margin = 4f
        val maxW = lines.maxOf { r.getTextWidth(Fonts.REGULAR, it.first, lineH) + pad * 2 }
        val bgH = lines.size * (lineH + 2f) + pad * 2

        val bgX = if (panelPos.w > 0f) panelPos.x else sw - maxW - margin
        val bgY = if (panelPos.w > 0f) panelPos.y else margin + 24f

        val blur = Beautifully.isBlurEnabled() && Beautifully.isFeatureEnabled("HUD Panel Blur")
        val c = colorUtil.rgba(12, 18, 22, 180)
        if (blur) r.drawRectBlurred(bgX, bgY, maxW, bgH, 6f, c, 3f, 15)
        else r.drawRect(bgX, bgY, maxW, bgH, 6f, c)

        panelPos.x = bgX; panelPos.y = bgY; panelPos.w = maxW; panelPos.h = bgH

        var textY = bgY + pad
        for ((text, color) in lines) {
            r.drawText(Fonts.REGULAR, text, bgX + pad, textY, lineH, color)
            textY += lineH + 2f
        }
    }

    private fun parseMode(idx: Int): DynamicIsland.Mode = when (idx) {
        1 -> DynamicIsland.Mode.LOGO
        2 -> DynamicIsland.Mode.AVATAR
        3 -> DynamicIsland.Mode.SKIN
        else -> DynamicIsland.Mode.AUTO
    }

    private fun snap(v: Float): Float = (v / 16).roundToInt() * 16f

    data class ElementRect(var x: Float, var y: Float, var w: Float, var h: Float)
}
