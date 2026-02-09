package aporia.cc.client.screens.panels.components.settings;

import aporia.cc.Aporia;
import aporia.cc.base.animations.base.Animation;
import aporia.cc.base.animations.base.Easing;
import aporia.cc.base.font.Font;
import aporia.cc.base.font.Fonts;
import aporia.cc.base.theme.Theme;
import aporia.cc.client.modules.api.setting.impl.ModeSetting;
import aporia.cc.client.screens.panels.components.PanelComponent;
import aporia.cc.utility.game.other.MouseButton;
import aporia.cc.utility.math.MathUtil;
import aporia.cc.utility.render.display.base.BorderRadius;
import aporia.cc.utility.render.display.base.Gradient;
import aporia.cc.utility.render.display.base.UIContext;
import aporia.cc.utility.render.display.base.color.ColorRGBA;

import java.util.HashMap;
import java.util.Map;

public class ModeComponent extends PanelComponent {
    private final ModeSetting setting;
    private final Map<ModeSetting.Value, Animation> selectAnimations = new HashMap<>();
    private final Map<ModeSetting.Value, Animation> hoverAnimations = new HashMap<>();

    public ModeComponent(ModeSetting setting) {
        this.setting = setting;
        this.height = 18f;
        for (ModeSetting.Value value : setting.getValues()) {
            selectAnimations.put(value, new Animation(200, 0f, Easing.EXPO_OUT));
            hoverAnimations.put(value, new Animation(150, 0f, Easing.EXPO_OUT));
        }
    }

    @Override
    public void render(UIContext ctx, float mouseX, float mouseY, float alpha) {
        Theme theme = Aporia.getInstance().getThemeManager().getCurrentTheme();
        Font labelFont = Fonts.MEDIUM.getFont(6.5f);
        Font valueFont = Fonts.MEDIUM.getFont(6f);

        ctx.drawText(labelFont, setting.getName(), x + 8, y - 6, theme.getWhite().mulAlpha(alpha));

        float offset = 0f;
        float heightOffset = 0f;
        float spacing = 5f;
        float maxWidth = width - 16;
        float chipHeight = valueFont.height() + 5;

        for (ModeSetting.Value value : setting.getValues()) {
            String text = value.getName();
            float textWidth = valueFont.width(text) + 8;

            if (offset + textWidth > maxWidth) {
                offset = 0f;
                heightOffset += chipHeight + spacing;
            }

            boolean selected = value.isSelected();
            boolean hovered = MathUtil.isHovered(mouseX, mouseY, x + 8 + offset, y + 2 + heightOffset, textWidth, chipHeight);
            
            float selectValue = selectAnimations.get(value).update(selected ? 1f : 0f);
            float hoverValue = hoverAnimations.get(value).update(hovered ? 1f : 0f);

            ColorRGBA chipInactive = theme.getForegroundDark();
            ColorRGBA chipActive = theme.getColor();
            ColorRGBA chipHover = theme.getWhite().withAlpha(15);
            
            ColorRGBA chipColor = chipInactive.mix(chipActive, selectValue);
            if (hoverValue > 0.01f && selectValue < 0.5f) {
                chipColor = chipColor.mix(chipHover, hoverValue);
            }

            Gradient chipGradient = Gradient.of(
                    chipColor.brighter(0.1f).mulAlpha(alpha),
                    chipColor.brighter(0.1f).mulAlpha(alpha),
                    chipColor.darker(0.1f).mulAlpha(alpha),
                    chipColor.darker(0.1f).mulAlpha(alpha)
            );

            float chipX = x + 8 + offset;
            float chipY = y + 2 + heightOffset;
            ctx.drawRoundedRect(chipX, chipY, textWidth, chipHeight, BorderRadius.all(3), chipGradient);

            ColorRGBA textColor = theme.getGrayLight().mix(theme.getWhite(), selectValue).mulAlpha(alpha);
            ctx.drawText(valueFont, text, chipX + 4, chipY + 2f, textColor);

            offset += textWidth + spacing;
        }

        height = 18 + heightOffset;
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, MouseButton button) {
        if (button != MouseButton.LEFT) {
            return;
        }

        Font valueFont = Fonts.MEDIUM.getFont(6f);
        float offset = 0f;
        float heightOffset = 0f;
        float spacing = 5f;
        float maxWidth = width - 16;
        float chipHeight = valueFont.height() + 5;

        for (ModeSetting.Value value : setting.getValues()) {
            String text = value.getName();
            float textWidth = valueFont.width(text) + 8;

            if (offset + textWidth > maxWidth) {
                offset = 0f;
                heightOffset += chipHeight + spacing;
            }

            float chipX = x + 8 + offset;
            float chipY = y + 2 + heightOffset;

            if (MathUtil.isHovered(mouseX, mouseY, chipX, chipY, textWidth, chipHeight)) {
                value.select();
                return;
            }

            offset += textWidth + spacing;
        }
    }

    @Override
    public boolean isVisible() {
        return setting.isVisible();
    }
}

