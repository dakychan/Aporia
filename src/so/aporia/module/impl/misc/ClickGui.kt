package so.aporia.module.impl.misc
import com.chaos.annotation.Obfuscate
import so.aporia.utils.imports.*
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.settings.SliderSetting
import so.aporia.module.settings.SelectSetting
import so.aporia.module.settings.TextSetting
import so.aporia.utils.files.impl.ConfigFile
import so.aporia.utils.user.render.ui.clickgui.ClickGuiScreen
import com.chaos.annotation.ChaosNative
@Obfuscate
@ChaosNative
class ClickGui : Module("ClickGui", Category.VISUAL, 41) {

    val guiMode = SelectSetting("Mode", "GUI render mode")
        .value("Default", "Beatiful")
        .selected("Beatiful")

    val browserUrl = TextSetting("BrowserURL", "URL для браузера", "about:blank")
    val blurStyle = SelectSetting("BlurStyle", "Panel blur shader style")
        .value("BasicBlur", "LiquidGlass")
        .selected("BasicBlur")

    val fontRendererMode = SelectSetting("Font Renderer", "Text rendering backend")
        .value("MSDF", "TTF", "OTF")
        .selected("MSDF")

    val fontFamily = SelectSetting("Font Family", "TTF/OTF font to use")
        .value("Default", "Inter")
        .selected("Default")

    /* ===== Сохраняемое состояние GUI ===== */
    val savedCategory = SliderSetting("SavedCategory", "Last open category", 0.0, 0.0, 255.0, 1.0)
    val savedScroll = SliderSetting("SavedScroll", "Category scroll offset", 0.0, -10000.0, 10000.0, 1.0)
    val savedSettingsScroll = SliderSetting("SavedSettingsScroll", "Settings scroll offset", 0.0, -10000.0, 10000.0, 1.0)

    init {
        ConfigFile.markModuleActivated(this)
    }

    override fun onEnable() {
        bus.register(this)
        mc.gui.setScreen(ClickGuiScreen(this))
    }

    override fun onDisable() {
        bus.unregister(this)
        if (mc.gui.screen() is ClickGuiScreen) mc.gui.setScreen(null)
    }

    override val settings = listOf(guiMode, blurStyle, browserUrl, fontRendererMode, fontFamily, savedCategory, savedScroll, savedSettingsScroll)
}