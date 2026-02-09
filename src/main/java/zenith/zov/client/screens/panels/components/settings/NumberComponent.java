package zenith.zov.client.screens.panels.components.settings;

import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import zenith.zov.Zenith;
import zenith.zov.base.animations.base.Animation;
import zenith.zov.base.animations.base.Easing;
import zenith.zov.base.font.Font;
import zenith.zov.base.font.Fonts;
import zenith.zov.base.theme.Theme;
import zenith.zov.client.modules.api.setting.impl.NumberSetting;
import zenith.zov.client.screens.panels.components.PanelComponent;
import zenith.zov.utility.game.other.MouseButton;
import zenith.zov.utility.math.MathUtil;
import zenith.zov.utility.render.display.base.BorderRadius;
import zenith.zov.utility.render.display.base.Gradient;
import zenith.zov.utility.render.display.base.UIContext;
import zenith.zov.utility.render.display.base.color.ColorRGBA;

public class NumberComponent extends PanelComponent {
    private final NumberSetting setting;
    private final Animation fillAnimation = new Animation(180, 0f, Easing.EXPO_OUT);
    private final Animation thumbAnimation = new Animation(150, 0f, Easing.EXPO_OUT);
    private boolean dragging;
    private float lastValue;

    public NumberComponent(NumberSetting setting) {
        this.setting = setting;
        this.height = 18f;
        this.lastValue = setting.getCurrent();
    }

    @Override
    public void render(UIContext ctx, float mouseX, float mouseY, float alpha) {
        Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
        Font font = Fonts.MEDIUM.getFont(6.5f);

        // Название слева
        ctx.drawText(font, setting.getName(), x + 8, y - 6, theme.getWhite().mulAlpha(alpha));

        // Значение справа в красивом бейдже
        String valueText = String.valueOf(setting.getCurrent());
        float valueWidth = font.width(valueText) + 8;
        float valueX = x + width - valueWidth - 6;
        float valueY = y - 8;
        
        // Градиент для бейджа значения
        Gradient valueGradient = Gradient.of(
                theme.getColor().mulAlpha(alpha),
                theme.getColor().darker(0.2f).mulAlpha(alpha),
                theme.getColor().darker(0.2f).mulAlpha(alpha),
                theme.getColor().mulAlpha(alpha)
        );
        ctx.drawRoundedRect(valueX, valueY, valueWidth, 10, BorderRadius.all(3), valueGradient);
        ctx.drawText(font, valueText, valueX + 4, valueY + 1.5f, theme.getWhite().mulAlpha(alpha));

        // Трек слайдера
        float trackX = x + 8;
        float trackY = y + 3;
        float trackWidth = width - 16;
        float trackHeight = 4f;
        
        float ratio = (setting.getCurrent() - setting.getMin()) / (setting.getMax() - setting.getMin());
        float targetWidth = trackWidth * MathHelper.clamp(ratio, 0f, 1f);
        float fillWidth = fillAnimation.update(targetWidth);
        
        float thumbValue = thumbAnimation.update(dragging ? 1f : 0f);

        // Фон трека
        ctx.drawRoundedRect(trackX, trackY, trackWidth, trackHeight, BorderRadius.all(2), theme.getForegroundDark().mulAlpha(alpha));
        
        // Заполненная часть с градиентом
        if (fillWidth > 0.5f) {
            Gradient fillGradient = Gradient.of(
                    theme.getColor().brighter(0.2f).mulAlpha(alpha),
                    theme.getColor().mulAlpha(alpha),
                    theme.getColor().mulAlpha(alpha),
                    theme.getColor().brighter(0.2f).mulAlpha(alpha)
            );
            ctx.drawRoundedRect(trackX, trackY, fillWidth, trackHeight, BorderRadius.all(2), fillGradient);
        }

        // Ползунок (thumb)
        float thumbSize = 7f + thumbValue * 2f;
        float thumbX = trackX + fillWidth - thumbSize / 2;
        float thumbY = trackY + trackHeight / 2 - thumbSize / 2;
        
        // Тень ползунка
        if (thumbValue > 0.01f) {
            ctx.drawRoundedRect(thumbX - 1, thumbY - 1, thumbSize + 2, thumbSize + 2, 
                    BorderRadius.all(thumbSize / 2 + 1), theme.getColor().withAlpha((int)(40 * thumbValue * alpha)));
        }
        
        // Сам ползунок
        Gradient thumbGradient = Gradient.of(
                theme.getWhite().mulAlpha(alpha),
                theme.getWhite().darker(0.1f).mulAlpha(alpha),
                theme.getWhite().darker(0.1f).mulAlpha(alpha),
                theme.getWhite().mulAlpha(alpha)
        );
        ctx.drawRoundedRect(thumbX, thumbY, thumbSize, thumbSize, BorderRadius.all(thumbSize / 2), thumbGradient);

        // Обработка перетаскивания
        if (dragging) {
            float value = (float) ((mouseX - trackX) / trackWidth * (setting.getMax() - setting.getMin()) + setting.getMin());
            value = (float) MathUtil.round(value, setting.getIncrement());
            value = MathHelper.clamp(value, setting.getMin(), setting.getMax());
            if (value != lastValue) {
                setting.setCurrent(value);
                lastValue = value;
            }
        }

        height = 18f;
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, MouseButton button) {
        if (button == MouseButton.LEFT && MathUtil.isHovered(mouseX, mouseY, x + 6, y + 1, width - 12, 8)) {
            dragging = true;
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, MouseButton button) {
        dragging = false;
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            dragging = false;
        }
    }

    @Override
    public boolean isVisible() {
        return setting.isVisible();
    }
}
