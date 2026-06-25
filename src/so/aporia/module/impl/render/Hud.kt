package so.aporia.module.impl.render

import com.chaos.annotation.Obfuscate
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.impl.render.hud.DynamicIsland
import so.aporia.module.impl.render.hud.TargetHud
import so.aporia.module.settings.BooleanSetting
import so.aporia.module.settings.SelectSetting
import so.aporia.utils.events.EventBus
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.MouseClickEvent
import so.aporia.utils.user.render.color.ColorUtil
import so.aporia.utils.user.render.core.AporiaRenderer
import so.aporia.utils.user.render.font.Fonts
import java.text.DecimalFormat

@Obfuscate
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

    init { enable() }

    override fun onEnable() { EventBus.register(this) }
    override fun onDisable() { EventBus.unregister(this) }

    @EventHandler
    fun onMouseClick(e: MouseClickEvent) {
        if (e.action() != MouseClickEvent.Action.PRESS) return
        if (DynamicIsland.handleMediaClick(e.x(), e.y(), e.button())) e.cancel()
    }

    fun render(gfx: GuiGraphics, mx: Int, my: Int, delta: Float) {
        val mc = Minecraft.getInstance()
        if (mc.player == null) return

        val r = AporiaRenderer.INSTANCE
        val sw = mc.window.guiScaledWidth
        val sh = mc.window.guiScaledHeight

        DynamicIsland.render(r, parseMode(watermarkMode.getSelectedIndex()))

        if (showTargetHud.isEnabled) TargetHud.render(gfx)

        renderInfoPanel(r, mc, sw, sh)
    }

    private fun renderInfoPanel(r: AporiaRenderer, mc: Minecraft, sw: Int, sh: Int) {
        val lines = mutableListOf<Pair<String, Int>>()

        if (showFps.isEnabled) {
            val fps = mc.fps
            val fpsColor = when {
                fps >= 60 -> ColorUtil.rgba(85, 255, 85, 255)
                fps >= 30 -> ColorUtil.rgba(255, 255, 85, 255)
                else -> ColorUtil.rgba(255, 85, 85, 255)
            }
            lines.add("FPS: $fps" to fpsColor)
        }

        if (showPing.isEnabled) {
            val ping = mc.player?.connection?.getPlayerInfo(mc.player!!.uuid)?.latency ?: -1
            val pingColor = when {
                ping <= 0 -> ColorUtil.rgba(160, 160, 160, 255)
                ping <= 50 -> ColorUtil.rgba(85, 255, 85, 255)
                ping <= 150 -> ColorUtil.rgba(255, 255, 85, 255)
                else -> ColorUtil.rgba(255, 85, 85, 255)
            }
            lines.add("Ping: ${ping}ms" to pingColor)
        }

        if (showPlayerCount.isEnabled) {
            val count = mc.level?.players()?.size ?: 0
            lines.add("Players: $count" to ColorUtil.rgba(160, 200, 255, 255))
        }

        if (showCoords.isEnabled) {
            val pos = mc.player!!.position()
            val x = DecimalFormat("0.0").format(pos.x)
            val y = DecimalFormat("0.0").format(pos.y)
            val z = DecimalFormat("0.0").format(pos.z)
            lines.add("XYZ: $x $y $z" to ColorUtil.rgba(180, 180, 200, 255))
        }

        if (showDirection.isEnabled) {
            val yaw = ((mc.player!!.yRot % 360) + 360) % 360
            val dir = when {
                yaw < 45 || yaw >= 315 -> "South"
                yaw < 135 -> "West"
                yaw < 225 -> "North"
                else -> "East"
            }
            lines.add("$dir" to ColorUtil.rgba(200, 180, 140, 255))
        }

        if (showSpeed.isEnabled) {
            val vel = mc.player!!.deltaMovement
            val speed = Math.sqrt(vel.x * vel.x + vel.z * vel.z) * 20.0
            val speedColor = if (speed > 10) ColorUtil.rgba(85, 255, 85, 255) else ColorUtil.rgba(200, 200, 200, 255)
            lines.add("Speed: ${DecimalFormat("0.0").format(speed)} bps" to speedColor)
        }

        if (lines.isEmpty()) return

        val lineH = 11f
        val pad = 5f
        val margin = 4f
        val maxW = lines.maxOf { r.getTextWidth(Fonts.REGULAR, it.first, lineH) + pad * 2 }

        val bgH = lines.size * (lineH + 2f) + pad * 2
        val bgX = (sw - maxW - margin).toFloat()
        val bgY = margin + 24f

        val blur = Beautifully.isBlurEnabled()
        if (blur) r.drawRectBlurred(bgX, bgY, maxW, bgH, 6f, ColorUtil.rgba(12, 18, 22, 180), 3f, 15)
        else r.drawRect(bgX, bgY, maxW, bgH, 6f, ColorUtil.rgba(12, 18, 22, 180))

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
}
