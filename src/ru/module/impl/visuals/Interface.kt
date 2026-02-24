package ru.module.impl.visuals

import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin
import ru.module.Module
import ru.render.IconFont
import ru.render.MsdfFont
import ru.render.MsdfTextRenderer
import ru.ui.hud.HudManager.HudComponent
import ru.ui.hud.components.ArrayList
import ru.ui.hud.components.KeyBinds
import ru.ui.hud.components.WaterMark

class Interface : Module("Interface", "HUD элементы", C.VISUALS) {
    
    private var textRenderer: MsdfTextRenderer? = null
    private var iconRenderer: MsdfTextRenderer? = null
    
    private val components = mutableListOf<HudComponent>()
    private var arrayList: ArrayList? = null
    private var waterMark: WaterMark? = null
    private var keyBinds: KeyBinds? = null
    
    private val enabledComponents: MultiSetting
    
    private val arrayListX: NumberSetting
    private val arrayListY: NumberSetting
    private val waterMarkX: NumberSetting
    private val waterMarkY: NumberSetting
    private val keyBindsX: NumberSetting
    private val keyBindsY: NumberSetting
    
    init {
        try {
            val font = MsdfFont(
                "assets/aporia/fonts/Inter_Medium.json",
                "assets/aporia/fonts/Inter_Medium.png"
            )
            textRenderer = MsdfTextRenderer(font)
            
            if (IconFont.isInitialized()) {
                iconRenderer = IconFont.getRenderer()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        arrayListX = NumberSetting("ArrayList.X", 10.0, 0.0, 3000.0, 1.0)
        arrayListY = NumberSetting("ArrayList.Y", 50.0, 0.0, 2000.0, 1.0)
        waterMarkX = NumberSetting("WaterMark.X", 10.0, 0.0, 3000.0, 1.0)
        waterMarkY = NumberSetting("WaterMark.Y", 10.0, 0.0, 2000.0, 1.0)
        keyBindsX = NumberSetting("KeyBinds.X", 10.0, 0.0, 3000.0, 1.0)
        keyBindsY = NumberSetting("KeyBinds.Y", 150.0, 0.0, 2000.0, 1.0)

        arrayList = ArrayList(textRenderer)
        waterMark = WaterMark(textRenderer, iconRenderer)
        keyBinds = KeyBinds(textRenderer)

        arrayList?.let {
            it.x = arrayListX.value.toFloat()
            it.y = arrayListY.value.toFloat()
            it.position = "Слева сверху"
            components.add(it)
        }
        
        waterMark?.let {
            it.x = waterMarkX.value.toFloat()
            it.y = waterMarkY.value.toFloat()
            it.position = "Слева сверху"
            components.add(it)
        }
        
        keyBinds?.let {
            it.x = keyBindsX.value.toFloat()
            it.y = keyBindsY.value.toFloat()
            components.add(it)
        }

        enabledComponents = MultiSetting(
            "Элементы",
            mutableListOf("ArrayList", "WaterMark", "KeyBinds"),
            mutableListOf("ArrayList", "WaterMark", "KeyBinds")
        )
        
        addSetting(enabledComponents)
        addSetting(arrayListX)
        addSetting(arrayListY)
        addSetting(waterMarkX)
        addSetting(waterMarkY)
        addSetting(keyBindsX)
        addSetting(keyBindsY)
    }
    
    override fun onEnable() {
        loadPositions()
    }
    
    override fun onDisable() {
        savePositions()
    }
    
    override fun onTick() {
    }
    
    private fun savePositions() {
        arrayList?.let {
            arrayListX.setValue(it.x.toDouble())
            arrayListY.setValue(it.y.toDouble())
        }
        
        waterMark?.let {
            waterMarkX.setValue(it.x.toDouble())
            waterMarkY.setValue(it.y.toDouble())
        }
        
        keyBinds?.let {
            keyBindsX.setValue(it.x.toDouble())
            keyBindsY.setValue(it.y.toDouble())
        }
    }
    
    private fun loadPositions() {
        arrayList?.let {
            it.x = arrayListX.value.toFloat()
            it.y = arrayListY.value.toFloat()
        }
        
        waterMark?.let {
            it.x = waterMarkX.value.toFloat()
            it.y = waterMarkY.value.toFloat()
        }
        
        keyBinds?.let {
            it.x = keyBindsX.value.toFloat()
            it.y = keyBindsY.value.toFloat()
        }
    }
    
    fun render() {
        if (!isEnabled() || textRenderer == null) return
        
        val plugin = MinecraftPlugin.getInstance()
        plugin.bindMainFramebuffer(true)
        
        val fbWidth = plugin.mainFramebufferWidth
        val fbHeight = plugin.mainFramebufferHeight

        val enabled = enabledComponents.value
        
        if (enabled.contains("ArrayList") && arrayList != null) {
            arrayList!!.render(plugin)
        }
        
        if (enabled.contains("WaterMark") && waterMark != null) {
            waterMark!!.render(plugin)
        }
        
        if (enabled.contains("KeyBinds") && keyBinds != null) {
            keyBinds!!.render(plugin)
        }
    }
    
    fun handleMouseClick(mouseX: Int, mouseY: Int, button: Int): Boolean {
        if (!isEnabled() || button != 0) return false
        
        val enabled = enabledComponents.value

        for (component in components.reversed()) {
            val componentName = component.name
            if (!enabled.contains(componentName)) continue
            
            if (component.isHovered(mouseX, mouseY)) {
                component.startDrag(mouseX, mouseY)
                return true
            }
        }
        
        return false
    }
    
    fun handleMouseDrag(mouseX: Int, mouseY: Int) {
        if (!isEnabled()) return
        
        val plugin = MinecraftPlugin.getInstance()
        val fbWidth = plugin.mainFramebufferWidth
        val fbHeight = plugin.mainFramebufferHeight
        
        for (component in components) {
            if (component.isDragging) {
                component.drag(mouseX, mouseY, fbWidth, fbHeight)
            }
        }
    }
    
    fun handleMouseRelease() {
        if (!isEnabled()) return
        
        for (component in components) {
            component.stopDrag()
        }

        savePositions()
    }

    class MultiSetting(
        name: String,
        private val options: MutableList<String>,
        var value: MutableList<String>
    ) : Setting<MutableList<String>>(name, value) {
        
        fun toggle(option: String) {
            if (value.contains(option)) {
                value.remove(option)
            } else {
                value.add(option)
            }
        }
        
        fun getOptions(): List<String> = options
    }
}
