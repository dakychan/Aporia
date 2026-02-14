package ru.ui.hud.components

import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin
import com.ferra13671.cometrenderer.plugins.minecraft.RectColors
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor
import ru.render.IconFont
import ru.render.MsdfTextRenderer
import ru.ui.hud.HudComponent

class WaterMark(
    private val textRenderer: MsdfTextRenderer?,
    private val iconRenderer: MsdfTextRenderer?
) : HudComponent("WaterMark") {
    
    var position = "Слева сверху" // Слева сверху, Справа сверху, Центр сверху, Слева снизу, Справа снизу
    
    init {
        x = 10f
        y = 10f
    }
    
    override fun render(plugin: MinecraftPlugin) {
        if (textRenderer == null) return
        
        val fbWidth = plugin.mainFramebufferWidth
        val fbHeight = plugin.mainFramebufferHeight
        
        val clientName = "Aporia.cc"
        val fontSize = 18f
        val padding = 6f
        val spacing = 6f
        
        // Calculate sizes
        val clientWidth = textRenderer.measureWidth(clientName, fontSize)
        val clientHeight = textRenderer.measureHeight(clientName, fontSize)
        
        // Get system username
        val username = System.getProperty("user.name") ?: "User"
        
        // Player icon
        val playerIcon = if (IconFont.isInitialized()) IconFont.getIcon(ru.module.Module.C.PLAYER) else ""
        val iconWidth = if (iconRenderer != null && playerIcon.isNotEmpty()) {
            iconRenderer.measureWidth(playerIcon, fontSize)
        } else 0f
        
        val usernameWidth = textRenderer.measureWidth(username, fontSize)
        
        // Separator " | "
        val separatorWidth = textRenderer.measureWidth(" | ", fontSize)
        
        // Calculate total width: "Aporia.cc | (icon) Username"
        val totalWidth = clientWidth + separatorWidth + iconWidth + spacing + usernameWidth + padding * 2
        val rectHeight = clientHeight + padding * 2
        
        // Update component bounds
        width = totalWidth
        height = rectHeight
        
        // Use current x, y (set by dragging or loaded from config)
        val startX = x
        val startY = y
        
        // Draw background
        ru.render.RectRenderer.drawRoundedRect(
            startX, startY, totalWidth, rectHeight, 8f,
            RectColors.oneColor(RenderColor.of(20, 20, 25, 200))
        )
        
        var textX = startX + padding
        val textY = startY + padding + clientHeight - 2
        
        // Draw "Aporia.cc"
        textRenderer.drawText(
            textX, textY, fontSize, clientName,
            RenderColor.WHITE
        )
        textX += clientWidth
        
        // Draw " | "
        textRenderer.drawText(
            textX, textY, fontSize, " | ",
            RenderColor.of(100, 100, 110, 255)
        )
        textX += separatorWidth
        
        // Draw player icon
        if (iconRenderer != null && playerIcon.isNotEmpty()) {
            iconRenderer.drawText(
                textX, textY, fontSize, playerIcon,
                RenderColor.of(100, 200, 255, 255)
            )
            textX += iconWidth + spacing
        }
        
        // Draw username
        textRenderer.drawText(
            textX, textY, fontSize, username,
            RenderColor.WHITE
        )
    }
    
    override fun updateSize(plugin: MinecraftPlugin) {
        // Size is calculated in render
    }
}
