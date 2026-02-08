package ru.input;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import ru.event.EventSystem;
import ru.event.KeyPressEvent;
import ru.gui.GuiManager;

public class KeyInputHandler {
    private static boolean[] keyStates = new boolean[512];
    private static double lastMouseX = 0;
    private static double lastMouseY = 0;
    private static boolean[] mouseButtonStates = new boolean[3];

    public static void register() {
        KeyBindings.register();
        
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            long window = MinecraftClient.getInstance().getWindow().getHandle();
            
            // Handle keyboard input
            for (Keyboard key : Keyboard.values()) {
                if (key == Keyboard.KEY_NONE) continue;
                
                int keyCode = key.getKey();
                if (keyCode < 0 || keyCode >= keyStates.length) continue;
                
                boolean isPressed = GLFW.glfwGetKey(window, keyCode) == GLFW.GLFW_PRESS;
                
                // Если клавиша была отпущена и теперь нажата - это нажатие
                if (isPressed && !keyStates[keyCode]) {
                    // Если наш GUI открыт, обрабатываем события сами
                    if (GuiManager.isScreenOpen()) {
                        GuiManager.getCurrentScreen().onKeyPressed(keyCode);
                    } else {
                        EventSystem.fire(new KeyPressEvent(key, keyCode));
                    }
                }
                
                keyStates[keyCode] = isPressed;
            }
            
            // Handle mouse input
            if (GuiManager.isScreenOpen()) {
                double[] mouseX = new double[1];
                double[] mouseY = new double[1];
                GLFW.glfwGetCursorPos(window, mouseX, mouseY);
                
                // Используем координаты как есть (в пикселях окна)
                double screenX = mouseX[0];
                double screenY = mouseY[0];
                
                // Mouse move
                if (screenX != lastMouseX || screenY != lastMouseY) {
                    GuiManager.getCurrentScreen().onMouseMove(screenX, screenY);
                    lastMouseX = screenX;
                    lastMouseY = screenY;
                }
                
                // Mouse click - track state changes
                int[] buttons = {GLFW.GLFW_MOUSE_BUTTON_LEFT, GLFW.GLFW_MOUSE_BUTTON_RIGHT, GLFW.GLFW_MOUSE_BUTTON_MIDDLE};
                for (int i = 0; i < buttons.length; i++) {
                    boolean isPressed = GLFW.glfwGetMouseButton(window, buttons[i]) == GLFW.GLFW_PRESS;
                    
                    // On press
                    if (isPressed && !mouseButtonStates[i]) {
                        GuiManager.getCurrentScreen().onMouseClick(screenX, screenY, i);
                    }
                    // On release
                    else if (!isPressed && mouseButtonStates[i]) {
                        GuiManager.getCurrentScreen().onMouseRelease(screenX, screenY, i);
                    }
                    
                    mouseButtonStates[i] = isPressed;
                }
            }
        });
    }
}
