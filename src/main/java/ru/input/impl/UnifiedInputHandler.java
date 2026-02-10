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

public class UnifiedInputHandler {
    private static final boolean[] keyStates = new boolean[512];
    private static final boolean[] mouseStates = new boolean[8];
    private static final EventBus eventBus = EventSystemImpl.getInstance();
    private static double lastMouseX = 0;
    private static double lastMouseY = 0;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            pollKeyboard();
            pollMouse();
        });
    }

    private static void pollKeyboard() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            return;
        }
        
        long window = client.getWindow().getHandle();
        
        for (KeyboardKeys key : KeyboardKeys.values()) {
            if (key == KeyboardKeys.KEY_NONE || key.getKeyCode() < 0) {
                continue;
            }
            if (key.name().startsWith("MOUSE_")) {
                continue;
            }
            
            int keyCode = key.getKeyCode();
            if (keyCode >= keyStates.length) {
                continue;
            }
            
            boolean isPressed = GLFW.glfwGetKey(window, keyCode) == GLFW.GLFW_PRESS;
            
            if (isPressed && !keyStates[keyCode]) {
                String keyName = key.getName();
                
                if (GuiManager.isScreenOpen()) {
                    GuiManager.getCurrentScreen().onKeyPressed(keyCode);
                } else {
                    eventBus.fire(new KeyPressEvent(keyCode, keyName));
                }
            } else if (!isPressed && keyStates[keyCode]) {
                String keyName = key.getName();
                
                if (GuiManager.isScreenOpen()) {
                    GuiManager.getCurrentScreen().onKeyReleased(keyCode);
                } else {
                    eventBus.fire(new KeyReleaseEvent(keyCode, keyName));
                }
            }
            
            keyStates[keyCode] = isPressed;
        }
    }

    private static void pollMouse() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            return;
        }
        
        long window = client.getWindow().getHandle();
        
        double[] mouseX = new double[1];
        double[] mouseY = new double[1];
        GLFW.glfwGetCursorPos(window, mouseX, mouseY);
        
        double currentX = mouseX[0];
        double currentY = mouseY[0];
        
        if (GuiManager.isScreenOpen() && (currentX != lastMouseX || currentY != lastMouseY)) {
            GuiManager.getCurrentScreen().onMouseMove(currentX, currentY);
        }
        
        lastMouseX = currentX;
        lastMouseY = currentY;
        
        for (int button = 0; button < 8; button++) {
            boolean isPressed = GLFW.glfwGetMouseButton(window, button) == GLFW.GLFW_PRESS;
            
            if (isPressed && !mouseStates[button]) {
                if (GuiManager.isScreenOpen()) {
                    GuiManager.getCurrentScreen().onMouseClick(currentX, currentY, button);
                } else {
                    eventBus.fire(new MouseClickEvent(currentX, currentY, button));
                }
            } else if (!isPressed && mouseStates[button]) {
                if (GuiManager.isScreenOpen()) {
                    GuiManager.getCurrentScreen().onMouseRelease(currentX, currentY, button);
                }
            }
            
            mouseStates[button] = isPressed;
        }
    }

    public static double getMouseX() {
        return lastMouseX;
    }

    public static double getMouseY() {
        return lastMouseY;
    }

    public static boolean isKeyPressed(int keyCode) {
        if (keyCode < 0 || keyCode >= keyStates.length) {
            return false;
        }
        return keyStates[keyCode];
    }

    public static boolean isMouseButtonPressed(int button) {
        if (button < 0 || button >= mouseStates.length) {
            return false;
        }
        return mouseStates[button];
    }
}
