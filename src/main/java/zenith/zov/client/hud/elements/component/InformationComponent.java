package zenith.zov.client.hud.elements.component;

import zenith.zov.Zenith;
import zenith.zov.base.animations.base.Animation;
import zenith.zov.base.animations.base.Easing;
import zenith.zov.base.font.Font;
import zenith.zov.base.font.Fonts;
import zenith.zov.base.theme.Theme;
import zenith.zov.client.hud.elements.draggable.DraggableHudElement;
import zenith.zov.utility.render.display.base.BorderRadius;
import zenith.zov.utility.render.display.base.CustomDrawContext;
import zenith.zov.utility.render.display.base.color.ColorRGBA;
import zenith.zov.utility.render.display.shader.DrawUtil;

import java.util.Locale;

public class InformationComponent extends DraggableHudElement {

    public InformationComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, Align align) {
        super(name,initialX, initialY,windowWidth,windowHeight,offsetX,offsetY,align);

    }

    Animation cordsWidthAnimation = new Animation(200, Easing.QUAD_IN_OUT);
    Animation speedWidthAnimation = new Animation(200, Easing.QUAD_IN_OUT);
    @Override
    public void render(CustomDrawContext ctx) {
        float iconSize = 6f;
        float verticalPadding = 6f;
        float iconTextSpacing = 4f;
        float cellPadding = 5f;
        float borderRadius = 4f;
        Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();

        ColorRGBA mainBgColor =theme.getForegroundColor();
        ColorRGBA highlightBgColor = theme.getForegroundLight();
        ColorRGBA iconColor =theme.getColor();
        ColorRGBA textColor = theme.getWhite();
        ColorRGBA grayTextColor = theme.getGrayLight();
        Font font = Fonts.MEDIUM.getFont(6);

        double speed = Math.hypot(mc.player.getX() - mc.player.lastX, mc.player.getZ() - mc.player.lastZ);
        String coordsText = String.format(Locale.US, "%d y%d z%d", (int)mc.player.getX(), (int)mc.player.getY(), (int)mc.player.getZ());
        String speedText = String.format("%.2f", speed * 20F).replace(",", ".");

        float coordsWidth = cellPadding * 2 + iconSize + iconTextSpacing  + coordsText.length()*3.8f;
        float speedWidth = cellPadding * 2 + iconSize + iconTextSpacing + font.width(speedText) + font.width("bps");
        coordsWidth = cordsWidthAnimation.update(coordsWidth);
        speedWidth=speedWidthAnimation.update(speedWidth);
        float totalWidth = coordsWidth + speedWidth;
        float totalHeight = iconSize + (verticalPadding * 2);

        this.width = totalWidth;
        this.height = totalHeight;
        DrawUtil.drawBlurHud(ctx.getMatrices(),x, y, coordsWidth+speedWidth, totalHeight,21,BorderRadius.all(4),ColorRGBA.WHITE);

        ctx.drawRoundedRect(x, y, coordsWidth, totalHeight, BorderRadius.left(borderRadius, borderRadius), highlightBgColor);
        ctx.drawRoundedRect(x + coordsWidth, y, speedWidth, totalHeight, BorderRadius.right(borderRadius, borderRadius), mainBgColor);

        ctx.enableScissor((int) x, (int) y, (int) (x +coordsWidth), (int) (y +height));
        float currentX = x + cellPadding;
        float iconY = y + (totalHeight - iconSize) / 2f;
        float textY = y + (totalHeight - font.height()) / 2f;
       // ctx.drawTexture(Zenith.id("icons/cord.png"), currentX, iconY, iconSize, iconSize, iconColor);
        Font iconFont =Fonts.ICONS.getFont(6);
        ctx.drawText(iconFont,"J",currentX,iconY,iconColor);
        currentX += iconSize + iconTextSpacing;
        currentX = drawPrefixedText(ctx, font, "x", String.valueOf((int)mc.player.getX()), currentX, textY, grayTextColor, textColor);
        currentX = drawPrefixedText(ctx, font, " y", String.valueOf((int)mc.player.getY()), currentX, textY, grayTextColor, textColor);
        currentX = drawPrefixedText(ctx, font, " z", String.valueOf((int)mc.player.getZ()), currentX, textY, grayTextColor, textColor);
        ctx.disableScissor();
        currentX = x + coordsWidth + cellPadding;
        ctx.enableScissor((int) currentX, (int) y, (int) (currentX+speedWidth), (int) (y +height));

       // ctx.drawTexture(Zenith.id("icons/bps.png"), currentX, iconY, iconSize, iconSize, iconColor);

        ctx.drawText(iconFont,"K", currentX, iconY, iconColor);
        currentX += iconSize + iconTextSpacing;
        currentX = drawPrefixedText(ctx, font, speedText, "bps", currentX, textY, textColor, grayTextColor);
        ctx.disableScissor();

        ctx.drawRoundedBorder(x, y, coordsWidth+speedWidth, totalHeight,0.1f,BorderRadius.all(4),theme.getForegroundStroke());

        DrawUtil.drawRoundedCorner(ctx.getMatrices(), x, y, coordsWidth+speedWidth, totalHeight,0.1f,12,theme.getColor(),BorderRadius.all(4));

    }

    private float drawPrefixedText(CustomDrawContext ctx, Font font, String prefix, String value, float x, float y, ColorRGBA prefixColor, ColorRGBA valueColor) {
        ctx.drawText(font, prefix, x, y, prefixColor);
        float prefixWidth = font.width(prefix);
        ctx.drawText(font, value, x + prefixWidth, y, valueColor);

        return x + prefix.length()*3.8f +value.length()*3.8f;
    }


}
