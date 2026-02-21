package ru.ui.hud.components

import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin
import com.ferra13671.cometrenderer.plugins.minecraft.RectColors
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor
import ru.module.ModuleManager
import ru.render.MsdfTextRenderer
import ru.ui.hud.HudManager.HudComponent

class ArrayList(private val textRenderer: MsdfTextRenderer?) : HudComponent("ArrayList") {
    
    private val moduleAnimations = mutableMapOf<ru.module.Module, Float>()
    private val modulePositions = mutableMapOf<ru.module.Module, Float>()
    
    var position = "Справа сверху"
    var sortByLength = true
    
    init {
        x = 10f
        y = 10f
    }
    
    override fun render(plugin: MinecraftPlugin) {
        if (textRenderer == null) return
        
        val fbWidth = plugin.mainFramebufferWidth
        val fbHeight = plugin.mainFramebufferHeight
        
        val enabledModules = ModuleManager.getInstance().modules
            .filter { it.isEnabled() && it.name != "Interface" }
            .toMutableList()
        
        if (sortByLength) {
            enabledModules.sortByDescending { textRenderer.measureWidth(it.name, 16f) }
        }

        for (module in ModuleManager.getInstance().modules) {
            val target = if (module.isEnabled() && module.name != "Interface") 1f else 0f
            val current = moduleAnimations.getOrDefault(module, 0f)
            val newValue = current + (target - current) * 0.15f
            
            if (Math.abs(newValue - target) < 0.01f) {
                moduleAnimations[module] = target
            } else {
                moduleAnimations[module] = newValue
            }
        }

        var maxWidth = 0f
        for (module in enabledModules) {
            val alpha = moduleAnimations.getOrDefault(module, 0f)
            if (alpha < 0.01f) continue
            
            val textWidth = textRenderer.measureWidth(module.name, 16f)
            if (textWidth > maxWidth) maxWidth = textWidth
        }
        
        val padding = 4f
        val spacing = 2f
        val fontSize = 16f

        var totalHeight = 0f
        for (module in enabledModules) {
            val alpha = moduleAnimations.getOrDefault(module, 0f)
            if (alpha < 0.01f) continue
            
            val textHeight = textRenderer.measureHeight(module.name, fontSize)
            totalHeight += (textHeight + padding * 2 + spacing) * alpha
        }
        
        if (totalHeight > 0) {
            totalHeight -= spacing
        }

        val bgWidth = maxWidth + padding * 2
        val bgHeight = totalHeight

        width = bgWidth
        height = bgHeight

        val bgX = x
        val bgY = y

        if (maxWidth > 0 && totalHeight > 0) {
            ru.render.RectRenderer.drawRoundedRect(
                bgX, bgY, bgWidth, bgHeight, 8f,
                RectColors.oneColor(RenderColor.of(20, 20, 25, 200))
            )
        }

        var yOffset = bgY + padding
        
        for (module in enabledModules) {
            val alpha = moduleAnimations.getOrDefault(module, 0f)
            if (alpha < 0.01f) continue
            
            val text = module.name
            val textWidth = textRenderer.measureWidth(text, fontSize)
            val textHeight = textRenderer.measureHeight(text, fontSize)
            
            val targetY = yOffset
            val currentY = modulePositions.getOrDefault(module, targetY)
            val newY = currentY + (targetY - currentY) * 0.2f
            modulePositions[module] = newY
            
            val textX = when {
                position.contains("Справа") -> bgX + bgWidth - textWidth - padding
                else -> bgX + padding
            }
            
            textRenderer.drawText(
                textX, newY + textHeight - 2, fontSize, text,
                RenderColor.of(255, 255, 255, (255 * alpha).toInt())
            )
            
            yOffset += (textHeight + padding * 2 + spacing) * alpha
        }
    }
    
    override fun updateSize(plugin: MinecraftPlugin) {
    }
}
