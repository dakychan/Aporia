package zenith.zov.client.screens.menu.elements.api;

import net.minecraft.client.gui.DrawContext;
import zenith.zov.base.font.Font;
import zenith.zov.client.modules.api.Category;
import zenith.zov.client.screens.menu.settings.api.MenuSetting;
import zenith.zov.utility.game.other.MouseButton;
import zenith.zov.utility.render.display.base.UIContext;

public abstract class AbstractMenuElement {
    public abstract void render(UIContext ctx, float mouseX, float mouseY, Font font,
                                float x, float y, float moduleWidth, float alpha, int colum);

    public abstract float getHeight();

    public abstract void onMouseClicked(double mouseX, double mouseY, MouseButton button);

    public abstract void onMouseReleased(double mouseX, double mouseY, MouseButton button);

    public abstract void onMouseDragged(double mouseX, double mouseY, MouseButton button,
                                        double deltaX, double deltaY);

    public abstract boolean keyPressed(int keyCode, int scanCode, int modifiers);

    public abstract boolean mouseScrolled(double mouseX, double mouseY,
                                          double horizontalAmount, double verticalAmount);
    public abstract Category getCategory();
    public abstract String getName();
}
