package ru.input.impl;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import ru.event.impl.EventSystemImpl;
import ru.event.impl.KeyPressEvent;
import ru.event.impl.KeyReleaseEvent;
import ru.event.impl.MouseClickEvent;
import ru.event.api.EventBus;
import ru.gui.GuiManager;
import ru.input.api.KeyboardKeys;

/**
 * Unified input handler that consolidates all input event handling.
 * Polls GLFW for keyboard and mouse state, tracks state changes,
 * and fires events through EventSystem only.
 */
public class UnifiedInputHandler {
    private static final boolean[] keyStates = new boolean[512];
    private static final boolean[] mouseStates = new boolean[8];
    private static final EventBus eventBus = EventSystemImpl.getInstance();
    private static double lastMouseX = 0;
    private static double lastMouseY = 0;
    
    /**
     * Initializes the unified input handler by registering with client tick events
     */
    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            pollKeyboard();
            pollMouse();
        });
    }
    
    /**
     * Polls keyboard state and fires KeyPressEvent/KeyReleaseEvent for state changes
     */
    private static void pollKeyboard() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            return;
        }
        
        long window = client.getWindow().getHandle();
        
        // Poll only valid GLFW key codes from KeyboardKeys enum
        for (KeyboardKeys key : KeyboardKeys.values()) {
            // Skip mouse buttons and special keys
            if (key == KeyboardKeys.KEY_NONE || key.getKeyCode() < 0) {
                continue;
            }
            
            // Skip mouse button entries (they're handled in pollMouse)
            if (key.name().startsWith("MOUSE_")) {
                continue;
            }
            
            int keyCode = key.getKeyCode();
            if (keyCode >= keyStates.length) {
                continue;
            }
            
            boolean isPressed = GLFW.glfwGetKey(window, keyCode) == GLFW.GLFW_PRESS;
            
            // Fire event only on state change
            if (isPressed && !keyStates[keyCode]) {
                // Key was just pressed
                String keyName = key.getName();
                
                // If GUI is open, route to GUI manager, otherwise fire event
                if (GuiManager.isScreenOpen()) {
                    GuiManager.getCurrentScreen().onKeyPressed(keyCode);
                } else {
                    eventBus.fire(new KeyPressEvent(keyCode, keyName));
                }
            } else if (!isPressed && keyStates[keyCode]) {
                // Key was just released
                String keyName = key.getName();
                
                // If GUI is open, route to GUI manager, otherwise fire event
                if (GuiManager.isScreenOpen()) {
                    GuiManager.getCurrentScreen().onKeyReleased(keyCode);
                } else {
                    eventBus.fire(new KeyReleaseEvent(keyCode, keyName));
                }
            }
            
            keyStates[keyCode] = isPressed;
        }
    }
    
    /**
     * Polls mouse state and fires MouseClickEvent for button presses
     */
    private static void pollMouse() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            return;
        }
        
        long window = client.getWindow().getHandle();
        
        // Get current mouse position
        double[] mouseX = new double[1];
        double[] mouseY = new double[1];
        GLFW.glfwGetCursorPos(window, mouseX, mouseY);
        
        double currentX = mouseX[0];
        double currentY = mouseY[0];
        
        // Handle mouse movement for GUI
        if (GuiManager.isScreenOpen() && (currentX != lastMouseX || currentY != lastMouseY)) {
            GuiManager.getCurrentScreen().onMouseMove(currentX, currentY);
        }
        
        // Update last position
        lastMouseX = currentX;
        lastMouseY = currentY;
        
        // Poll mouse buttons (0-7 covers all standard mouse buttons)
        for (int button = 0; button < 8; button++) {
            boolean isPressed = GLFW.glfwGetMouseButton(window, button) == GLFW.GLFW_PRESS;
            
            // Handle button state changes
            if (isPressed && !mouseStates[button]) {
                // Button was just pressed
                if (GuiManager.isScreenOpen()) {
                    GuiManager.getCurrentScreen().onMouseClick(currentX, currentY, button);
                } else {
                    eventBus.fire(new MouseClickEvent(currentX, currentY, button));
                }
            } else if (!isPressed && mouseStates[button]) {
                // Button was just released
                if (GuiManager.isScreenOpen()) {
                    GuiManager.getCurrentScreen().onMouseRelease(currentX, currentY, button);
                }
                // Note: No MouseReleaseEvent in design, only MouseClickEvent on press
            }
            
            mouseStates[button] = isPressed;
        }
    }
    
    /**
     * Gets the current mouse X position
     * @return mouse X coordinate
     */
    public static double getMouseX() {
        return lastMouseX;
    }
    
    /**
     * Gets the current mouse Y position
     * @return mouse Y coordinate
     */
    public static double getMouseY() {
        return lastMouseY;
    }
    
    /**
     * Checks if a key is currently pressed
     * @param keyCode the GLFW key code
     * @return true if the key is pressed
     */
    public static boolean isKeyPressed(int keyCode) {
        if (keyCode < 0 || keyCode >= keyStates.length) {
            return false;
        }
        return keyStates[keyCode];
    }
    
    /**
     * Checks if a mouse button is currently pressed
     * @param button the mouse button (0=left, 1=right, 2=middle)
     * @return true if the button is pressed
     */
    public static boolean isMouseButtonPressed(int button) {
        if (button < 0 || button >= mouseStates.length) {
            return false;
        }
        return mouseStates[button];
    }
}
