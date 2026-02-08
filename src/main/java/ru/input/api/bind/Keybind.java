package ru.input.api.bind;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Represents a keybind that maps a keyboard key to an action.
 * 
 * A keybind consists of:
 * - A unique identifier (id) for persistence and lookup
 * - A key code representing the bound key
 * - An action (Runnable) to execute when the key is pressed
 * 
 * Keybinds can be rebound to different keys using setKeyCode().
 * 
 * Requirements: 3.3, 3.4
 */
@Environment(EnvType.CLIENT)
public class Keybind {
    private final String id;
    private int keyCode;
    private final Runnable action;

    /**
     * Creates a new keybind.
     * 
     * @param id Unique identifier for this keybind (e.g., "module.killaura.toggle")
     * @param keyCode The GLFW key code to bind to
     * @param action The action to execute when the key is pressed
     */
    public Keybind(String id, int keyCode, Runnable action) {
        this.id = id;
        this.keyCode = keyCode;
        this.action = action;
    }

    /**
     * Executes the action associated with this keybind.
     * This method is called by the KeybindManager when the bound key is pressed.
     */
    public void execute() {
        if (action != null) {
            action.run();
        }
    }

    /**
     * Updates the key code for this keybind (rebinding).
     * 
     * @param keyCode The new GLFW key code to bind to
     */
    public void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
    }

    /**
     * Gets the current key code for this keybind.
     * 
     * @return The GLFW key code
     */
    public int getKeyCode() {
        return keyCode;
    }

    /**
     * Gets the unique identifier for this keybind.
     * 
     * @return The keybind ID
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the action associated with this keybind.
     * 
     * @return The action Runnable
     */
    public Runnable getAction() {
        return action;
    }

    @Override
    public String toString() {
        return "Keybind{id='" + id + "', keyCode=" + keyCode + "}";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Keybind keybind = (Keybind) obj;
        return id.equals(keybind.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
