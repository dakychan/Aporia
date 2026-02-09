package aporia.cc.client.screens.panels.components.settings;

import aporia.cc.Aporia;
import aporia.cc.base.font.Font;
import aporia.cc.base.font.Fonts;
import aporia.cc.base.theme.Theme;
import aporia.cc.client.modules.api.setting.impl.ButtonSetting;
import aporia.cc.client.screens.panels.components.PanelComponent;
import aporia.cc.utility.game.other.MouseButton;
import aporia.cc.utility.math.MathUtil;
import aporia.cc.utility.render.display.base.BorderRadius;
import aporia.cc.utility.render.display.base.UIContext;

public class ButtonComponent extends PanelComponent {
    private final ButtonSetting setting;

    public ButtonComponent(ButtonSetting setting) {
        this.setting = setting;
        this.height = 12f;
    }

    @Override
    public void render(UIContext ctx, float mouseX, float mouseY, float alpha) {
        Theme theme = Aporia.getInstance().getThemeManager().getCurrentTheme();
        Font font = Fonts.MEDIUM.getFont(6.5f);

        float buttonX = x + 8;
        float buttonY = y - 7;
        float buttonWidth = width - 16;
        float buttonHeight = 10f;

        ctx.drawRoundedRect(buttonX, buttonY, buttonWidth, buttonHeight, BorderRadius.all(2), theme.getForegroundGray().mulAlpha(alpha));
        ctx.drawText(font, setting.getName(), buttonX + 4, buttonY + 1.5f, theme.getWhite().mulAlpha(alpha));
        height = 12f;
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, MouseButton button) {
        if (button == MouseButton.LEFT && MathUtil.isHovered(mouseX, mouseY, x + 8, y - 7, width - 16, 10f)) {
            setting.toggle();
        }
    }

    @Override
    public boolean isVisible() {
        return setting.isVisible();
    }
}

