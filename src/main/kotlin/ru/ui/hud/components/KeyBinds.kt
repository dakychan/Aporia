package ru.ui.hud.components

import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin
import com.ferra13671.cometrenderer.plugins.minecraft.RectColors
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor
import org.lwjgl.glfw.GLFW
import ru.input.impl.bind.KeybindManager
import ru.module.ModuleManager
import ru.render.MsdfTextRenderer
import ru.ui.hud.HudComponent

class KeyBinds(private val textRenderer: MsdfTextRenderer?) : HudComponent("KeyBinds") {
    
    init {
        x = 10f
        y = 100f
    }
    
    override fun render(plugin: MinecraftPlugin) {
        if (textRenderer == null) return
        
        val fbWidth = plugin.mainFramebufferWidth
        val fbHeight = plugin.mainFramebufferHeight
        
        val padding = 6f
        val spacing = 2f
        val fontSize = 14f
        
        // Get modules with keybinds that are enabled
        val boundModules = mutableListOf<Pair<String, Int>>()
        
        for (module in ModuleManager.getInstance().modules) {
            if (!module.isEnabled()) continue
            
            val keybindId = "module.${module.name.lowercase()}.toggle"
            val keybind = KeybindManager.getInstance().getKeybind(keybindId)
            if (keybind != null && keybind.keyCode > 0) {
                boundModules.add(Pair(module.name, keybind.keyCode))
            }
        }
        
        boundModules.sortBy { it.first }
        
        if (boundModules.isEmpty()) {
            width = 0f
            height = 0f
            return
        }
        
        // Calculate max width
        var maxWidth = textRenderer.measureWidth("KeyBinds", 16f)
        for ((moduleName, keyCode) in boundModules) {
            val keyName = getKeyName(keyCode)
            val text = "$moduleName [$keyName]"
            val textWidth = textRenderer.measureWidth(text, fontSize)
            if (textWidth > maxWidth) maxWidth = textWidth
        }
        
        val rectWidth = maxWidth + padding * 2
        val titleHeight = textRenderer.measureHeight("KeyBinds", 16f)
        val itemHeight = textRenderer.measureHeight("A", fontSize)
        val rectHeight = titleHeight + padding * 3 + (itemHeight + spacing) * boundModules.size
        
        width = rectWidth
        height = rectHeight
        
        // Draw background
        ru.render.RectRenderer.drawRoundedRect(
            x, y, rectWidth, rectHeight, 8f,
            RectColors.oneColor(RenderColor.of(20, 20, 25, 200))
        )
        
        // Draw title
        textRenderer.drawText(
            x + padding, y + padding + titleHeight - 2, 16f, "KeyBinds",
            RenderColor.of(100, 200, 255, 255)
        )
        
        // Draw separator line
        val lineY = y + titleHeight + padding * 2
        ru.render.RectRenderer.drawRoundedRect(
            x + padding, lineY, rectWidth - padding * 2, 1f, 0f,
            RectColors.oneColor(RenderColor.of(60, 60, 70, 255))
        )
        
        // Draw modules
        var yOffset = lineY + padding
        for ((moduleName, keyCode) in boundModules) {
            val keyName = getKeyName(keyCode)
            val text = "$moduleName [$keyName]"
            
            textRenderer.drawText(
                x + padding, yOffset + itemHeight - 2, fontSize, text,
                RenderColor.WHITE
            )
            
            yOffset += itemHeight + spacing
        }
    }
    
    private fun getKeyName(keyCode: Int): String {
        return when (keyCode) {
            GLFW.GLFW_KEY_UNKNOWN -> "None"
            GLFW.GLFW_KEY_SPACE -> "Space"
            GLFW.GLFW_KEY_LEFT_SHIFT -> "LShift"
            GLFW.GLFW_KEY_RIGHT_SHIFT -> "RShift"
            GLFW.GLFW_KEY_LEFT_CONTROL -> "LCtrl"
            GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCtrl"
            GLFW.GLFW_KEY_LEFT_ALT -> "LAlt"
            GLFW.GLFW_KEY_RIGHT_ALT -> "RAlt"
            else -> GLFW.glfwGetKeyName(keyCode, 0)?.uppercase() ?: "Key$keyCode"
        }
    }
    
    override fun updateSize(plugin: MinecraftPlugin) {
        // Size is calculated in render
    }
}
