package aporia.cc.client.screens.panels;

import aporia.cc.Aporia;
import aporia.cc.base.font.Font;
import aporia.cc.base.font.Fonts;
import aporia.cc.base.theme.Theme;
import aporia.cc.utility.game.other.MouseButton;
import aporia.cc.utility.math.MathUtil;
import aporia.cc.utility.render.display.base.BorderRadius;
import aporia.cc.utility.render.display.base.Gradient;
import aporia.cc.utility.render.display.base.Rect;
import aporia.cc.utility.render.display.base.UIContext;
import aporia.cc.utility.render.display.base.color.ColorRGBA;

import java.util.ArrayList;
import java.util.List;

public class PanelsThemePanel {
    private static final float PANEL_WIDTH = 110f;
    private static final float ITEM_HEIGHT = 18f;
    private static final float PADDING = 6f;
    private final List<Theme> themes = List.of(Theme.DARK, Theme.LIGHT, Theme.CUSTOM_THEME);
    private final List<Rect> bounds = new ArrayList<>();

    public void render(UIContext ctx, float mouseX, float mouseY, float alpha) {
        Theme theme = Aporia.getInstance().getThemeManager().getCurrentTheme();
        float x = 12f;
        float y = 12f;
        float panelHeight = PADDING * 2 + themes.size() * ITEM_HEIGHT + (themes.size() - 1) * 4f;

        ctx.drawRoundedRect(x, y, PANEL_WIDTH, panelHeight, BorderRadius.all(6), theme.getForegroundColor().mulAlpha(alpha));

        Font font = Fonts.MEDIUM.getFont(6.5f);
        bounds.clear();
        float itemY = y + PADDING;

        for (Theme themeOption : themes) {
            float itemX = x + PADDING;
            float swatchSize = 12f;

            Gradient gradient = Gradient.of(
                    themeOption.getColor().mulAlpha(alpha),
                    themeOption.getColor().mulAlpha(alpha),
                    themeOption.getSecondColor().mulAlpha(alpha),
                    themeOption.getSecondColor().mulAlpha(alpha)
            );
            ctx.drawRoundedRect(itemX, itemY + 2, swatchSize, swatchSize, BorderRadius.all(3), gradient);

            if (themeOption == theme) {
                ctx.drawRoundedBorder(itemX - 1, itemY + 1, swatchSize + 2, swatchSize + 2, 0.4f, BorderRadius.all(4), theme.getColor().mulAlpha(alpha));
            }

            ColorRGBA textColor = theme.getWhite().mulAlpha(alpha);
            ctx.drawText(font, themeOption.getName(), itemX + swatchSize + 6, itemY + 4.5f, textColor);

            bounds.add(new Rect(itemX, itemY, PANEL_WIDTH - PADDING * 2, ITEM_HEIGHT));
            itemY += ITEM_HEIGHT + 4f;
        }
    }

    public void mouseClicked(double mouseX, double mouseY, MouseButton button) {
        if (button != MouseButton.LEFT) {
            return;
        }
        for (int i = 0; i < themes.size(); i++) {
            Rect rect = bounds.get(i);
            if (MathUtil.isHovered(mouseX, mouseY, rect.x(), rect.y(), rect.width(), rect.height())) {
                Aporia.getInstance().getThemeManager().switchTheme(themes.get(i));
                return;
            }
        }
    }
}

