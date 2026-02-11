package ru.module.impl.visuals

import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin
import com.ferra13671.cometrenderer.plugins.minecraft.RectColors
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor
import com.ferra13671.cometrenderer.plugins.minecraft.drawer.impl.RoundedRectDrawer
import ru.module.Module
import ru.render.AnimationSystem

class AnimCheck : Module("AnimCheck", "Тестовый модуль анимаций", C.VISUALS) {
    
    private val loadingAnimation = LoopingAnimation(2000L)
    private val borderAnimation = LoopingAnimation(3000L)
    
    init {
        AnimationSystem.getInstance().ensureRegistered(loadingAnimation)
        AnimationSystem.getInstance().ensureRegistered(borderAnimation)
    }
    
    override fun onEnable() {
        loadingAnimation.reset()
        borderAnimation.reset()
    }
    
    override fun onDisable() {
    }
    
    override fun onTick() {
        if (!isEnabled) return
        
        val plugin = MinecraftPlugin.getInstance()
        plugin.bindMainFramebuffer(true)
        
        val x = 100f
        val y = 100f
        
        renderLoadingRect(x, y, 200f, 50f)
        renderBorderAnimRect(x, y + 70f, 200f, 50f)
    }
    
    private fun renderLoadingRect(x: Float, y: Float, width: Float, height: Float) {
        val progress = loadingAnimation.getValue()
        val fillWidth = width * progress
        
        RoundedRectDrawer()
            .rectSized(x, y, width, height, 0f, RectColors.oneColor(RenderColor.of(255, 255, 255, 64)))
            .build()
            .tryDraw()
            .close()
        
        if (fillWidth > 0) {
            RoundedRectDrawer()
                .rectSized(x, y, fillWidth, height, 0f, RectColors.oneColor(RenderColor.of(0, 255, 0, 255)))
                .build()
                .tryDraw()
                .close()
        }
    }
    
    private fun renderBorderAnimRect(x: Float, y: Float, width: Float, height: Float) {
        val progress = borderAnimation.getValue()
        val borderColor = interpolateColor(0xFF0000FFu.toInt(), 0xFFFF0000u.toInt(), progress)
        
        val radius = 10f
        
        RoundedRectDrawer()
            .rectSized(x, y, width, height, radius, RectColors.oneColor(RenderColor.of(40, 40, 50, 200)))
            .build()
            .tryDraw()
            .close()
        
        val color = RenderColor.of(
            (borderColor shr 16) and 0xFF,
            (borderColor shr 8) and 0xFF,
            borderColor and 0xFF,
            (borderColor shr 24) and 0xFF
        )
        
        val borderWidth = 2f
        val animatedWidth = width * progress
        
        RoundedRectDrawer()
            .rectSized(x, y, animatedWidth, borderWidth, radius, RectColors.oneColor(color))
            .build()
            .tryDraw()
            .close()
        
        RoundedRectDrawer()
            .rectSized(x, y + height - borderWidth, animatedWidth, borderWidth, radius, RectColors.oneColor(color))
            .build()
            .tryDraw()
            .close()
        
        if (animatedWidth > 0) {
            RoundedRectDrawer()
                .rectSized(x, y, borderWidth, height, radius, RectColors.oneColor(color))
                .build()
                .tryDraw()
                .close()
        }
        
        if (animatedWidth >= width - borderWidth) {
            RoundedRectDrawer()
                .rectSized(x + width - borderWidth, y, borderWidth, height, radius, RectColors.oneColor(color))
                .build()
                .tryDraw()
                .close()
        }
    }
    
    private fun interpolateColor(color1: Int, color2: Int, progress: Float): Int {
        val a1 = (color1 shr 24) and 0xFF
        val r1 = (color1 shr 16) and 0xFF
        val g1 = (color1 shr 8) and 0xFF
        val b1 = color1 and 0xFF
        
        val a2 = (color2 shr 24) and 0xFF
        val r2 = (color2 shr 16) and 0xFF
        val g2 = (color2 shr 8) and 0xFF
        val b2 = color2 and 0xFF
        
        val a = (a1 + (a2 - a1) * progress).toInt()
        val r = (r1 + (r2 - r1) * progress).toInt()
        val g = (g1 + (g2 - g1) * progress).toInt()
        val b = (b1 + (b2 - b1) * progress).toInt()
        
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }
    
    private class LoopingAnimation(private val duration: Long) : AnimationSystem.Animated {
        private var startTime: Long = System.currentTimeMillis()
        
        fun reset() {
            startTime = System.currentTimeMillis()
        }
        
        fun getValue(): Float {
            val elapsed = System.currentTimeMillis() - startTime
            val progress = (elapsed % duration).toFloat() / duration
            return progress
        }
        
        override fun update(delta: Float): Boolean {
            return true
        }
    }
}
