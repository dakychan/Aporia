package zenith.zov.client.screens.menu.panels;

import net.minecraft.util.math.MathHelper;
import zenith.zov.Zenith;
import zenith.zov.base.animations.base.Animation;
import zenith.zov.base.animations.base.Easing;
import zenith.zov.base.font.Font;
import zenith.zov.base.font.Fonts;
import zenith.zov.base.theme.Theme;
import zenith.zov.client.modules.api.Category;
import zenith.zov.utility.render.display.TextBox;
import zenith.zov.utility.render.display.base.BorderRadius;
import zenith.zov.utility.render.display.base.CustomSprite;
import zenith.zov.utility.render.display.base.Rect;
import zenith.zov.utility.render.display.base.UIContext;
import zenith.zov.utility.render.display.base.color.ColorRGBA;
import zenith.zov.utility.render.display.shader.DrawUtil;
import zenith.zov.client.modules.api.Module;

public class HeaderPanel {

    public Rect themeButtonBounds;
    public Rect searchBarBounds;
    public Rect layoutToggleButtonBounds;

    private final TextBox searchField;
    private final Runnable onLayoutToggle;
    private final Runnable onThemeSwitch;
    private Category lastCategory = Category.COMBAT;

    public HeaderPanel(TextBox searchField, Runnable onLayoutToggle, Runnable onThemeSwitch) {
        this.searchField = searchField;
        this.onLayoutToggle = onLayoutToggle;
        this.onThemeSwitch = onThemeSwitch;
    }

    Animation animation = new Animation(300, 1, Easing.QUAD_IN_OUT);

    public void render(UIContext ctx, float contentStartX, float sidebarY,
                       float boxX, int columns,float boxWidth, float progress,
                       Theme theme, Category selectedCategory) {


        animation.update(1);


        ColorRGBA sideBar = theme.getForegroundColor().mulAlpha(progress);
        ColorRGBA textColor = theme.getWhite().mulAlpha(progress);


        float x = contentStartX;
        x = renderBreadcrumbs(ctx, x, sidebarY, progress, theme, selectedCategory, textColor);
        x += 8f; // panelGap
        x = renderStats(ctx, x, sidebarY, progress, theme, selectedCategory, textColor);
        x += 8f;
        x = renderThemeButton(ctx, x, sidebarY, progress, theme);
        x += 8f;
        renderLayoutButton(ctx, x, sidebarY, progress, theme, columns);
        renderSearchBar(ctx, boxX, sidebarY, boxWidth, progress, theme);
    }

