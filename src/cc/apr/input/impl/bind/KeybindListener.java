package cc.apr.input.impl.bind;

import net.minecraft.client.Minecraft;
import cc.apr.event.impl.EventSystemImpl;
import cc.apr.event.impl.KeyPressEvent;
import cc.apr.gui.GuiManager;

public class KeybindListener {
   private static final KeybindManager manager = KeybindManager.getInstance();
   private static boolean initialized = false;

   public static void init() {
      if (!initialized) {
         EventSystemImpl.getInstance().subscribe(KeyPressEvent.class, event -> {
            if (shouldProcessKeybinds()) {
               manager.handleKeyPress(event.keyCode);
            }
         });
         initialized = true;
      }
   }

   private static boolean shouldProcessKeybinds() {
      if (GuiManager.isScreenOpen()) {
         return false;
      } else {
         Minecraft client = Minecraft.getInstance();
         return client == null ? false : client.screen == null;
      }
   }
}
