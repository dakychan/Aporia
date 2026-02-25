package cc.apr.ui.clickgui;

import cc.apr.input.api.KeyboardKeys;

public class KeybindUI {
   private KeybindUI() {
      throw new UnsupportedOperationException("Utility class");
   }

   public static String formatKeyName(int keyCode) {
      return KeyboardKeys.getKeyName(keyCode);
   }
}
