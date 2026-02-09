package zenith.zov.client.screens.panels.components;

import java.util.ArrayList;
import java.util.List;

import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;

import net.minecraft.util.math.MathHelper;
import zenith.zov.Zenith;
import zenith.zov.base.animations.base.Animation;
import zenith.zov.base.animations.base.Easing;
import zenith.zov.base.font.Font;
import zenith.zov.base.font.Fonts;
import zenith.zov.base.theme.Theme;
import zenith.zov.client.modules.api.Module;
import zenith.zov.client.modules.api.setting.Setting;
import zenith.zov.client.modules.api.setting.impl.BooleanSetting;
import zenith.zov.client.modules.api.setting.impl.ButtonSetting;
import zenith.zov.client.modules.api.setting.impl.ColorSetting;
import zenith.zov.client.modules.api.setting.impl.ItemSelectSetting;
import zenith.zov.client.modules.api.setting.impl.KeySetting;
import zenith.zov.client.modules.api.setting.impl.ModeSetting;
import zenith.zov.client.modules.api.setting.impl.MultiBooleanSetting;
import zenith.zov.client.modules.api.setting.impl.NumberSetting;
import zenith.zov.client.screens.panels.components.settings.BooleanComponent;
import zenith.zov.client.screens.panels.components.settings.ButtonComponent;
import zenith.zov.client.screens.panels.components.settings.ColorComponent;
import zenith.zov.client.screens.panels.components.settings.ItemSelectComponent;
import zenith.zov.client.screens.panels.components.settings.KeyComponent;
import zenith.zov.client.screens.panels.components.settings.ModeComponent;
import zenith.zov.client.screens.panels.components.settings.MultiBooleanComponent;
import zenith.zov.client.screens.panels.components.settings.NumberComponent;
import zenith.zov.utility.game.other.MouseButton;
import zenith.zov.utility.math.MathUtil;
import zenith.zov.utility.render.display.Keyboard;
import zenith.zov.utility.render.display.base.BorderRadius;
import zenith.zov.utility.render.display.base.Gradient;
import zenith.zov.utility.render.display.base.UIContext;
import zenith.zov.utility.render.display.base.color.ColorRGBA;
import zenith.zov.utility.render.display.shader.DrawUtil;

public class ModuleComponent extends PanelComponent {
    private static final float HEADER_HEIGHT = 20f;

    private final Module module;
    private final List<PanelComponent> components = new ArrayList<>();
    private final Animation openAnimation = new Animation(350, 0f, Easing.EXPO_OUT);
    private final Animation toggleAnimation = new Animation(250, 0f, Easing.EXPO_OUT);
    private final Animation bindAnimation = new Animation(250, 0f, Easing.EXPO_OUT);
    private final Animation hoverAnimation = new Animation(200, 0f, Easing.EXPO_OUT);
    private final Animation arrowAnimation = new Animation(300, 0f, Easing.EXPO_OUT);
    private boolean opened;
    private boolean binding;
    private boolean hovered;

    public ModuleComponent(Module module) {
        this.module = module;
        this.height = HEADER_HEIGHT;
        for (Setting setting : module.getSettings()) {
            if (setting instanceof BooleanSetting booleanSetting) {
                components.add(new BooleanComponent(booleanSetting));
            } else if (setting instanceof ModeSetting modeSetting) {
                components.add(new ModeComponent(modeSetting));
            } else if (setting instanceof MultiBooleanSetting multiBooleanSetting) {
                components.add(new MultiBooleanComponent(multiBooleanSetting));
            } else if (setting instanceof NumberSetting numberSetting) {
                components.add(new NumberComponent(numberSetting));
            } else if (setting instanceof KeySetting keySetting) {
                components.add(new KeyComponent(keySetting));
            } else if (setting instanceof ColorSetting colorSetting) {
                components.add(new ColorComponent(colorSetting));
            } else if (setting instanceof ItemSelectSetting itemSelectSetting) {
                components.add(new ItemSelectComponent(itemSelectSetting));
            } else if (setting instanceof ButtonSetting buttonSetting) {
                components.add(new ButtonComponent(buttonSetting));
            }
        }
    }

