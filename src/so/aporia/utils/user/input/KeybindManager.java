package so.aporia.utils.user.input;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import so.aporia.module.Module;
import so.aporia.module.ModuleManager;
import so.aporia.utils.events.EventBus;
import so.aporia.utils.events.EventHandler;
import so.aporia.utils.events.impl.KeyInputEvent;
import so.aporia.utils.events.impl.MouseClickEvent;
import so.aporia.utils.user.render.ui.clickgui.ClickGuiScreen;

public final class KeybindManager {

    public static final KeybindManager INSTANCE = new KeybindManager();
    public static final int KEY_CLICK_GUI = KeyCodeMap.getScancode("GRAVE");

    private KeybindManager() {
        EventBus.INSTANCE.register(this);
    }

    @EventHandler
    public void onKey(KeyInputEvent e) {
        if (e.action() != KeyInputEvent.Action.PRESS) return;

        Minecraft mc = Minecraft.getInstance();
        Screen screen = mc.screen;

        if (e.scancode() == KEY_CLICK_GUI) {
            if (screen instanceof ClickGuiScreen) {
                mc.execute(() -> mc.setScreen(null));
            } else if (screen == null) {
                mc.execute(() -> mc.setScreen(new ClickGuiScreen()));
            }
            return;
        }

        if (screen == null) {
            for (Module m : ModuleManager.INSTANCE.getAll()) {
                if (m.keybind() != -1 && m.keybind() == e.scancode()) {
                    m.toggle();
                }
            }
        }
    }

    @EventHandler
    public void onMouseClick(MouseClickEvent e) {
        if (e.action() != MouseClickEvent.Action.PRESS) return;

        Minecraft mc = Minecraft.getInstance();
        Screen screen = mc.screen;

        if (screen == null) {
            for (Module m : ModuleManager.INSTANCE.getAll()) {
                if (m.keybind() != -1 && m.keybind() == e.button()) {
                    m.toggle();
                }
            }
        }
    }
}
