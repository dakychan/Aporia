package zenith.zov.client.screens.panels.components;

import zenith.zov.utility.game.other.MouseButton;
import zenith.zov.utility.render.display.base.UIContext;

public abstract class PanelComponent {
    protected float x;
    protected float y;
    protected float width;
    protected float height;

    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public float getHeight() {
        return height;
    }

    public abstract void render(UIContext ctx, float mouseX, float mouseY, float alpha);

    public void mouseClicked(double mouseX, double mouseY, MouseButton button) {
    }

    public void mouseReleased(double mouseX, double mouseY, MouseButton button) {
    }

    public void keyPressed(int keyCode, int scanCode, int modifiers) {
    }

    public boolean charTyped(char chr, int modifiers) {
        return false;
    }

    public boolean isVisible() {
        return true;
    }
}
