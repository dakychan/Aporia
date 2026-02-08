package ru.input.impl.bind;

import net.minecraft.client.MinecraftClient;
import ru.event.impl.EventSystemImpl;
import ru.event.impl.KeyPressEvent;
import ru.gui.GuiManager;

/**
 * Listener that subscribes to KeyPressEvent and triggers keybinds.
 * 
 * Only processes keybinds when:
 * - No custom GUI is open (GuiManager.isScreenOpen() == false)
 * - No Minecraft screen is open (chat, inventory, etc.)
 * 
 * Requirements: 3.4
 */
public class KeybindListener {
    private static final KeybindManager manager = KeybindManager.getInstance();
    private static boolean initialized = false;
    
    /**
     * Initializes the keybind listener by subscribing to KeyPressEvent.
     * Should be called once during client initialization.
     */
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
    
    /**
     * Determines if keybinds should be processed based on current game state.
     * 
     * Keybinds are NOT processed when:
     * - A custom GUI screen is open (ClickGUI, etc.)
     * - A Minecraft screen is open (chat, inventory, pause menu, etc.)
     * 
     * @return true if keybinds should be processed, false otherwise
     */
    private static boolean shouldProcessKeybinds() {
        if (GuiManager.isScreenOpen()) {
            return false;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return false;
        }
        
        return client.currentScreen == null;
    }
}
