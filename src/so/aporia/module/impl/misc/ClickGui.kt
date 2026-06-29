package so.aporia.module.impl.misc

import com.chaos.annotation.Obfuscate
import so.aporia.utils.imports.*
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.settings.SelectSetting
import so.aporia.module.settings.TextSetting
import so.aporia.utils.files.impl.ConfigFile
import so.aporia.utils.user.render.ui.clickgui.ClickGuiScreen

@Obfuscate
class ClickGui : Module("ClickGui", Category.VISUAL, 41) {

    val browserUrl = TextSetting("BrowserURL", "URL для браузера", "about:blank")
    val fontRendererMode = SelectSetting("Font Renderer", "Text rendering backend")
        .value("MSDF", "TTF", "OTF")
        .selected("MSDF")

    val fontFamily = SelectSetting("Font Family", "TTF/OTF font to use")
        .value("Default", "Inter")
        .selected("Default")

    init {
        ConfigFile.markModuleActivated(this)
    }

    override fun onEnable() {
        bus.register(this)
        mc.setScreen(ClickGuiScreen(this))
    }

    override fun onDisable() {
        bus.unregister(this)
        if (mc.screen is ClickGuiScreen) mc.setScreen(null)
    }
}
