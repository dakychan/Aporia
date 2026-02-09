package zenith.zov.client.screens.menu.panels;

import lombok.Getter;
import net.minecraft.util.math.MathHelper;
import zenith.zov.base.animations.base.Animation;
import zenith.zov.base.animations.base.Easing;
import zenith.zov.base.font.Font;
import zenith.zov.base.font.Fonts;
import zenith.zov.client.modules.api.Category;
import zenith.zov.utility.render.display.base.UIContext;
import zenith.zov.utility.render.display.base.color.ColorRGBA;

public class SideBarCategory {
    @Getter
    private final Category category;
    private final Animation animationSwitch;

    public SideBarCategory(Category category) {
        this.category = category;
        animationSwitch = new Animation(200, category == Category.COMBAT ? 1 : 0, Easing.LINEAR);
    }

    public void render(UIContext ctx, float x, float y, float width, float height, float sidebarProgress, boolean selected, ColorRGBA textColor,ColorRGBA textColorDisable, ColorRGBA iconColorDisable, ColorRGBA primary) {
        animationSwitch.animateTo(selected ? 1 : 0);
        animationSwitch.update();
        ColorRGBA mixColor = iconColorDisable.mix(primary, animationSwitch.getValue());
        ColorRGBA mixColorText = textColorDisable.mix(textColor, animationSwitch.getValue());
        Font font = Fonts.ICONS.getFont(7);

        float offestY = (height - font.height()) / 2;
        float scale = MathHelper.lerp(sidebarProgress,1f,0.8f);
        float iconWidth = font.width(category.getIcon());
        ctx.pushMatrix();
        ctx.getMatrices().translate(x + 8 + iconWidth/2 +(category==Category.PLAYER?1:0), y + offestY+font.height()/2);
        ctx.getMatrices().scale(scale,scale);
        ctx.getMatrices().translate(-(x + 8 + iconWidth/2), -(y + offestY+font.height()/2));
        ctx.drawText(Fonts.ICONS.getFont(7), category.getIcon(), x + 8, y + offestY, mixColor);
        ctx.popMatrix();
        Font categoryFont = Fonts.MEDIUM.getFont(7);
        ctx.drawText(categoryFont,category.getName(),x + 8+iconWidth*scale+6,y+(height-font.height())/2,mixColorText);
    }

}

