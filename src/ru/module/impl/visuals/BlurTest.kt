package ru.module.impl.visuals

import com.ferra13671.cometrenderer.CometRenderer
import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor
import net.minecraft.client.Minecraft
import ru.module.Module
import ru.render.RectRenderer

class BlurTest : Module("BlurTest", "Тест блюра", C.VISUALS) {

    private val blurAmount: NumberSetting

    init {
        blurAmount = NumberSetting("Blur Amount", 15.0, 0.0, 30.0, 1.0)
        addSetting(blurAmount)
    }

    override fun onEnable() {}
    override fun onDisable() {}
    override fun onTick() {}

    fun onRender2D() {
        if (!isEnabled) return

        val minecraft = Minecraft.getInstance()
        val window = minecraft.window
        val plugin = MinecraftPlugin.getInstance()
        plugin.bindMainFramebuffer(true)

        // Enable blending
        CometRenderer.applyDefaultBlend()

        val x = 100f
        val y = 100f
        val width = 300f
        val height = 200f
        val radius = 10f
        val blur = blurAmount.value.toFloat()

        // Рисуем прямоугольники С BLUR
        RectRenderer.drawRectangleWithBlur(x, y, width, height, RenderColor.of(0, 0, 0, 200), radius, blur)
        RectRenderer.drawRectangleWithBlur(x + 400f, y, width, height, RenderColor.of(255, 0, 0, 200), radius, blur)

        // Disable blending
        CometRenderer.disableBlend()
    }
}
