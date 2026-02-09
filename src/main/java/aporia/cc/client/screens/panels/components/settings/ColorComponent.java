package aporia.cc.client.screens.panels.components.settings;

import aporia.cc.Aporia;
import aporia.cc.base.font.Font;
import aporia.cc.base.font.Fonts;
import aporia.cc.base.theme.Theme;
import aporia.cc.client.modules.api.setting.impl.ColorSetting;
import aporia.cc.client.screens.menu.settings.impl.popup.MenuColorPopupSetting;
import aporia.cc.client.screens.panels.components.PanelComponent;
import aporia.cc.utility.game.other.MouseButton;
import aporia.cc.utility.math.MathUtil;
import aporia.cc.utility.render.display.base.BorderRadius;
import aporia.cc.utility.render.display.base.ChangeRect;
import aporia.cc.utility.render.display.base.UIContext;

public class ColorComponent extends PanelComponent {
    private final ColorSetting setting;
    private final ChangeRect popupBounds = new ChangeRect(0, 0, 120, 160);

    public ColorComponent(ColorSetting setting) {
        this.setting = setting;
        this.height = 12f;
    }

    @Override
    public void render(UIContext ctx, float mouseX, float mouseY, float alpha) {
        Theme theme = Aporia.getInstance().getThemeManager().getCurrentTheme();
        Font font = Fonts.MEDIUM.getFont(6.5f);

        float boxSize = 10f;
        float boxX = x + width - boxSize - 8;
        float boxY = y - 7;

        popupBounds.setX(boxX - 80);
        popupBounds.setY(boxY - 8);

        ctx.drawText(font, setting.getName(), x + 8, y - 6, theme.getWhite().mulAlpha(alpha));
        ctx.drawRoundedRect(boxX, boxY, boxSize, boxSize, BorderRadius.all(2), setting.getColor().mulAlpha(alpha));
        ctx.drawRoundedBorder(boxX, boxY, boxSize, boxSize, 0.2f, BorderRadius.all(2), theme.getForegroundStroke().mulAlpha(alpha));
        height = 12f;
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, MouseButton button) {
        float boxSize = 10f;
        float boxX = x + width - boxSize - 8;
        float boxY = y - 7;
        if (button == MouseButton.LEFT && MathUtil.isHovered(mouseX, mouseY, boxX, boxY, boxSize, boxSize)) {
            Aporia.getInstance().getPanelsScreen().addPopupSetting(new MenuColorPopupSetting(popupBounds, setting));
        }
    }

    @Override
    public boolean isVisible() {
        return setting.isVisible();
    }
}

