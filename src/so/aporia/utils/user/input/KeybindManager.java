package so.aporia.utils.user.input;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import so.aporia.module.Module;
import so.aporia.module.ModuleManager;
import so.aporia.utils.events.EventBus;
import so.aporia.utils.events.EventHandler;
import so.aporia.utils.events.impl.KeyInputEvent;
import so.aporia.utils.user.render.ui.clickgui.ClickGuiScreen;

/**
 * Handles global keybinds for modules and GUI.
 * Keys only fire when no vanilla screen is open — prevents laptop keys
 * from triggering modules while typing in chat/inventory/etc.
 * <p>
 * Обрабатывает глобальные клавиши для модулей и GUI.
 * Клавиши работают только когда нет открытого vanilla экрана.
 */
public final class KeybindManager {

    public static final KeybindManager INSTANCE = new KeybindManager();

    /**
     * GLFW key code for tilde/grave (~) — opens ClickGui.
     * <p>
     * Код клавиши GLFW для тильды (~) — открывает ClickGui.
     */
    public static final int KEY_CLICK_GUI = 96;

    private KeybindManager() {
        EventBus.INSTANCE.register(this);
    }

    @EventHandler
    public void onKey(KeyInputEvent e) {
        if (e.action() != KeyInputEvent.Action.PRESS) return;

        Minecraft mc = Minecraft.getInstance();
        Screen screen = mc.screen;

        if (e.key() == KEY_CLICK_GUI) {
            if (screen instanceof ClickGuiScreen) {
                mc.execute(() -> mc.setScreen(null));
            } else if (screen == null) {
                mc.execute(() -> mc.setScreen(new ClickGuiScreen()));
            }
            return;
        }

        if (screen == null) {
            for (Module m : ModuleManager.INSTANCE.getAll()) {
                if (m.keybind() != -1 && m.keybind() == e.key()) {
                    m.toggle();
                }
            }
        }
    }
}