    public List<PanelComponent> getComponents() {
        return components;
    }

    public Module getModule() {
        return module;
    }

    @Override
    public void render(UIContext ctx, float mouseX, float mouseY, float alpha) {
        Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
        
        // Обновляем hover состояние
        hovered = MathUtil.isHovered(mouseX, mouseY, x, y, width, HEADER_HEIGHT);
        
        float openValue = openAnimation.update(opened ? 1f : 0f);
        float toggleValue = toggleAnimation.update(module.isEnabled() ? 1f : 0f);
        float bindValue = bindAnimation.update(binding ? 1f : 0f);
        float hoverValue = hoverAnimation.update(hovered ? 1f : 0f);
        float arrowValue = arrowAnimation.update(opened ? 1f : 0f);

        float extraHeight = 0f;
        for (PanelComponent component : components) {
            if (component.isVisible()) {
                extraHeight += component.getHeight();
            }
        }

        height = HEADER_HEIGHT + extraHeight * openValue;

        // Градиентный фон с учётом состояния
        ColorRGBA baseTop = theme.getForegroundLight();
        ColorRGBA baseBottom = theme.getForegroundColor();
        ColorRGBA activeColor = theme.getColor();
        
        // Подсветка при hover
        ColorRGBA hoverTint = theme.getWhite().withAlpha(15);
        
        ColorRGBA bgTop = baseTop.mix(activeColor, toggleValue * 0.3f).mulAlpha(alpha);
        ColorRGBA bgBottom = baseBottom.mix(activeColor, toggleValue * 0.2f).mulAlpha(alpha);
        
        if (hoverValue > 0.01f) {
            bgTop = bgTop.mix(hoverTint, hoverValue * 0.5f);
            bgBottom = bgBottom.mix(hoverTint, hoverValue * 0.3f);
        }

        // Основной фон с градиентом
        Gradient bgGradient = Gradient.of(bgTop, bgTop, bgBottom, bgBottom);
        ctx.drawRoundedRect(x, y, width, height, BorderRadius.all(5), bgGradient);

        // Акцентная полоска слева при включённом модуле
        if (toggleValue > 0.01f) {
            ColorRGBA accentTop = activeColor.mulAlpha(alpha * toggleValue);
            ColorRGBA accentBottom = activeColor.darker(0.3f).mulAlpha(alpha * toggleValue);
            Gradient accentGradient = Gradient.of(accentTop, accentTop, accentBottom, accentBottom);
            ctx.drawRoundedRect(x, y, 2.5f, HEADER_HEIGHT, BorderRadius.left(5, 5), accentGradient);
        }

        // Свечение при hover
        if (hoverValue > 0.01f) {
            DrawUtil.drawShadow(ctx.getMatrices(), x - 2, y - 2, width + 4, height + 4, 
                    8f * hoverValue, BorderRadius.all(6), theme.getColor().withAlpha((int)(30 * hoverValue * alpha)));
        }

        Font font = Fonts.MEDIUM.getFont(7);
        ColorRGBA textColor = theme.getWhite().mulAlpha(alpha);
        
        // Текст с плавным переходом при binding
        String keyName = module.getKeyCode() == -1 ? "None" : Keyboard.getKeyName(module.getKeyCode());
        if (keyName.length() > 6) {
            keyName = keyName.substring(0, 6) + "..";
        }
        String bindingText = "[" + keyName + "] Binding...";
        
        float textAlpha = MathHelper.clamp(1f - bindValue, 0f, 1f);
        float bindTextAlpha = MathHelper.clamp(bindValue, 0f, 1f);
        
        ctx.drawText(font, module.getName(), x + 10, y + 6.5f, textColor.mulAlpha(textAlpha));
        ctx.drawText(font, bindingText, x + 10, y + 6.5f, theme.getColor().mulAlpha(bindTextAlpha * alpha));

        // Анимированная стрелка
        boolean hasComponents = components.stream().anyMatch(PanelComponent::isVisible);
        if (hasComponents) {
            ctx.pushMatrix();
            float arrowX = x + width - 12;
            float arrowY = y + 10;
            ctx.getMatrices().translate(arrowX, arrowY);
            ctx.getMatrices().rotate((float) Math.toRadians(-90f * arrowValue));
            ctx.getMatrices().translate(-arrowX, -arrowY);
            
            Font iconFont = Fonts.MEDIUM.getFont(6);
            ctx.drawText(iconFont, ">", arrowX - 2, arrowY - 4, theme.getGrayLight().mulAlpha(alpha));
            ctx.popMatrix();
        }

        // Рендер компонентов настроек
        if (openValue > 0.01f) {
            float offset = HEADER_HEIGHT + 4;
            for (PanelComponent component : components) {
                if (!component.isVisible()) {
                    continue;
                }
                component.setX(x + 2);
                component.setY(y + offset);
                component.setWidth(width - 4);
                component.render(ctx, mouseX, mouseY, alpha * openValue);
                offset += component.getHeight() * openValue;
            }
        }
    }

