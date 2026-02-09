package zenith.zov.client.screens.panels.components.settings;

import zenith.zov.Zenith;
import zenith.zov.base.font.Font;
import zenith.zov.base.font.Fonts;
import zenith.zov.base.theme.Theme;
import zenith.zov.client.modules.api.setting.impl.ItemSelectSetting;
import zenith.zov.client.screens.menu.settings.impl.popup.MenuItemPopupSetting;
import zenith.zov.client.screens.panels.components.PanelComponent;
import zenith.zov.utility.game.other.MouseButton;
import zenith.zov.utility.math.MathUtil;
import zenith.zov.utility.render.display.base.BorderRadius;
import zenith.zov.utility.render.display.base.ChangeRect;
import zenith.zov.utility.render.display.base.UIContext;

public class ItemSelectComponent extends PanelComponent {
    private final ItemSelectSetting setting;
    private final ChangeRect popupBounds = new ChangeRect(0, 0, 180, 220);

    public ItemSelectComponent(ItemSelectSetting setting) {
        this.setting = setting;
        this.height = 12f;
    }

    @Override
    public void render(UIContext ctx, float mouseX, float mouseY, float alpha) {
        Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
        Font font = Fonts.MEDIUM.getFont(6.5f);

        float buttonWidth = 42f;
        float buttonHeight = 10f;
        float buttonX = x + width - buttonWidth - 8;
        float buttonY = y - 7;

        popupBounds.setX(buttonX - 120);
        popupBounds.setY(buttonY - 8);

        ctx.drawText(font, setting.getName(), x + 8, y - 6, theme.getWhite().mulAlpha(alpha));
        ctx.drawRoundedRect(buttonX, buttonY, buttonWidth, buttonHeight, BorderRadius.all(2), theme.getForegroundGray().mulAlpha(alpha));
        String label = "Select (" + setting.getItemsById().size() + ")";
        ctx.drawText(font, label, buttonX + 3, buttonY + 1.5f, theme.getWhite().mulAlpha(alpha));
        height = 12f;
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, MouseButton button) {
        float buttonWidth = 42f;
        float buttonHeight = 10f;
        float buttonX = x + width - buttonWidth - 8;
        float buttonY = y - 7;
        if (button == MouseButton.LEFT && MathUtil.isHovered(mouseX, mouseY, buttonX, buttonY, buttonWidth, buttonHeight)) {
            Zenith.getInstance().getPanelsScreen().addPopupSetting(new MenuItemPopupSetting(setting, popupBounds));
        }
    }

    @Override
    public boolean isVisible() {
        return setting.isVisible();
    }
}
