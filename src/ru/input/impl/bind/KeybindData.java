package ru.input.impl.bind;

public class KeybindData {
   private String id;
   private int keyCode;
   private String keyName;

   public KeybindData() {
   }

   public KeybindData(String id, int keyCode, String keyName) {
      this.id = id;
      this.keyCode = keyCode;
      this.keyName = keyName;
   }

   public String getId() {
      return this.id;
   }

   public void setId(String id) {
      this.id = id;
   }

   public int getKeyCode() {
      return this.keyCode;
   }

   public void setKeyCode(int keyCode) {
      this.keyCode = keyCode;
   }

   public String getKeyName() {
      return this.keyName;
   }

   public void setKeyName(String keyName) {
      this.keyName = keyName;
   }
}
