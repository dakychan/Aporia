package zenith.zov.utility.render.display.base;

import lombok.Getter;
import lombok.Setter;
import zenith.zov.utility.game.other.MouseButton;
import zenith.zov.utility.interfaces.IMinecraft;

@Getter
@Setter
public abstract class CustomComponent implements IMinecraft {
    protected float x, y, width, height;

    protected CustomComponent(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    protected CustomComponent() {
        this(0, 0, 0, 0);
    }

    public void render(UIContext context) {
        update(context);
        renderComponent(context);
    }

    protected abstract void renderComponent(UIContext context);

    public void onInit() {}

    public void update(UIContext context) {}

    public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {}

    public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {}

    public void onKeyPressed(int keyCode, int scanCode, int modifiers) {}

    public boolean charTyped(char chr, int modifiers) { return false; }

    public void onScroll(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {}

    public void pos(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void set(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean isHovered(float mouseX, float mouseY) {
        return GuiUtil.isHovered(x, y, width, height, mouseX, mouseY);
    }

    public boolean isHovered(double mouseX, double mouseY) {
        return GuiUtil.isHovered(x, y, width, height, mouseX, mouseY);
    }

    public boolean isHovered(UIContext context) {
        return isHovered(context.getMouseX(), context.getMouseY());
    }
}
