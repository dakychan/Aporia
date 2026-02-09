package zenith.zov.client.screens.menu.elements.impl;


import zenith.zov.Zenith;
import zenith.zov.base.animations.base.Animation;
import zenith.zov.base.animations.base.Easing;
import zenith.zov.base.font.Font;
import zenith.zov.base.font.Fonts;
import zenith.zov.base.theme.Theme;
import zenith.zov.client.modules.api.Category;
import zenith.zov.client.modules.api.setting.impl.ButtonSetting;
import zenith.zov.client.modules.api.setting.impl.ColorSetting;
import zenith.zov.client.screens.menu.elements.api.AbstractMenuElement;
import zenith.zov.client.screens.menu.settings.api.MenuSetting;
import zenith.zov.client.screens.menu.settings.impl.MenuButtonSetting;
import zenith.zov.client.screens.menu.settings.impl.MenuColorSetting;
import zenith.zov.utility.game.other.MouseButton;
import zenith.zov.utility.render.display.base.BorderRadius;
import zenith.zov.utility.render.display.base.Rect;
import zenith.zov.utility.render.display.base.UIContext;
import zenith.zov.utility.render.display.base.color.ColorRGBA;
import zenith.zov.utility.render.display.shader.DrawUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class MenuThemeElement extends AbstractMenuElement {
    private ColorSetting color;
    private ColorSetting secondColor;
    private ColorSetting gray;
    private ColorSetting grayLight;
    private ColorSetting foregroundLight;
    private ColorSetting whiteGray;
    private ColorSetting foregroundGray;
    private ColorSetting foregroundLightStroke;
    private ColorSetting foregroundColor;
    private ColorSetting foregroundStroke;
    private ColorSetting foregroundDark;
    private ColorSetting white;
    private ColorSetting backgroundColor;
    private MenuButtonSetting button;
    private final List<MenuSetting> settings = new ArrayList<>();
    private final Theme theme;

    private final Animation animation;
    private final Animation animationPosition;
    private final Animation animationY;
    private Rect bounds;

    private int lastColum = -1;
    boolean animated = false;

    public MenuThemeElement(Theme theme) {
        this.theme = theme;

        animation = new Animation(200, Zenith.getInstance().getThemeManager().is(theme) ? 1 : 0, Easing.LINEAR);
        animationPosition = new Animation(150, 1, Easing.QUAD_IN_OUT);
        animationY = new Animation(150, 1, Easing.QUAD_IN_OUT);
        this.color = new ColorSetting("Основной цвет", theme.getColor(), Theme.DARK::getColor);
        this.secondColor = new ColorSetting("Вторичный цвет", theme.getSecondColor(), Theme.DARK::getSecondColor);

        if (this.theme == Theme.CUSTOM_THEME) {

            this.backgroundColor = new ColorSetting("Цвет фона гуи", theme.getBackgroundColor(), Theme.DARK::getBackgroundColor);
            this.foregroundColor = new ColorSetting("Фон переднего", theme.getForegroundColor(), Theme.DARK::getForegroundColor);
            this.foregroundLight = new ColorSetting("Фон светлее", theme.getForegroundLight(), Theme.DARK::getForegroundLight);
            this.foregroundDark = new ColorSetting("Фон тёмный", theme.getForegroundDark(), Theme.DARK::getForegroundDark);
            this.foregroundGray = new ColorSetting("Фон (серый)", theme.getForegroundGray(), Theme.DARK::getForegroundGray);

            this.white = new ColorSetting("Текст основной", theme.getWhite(), Theme.DARK::getWhite);
            this.whiteGray = new ColorSetting("Иконки выкл", theme.getWhiteGray(), Theme.DARK::getWhiteGray);

            this.gray = new ColorSetting("Выкл текст", theme.getGray(), Theme.DARK::getGray);
            this.grayLight = new ColorSetting("Полу выкл текст", theme.getGrayLight(), Theme.DARK::getGrayLight);

            this.foregroundLightStroke = new ColorSetting("Обводка ярче", theme.getForegroundLightStroke(), Theme.DARK::getForegroundLightStroke);
            this.foregroundStroke = new ColorSetting("Обводка", theme.getForegroundStroke(), Theme.DARK::getForegroundStroke);

            Collections.addAll(settings,
                    new MenuColorSetting(color),
                    new MenuColorSetting(secondColor),

                    new MenuColorSetting(backgroundColor),
                    new MenuColorSetting(foregroundColor),
                    new MenuColorSetting(foregroundLight),
                    new MenuColorSetting(foregroundDark),
                    new MenuColorSetting(foregroundGray),

                    new MenuColorSetting(white),
                    new MenuColorSetting(whiteGray),

                    new MenuColorSetting(gray),
                    new MenuColorSetting(grayLight),

                    new MenuColorSetting(foregroundLightStroke),
                    new MenuColorSetting(foregroundStroke)
            );

            this.button = new MenuButtonSetting(new ButtonSetting("Сбросить",()->{
                for(MenuSetting setting : settings) {
                    if(setting instanceof MenuColorSetting colorSetting) {
                        colorSetting.getSetting().reset();
                    }
                }
            }));
            settings.add(button);

        }else {
            Collections.addAll(settings,new MenuColorSetting(color),new MenuColorSetting(secondColor));
        }
    }



    @Override
    public void render(UIContext ctx, float mouseX, float mouseY, Font font, float x, float y, float moduleWidth, float alpha, int colum) {

        if (lastColum == -1) lastColum = colum;
        if (lastColum != colum) {
            animated = true;
            animationPosition.animateTo(x);
            animationY.animateTo(y);
            lastColum = colum;
        }
        if (animated) {
            x = animationPosition.update(x);
            y = animationY.update(y);
            if (animationPosition.isDone() && animationY.isDone()) animated = false;
        } else {
            animationPosition.reset(x);
            animationY.reset(y);
        }

        animation.update(Zenith.getInstance().getThemeManager().is(theme) ? 1 : 0);

        float moduleHeight = 22;
        Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
        ColorRGBA moduleBg = theme.getForegroundColor().mulAlpha(alpha);

        boolean hasSettings = hasSettings();
        float settingAreaHeight = getHeight();
        ColorRGBA settingBg = theme.getForegroundDark().mulAlpha(alpha);
        bounds = new Rect(x, y, moduleWidth, moduleHeight);

        if (hasSettings) {
            ctx.drawRoundedRect(x, y, moduleWidth, settingAreaHeight, BorderRadius.all(8), settingBg);
            ctx.drawRoundedRect(x, y, moduleWidth, moduleHeight, BorderRadius.top(8, 8), moduleBg);

            DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, moduleWidth, settingAreaHeight, -0.1f,
                    BorderRadius.all(8), theme.getForegroundStroke().mulAlpha(alpha));
        } else {
            ctx.drawRoundedRect(x, y, moduleWidth, moduleHeight, BorderRadius.all(8), moduleBg);
            DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, moduleWidth, moduleHeight, -0.1f,
                    BorderRadius.all(8), theme.getForegroundStroke().mulAlpha(alpha));
        }

        ColorRGBA enabledColor = theme.getGray().mix(theme.getColor(), animation.getValue()).mulAlpha(alpha);
        ColorRGBA textColor = theme.getGrayLight().mix(theme.getWhite(), animation.getValue()).mulAlpha(alpha);
        ctx.drawText(Fonts.ICONS.getFont(5.5f), "B", x + 8, y + 9, enabledColor);
        ctx.drawText(font, this.theme.getName(), x + 18, y + 9, textColor);

        float keyBoxWidth = 22.5f;
        float keyBoxX = x + moduleWidth - keyBoxWidth;
        ColorRGBA badgeColor;


        badgeColor = this.theme.getColor().mulAlpha(alpha);


        ctx.drawRoundedRect(keyBoxX, y, keyBoxWidth, moduleHeight,
                hasSettings ? BorderRadius.topRight(8) : new BorderRadius(0, 6, 6, 0), badgeColor);

