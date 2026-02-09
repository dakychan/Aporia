package zenith.zov.client.screens.menu.elements.impl;

import lombok.Getter;
import org.lwjgl.glfw.GLFW;
import zenith.zov.Zenith;
import zenith.zov.base.animations.base.Animation;
import zenith.zov.base.animations.base.Easing;
import zenith.zov.base.font.Font;
import zenith.zov.base.font.Fonts;
import zenith.zov.client.modules.api.Category;
import zenith.zov.client.modules.api.Module;
import zenith.zov.base.theme.Theme;
import zenith.zov.client.screens.menu.elements.api.AbstractMenuElement;
import zenith.zov.client.screens.menu.settings.api.MenuSetting;
import zenith.zov.client.screens.menu.settings.impl.*;
import zenith.zov.client.modules.api.setting.Setting;
import zenith.zov.client.modules.api.setting.impl.*;
import zenith.zov.utility.game.other.MouseButton;
import zenith.zov.utility.render.display.Keyboard;
import zenith.zov.utility.render.display.base.BorderRadius;
import zenith.zov.utility.render.display.base.Rect;
import zenith.zov.utility.render.display.base.UIContext;
import zenith.zov.utility.render.display.base.color.ColorRGBA;
import zenith.zov.utility.render.display.shader.DrawUtil;

import java.util.ArrayList;
import java.util.List;

public class MenuModuleElement extends AbstractMenuElement {
    @Getter
    private final Module module;
    private final List<MenuSetting> settings = new ArrayList<>();
    private final Animation animation;
    private final Animation animationPosition;
    private final Animation animationY;
    private Rect bounds;
    private Rect boundsBind;
    @Getter
    private boolean binding = false;
    private int lastColum = -1;

    public MenuModuleElement(Module module) {
        this.module = module;
        animation = new Animation(200, module.isEnabled() ? 1 : 0, Easing.LINEAR);
        animationPosition = new Animation(150, 1, Easing.QUAD_IN_OUT);
        animationY = new Animation(150, 1, Easing.QUAD_IN_OUT);

        for (Setting setting : module.getSettings()) {
            if (setting instanceof NumberSetting sliderSetting) {
                settings.add(new MenuSliderSetting(sliderSetting));
            } else if (setting instanceof ModeSetting modeSetting) {
                settings.add(new MenuModeSetting(modeSetting));
            } else if (setting instanceof MultiBooleanSetting selectSetting) {
                settings.add(new MenuSelectSetting(selectSetting));
            } else if (setting instanceof BooleanSetting booleanSetting) {
                settings.add(new MenuBooleanSetting(booleanSetting));
            } else if (setting instanceof ColorSetting colorSetting) {
                settings.add(new MenuColorSetting(colorSetting));
            }
            else if (setting instanceof ButtonSetting buttonSetting) {
                settings.add(new MenuButtonSetting(buttonSetting));
            }
            else if (setting instanceof ItemSelectSetting itemSelectSetting) {
                settings.add(new MenuItemSetting(itemSelectSetting));
            }
            else if (setting instanceof KeySetting keySetting) {
                settings.add(new MenuKeySetting(keySetting));
            }
        }
    }

    boolean animated = false;

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

