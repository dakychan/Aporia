package cc.apr.event.impl;

import cc.apr.event.api.Event;
import cc.apr.input.api.KeyboardKeys;

public class KeyReleaseEvent extends Event {
   public final KeyboardKeys key;
   public final int keyCode;
   public final String keyName;

   public KeyReleaseEvent(KeyboardKeys key, int keyCode) {
      this.key = key;
      this.keyCode = keyCode;
      this.keyName = key.getName();
   }

   public KeyReleaseEvent(int keyCode, String keyName) {
      this.key = KeyboardKeys.findByKeyCode(keyCode);
      this.keyCode = keyCode;
      this.keyName = keyName;
   }
}
