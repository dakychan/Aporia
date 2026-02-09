package aporia.cc.client.screens.menu.settings.impl;

import lombok.Getter;
import aporia.cc.base.animations.base.Animation;
import aporia.cc.base.animations.base.Easing;
import aporia.cc.base.font.Font;
import aporia.cc.base.font.Fonts;
import aporia.cc.base.theme.Theme;
import aporia.cc.client.screens.menu.settings.api.MenuSetting;


import aporia.cc.client.modules.api.setting.impl.BooleanSetting;
import aporia.cc.utility.game.other.MouseButton;
import aporia.cc.utility.render.display.base.BorderRadius;
import aporia.cc.utility.render.display.base.Rect;
import aporia.cc.utility.render.display.base.UIContext;
import aporia.cc.utility.render.display.base.color.ColorRGBA;

public class MenuBooleanSetting extends MenuSetting {
    @Getter
    private final BooleanSetting setting;
    private final Animation animation = new Animation(300, Easing.QUARTIC_OUT);
    private Rect bounds;

    public MenuBooleanSetting(BooleanSetting setting) {

        this.setting = setting;
    }

    @Override
    public void render(UIContext ctx, float mouseX, float mouseY, float x, float settingY, float moduleWidth, float alpha, float animEnable, ColorRGBA themeColor, ColorRGBA textColor, ColorRGBA descriptionColor, Theme theme) {
        float settingHeight = 24;
        float settingX = x + 8;
        Font settingFont = Fonts.MEDIUM.getFont(7);
        Font descFont = Fonts.MEDIUM.getFont(6);
        float textY = settingY + (8 - settingFont.height()) / 2 - 0.5f;

        ctx.drawText(settingFont, setting.getName(), x + 8 + 10, textY, textColor);

        animation.animateTo(setting.isEnabled() ? 1.0f : 0.0f);
        float progress = animation.update();




        float iconSize = 6;
        float iconY = textY - 1;
        Font iconFont = Fonts.ICONS.getFont(6);
        ctx.drawRoundedRect(settingX, iconY, iconSize, iconSize, BorderRadius.all(1), themeColor);

        ctx.drawText(Fonts.ICONS.getFont(5.5f), "S", settingX + 1.2f, iconY + 0.5f,theme.getForegroundDark().mulAlpha(alpha));

        float toggleSize = 8;
        float toggleX = x + moduleWidth - toggleSize - 8;
        float toggleY = settingY;

        ColorRGBA colorEnable = theme.getWhiteGray().mix(theme.getColor(),animEnable);
        ColorRGBA colorToggle = theme.getForegroundLight().mix(colorEnable,animation.getValue()).mulAlpha(alpha);
        ColorRGBA borderColor = theme.getForegroundLightStroke().mix(new ColorRGBA(0,0,0,0),animation.getValue()).mulAlpha(alpha);
        ColorRGBA golochakaColor = theme.getGrayLight().mix(theme.getWhite(),animEnable);
        ColorRGBA golochakaFinalColor = new ColorRGBA(0,0,0,0).mix(golochakaColor,animation.getValue()).mulAlpha(alpha);

        ctx.drawRoundedRect(toggleX, toggleY, toggleSize, toggleSize, BorderRadius.all(2), colorToggle);
        ctx.drawRoundedBorder(toggleX, toggleY, toggleSize, toggleSize,-0.1f, BorderRadius.all(2),borderColor);
        ctx.drawText(iconFont,"S",toggleX+2,toggleY+1,golochakaFinalColor);
        bounds = new Rect(toggleX, toggleY, toggleSize, toggleSize);


    }


    @Override
    public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
        if (bounds != null && bounds.contains(mouseX, mouseY) && button == MouseButton.LEFT) {
            setting.toggle();
        }
    }

    @Override
    public float getWidth() {
        return 0;
    }

    @Override
    public float getHeight() {
        return 8;
    }

    @Override
    public boolean isVisible() {
        return setting.getVisible().get();
    }
}

