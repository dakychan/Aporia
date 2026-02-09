package zenith.zov.client.screens.panels.components.settings;

import org.lwjgl.glfw.GLFW;
import zenith.zov.Zenith;
import zenith.zov.base.font.Font;
import zenith.zov.base.font.Fonts;
import zenith.zov.base.theme.Theme;
import zenith.zov.client.modules.api.setting.impl.KeySetting;
import zenith.zov.client.screens.panels.components.PanelComponent;
import zenith.zov.utility.game.other.MouseButton;
import zenith.zov.utility.math.MathUtil;
import zenith.zov.utility.render.display.base.BorderRadius;
import zenith.zov.utility.render.display.base.UIContext;

public class KeyComponent extends PanelComponent {
    private final KeySetting setting;
    private boolean binding;

    public KeyComponent(KeySetting setting) {
        this.setting = setting;
        this.height = 12f;
    }

    @Override
    public void render(UIContext ctx, float mouseX, float mouseY, float alpha) {
        Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
        Font font = Fonts.MEDIUM.getFont(6.5f);

        String keyName = binding ? "..." : setting.getNameKey();
        if (keyName == null || keyName.isBlank()) {
            keyName = "None";
        }

        ctx.drawText(font, setting.getName(), x + 8, y - 6, theme.getWhite().mulAlpha(alpha));

        float boxWidth = font.width(keyName) + 6;
        float boxX = x + width - boxWidth - 8;
        ctx.drawRoundedRect(boxX, y - 7, boxWidth, 10, BorderRadius.all(2), theme.getForegroundGray().mulAlpha(alpha));
        ctx.drawText(font, keyName, boxX + 3, y - 5, theme.getWhite().mulAlpha(alpha));
        height = 12f;
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, MouseButton button) {
        Font font = Fonts.MEDIUM.getFont(6.5f);
        String keyName = setting.getNameKey();
        if (keyName == null || keyName.isBlank()) {
            keyName = "None";
        }
        float boxWidth = font.width(binding ? "..." : keyName) + 6;
        float boxX = x + width - boxWidth - 8;
        if (button == MouseButton.LEFT && MathUtil.isHovered(mouseX, mouseY, boxX, y - 7, boxWidth, 10)) {
            binding = !binding;
        } else if (button == MouseButton.LEFT) {
            binding = false;
        }
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!binding) {
            return;
        }
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            setting.setKeyCode(-1);
        } else if (keyCode != GLFW.GLFW_KEY_ESCAPE) {
            setting.setKeyCode(keyCode);
        }
        binding = false;
    }

    @Override
    public boolean isVisible() {
        return setting.isVisible();
    }
}
