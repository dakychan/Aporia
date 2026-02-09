package zenith.zov.utility.render.display.base;

import lombok.experimental.UtilityClass;
import net.minecraft.client.util.math.Vector2f;
import zenith.zov.utility.interfaces.IMinecraft;

@UtilityClass
public class GuiUtil implements IMinecraft {

    public boolean isHovered(double x, double y, double width, double height, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public boolean isHovered(double x, double y, double width, double height, UIContext context) {
        return isHovered(x, y, width, height, context.getMouseX(), context.getMouseY());
    }

    public boolean isHovered(double x, double y, double width, double height, double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public Vector2f getMouse(double customScale) {
        return new Vector2f((float) (mc.mouse.getX() / customScale), (float) (mc.mouse.getY() /customScale));
    }
}
