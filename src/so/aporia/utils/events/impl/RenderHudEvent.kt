package so.aporia.utils.events.impl
import net.minecraft.client.gui.GuiGraphicsExtractor
import com.chaos.annotation.ChaosNative
@ChaosNative
class RenderHudEvent(val graphics: GuiGraphicsExtractor, val partialTick: Float) {
    fun graphics(): GuiGraphicsExtractor = graphics
    fun partialTick(): Float = partialTick
}