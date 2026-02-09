package zenith.zov.client.screens.panels.components.settings;

import zenith.zov.Zenith;
import zenith.zov.base.font.Font;
import zenith.zov.base.font.Fonts;
import zenith.zov.base.theme.Theme;
import zenith.zov.client.modules.api.setting.impl.MultiBooleanSetting;
import zenith.zov.client.screens.panels.components.PanelComponent;
import zenith.zov.utility.game.other.MouseButton;
import zenith.zov.utility.math.MathUtil;
import zenith.zov.utility.render.display.base.BorderRadius;
import zenith.zov.utility.render.display.base.UIContext;
import zenith.zov.utility.render.display.base.color.ColorRGBA;

public class MultiBooleanComponent extends PanelComponent {
    private final MultiBooleanSetting setting;

    public MultiBooleanComponent(MultiBooleanSetting setting) {
        this.setting = setting;
        this.height = 18f;
    }

    @Override
    public void render(UIContext ctx, float mouseX, float mouseY, float alpha) {
        Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
        Font labelFont = Fonts.MEDIUM.getFont(6.5f);
        Font valueFont = Fonts.MEDIUM.getFont(6f);

        ctx.drawText(labelFont, setting.getName(), x + 8, y - 6, theme.getWhite().mulAlpha(alpha));

        float offset = 0f;
        float heightOffset = 0f;
        float spacing = 4f;
        float maxWidth = width - 16;
        float chipHeight = valueFont.height() + 3;

        for (MultiBooleanSetting.Value value : setting.getBooleanSettings()) {
            String text = value.getName();
            float textWidth = valueFont.width(text) + 6;

            if (offset + textWidth > maxWidth) {
                offset = 0f;
                heightOffset += chipHeight + spacing;
            }

            boolean selected = value.isEnabled();
            ColorRGBA chip = theme.getForegroundGray().mix(theme.getColor(), selected ? 1f : 0f).mulAlpha(alpha);
            ColorRGBA textColor = theme.getWhite().mix(theme.getGrayLight(), selected ? 0f : 1f).mulAlpha(alpha);

            float chipX = x + 8 + offset;
            float chipY = y + 2 + heightOffset;
            ctx.drawRoundedRect(chipX, chipY, textWidth, chipHeight, BorderRadius.all(2), chip);
            ctx.drawText(valueFont, text, chipX + 3, chipY + 1.5f, textColor);

            offset += textWidth + spacing;
        }

        height = 16 + heightOffset;
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, MouseButton button) {
        if (button != MouseButton.LEFT) {
            return;
        }

        Font valueFont = Fonts.MEDIUM.getFont(6f);
        float offset = 0f;
        float heightOffset = 0f;
        float spacing = 4f;
        float maxWidth = width - 16;
        float chipHeight = valueFont.height() + 3;

        for (MultiBooleanSetting.Value value : setting.getBooleanSettings()) {
            String text = value.getName();
            float textWidth = valueFont.width(text) + 6;

            if (offset + textWidth > maxWidth) {
                offset = 0f;
                heightOffset += chipHeight + spacing;
            }

            float chipX = x + 8 + offset;
            float chipY = y + 2 + heightOffset;

            if (MathUtil.isHovered(mouseX, mouseY, chipX, chipY, textWidth, chipHeight)) {
                value.toggle();
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
