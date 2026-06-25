package so.aporia.module.impl.misc

import com.chaos.annotation.Obfuscate
import net.minecraft.client.Minecraft
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.settings.TextSetting
import so.aporia.utils.events.EventBus
import so.aporia.utils.files.impl.ConfigFile
import so.aporia.utils.user.render.ui.clickgui.ClickGuiScreen

@Obfuscate
class ClickGui : Module("ClickGui", Category.VISUAL, 41) {

    val browserUrl = TextSetting("BrowserURL", "URL для браузера", "about:blank")

    init {
        ConfigFile.markModuleActivated(this)
    }

    override fun onEnable() {
        EventBus.register(this)
        Minecraft.getInstance().setScreen(ClickGuiScreen(this))
    }

    override fun onDisable() {
        EventBus.unregister(this)
        val mc = Minecraft.getInstance()
        if (mc.screen is ClickGuiScreen) mc.setScreen(null)
    }
}
