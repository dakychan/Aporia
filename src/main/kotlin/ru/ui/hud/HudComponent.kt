package ru.ui.hud

import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin

abstract class HudComponent(val name: String) {
    var x: Float = 0f
    var y: Float = 0f
    var width: Float = 0f
    var height: Float = 0f
    var zIndex: Int = 0
    
    var isDragging = false
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f
    
    abstract fun render(plugin: MinecraftPlugin)
    
    open fun updateSize(plugin: MinecraftPlugin) {
        // Override in subclasses to calculate width/height
    }
    
    fun isHovered(mouseX: Int, mouseY: Int): Boolean {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height
    }
    
    fun startDrag(mouseX: Int, mouseY: Int) {
        isDragging = true
        dragOffsetX = mouseX - x
        dragOffsetY = mouseY - y
    }
    
    fun drag(mouseX: Int, mouseY: Int, fbWidth: Int, fbHeight: Int) {
        if (isDragging) {
            x = (mouseX - dragOffsetX).coerceIn(0f, (fbWidth - width).coerceAtLeast(0f))
            y = (mouseY - dragOffsetY).coerceIn(0f, (fbHeight - height).coerceAtLeast(0f))
        }
    }
    
    fun stopDrag() {
        isDragging = false
    }
}
