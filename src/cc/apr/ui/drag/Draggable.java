package cc.apr.ui.drag;

public interface Draggable {
   int getX();

   int getY();

   int getWidth();

   int getHeight();

   void setX(int var1);

   void setY(int var1);

   boolean isDragging();

   void setDragging(boolean var1);

   default boolean isMouseOver(int mouseX, int mouseY) {
      return mouseX >= this.getX() && mouseX <= this.getX() + this.getWidth() && mouseY >= this.getY() && mouseY <= this.getY() + this.getHeight();
   }
}
