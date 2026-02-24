package ru.ui.clickgui;

import ru.input.api.KeyboardKeys;

public class KeybindUI {
   private KeybindUI() {
      throw new UnsupportedOperationException("Utility class");
   }

   public static String formatKeyName(int keyCode) {
      return KeyboardKeys.getKeyName(keyCode);
   }
}
