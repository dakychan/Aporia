package zenith.zov.client.screens.menu.settings.impl;

import net.minecraft.util.math.RotationAxis;
import zenith.zov.base.animations.base.Animation;
import zenith.zov.base.animations.base.Easing;
import zenith.zov.base.font.Font;
import zenith.zov.base.font.Fonts;
import zenith.zov.base.theme.Theme;
import zenith.zov.client.screens.menu.settings.api.MenuSetting;

import zenith.zov.client.modules.api.setting.impl.ModeSetting;
import zenith.zov.utility.game.other.MouseButton;
import zenith.zov.utility.render.display.base.BorderRadius;
import zenith.zov.utility.render.display.base.Rect;
import zenith.zov.utility.render.display.base.UIContext;
import zenith.zov.utility.render.display.base.color.ColorRGBA;
import zenith.zov.utility.render.display.shader.DrawUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MenuModeSetting extends MenuSetting {
    private final ModeSetting setting;
    private final Map<ModeSetting.Value, Rect> modeSettingOptionBounds = new HashMap<>();
    private Rect bounds;
    private boolean expanded;
    private float maxWidthText;
    private final Animation expandedAnimation = new Animation(200, 0, Easing.QUAD_IN_OUT);

    public MenuModeSetting(ModeSetting setting) {

        this.setting = setting;
        maxWidthText = -1;
    }

    @Override
    public void render(UIContext ctx, float mouseX, float mouseY, float x, float settingY, float moduleWidth, float alpha, float animEnable, ColorRGBA themeColor, ColorRGBA textColor, ColorRGBA descriptionColor, Theme theme) {
        if (maxWidthText == -1)
            maxWidthText = (float) setting.getValues().stream().mapToDouble(value -> Fonts.MEDIUM.getFont(6).width(value.getName())).max().orElse(0);

        Font settingFont = Fonts.MEDIUM.getFont(7);
        Font optionFont = Fonts.MEDIUM.getFont(6);
        Font iconFont = Fonts.ICONS.getFont(6);
       // ctx.drawSprite(new CustomSprite("icons/dropdown.png"), settingX, settingY , 7, 7, new ColorRGBA(181, 162, 255).mulAlpha(alpha));

        float nameX = x + 18;
        ctx.drawText(settingFont, setting.getName(), nameX, settingY + (13 - settingFont.height()) / 2f-0.5f , textColor);

        ctx.drawText(iconFont,"K", x +9, settingY + (13 - iconFont.height()) / 2f-1 , themeColor);

        float dropdownWidth = moduleWidth / 2.2f;
        float dropdownHeight = 13 + expandedAnimation.update(expanded ? 1 : 0) * setting.getValues().size() * 13;
        float dropdownX = x + moduleWidth - dropdownWidth - 8;

        ctx.drawRoundedRect(dropdownX, settingY, dropdownWidth, dropdownHeight, BorderRadius.all(3), theme.getForegroundColor().mulAlpha(alpha));
        ctx.drawRoundedRect(dropdownX, settingY, dropdownWidth, 13, expanded ? BorderRadius.top(3, 3) : BorderRadius.all(3), theme.getForegroundLight().mulAlpha(alpha));


        String currentModeText = setting.getValue().getName();
        ctx.drawText(optionFont, currentModeText, dropdownX + 6, settingY + (13 - optionFont.height()) / 2f, textColor);
        // ctx.drawSprite(new CustomSprite("icons/next.png"), dropdownX + 8, dropdownY + (13 - optionFont.height()) / 2f, 4, 4, ColorRGBA.WHITE.mulAlpha(alpha));

        {
            
            float thickness = 2;
            float length1 = 4;
            float length2 = 4;
            float arrowX =dropdownX + dropdownWidth - 8-4;
            float arrowY = settingY +5.5f;
            BorderRadius radius = BorderRadius.ZERO;
            ColorRGBA color = theme.getGray().mix(theme.getGrayLight(),animEnable).mulAlpha(alpha);

            float angle1 = -45f;

            float angle2 = 45;

            ctx.pushMatrix();

            {
                //ctx.drawText(iconFont,"Q", (int) arrowX, (int) arrowY,color);

                float endX = arrowX+iconFont.width("Q")/2-1 ;
                float endY = arrowY +iconFont.height()/2-1;

                ctx.getMatrices().translate(endX, endY);
                ctx.getMatrices().rotate((float) Math.toRadians(180 * expandedAnimation.getValue()));
                ctx.getMatrices().translate(-endX, -endY);
            }

            ctx.drawText(iconFont,"Q", (int) arrowX, (int) arrowY,color);

            ctx.popMatrix();
        }


        ctx.enableScissor((int) dropdownX-1, (int) settingY, (int) (dropdownX + dropdownWidth+1), (int) (settingY + dropdownHeight + 1));

        bounds = new Rect(dropdownX, settingY, dropdownWidth, dropdownHeight);



        if (expandedAnimation.getValue() != 0) {
            List<ModeSetting.Value> modes = setting.getValues();
            ColorRGBA disableColor = theme.getGray().mix(theme.getGrayLight(), animEnable).mulAlpha(alpha);
            ColorRGBA enabledColor = theme.getForegroundGray().mix(theme.getColor(), animEnable).mulAlpha(alpha);
            float optionY = settingY + 13;
            for (ModeSetting.Value mode : modes) {
                Rect optionRect = new Rect(dropdownX, optionY, dropdownWidth, 13);
                if (optionY > settingY + dropdownHeight) break;
                if (mode == setting.getValue()) {
                    ctx.drawRoundedRect(dropdownX+1f, optionY, dropdownWidth-2f, 13, mode == modes.getLast() ? BorderRadius.bottom(3, 3) : BorderRadius.all(0), enabledColor.mulAlpha(expandedAnimation.getValue()));
                    ctx.drawText(optionFont, mode.getName(), dropdownX + 6, optionY + (13 - optionFont.height()) / 2f,
                            textColor.mulAlpha(expandedAnimation.getValue()));

                } else {
                    ctx.drawText(optionFont, mode.getName(), dropdownX + 6, optionY + (13 - optionFont.height()) / 2f, disableColor.mulAlpha(expandedAnimation.getValue()));

                }


                modeSettingOptionBounds.put(mode, optionRect);
                optionY += 13;
            }
        }
//            totalHeight += optionsHeight;



        ctx.disableScissor();
        DrawUtil.drawRoundedBorder(
                ctx.getMatrices(),
                dropdownX, settingY,
                dropdownWidth, dropdownHeight,
                0.2f,
                BorderRadius.all(3),
                theme.getForegroundLightStroke().mulAlpha(alpha)
        );
    }

    @Override
    public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
        if (bounds != null && button == MouseButton.RIGHT && bounds.contains(mouseX, mouseY)) {
            expanded = !expanded;
        }
        if (expanded && button == MouseButton.LEFT) {
            for (Map.Entry<ModeSetting.Value, Rect> entry : modeSettingOptionBounds.entrySet()) {
                if (entry.getValue().contains(mouseX, mouseY)) {
                    setting.setValue(entry.getKey());
                }
            }
        }
    }

    @Override
    public float getWidth() {
        return 0;
    }

    @Override
    public float getHeight() {
        return 13 + expandedAnimation.getValue() * setting.getValues().size() * 13;
    }

    @Override
    public boolean isVisible() {
        return setting.getVisible().get();
    }
}

