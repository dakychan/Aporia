package cc.apr.input.api.bind;

public class Keybind {
   private final String id;
   private int keyCode;
   private final Runnable action;

   public Keybind(String id, int keyCode, Runnable action) {
      this.id = id;
      this.keyCode = keyCode;
      this.action = action;
   }

   public void execute() {
      if (this.action != null) {
         this.action.run();
      }
   }

   public void setKeyCode(int keyCode) {
      this.keyCode = keyCode;
   }

   public int getKeyCode() {
      return this.keyCode;
   }

   public String getId() {
      return this.id;
   }

   public Runnable getAction() {
      return this.action;
   }

   @Override
   public String toString() {
      return "Keybind{id='" + this.id + "', keyCode=" + this.keyCode + "}";
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      } else if (obj != null && this.getClass() == obj.getClass()) {
         Keybind keybind = (Keybind)obj;
         return this.id.equals(keybind.id);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return this.id.hashCode();
   }
}
