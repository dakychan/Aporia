package ru.ui.hud

import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.files.FilesManager
import ru.files.Logger
import ru.files.PathResolver
import ru.render.RenderLayerManager
import java.nio.file.Files
import java.nio.file.Path

object HudManager {
    
    private val renderLayerManager = RenderLayerManager()
    private val gson = Gson()
    private val configPath: Path = PathResolver.mainDirectory.resolve("HudConfig.json")
    
    private val components = mutableListOf<HudComponent>()
    
    fun initialize() {
        Logger.info("Initializing HudManager...")
        loadConfig()
        Logger.info("HudManager initialized")
    }
    
    fun addComponent(component: HudComponent) {
        if (!components.contains(component)) {
            components.add(component)
            renderLayerManager.addComponent(component)
        }
    }
    
    fun removeComponent(component: HudComponent) {
        components.remove(component)
        renderLayerManager.removeComponent(component)
    }
    
    fun getComponents(): List<HudComponent> {
        return components.toList()
    }
    
    fun bringToFront(component: HudComponent) {
        renderLayerManager.bringToFront(component)
    }
    
    fun render(plugin: MinecraftPlugin) {
        val componentsInOrder = renderLayerManager.getComponentsInRenderOrder()
        for (component in componentsInOrder) {
            component.render(plugin)
        }
    }
    
    fun handleMouseClick(mouseX: Int, mouseY: Int, button: Int): HudComponent? {
        val componentsInOrder = renderLayerManager.getComponentsInRenderOrder().reversed()
        
        for (component in componentsInOrder) {
            if (component.isHovered(mouseX, mouseY)) {
                bringToFront(component)
                component.startDrag(mouseX, mouseY)
                return component
            }
        }
        
        return null
    }
    
    fun handleMouseDrag(mouseX: Int, mouseY: Int, fbWidth: Int, fbHeight: Int) {
        for (component in components) {
            if (component.isDragging) {
                component.drag(mouseX, mouseY, fbWidth, fbHeight)
            }
        }
    }
    
    fun handleMouseRelease() {
        for (component in components) {
            component.stopDrag()
        }
        saveConfig()
    }
    
    fun saveConfig() {
        try {
            val configData = components.map { component ->
                HudComponentConfig(
                    name = component.name,
                    x = component.x,
                    y = component.y,
                    zIndex = component.getZIndex()
                )
            }
            
            val json = gson.toJson(configData)
            Files.writeString(configPath, json)
            Logger.info("HUD configuration saved")
        } catch (e: Exception) {
            Logger.error("Failed to save HUD configuration", e)
        }
    }
    
    fun loadConfig() {
        try {
            if (!Files.exists(configPath)) {
                Logger.info("HUD config file not found, using defaults")
                return
            }
            
            val json = Files.readString(configPath)
            val type = object : TypeToken<List<HudComponentConfig>>() {}.type
            val configData: List<HudComponentConfig> = gson.fromJson(json, type)
            
            for (config in configData) {
                val component = components.find { it.name == config.name }
                if (component != null) {
                    component.x = config.x
                    component.y = config.y
                    component.setZIndex(config.zIndex)
                }
            }
            
            Logger.info("HUD configuration loaded")
        } catch (e: Exception) {
            Logger.error("Failed to load HUD configuration", e)
        }
    }
    
    data class HudComponentConfig(
        val name: String,
        val x: Float,
        val y: Float,
        val zIndex: Int
    )
}
