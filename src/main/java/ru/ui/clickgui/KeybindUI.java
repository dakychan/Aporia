package ru.ui.clickgui;

import ru.input.api.KeyboardKeys;

/**
 * Utility class for keybind-related operations.
 * This class provides helper methods for formatting key names and other keybind utilities.
 * 
 * Note: Keybind mode is now handled directly in ClickGuiScreen to avoid
 * ConcurrentModificationException issues with EventSystemImpl.
 */
public class KeybindUI {
    
    // Private constructor to prevent instantiation
    private KeybindUI() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * Formats a key code into a human-readable key name.
     * This is a convenience wrapper around KeyboardKeys.getKeyName().
     * 
     * @param keyCode The GLFW key code
     * @return The formatted key name (e.g., "F", "LSHIFT", "MOUSE1")
     */
    public static String formatKeyName(int keyCode) {
        return KeyboardKeys.getKeyName(keyCode);
    }
}
