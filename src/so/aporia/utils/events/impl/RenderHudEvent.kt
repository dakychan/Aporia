package so.aporia.utils.events.impl

import net.minecraft.client.gui.GuiGraphics

class RenderHudEvent(val graphics: GuiGraphics, val partialTick: Float) {
    fun graphics(): GuiGraphics = graphics
    fun partialTick(): Float = partialTick
}
