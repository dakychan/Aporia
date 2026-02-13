package ru.module.impl.visuals

import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin
import com.ferra13671.cometrenderer.plugins.minecraft.RectColors
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor
import com.ferra13671.cometrenderer.plugins.minecraft.drawer.impl.RoundedRectDrawer
import ru.module.Module
import ru.render.AnimationSystem
import ru.render.MsdfTextRenderer

class AnimCheck : Module("AnimCheck", "Тестовый модуль анимаций", C.VISUALS) {
    
    private val loadingAnimation = LoopingAnimation(2000L)
    private val borderAnimation = LoopingAnimation(3000L)
    private val textAnimation = LoopingAnimation(2500L)
    
    private var textRenderer: MsdfTextRenderer? = null
    
    init {
        AnimationSystem.getInstance().ensureRegistered(loadingAnimation)
        AnimationSystem.getInstance().ensureRegistered(borderAnimation)
        AnimationSystem.getInstance().ensureRegistered(textAnimation)
        
        try {
            val font = ru.render.MsdfFont(
                "assets/aporia/fonts/Inter_Medium.json",
                "assets/aporia/fonts/Inter_Medium.png"
            )
            textRenderer = MsdfTextRenderer(font)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    override fun onEnable() {
        loadingAnimation.reset()
        borderAnimation.reset()
        textAnimation.reset()
    }
    
    override fun onDisable() {
    }
    
    override fun onTick() {
    }
    
    private fun renderLoadingRect(x: Float, y: Float, width: Float, height: Float) {
        val progress = loadingAnimation.getValue()
        val fillWidth = width * progress
        
        // Background with blur
        ru.render.RectRenderer.drawRectangleWithBlur(
            x, y, width, height,
            RenderColor.of(40, 40, 50, 200),
            8f, 3f
        )
        
        // Progress fill
        if (fillWidth > 0) {
            ru.render.RectRenderer.drawRoundedRect(
                x, y, fillWidth, height, 8f,
                RectColors.oneColor(RenderColor.of(0, 255, 0, 255))
            )
        }
    }
    
    private fun renderBorderAnimRect(x: Float, y: Float, width: Float, height: Float) {
        val progress = borderAnimation.getValue()
        val borderColor = interpolateColor(0xFF0000FFu.toInt(), 0xFFFF0000u.toInt(), progress)
        
        val radius = 10f
        
        // Background with blur
        ru.render.RectRenderer.drawRectangleWithBlur(
            x, y, width, height,
            RenderColor.of(40, 40, 50, 200),
            radius, 4f
        )
        
        val color = RenderColor.of(
            (borderColor shr 16) and 0xFF,
            (borderColor shr 8) and 0xFF,
            borderColor and 0xFF,
            (borderColor shr 24) and 0xFF
        )
        
        val borderWidth = 2f
        val animatedWidth = width * progress
        
        // Animated borders with rounded corners
        ru.render.RectRenderer.drawRoundedRect(
            x, y, animatedWidth, borderWidth, radius,
            RectColors.oneColor(color)
        )
        
        ru.render.RectRenderer.drawRoundedRect(
            x, y + height - borderWidth, animatedWidth, borderWidth, radius,
            RectColors.oneColor(color)
        )
        
        if (animatedWidth > 0) {
            ru.render.RectRenderer.drawRoundedRect(
                x, y, borderWidth, height, radius,
                RectColors.oneColor(color)
            )
        }
        
        if (animatedWidth >= width - borderWidth) {
            ru.render.RectRenderer.drawRoundedRect(
                x + width - borderWidth, y, borderWidth, height, radius,
                RectColors.oneColor(color)
            )
        }
    }
    
    private fun renderTextAnimRect(x: Float, y: Float, width: Float, height: Float) {
        val progress = textAnimation.getValue()
        val radius = 10f
        
        // Background with blur and rounded corners
        ru.render.RectRenderer.drawRectangleWithBlur(
            x, y, width, height,
            RenderColor.of(40, 40, 50, 200),
            radius, 5f
        )
        
        val text = "APORIA"
        val fontSize = 24f
        
        if (textRenderer != null) {
            val textWidth = textRenderer!!.measureWidth(text, fontSize)
            val textHeight = textRenderer!!.measureHeight(text, fontSize)
            
            val textX = x + (width - textWidth) / 2
            val textY = y + (height + textHeight) / 2 - 5
            
            val fillWidth = textWidth * progress
            
            val plugin = MinecraftPlugin.getInstance()
            
            org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST)
            
            val fbHeight = plugin.mainFramebufferHeight
            val scissorX = textX.toInt()
            val scissorY = (fbHeight - textY - textHeight).toInt()
            val scissorWidth = fillWidth.toInt()
            val scissorHeight = (textHeight + 10).toInt()
            
            org.lwjgl.opengl.GL11.glScissor(scissorX, scissorY, scissorWidth, scissorHeight)
            
            textRenderer!!.drawText(
                textX, textY, fontSize, text,
                RenderColor.of(100, 200, 255, 255)
            )
            
            org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST)
            
            if (fillWidth < textWidth) {
                val remainingText = text.substring(
                    ((text.length * progress).toInt().coerceAtMost(text.length))
                )
                if (remainingText.isNotEmpty()) {
                    textRenderer!!.drawText(
                        textX + fillWidth, textY, fontSize, 
                        remainingText,
                        RenderColor.of(150, 150, 160, 100)
                    )
                }
            }
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
