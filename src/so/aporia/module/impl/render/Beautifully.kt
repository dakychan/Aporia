package so.aporia.module.impl.render
import com.chaos.annotation.Obfuscate
import net.minecraft.client.gui.GuiGraphicsExtractor
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.ModuleManager
import so.aporia.module.settings.BooleanSetting
import so.aporia.module.settings.MultiSelectSetting
import com.chaos.annotation.ChaosNative
@Obfuscate
@ChaosNative
class Beautifully : Module("Beautifully", Category.VISUAL) {

    val blur = BooleanSetting("Blur", "Enable blur on UI elements", true)
    val features = MultiSelectSetting("Features", "Toggle UI features")
        .options("Custom Chat", "Blur", "Dynamic Island Blur", "Target HUD Blur", "HUD Panel Blur",
            "Keybinds Blur", "Scoreboard Blur", "Potions Blur", "HotBar Blur")

    override val settings = listOf(blur, features)

    override fun onEnable() {}
    override fun onDisable() {}

    fun render(gfx: GuiGraphicsExtractor, mx: Int, my: Int, delta: Float) {}

    companion object {
        @JvmStatic
        fun isBlurEnabled(): Boolean {
            val mod = ModuleManager.get("Beautifully")
            return mod != null && mod.isEnabled && (mod as Beautifully).blur.isEnabled
        }

        @JvmStatic
        fun isFeatureEnabled(feature: String): Boolean {
            val mod = ModuleManager.get("Beautifully")
            return mod != null && mod.isEnabled && (mod as Beautifully).features.isSelected(feature)
        }

        @JvmStatic
        fun isCustomChatEnabled(): Boolean {
            val mod = ModuleManager.get("Beautifully")
            if (mod == null || !mod.isEnabled) return true
            return (mod as Beautifully).features.isSelected("Custom Chat")
        }
    }
}