    private float renderBreadcrumbs(UIContext ctx, float startX, float y,
                                    float progress, Theme theme,
                                    Category selectedCategory,
                                    ColorRGBA textColor) {
        String name = selectedCategory.getName();
        Font font = Fonts.MEDIUM.getFont(7);
        Font icon7 = Fonts.ICONS.getFont(7);
        Font icon6 = Fonts.ICONS.getFont(5);

        float homeIcon = 7, arrowIcon = 6, catIcon = 7;
        float pad = 8, gap = 4, tgap = 2;
        float textW = MathHelper.lerp(animation.getValue(), font.width(lastCategory.getName()), font.width(name));
        float width = pad * 2 + homeIcon + gap + arrowIcon  + catIcon + tgap + textW;
        float h = 22;

        ColorRGBA bar = theme.getForegroundColor().mulAlpha(progress);
        ctx.drawRoundedRect(startX, y, width, h, BorderRadius.all(7), bar);
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), startX, y, width, h, -0.1f,
                BorderRadius.all(7), theme.getForegroundStroke().mulAlpha(progress));


        float cx = startX + pad;
        float vy = y + h / 2f;
        ctx.drawText(icon7, "7", cx, vy - icon7.height() / 2 - .5f, theme.getColor().mulAlpha(progress));
        cx += icon7.width("7") + gap;
        ctx.drawText(icon6, "A", cx + 1, vy - icon6.height() / 2 - .3f, theme.getForegroundGray().mulAlpha(progress));
        cx += icon6.width("A") + gap;
        ctx.enableScissor((int) startX+20, (int) y, (int) (startX + width), (int) (y + h));

        float offset = (1-animation.getValue())*font.width(name)*2;
        float offset2 = (animation.getValue())*font.width(lastCategory.getName());

        ctx.drawText(font, name, cx+offset, vy - font.height() / 2f, textColor.mulAlpha(animation.getValue()));
        ctx.drawText(font, lastCategory.getName(), cx-offset2, vy - font.height() / 2f, textColor.mulAlpha(1-animation.getValue()));
        ctx.disableScissor();
        return startX + width;
    }

    private float renderStats(UIContext ctx, float startX, float y,
                              float progress, Theme theme,
                              Category cat, ColorRGBA textColor) {
        int enabled = 0, total = 0;
        for (Module m : Zenith.getInstance().getModuleManager().getModules()) {
            if (m.getCategory() == cat) {
                total++;
                if (m.isEnabled()) enabled++;
            }
        }
        Font font = Fonts.MEDIUM.getFont(7);
        Font iconFont = Fonts.ICONS.getFont(7);
        float w = 8 +iconFont.width(cat.getIcon()) + 4 +font.width(String.valueOf(enabled))+ 1 + 8+1 + iconFont.width(cat.getIcon()) + 4 +font.width(String.valueOf(total))+8 ;
        float h = 22;
        ColorRGBA bar = theme.getForegroundColor().mulAlpha(progress);
        ctx.drawRoundedRect(startX, y, w, h, BorderRadius.all(7), bar);
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), startX, y, w, h, -0.1f,
                BorderRadius.all(7), theme.getForegroundStroke().mulAlpha(progress));


        float iconSz = 7;
        float cx = startX + 8;
        float ty = y + (h - font.height()) / 2f;
        float iy = y + (h - iconFont.height()) / 2f -0.5f;

        ctx.drawText(iconFont,cat.getIcon(),cx+(cat.getIcon().equals("2")?1:0),iy,theme.getColor().mulAlpha(progress));
        cx += iconFont.width(cat.getIcon()) + 4;
        ctx.drawText(font, String.valueOf(enabled), cx, ty, textColor);
        cx += font.width(String.valueOf(enabled)) ;
        cx+=1f;
        ctx.drawSprite(new CustomSprite("icons/separator.png"), cx, iy-1, 8, 8,
                ColorRGBA.WHITE.mulAlpha(progress));
        cx += 8+1 ;
        ctx.drawText(iconFont,cat.getIcon(),cx,iy,theme.getColor().mulAlpha(progress));
        cx +=iconFont.width(cat.getIcon())+4;

        ctx.drawText(font, String.valueOf(total), cx, ty, textColor);

        return startX + w;
    }

    private float renderThemeButton(UIContext ctx, float startX, float y,
                                    float progress, Theme theme) {
        float size = 22;
        this.themeButtonBounds = new Rect(startX, y, size, size);
        drawIconButton(ctx, startX, y, size, theme.getIcon(), progress, theme);
        return startX + size;
    }

    private float renderLayoutButton(UIContext ctx, float startX, float y,
                                     float progress, Theme theme, int cols) {
        float size = 22;
        this.layoutToggleButtonBounds = new Rect(startX, y, size, size);
        String icon = cols == 2 ? ":" : cols == 3 ? ";" : "9";
        drawIconButton(ctx, startX, y, size, icon, progress, theme);
        return startX + size;
    }

    private void drawIconButton(UIContext ctx, float x, float y,
                                float s, String icon,
                                float progress, Theme theme) {
        ColorRGBA bar = theme.getForegroundColor().mulAlpha(progress);
        ctx.drawRoundedRect(x, y, s, s, BorderRadius.all(6), bar);
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, s, s, -0.1f,
                BorderRadius.all(6), theme.getForegroundStroke().mulAlpha(progress));
        Font iconF = Fonts.ICONS.getFont(7);
        float ix = x + (s - iconF.width(icon)) / 2f;
        float iy = y + (s - iconF.height()) / 2f;
        ctx.drawText(iconF, icon, ix, iy, theme.getColor().mulAlpha(progress));
    }

    private void renderSearchBar(UIContext ctx, float boxX, float y,
                                 float boxWidth, float progress, Theme theme) {

        float w = 128, h = 22, pad = 8;
        float x = boxX + boxWidth - pad - w;
        this.searchBarBounds = new Rect(x, y, w, h);

        ColorRGBA bar = theme.getForegroundColor().mulAlpha(progress);
        ctx.drawRoundedRect(x, y, w, h, BorderRadius.all(6), bar);
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, w, h, -0.1f,
                BorderRadius.all(6), theme.getForegroundStroke().mulAlpha(progress));

        Font font = Fonts.MEDIUM.getFont(7);
        String txt = searchField.getText();
        boolean empty = txt.isEmpty() && !searchField.isSelected();
        float ty = y + (h - font.height()) / 2f;
        if (empty) {
            ctx.drawText(font, "Search", x + 8, ty, theme.getWhite().mulAlpha(progress * 0.5f));
        } else {
            //ctx.drawText(font, txt, x + 8, ty, theme.getWhite().mulAlpha(progress));
            ctx.enableScissor((int) x+8,(int) ty-10,(int)Math.ceil( x+8+128),(int) Math.ceil(ty)+10);
            searchField.render(ctx,x+8,ty,theme.getWhite().mulAlpha(progress),theme.getWhite().mulAlpha(progress * 0.5f));
//            if (searchField.isSelected() && (System.currentTimeMillis() / 500) % 2 == 0) {
//                float tw = font.width(txt);
//                ctx.drawRect(x + 8 + tw, y + 4, 1, h - 8, theme.getWhite().mulAlpha(progress));
//            }
            ctx.disableScissor();
        }
    }
    public void resetAnim(Category last,Category next) {

            animation.reset(0);

        this.lastCategory = last;
    }
    public boolean handleMouseClicked(double mouseX, double mouseY) {
        if (layoutToggleButtonBounds.contains(mouseX, mouseY)) {
            onLayoutToggle.run();
            return true;
        }
        if (themeButtonBounds.contains(mouseX, mouseY)) {
            onThemeSwitch.run();
            return true;
        }
        return searchBarBounds.contains(mouseX, mouseY);
    }
}
