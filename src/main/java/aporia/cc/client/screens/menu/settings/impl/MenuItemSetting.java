package aporia.cc.client.screens.menu.settings.impl;

import lombok.Getter;
import aporia.cc.Aporia;
import aporia.cc.base.font.Font;
import aporia.cc.base.font.Fonts;
import aporia.cc.base.theme.Theme;
import aporia.cc.client.modules.api.setting.impl.ItemSelectSetting;
import aporia.cc.client.screens.menu.settings.api.MenuSetting;
import aporia.cc.client.screens.menu.settings.impl.popup.MenuItemPopupSetting;
import aporia.cc.utility.game.other.MouseButton;
import aporia.cc.utility.render.display.base.BorderRadius;
import aporia.cc.utility.render.display.base.ChangeRect;
import aporia.cc.utility.render.display.base.Rect;
import aporia.cc.utility.render.display.base.UIContext;
import aporia.cc.utility.render.display.base.color.ColorRGBA;

public class MenuItemSetting extends MenuSetting {
    @Getter
    private final ItemSelectSetting setting;


    private Rect bounds;
    private ChangeRect boundsColor;


    public MenuItemSetting(ItemSelectSetting setting) {

        this.setting = setting;
        boundsColor = new ChangeRect(0,0,156/2f,96);
    }

    @Override
    public void render(UIContext ctx, float mouseX, float mouseY, float x, float settingY, float moduleWidth, float alpha, float animEnable, ColorRGBA themeColor, ColorRGBA textColor, ColorRGBA descriptionColor, Theme theme) {
        float settingHeight = 24;
        float settingX = x + 8;
        Font settingFont = Fonts.MEDIUM.getFont(7);
        Font descFont = Fonts.MEDIUM.getFont(6);
        float textY = settingY + (8 - settingFont.height()) / 2 - 0.5f;

        ctx.drawText(settingFont, setting.getName(), x + 8 + 10, textY, textColor);






        float iconSize = 6;
        float iconY = textY - 1;
        Font iconFont = Fonts.ICONS.getFont(6);

        ctx.drawText(Fonts.ICONS.getFont(6), "V", settingX+1.5f, iconY + 1,themeColor);

        float toggleSize = 8;
        float toggleX = x + moduleWidth - toggleSize - 8;
        float toggleY = settingY;

        ctx.drawRoundedBorder(toggleX, toggleY, toggleSize, toggleSize,0.2f, BorderRadius.all(2), themeColor);
        bounds = new Rect(toggleX, toggleY, toggleSize, toggleSize);
        boundsColor.setX(toggleX+20);
        boundsColor.setY(toggleY+toggleSize-boundsColor.getHeight()/2);
        boundsColor.setHeight(200+20+4);
        boundsColor.setWidth(150);
    }


    @Override
    public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
        if(bounds!=null&&bounds.contains(mouseX, mouseY)) {
            Aporia.getInstance().getMenuScreen().addPopupMenuSetting(new MenuItemPopupSetting(this.setting,boundsColor));
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
        return true;
    }
}

