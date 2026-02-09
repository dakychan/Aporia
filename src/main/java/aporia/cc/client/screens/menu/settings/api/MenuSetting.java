package aporia.cc.client.screens.menu.settings.api;

import aporia.cc.base.theme.Theme;

import aporia.cc.utility.game.other.MouseButton;
import aporia.cc.utility.render.display.base.UIContext;
import aporia.cc.utility.render.display.base.color.ColorRGBA;

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
