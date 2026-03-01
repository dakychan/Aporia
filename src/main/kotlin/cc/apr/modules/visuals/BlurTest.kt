package cc.apr.modules.visuals

import cc.apr.module.api.Module
import ru.utils.render.RectRenderer

class BlurTest : Module("BlurTest", "Тест blur эффекта", C.VISUALS) {
    
    private val blurAmount: NumberSetting
    
    init {
        blurAmount = NumberSetting("Blur Amount", 8.0, 2.0, 30.0, 1.0)
        addSetting(blurAmount)
    }
    
    override fun onEnable() {
        RectRenderer.setBlurRadius(blurAmount.value.toFloat())
    }
    
    override fun onDisable() {
        // Nothing
    }
    
    override fun onTick() {
        // Nothing
    }
    
    fun onRender2D() {
        RectRenderer.setBlurRadius(blurAmount.value.toFloat())
        
        val x = 100f
        val y = 100f
        val width = 300f
        val height = 200f
        val color = 0x80FF0000.toInt()
        val cornerRadius = 10f
        
        RectRenderer.drawRectWithBlur(x, y, width, height, color, cornerRadius)
    }
}