    public void renderDescription(UIContext ctx, float mouseX, float mouseY, float alpha) {
        if (module.getInfo() == null || module.getInfo().description().isBlank()) {
            return;
        }
        if (!hovered) {
            return;
        }
        Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
        Font font = Fonts.MEDIUM.getFont(6.5f);
        String text = module.getInfo().description();
        float padding = 6;
        float textWidth = font.width(text);
        float textHeight = font.height();
        float tooltipX = x + width + 8;
        float tooltipY = y + 2;
        float tooltipWidth = textWidth + padding * 2;
        float tooltipHeight = textHeight + padding * 2;
        
        // Тень для тултипа
        DrawUtil.drawShadow(ctx.getMatrices(), tooltipX - 2, tooltipY - 2, tooltipWidth + 4, tooltipHeight + 4, 
                12f, BorderRadius.all(4), new ColorRGBA(0, 0, 0, (int)(80 * alpha)));
        
        // Фон тултипа с градиентом
        Gradient tooltipGradient = Gradient.of(
                theme.getForegroundLight().mulAlpha(alpha),
                theme.getForegroundLight().mulAlpha(alpha),
                theme.getForegroundColor().mulAlpha(alpha),
                theme.getForegroundColor().mulAlpha(alpha)
        );
        ctx.drawRoundedRect(tooltipX, tooltipY, tooltipWidth, tooltipHeight, BorderRadius.all(4), tooltipGradient);
        
        // Акцентная полоска
        ctx.drawRoundedRect(tooltipX, tooltipY, 2f, tooltipHeight, BorderRadius.left(4, 4), theme.getColor().mulAlpha(alpha));
        
        ctx.drawText(font, text, tooltipX + padding + 2, tooltipY + padding, theme.getWhite().mulAlpha(alpha));
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, MouseButton button) {
        if (openAnimation.getValue() > 0.01f) {
            for (PanelComponent component : components) {
                if (component.isVisible()) {
                    component.mouseClicked(mouseX, mouseY, button);
                }
            }
        }

        if (!MathUtil.isHovered(mouseX, mouseY, x, y, width, HEADER_HEIGHT)) {
            binding = false;
            return;
        }

        if (button == MouseButton.LEFT) {
            module.toggle();
        } else if (button == MouseButton.RIGHT) {
            if (components.stream().anyMatch(PanelComponent::isVisible)) {
                opened = !opened;
            }
        } else if (button == MouseButton.MIDDLE) {
            binding = !binding;
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, MouseButton button) {
        for (PanelComponent component : components) {
            if (component.isVisible()) {
                component.mouseReleased(mouseX, mouseY, button);
            }
        }
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        for (PanelComponent component : components) {
            if (component.isVisible()) {
                component.keyPressed(keyCode, scanCode, modifiers);
            }
        }

        if (!binding) {
            return;
        }

        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            module.setKeyCode(-1);
        } else if (keyCode != GLFW.GLFW_KEY_ESCAPE) {
            module.setKeyCode(keyCode);
        }
        binding = false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        boolean handled = false;
        for (PanelComponent component : components) {
            if (component.isVisible()) {
                handled |= component.charTyped(chr, modifiers);
            }
        }
        return handled;
    }
}

