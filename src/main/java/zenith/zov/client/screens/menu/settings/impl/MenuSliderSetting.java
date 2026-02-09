package zenith.zov.client.screens.menu.settings.impl;

import lombok.Getter;
import zenith.zov.base.animations.base.Animation;
import zenith.zov.base.animations.base.Easing;
import zenith.zov.base.font.Font;
import zenith.zov.base.font.Fonts;
import zenith.zov.base.theme.Theme;
import zenith.zov.client.screens.menu.settings.api.MenuSetting;

import zenith.zov.client.modules.api.setting.impl.NumberSetting;
import zenith.zov.utility.game.other.MouseButton;
import zenith.zov.utility.render.display.base.BorderRadius;
import zenith.zov.utility.render.display.base.Rect;
import zenith.zov.utility.render.display.base.UIContext;
import zenith.zov.utility.render.display.base.color.ColorRGBA;

import java.util.Locale;

public class MenuSliderSetting extends MenuSetting {
    @Getter
    private final NumberSetting setting;
    private boolean dragging = false;
    private Rect rect;

    public MenuSliderSetting(NumberSetting setting) {
        this.setting = setting;
    }

    @Override
    public void render(UIContext ctx,float mouseX,float mouseY, float x, float settingY, float moduleWidth, float alpha, float animEnable, ColorRGBA themeColor,ColorRGBA textColor,ColorRGBA descriptionColor, Theme theme) {
        float padding = 8f;
        Font settingFont = Fonts.MEDIUM.getFont(7);
        Font descFont = Fonts.REGULAR.getFont(7);
        Font iconFont = Fonts.ICONS.getFont(7);
        ctx.drawText(iconFont,"D",x+padding,settingY,themeColor);
        float nameX = x + padding + 10;
        ctx.drawText(settingFont, setting.getName(), nameX, settingY, textColor);

        float animatedValue = (setting.getCurrent());

        float sliderWidth = setting.getDescription().isEmpty()?moduleWidth-20:moduleWidth/2.8f;
        float valueTextAreaWidth = 35f;

        float sliderX = x + moduleWidth - padding-4 - sliderWidth;
        float sliderY = settingY + 12;

        String valueText = String.format(Locale.US,"%.1f", animatedValue);
        float valueTextWidth = settingFont.width(valueText);
        float valueTextX = x + moduleWidth - padding - valueTextWidth;

        ctx.drawText(settingFont, valueText, valueTextX, settingY , themeColor);
        ctx.drawRoundedRect(sliderX, sliderY, sliderWidth, 2, BorderRadius.all(0.2f), theme.getForegroundLight().mulAlpha(alpha));

        float percent = (animatedValue - setting.getMin()) / (setting.getMax() - setting.getMin());
        float filledWidth = sliderWidth * percent;
        ctx.drawRoundedRect(sliderX, sliderY, filledWidth-2, 2, BorderRadius.ZERO, theme.getGray().mix(theme.getColor(),animEnable).mulAlpha(alpha));

        float handleX = sliderX + filledWidth ;
        float handleY = sliderY - 1;
        ColorRGBA circleColor = theme.getGrayLight().mix(theme.getWhite(),animEnable).mulAlpha(alpha);
        ctx.drawRoundedRect(handleX, handleY, 4, 4, BorderRadius.all(2), circleColor);

        rect =new Rect(sliderX, sliderY - 2, sliderWidth, 6);

        if(!setting.getDescription().isEmpty()){
            float descY = settingY + 10;

            ctx.drawText(descFont, setting.getDescription(), x + padding, descY, descriptionColor);
        }

        updateSlider(mouseX);
    }
    public void updateSlider(double mouseX) {
        if (!dragging) return;
        Rect sliderRect = rect;
        if (sliderRect == null) return;

        double relativeX = mouseX - sliderRect.x();
        double percent = Math.max(0, Math.min(1, relativeX / sliderRect.width()));
        double min = setting.getMin();
        double max = setting.getMax();
        float increment = setting.getIncrement();

        double newValue = min + (max - min) * percent;

        newValue = Math.round((newValue - min) / increment) * increment + min;


        newValue = Math.max(min, Math.min(max, newValue));


        this.setting.setCurrent((float) newValue);
    }

    @Override
    public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
        if (button==MouseButton.LEFT&&rect!=null&&rect.contains(mouseX, mouseY)) {
            dragging =true;
        }
    }
    @Override
    public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
        if(button == MouseButton.LEFT) {
            dragging = false;
        }

    }
    @Override
    public float getWidth() {
        return 0;
    }

    @Override
    public float getHeight() {
        return 14;
    }

    @Override
    public boolean isVisible() {
        return setting.getVisible().get();
    }
}
