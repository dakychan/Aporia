package so.aporia.module.impl.render

import com.chaos.annotation.Obfuscate
import net.minecraft.client.gui.GuiGraphics
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.ModuleManager
import so.aporia.module.settings.BooleanSetting
import so.aporia.module.settings.MultiSelectSetting

@Obfuscate
class Beautifully : Module("Beautifully", Category.VISUAL) {

    val blur = BooleanSetting("Blur", "Enable blur on UI elements", true)
    val features = MultiSelectSetting("Features", "Toggle UI features")
        .options("Custom Chat")

    override fun onEnable() {}
    override fun onDisable() {}

    fun render(gfx: GuiGraphics, mx: Int, my: Int, delta: Float) {}

    companion object {
        @JvmStatic
        fun isBlurEnabled(): Boolean {
            val mod = ModuleManager.get("Beautifully")
            return mod != null && mod.isEnabled && (mod as Beautifully).blur.isEnabled
        }

        @JvmStatic
        fun isCustomChatEnabled(): Boolean {
            val mod = ModuleManager.get("Beautifully")
            if (mod == null || !mod.isEnabled) return true
            return (mod as Beautifully).features.isSelected("Custom Chat")
        }
    }
}