        animation.animateTo(module.isEnabled() ? 1 : 0);
        animation.update();
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

                } else {
            ctx.drawRoundedRect(x, y, moduleWidth, moduleHeight, BorderRadius.all(8), moduleBg);
              }

        ColorRGBA enabledColor = theme.getGray().mix(theme.getColor(), animation.getValue()).mulAlpha(alpha);
        ColorRGBA textColor = theme.getGrayLight().mix(theme.getWhite(), animation.getValue()).mulAlpha(alpha);
        ctx.drawText(Fonts.ICONS.getFont(5.5f), "B", x + 8, y + 9, enabledColor);
        ctx.drawText(font, module.getName(), x + 18, y + 9, textColor);

        float keyBoxWidth = 22.5f;
        float keyBoxX = x + moduleWidth - keyBoxWidth;
        ColorRGBA badgeColor;

        if (isBinding()) {
            badgeColor = theme.getSecondColor();
        } else if (module.getKeyCode() != -1) {
            badgeColor = theme.getWhiteGray().mix(theme.getColor(), animation.getValue()).mulAlpha(alpha);
        } else {
            badgeColor = theme.getForegroundLight().mulAlpha(alpha);
        }


        ctx.drawRoundedRect(keyBoxX, y, keyBoxWidth, moduleHeight,
                hasSettings ? BorderRadius.topRight(8) : new BorderRadius(0, 8, 8, 0), badgeColor);

        String keyText = "n/a";
        int keyCode = module.getKeyCode();
        if (keyCode != -1 && keyCode != 0) {
            try {
                String name = Keyboard.getKeyName(keyCode);
                if (name != null && !name.isBlank()) {
                    keyText = name.toUpperCase();
                }
            } catch (Exception ignored) {
            }
        }

        Font keyFont = Fonts.MEDIUM.getFont(7);

        float keyTextY = y + (moduleHeight - keyFont.height()) / 2f;

        float keyPadding = 2f;
        float keyContentWidth = keyBoxWidth - keyPadding * 2f;
        float keyContentX = keyBoxX + keyPadding;

        ColorRGBA keyColor = (keyCode != -1
                ? theme.getGrayLight().mix(theme.getWhite(), animation.getValue())
                : theme.getGray()
        ).mulAlpha(alpha);
        boundsBind = new Rect(keyBoxX, y, keyBoxWidth, moduleHeight);
        ctx.enableScissor((int) keyBoxX+1, (int) y, (int) (keyBoxX+ keyBoxWidth-2), (int) (y+ moduleHeight));
        drawScrollingText(ctx, keyFont, keyText, keyContentX, keyTextY, keyContentWidth, keyColor);

        ctx.disableScissor();

        float padding = 8;
        float startY = y + moduleHeight + padding;
        ColorRGBA descriptionColor = theme.getWhiteGray().mix(theme.getGrayLight(), animation.getValue()).mulAlpha(alpha);
        for (MenuSetting setting : settings) {
            if(!setting.isVisible()) continue;
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
    }
    //this sheet code 2023
    private void drawScrollingText(UIContext ctx, Font font, String text,
                                   float x, float y, float maxWidth, ColorRGBA color) {
        float textW = font.width(text);

        
        if (textW <= maxWidth) {
            float centeredX = x + (maxWidth - textW) / 2f;
            ctx.drawText(font, text, centeredX, y, color);
            return;
        }

        
        float scrollMax = textW - maxWidth;

        
        float pauseMs = 700f;        
        float slideMs = 1400f;       
        float total = pauseMs + slideMs + pauseMs + slideMs;

        long now = System.currentTimeMillis();
        float t = now % (long) total;

        float offset; 
        if (t < pauseMs) {
            
            offset = 0f;
        } else if (t < pauseMs + slideMs) {
            
            float k = (t - pauseMs) / slideMs;          
            
            k = k * k * (3f - 2f * k);
            offset = k * scrollMax;
        } else if (t < pauseMs + slideMs + pauseMs) {
            
            offset = scrollMax;
        } else {
            
            float k = (t - pauseMs - slideMs - pauseMs) / slideMs; 
            k = k * k * (3f - 2f * k);
            offset = scrollMax * (1f - k);
        }

        
        ctx.drawText(font, text, x - offset, y, color);
    }
    @Override
    public float getHeight() {
        return (float) (22 + (hasSettings() ? settings.stream().filter(MenuSetting::isVisible).mapToDouble(m -> m.getHeight()+8).sum()
                + 8  : 0));
    }

    public boolean hasSettings() {
        return !settings.isEmpty() && settings.stream().anyMatch(MenuSetting::isVisible);
    }

    @Override
    public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
        if (bounds != null && bounds.contains(mouseX, mouseY)) {
            if(button .getButtonIndex()>2 &&binding){
                binding = false;
                this.module.setKeyCode(button.getButtonIndex());
            }
            if (button == MouseButton.LEFT ) {
                if(boundsBind!=null && boundsBind.contains(mouseX, mouseY)){
                    binding = !binding;
                }else {
                    module.toggle();
                }
            } else if (button == MouseButton.MIDDLE) {
                binding = !binding;
            }
        }

        for (MenuSetting setting : settings) {
            setting.onMouseClicked(mouseX, mouseY, button);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (binding) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                module.setKeyCode(-1);
            } else {
                module.setKeyCode(keyCode);
            }
            binding = false;
            return true;
        }
        boolean result = false;
        for (MenuSetting setting : settings) {
            if(setting.keyPressed(keyCode, scanCode, modifiers)) {
               result = true;
            }
        }
        return result;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return true;
    }

    @Override
    public Category getCategory() {
        return module.getCategory();
    }

    @Override
    public String getName() {
        return module.getName();
    }

    @Override
    public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
        for (MenuSetting setting : settings) {
            setting.onMouseReleased(mouseX, mouseY, button);
        }
    }

    @Override
    public void onMouseDragged(double mouseX, double mouseY, MouseButton button, double deltaX, double deltaY) {

    }
}
