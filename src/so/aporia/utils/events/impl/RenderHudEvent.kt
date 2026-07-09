package so.aporia.utils.events.impl
import net.minecraft.client.gui.GuiGraphics
import com.chaos.annotation.ChaosNative
@ChaosNative
class RenderHudEvent(val graphics: GuiGraphics, val partialTick: Float) {
    fun graphics(): GuiGraphics = graphics
    fun partialTick(): Float = partialTick
}