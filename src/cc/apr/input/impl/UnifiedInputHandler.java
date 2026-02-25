package cc.apr.input.impl;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import cc.apr.event.api.EventBus;
import cc.apr.event.impl.EventSystemImpl;
import cc.apr.event.impl.KeyPressEvent;
import cc.apr.event.impl.KeyReleaseEvent;
import cc.apr.event.impl.MouseClickEvent;
import cc.apr.gui.GuiManager;
import cc.apr.input.api.KeyboardKeys;

public class UnifiedInputHandler {
   private static final boolean[] keyStates = new boolean[512];
   private static final boolean[] mouseStates = new boolean[8];
   private static final EventBus eventBus = EventSystemImpl.getInstance();
   private static double lastMouseX = 0.0;
   private static double lastMouseY = 0.0;
   private static boolean initialized = false;

   public static void init() {
      initialized = true;
   }

   public static void tick() {
      if (initialized) {
         pollKeyboard();
         pollMouse();
      }
   }

   private static void pollKeyboard() {
      Minecraft client = Minecraft.getInstance();
      if (client != null && client.getWindow() != null) {
         long window = client.getWindow().handle();

         for (KeyboardKeys key : KeyboardKeys.values()) {
            if (key != KeyboardKeys.KEY_NONE && key.getKeyCode() >= 0 && !key.name().startsWith("MOUSE_")) {
               int keyCode = key.getKeyCode();
               if (keyCode < keyStates.length) {
                  boolean isPressed = GLFW.glfwGetKey(window, keyCode) == 1;
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
         }
      }
   }

   private static void pollMouse() {
      Minecraft client = Minecraft.getInstance();
      if (client != null && client.getWindow() != null) {
         long window = client.getWindow().handle();
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
            boolean isPressed = GLFW.glfwGetMouseButton(window, button) == 1;
            if (isPressed && !mouseStates[button]) {
               if (GuiManager.isScreenOpen()) {
                  GuiManager.getCurrentScreen().onMouseClick(currentX, currentY, button);
               } else {
                  eventBus.fire(new MouseClickEvent(currentX, currentY, button));
               }
            } else if (!isPressed && mouseStates[button] && GuiManager.isScreenOpen()) {
               GuiManager.getCurrentScreen().onMouseRelease(currentX, currentY, button);
            }

            mouseStates[button] = isPressed;
         }
      }
   }

   public static double getMouseX() {
      return lastMouseX;
   }

   public static double getMouseY() {
      return lastMouseY;
   }

   public static boolean isKeyPressed(int keyCode) {
      return keyCode >= 0 && keyCode < keyStates.length ? keyStates[keyCode] : false;
   }

   public static boolean isMouseButtonPressed(int button) {
      return button >= 0 && button < mouseStates.length ? mouseStates[button] : false;
   }
}
