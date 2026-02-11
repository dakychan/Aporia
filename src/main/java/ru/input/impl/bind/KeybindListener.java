package ru.input.impl.bind;

import net.minecraft.client.Minecraft;
import ru.event.impl.EventSystemImpl;
import ru.event.impl.KeyPressEvent;
import ru.gui.GuiManager;

public class KeybindListener {
    private static final KeybindManager manager = KeybindManager.getInstance();
    private static boolean initialized = false;

    public static void init() {
        if (initialized) {
            return;
        }
        
        EventSystemImpl.getInstance().subscribe(KeyPressEvent.class, event -> {
            if (shouldProcessKeybinds()) {
                manager.handleKeyPress(event.keyCode);
            }
        });
        
        initialized = true;
    }
    private static boolean shouldProcessKeybinds() {
        if (GuiManager.isScreenOpen()) {
            return false;
        }
        
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return false;
        }
        
        return client.screen == null;
    }
}