//        String keyText = "n/a";
//        int keyCode = module.getKeyCode();
//        if (keyCode != -1 && keyCode != 0) {
//            try {
//                String name = GLFW.glfwGetKeyName(keyCode, -1);
//                if (name != null && !name.isBlank()) {
//                    keyText = name.toUpperCase();
//                }
//            } catch (Exception ignored) {
//            }
//        }
//
//        Font keyFont = Fonts.MEDIUM.getFont(7);
//        float keyTextWidth = keyFont.width(keyText);
//        float keyTextX = keyBoxX + (keyBoxWidth - keyTextWidth) / 2f;
//        float keyTextY = y + (moduleHeight - keyFont.height()) / 2f;
//
//        ctx.drawText(keyFont, keyText, keyTextX, keyTextY,
//                keyCode != -1 ? theme.getGrayLight().mix(theme.getWhite(), animation.getValue()).mulAlpha(alpha)
//                        : theme.getGray().mulAlpha(alpha));

        float padding = 8;
        float startY = y + moduleHeight + padding;
        ColorRGBA descriptionColor = theme.getWhiteGray().mix(theme.getGrayLight(), animation.getValue()).mulAlpha(alpha);
        for (MenuSetting setting : settings) {
            setting.render(ctx, mouseX, mouseY, x, startY, moduleWidth, alpha, animation.getValue(),
                    enabledColor, textColor, descriptionColor, theme);
            startY += setting.getHeight() + 8;
        }
        if (hasSettings) {

            DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, moduleWidth, settingAreaHeight, -0.1f,
                    BorderRadius.all(8), theme.getForegroundStroke().mulAlpha(alpha));
        } else {
            DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, moduleWidth, moduleHeight, -0.1f,
                    BorderRadius.all(8), theme.getForegroundStroke().mulAlpha(alpha));
        }
        if (this.theme == Theme.CUSTOM_THEME) {
            this.theme.setColor(color.getColor());
            this.theme.setSecondColor(secondColor.getColor());
//            System.out.println(theme.getName());
            this.theme.setGray(gray.getColor());
            this.theme.setGrayLight(grayLight.getColor());
            this.theme.setForegroundLight(foregroundLight.getColor());
            this. theme.setWhiteGray(whiteGray.getColor());
            this.theme.setForegroundGray(foregroundGray.getColor());
            this.theme.setForegroundLightStroke(foregroundLightStroke.getColor());
            this.theme.setForegroundColor(foregroundColor.getColor());
            this. theme.setForegroundStroke(foregroundStroke.getColor());
            this.theme.setForegroundDark(foregroundDark.getColor());
            this.theme.setWhite(white.getColor());
            this.theme.setBackgroundColor(backgroundColor.getColor());
        } else {
            this. theme.setColor(color.getColor());
            this.theme.setSecondColor(secondColor.getColor());
        }

    }

    @Override
    public float getHeight() {
        return (float) (22 + (hasSettings() ? settings.stream().mapToDouble(MenuSetting::getHeight).sum()
                + 8 + (8 * settings.size()) : 0));
    }

    private boolean hasSettings() {
        return !settings.isEmpty();
    }

    @Override
    public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
        if (bounds != null && bounds.contains(mouseX, mouseY)) {
            Zenith.getInstance().getThemeManager().switchTheme(theme);
        }
        for (MenuSetting setting : settings) {
            setting.onMouseClicked(mouseX, mouseY, button);
        }
    }

    @Override
    public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {

    }

    @Override
    public void onMouseDragged(double mouseX, double mouseY, MouseButton button, double deltaX, double deltaY) {

    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return false;
    }

    @Override
    public Category getCategory() {
        return Category.THEMES;
    }

    @Override
    public String getName() {
        return theme.getName();
    }
}
