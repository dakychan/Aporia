package aporia.cc.client.screens.panels.components.settings;

import aporia.cc.Aporia;
import aporia.cc.base.animations.base.Animation;
import aporia.cc.base.animations.base.Easing;
import aporia.cc.base.font.Font;
import aporia.cc.base.font.Fonts;
import aporia.cc.base.theme.Theme;
import aporia.cc.client.modules.api.setting.impl.BooleanSetting;
import aporia.cc.client.screens.panels.components.PanelComponent;
import aporia.cc.utility.game.other.MouseButton;
import aporia.cc.utility.math.MathUtil;
import aporia.cc.utility.render.display.base.BorderRadius;
import aporia.cc.utility.render.display.base.Gradient;
import aporia.cc.utility.render.display.base.UIContext;
import aporia.cc.utility.render.display.base.color.ColorRGBA;

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
        Theme theme = Aporia.getInstance().getThemeManager().getCurrentTheme();
        Font font = Fonts.MEDIUM.getFont(6.5f);

        hovered = MathUtil.isHovered(mouseX, mouseY, x + 6, y - 4, width - 12, 14);
        float toggleValue = toggleAnimation.update(setting.isEnabled() ? 1f : 0f);
        float hoverValue = hoverAnimation.update(hovered ? 1f : 0f);

        float boxSize = 10f;
        float boxX = x + 8;
        float boxY = y - 2;

        ColorRGBA boxInactive = theme.getForegroundDark();
        ColorRGBA boxActive = theme.getColor();
        ColorRGBA boxHover = theme.getWhite().withAlpha(20);
        
        ColorRGBA boxColor = boxInactive.mix(boxActive, toggleValue);
        if (hoverValue > 0.01f) {
            boxColor = boxColor.mix(boxHover, hoverValue * 0.5f);
        }
        
        Gradient boxGradient = Gradient.of(
                boxColor.brighter(0.1f).mulAlpha(alpha),
                boxColor.brighter(0.1f).mulAlpha(alpha),
                boxColor.darker(0.1f).mulAlpha(alpha),
                boxColor.darker(0.1f).mulAlpha(alpha)
        );
        ctx.drawRoundedRect(boxX, boxY, boxSize, boxSize, BorderRadius.all(3), boxGradient);

        if (toggleValue > 0.01f) {
            Font checkFont = Fonts.MEDIUM.getFont(6f);
            ColorRGBA checkColor = theme.getWhite().mulAlpha(alpha * toggleValue);
            
            ctx.drawText(checkFont, "✓", boxX + 2f, boxY + 1.5f, checkColor);
        } else {
            Font xFont = Fonts.MEDIUM.getFont(5f);
            ctx.drawText(xFont, "×", boxX + 2.5f, boxY + 1.5f, theme.getGrayLight().mulAlpha(alpha * 0.5f));
        }

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

