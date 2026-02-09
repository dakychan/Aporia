package zenith.zov.client.screens.menu.settings.api;

import lombok.Getter;
import zenith.zov.base.animations.base.Animation;
import zenith.zov.base.animations.base.Easing;
import zenith.zov.base.theme.Theme;
import zenith.zov.utility.render.display.base.ChangeRect;
import zenith.zov.utility.render.display.base.Rect;
import zenith.zov.utility.render.display.base.UIContext;
import zenith.zov.utility.render.display.base.color.ColorRGBA;

public abstract class MenuPopupSetting extends MenuSetting {
    @Getter
    protected final ChangeRect bounds;
    @Getter
    protected Animation animationScale = new Animation(200,0.01f, Easing.QUAD_IN_OUT);

    protected MenuPopupSetting(ChangeRect bounds) {
        this.bounds = bounds;
    }

    public abstract void render(UIContext ctx, float mouseX, float mouseY, float alpha, Theme theme);

    @Override
    public final void render(UIContext ctx, float mouseX, float mouseY, float x, float settingY, float moduleWidth, float alpha, float animEnable, ColorRGBA themeColor, ColorRGBA textColor, ColorRGBA descriptionColor, Theme theme) {
    }


    public abstract boolean charTyped(char chr, int modifiers);
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return false;
    }
}
