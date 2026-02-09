package zenith.zov.client.screens.menu.settings.api;

import zenith.zov.base.theme.Theme;

import zenith.zov.utility.game.other.MouseButton;
import zenith.zov.utility.render.display.base.UIContext;
import zenith.zov.utility.render.display.base.color.ColorRGBA;

public abstract class MenuSetting {

    protected float height;

    public abstract void render(UIContext ctx, float mouseX, float mouseY, float x, float settingY, float moduleWidth, float alpha, float animEnable, ColorRGBA themeColor,ColorRGBA textColor,ColorRGBA descriptionColor, Theme theme);

    public abstract void onMouseClicked(double mouseX, double mouseY, MouseButton button);

    public abstract float getWidth();

    public abstract float getHeight();

    public abstract boolean isVisible();

    public boolean keyPressed(int keyCode, int scanCode, int modifiers){
        return false;
    }

    public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {

    }
}
