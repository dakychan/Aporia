package so.aporia.utils.user.input

import com.chaos.annotation.Obfuscate
import net.minecraft.client.Minecraft
import so.aporia.module.ModuleManager
import so.aporia.module.impl.misc.ClickGui
import so.aporia.utils.events.EventBus
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.KeyInputEvent
import so.aporia.utils.events.impl.MouseClickEvent
import so.aporia.utils.user.render.ui.clickgui.ClickGuiScreen

@Obfuscate
object KeybindManager {

    @JvmField
    val KEY_CLICK_GUI = KeyCodeMap.getScancode("GRAVE")

    init {
        EventBus.register(this)
    }

    @EventHandler
    fun onKey(e: KeyInputEvent) {
        if (e.action() != KeyInputEvent.Action.PRESS) return

        val mc = Minecraft.getInstance()
        val screen = mc.screen

        if (e.scancode() == KEY_CLICK_GUI) {
            if (screen is ClickGuiScreen) {
                mc.execute { mc.setScreen(null) }
            } else if (screen == null) {
                val clickGuiModule = ModuleManager.get("ClickGui") as? ClickGui ?: return
                mc.execute { mc.setScreen(ClickGuiScreen(clickGuiModule)) }
            }
            return
        }

        if (screen == null) {
            for (m in ModuleManager.getAll()) {
                if (m.keybind != -1 && m.keybind == e.scancode()) {
                    m.toggle()
                }
            }
        }
    }

    @EventHandler
    fun onMouseClick(e: MouseClickEvent) {
        if (e.action() != MouseClickEvent.Action.PRESS) return

        val mc = Minecraft.getInstance()
        val screen = mc.screen

        if (screen == null) {
            val virtualKey = if (e.button() in 0..7) 500 + e.button() else e.button()
            for (m in ModuleManager.getAll()) {
                if (m.keybind != -1 && m.keybind == virtualKey) {
                    m.toggle()
                }
            }
        }
    }
}
