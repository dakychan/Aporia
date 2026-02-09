package zenith.zov.client.hud.elements;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import zenith.zov.Zenith;
import zenith.zov.base.font.Font;
import zenith.zov.base.font.Fonts;
import zenith.zov.utility.render.display.base.CustomDrawContext;
import zenith.zov.utility.render.display.base.color.ColorRGBA;

import java.util.function.Supplier;

@RequiredArgsConstructor
public class HudElement {
    private final String icon;
    private final Supplier<String> text;
    private final String prefix;
    @Getter
    private float width;

    public HudElement(String icon, Supplier<String> text) {
        this(icon, text, "");
    }

    public void calculateWidth(Font font, float iconSize, float cellPadding, float iconTextSpacing) {
        float textWidth = (text != null) ? font.width(text.get()) + font.width(prefix) + (prefix.isEmpty() ? 0 : 1) : 0;
        this.width = cellPadding * 2;

        this.width += font.width(icon) + iconTextSpacing + textWidth;

    }

    public void drawContent(CustomDrawContext ctx, float blockX, float blockY, float blockHeight, float iconSize, float iconTextSpacing, ColorRGBA iconColor, ColorRGBA textColor, Font font) {
        Font iconFont = Fonts.ICONS.getFont(6);

        float iconY = blockY + (blockHeight - iconFont.height()) / 2f;
        float textY = blockY + (blockHeight - font.height()) / 2f;

        if (icon != null && text == null) {
            float centeredIconX = blockX + (this.width - iconSize) / 2f;
            // ctx.drawTexture(icon., centeredIconX, iconY, iconSize, iconSize, iconColor);
        } else if (text != null) {
            float contentBlockWidth = (text != null) ? font.width(text.get()) + font.width(prefix) + (prefix.isEmpty() ? 0 : 1) : 0;
            contentBlockWidth += font.width(icon) + iconTextSpacing+iconTextSpacing/2;
            float contentX = blockX + (this.width - contentBlockWidth) / 2f;

            if (icon != null) {
                //  ctx.drawTexture(icon, contentX, iconY, iconSize, iconSize, iconColor);
                ctx.drawText(iconFont, icon, contentX, textY, iconColor);
                contentX += iconSize + iconTextSpacing;
            }
            ctx.drawText(font, text.get(), contentX, textY, textColor);
            ctx.drawText(font, prefix, contentX + font.width(text.get()) + 1, textY, Zenith.getInstance().getThemeManager().getCurrentTheme().getGrayLight());
        }
    }


}
