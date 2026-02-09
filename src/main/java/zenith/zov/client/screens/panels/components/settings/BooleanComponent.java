package zenith.zov.client.screens.panels.components.settings;

import zenith.zov.Zenith;
import zenith.zov.base.animations.base.Animation;
import zenith.zov.base.animations.base.Easing;
import zenith.zov.base.font.Font;
import zenith.zov.base.font.Fonts;
import zenith.zov.base.theme.Theme;
import zenith.zov.client.modules.api.setting.impl.BooleanSetting;
import zenith.zov.client.screens.panels.components.PanelComponent;
import zenith.zov.utility.game.other.MouseButton;
import zenith.zov.utility.math.MathUtil;
import zenith.zov.utility.render.display.base.BorderRadius;
import zenith.zov.utility.render.display.base.Gradient;
import zenith.zov.utility.render.display.base.UIContext;
import zenith.zov.utility.render.display.base.color.ColorRGBA;

public class BooleanComponent extends PanelComponent {
    private final BooleanSetting setting;
    private final Animation toggleAnimation = new Animation(200, 0f, Easing.EXPO_OUT);
    private final Animation hoverAnimation = new Animation(150, 0f, Easing.EXPO_OUT);
    private boolean hovered;

    public BooleanComponent(BooleanSetting setting) {
        this.setting = setting;
        this.height = 14f;
    }

    @Override
    public void render(UIContext ctx, float mouseX, float mouseY, float alpha) {
        Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
        Font font = Fonts.MEDIUM.getFont(6.5f);

        hovered = MathUtil.isHovered(mouseX, mouseY, x + 6, y - 4, width - 12, 14);
        float toggleValue = toggleAnimation.update(setting.isEnabled() ? 1f : 0f);
        float hoverValue = hoverAnimation.update(hovered ? 1f : 0f);

        float boxSize = 10f;
        float boxX = x + 8;
        float boxY = y - 2;

        // Фон чекбокса с градиентом
        ColorRGBA boxInactive = theme.getForegroundDark();
        ColorRGBA boxActive = theme.getColor();
        ColorRGBA boxHover = theme.getWhite().withAlpha(20);
        
        ColorRGBA boxColor = boxInactive.mix(boxActive, toggleValue);
        if (hoverValue > 0.01f) {
            boxColor = boxColor.mix(boxHover, hoverValue * 0.5f);
        }
        
        // Градиент для объёма
        Gradient boxGradient = Gradient.of(
                boxColor.brighter(0.1f).mulAlpha(alpha),
                boxColor.brighter(0.1f).mulAlpha(alpha),
                boxColor.darker(0.1f).mulAlpha(alpha),
                boxColor.darker(0.1f).mulAlpha(alpha)
        );
        ctx.drawRoundedRect(boxX, boxY, boxSize, boxSize, BorderRadius.all(3), boxGradient);

        // Галочка с анимацией
        if (toggleValue > 0.01f) {
            Font checkFont = Fonts.MEDIUM.getFont(6f);
            ColorRGBA checkColor = theme.getWhite().mulAlpha(alpha * toggleValue);
            
            // Рисуем галочку (можно заменить на иконку)
            ctx.drawText(checkFont, "✓", boxX + 2f, boxY + 1.5f, checkColor);
        } else {
            // Крестик когда выключено
            Font xFont = Fonts.MEDIUM.getFont(5f);
            ctx.drawText(xFont, "×", boxX + 2.5f, boxY + 1.5f, theme.getGrayLight().mulAlpha(alpha * 0.5f));
        }

        // Название настройки
        ColorRGBA textColor = theme.getWhite().mix(theme.getGrayLight(), 1f - toggleValue * 0.3f).mulAlpha(alpha);
        ctx.drawText(font, setting.getName(), x + 22, y - 0.5f, textColor);
        
        height = 14f;
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, MouseButton button) {
        if (button == MouseButton.LEFT && MathUtil.isHovered(mouseX, mouseY, x + 6, y - 6, width - 12, 14)) {
            setting.toggle();
        }
    }

    @Override
    public boolean isVisible() {
        return setting.isVisible();
    }
}
