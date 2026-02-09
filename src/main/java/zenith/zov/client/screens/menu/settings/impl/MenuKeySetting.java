package zenith.zov.client.screens.menu.settings.impl;


import lombok.Getter;
import org.lwjgl.glfw.GLFW;
import zenith.zov.base.animations.base.Animation;
import zenith.zov.base.animations.base.Easing;
import zenith.zov.base.font.Font;
import zenith.zov.base.font.Fonts;
import zenith.zov.base.theme.Theme;
import zenith.zov.client.hud.elements.component.KeybindsComponent;
import zenith.zov.client.modules.api.setting.impl.KeySetting;
import zenith.zov.client.screens.menu.settings.api.MenuSetting;


import zenith.zov.client.modules.api.setting.impl.BooleanSetting;
import zenith.zov.utility.game.other.MouseButton;
import zenith.zov.utility.render.display.Keyboard;
import zenith.zov.utility.render.display.base.BorderRadius;
import zenith.zov.utility.render.display.base.Rect;
import zenith.zov.utility.render.display.base.UIContext;
import zenith.zov.utility.render.display.base.color.ColorRGBA;

public class MenuKeySetting extends MenuSetting {
    @Getter
    private final KeySetting setting;
    private Rect bounds;
    private boolean binding = false;

    public MenuKeySetting(KeySetting setting) {

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
        // ctx.drawText(descFont, setting.getDescription(), settingX + 15, textY + 10, theme.getWhite().mulAlpha(alpha));


        float iconSize = 6;
        float iconY = textY - 1;
        Font iconFont = Fonts.ICONS.getFont(6);

        ctx.drawText(Fonts.ICONS.getFont(5.5f), "L", settingX + 1.2f, iconY + 1.2f, theme.getGray().mix(theme.getColor(),animEnable).mulAlpha(alpha));
        // ctx.drawTexture(Zenith.id("icons/check.png"), settingX + 1.5f, iconY + 2f, 7, 7, Zenith.getInstance().getThemeManager().getCurrentTheme().getForegroundColor().mulAlpha(alpha));
        String keyText = binding?"...":"n/a";
        int keyCode = setting.getKeyCode();
        if (keyCode != -1 && keyCode != 0 &&!binding) {
            try {
                String name = Keyboard.getKeyName(keyCode);
                if (name != null && !name.isBlank()) {
                    keyText = name.toLowerCase();
                    if(keyText.length()>6){
                        keyText =keyText.substring(0,6)+"..";
                    }
                }
            } catch (Exception ignored) {
            }
        }
        Font font = Fonts.MEDIUM.getFont(7);
        float toggleWitdht = 4 + font.width(keyText) + 4;
        float toggleHeight = 8;
        float toggleX = x + moduleWidth - toggleWitdht - 8;
        float toggleY = settingY;

        ColorRGBA colorToggle = theme.getForegroundLight().mix(theme.getColor(), animEnable).mulAlpha(alpha);
        ColorRGBA borderColor = theme.getForegroundLightStroke().mix(theme.getForegroundLightStroke().mulAlpha(0), animEnable).mulAlpha(alpha);
        ColorRGBA golochakaColor = theme.getGrayLight().mix(theme.getWhite(), animEnable);

        ctx.drawRoundedRect(toggleX, toggleY, toggleWitdht, toggleHeight, BorderRadius.all(2), colorToggle);
        ctx.drawRoundedBorder(toggleX, toggleY, toggleWitdht, toggleHeight, -0.1f, BorderRadius.all(2), borderColor);
        ctx.drawText(font, keyText, toggleX + 4, toggleY + 1, textColor);
        bounds = new Rect(toggleX, toggleY, toggleWitdht, toggleHeight);


    }


    @Override
    public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
        if (binding &&button.getButtonIndex()>=2) {
            setting.setKeyCode(button.getButtonIndex());
            binding = false;
            return;
        }
        if (bounds != null && bounds.contains(mouseX, mouseY)) {
            binding = true;
            return;
        }

    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {

        if (binding) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                keyCode = -1;
            }
            setting.setKeyCode(keyCode);
            binding = false;
            return true;
        }
        return false;
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
