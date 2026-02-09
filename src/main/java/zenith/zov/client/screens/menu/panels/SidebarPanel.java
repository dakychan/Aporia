package zenith.zov.client.screens.menu.panels;

import by.saskkeee.user.UserInfo;
import lombok.Getter;
import zenith.zov.Zenith;
import zenith.zov.base.animations.base.Animation;
import zenith.zov.base.animations.base.Easing;
import zenith.zov.base.font.Font;
import zenith.zov.base.font.Fonts;
import zenith.zov.base.theme.Theme;
import zenith.zov.client.modules.api.Category;
import zenith.zov.utility.math.MathUtil;
import zenith.zov.utility.render.display.base.*;
import zenith.zov.utility.render.display.base.color.ColorRGBA;
import zenith.zov.utility.render.display.shader.DrawUtil;

import java.util.*;
import java.util.function.Consumer;

public class SidebarPanel {

    @Getter
    private final Map<Category, Rect> categoryBounds = new HashMap<>();
    @Getter
    private Rect sidebarToggleButtonBounds;
    private Rect animRect = new Rect(0, 0, 0, 0);
    private Animation animationChange = new Animation(200, 1, Easing.LINEAR);
    private final Animation sidebarAnimation;
    private final boolean isSidebarExpanded;

    private final Consumer<Category> onCategorySelect;
    private final Runnable onSidebarToggle;
    private final List<SideBarCategory> categories = new ArrayList<>();

    public SidebarPanel(Animation sidebarAnimation, boolean isSidebarExpanded, Consumer<Category> onCategorySelect, Runnable onSidebarToggle) {
        this.sidebarAnimation = sidebarAnimation;
        this.isSidebarExpanded = isSidebarExpanded;
        this.onCategorySelect = onCategorySelect;
        this.onSidebarToggle = onSidebarToggle;
        categories.addAll(Arrays.stream(Category.values()).map(SideBarCategory::new).toList());

    }

    public void render(UIContext ctx, float boxX, float boxY, float height, float progress, Theme theme, Category selectedCategory, ColorRGBA primary, ColorRGBA textColor, ColorRGBA selectedColor) {
        float sidebarProgress = sidebarAnimation.update();
        float collapsedSidebarWidth = 30f;
        float expandedSidebarWidth = 88;
        float sidebarWidth = collapsedSidebarWidth + (expandedSidebarWidth - collapsedSidebarWidth) * sidebarProgress;
        float sidebarPadding = 8;
        float sidebarX = boxX + sidebarPadding;
        float sidebarY = boxY + sidebarPadding;
        float sidebarHeight =height - sidebarPadding * 2;
        ColorRGBA sideBar = theme.getForegroundColor().mulAlpha(progress);

        categoryBounds.clear();

        // --- СЛАЙДБАР ---
        ctx.drawRoundedRect(sidebarX, sidebarY, sidebarWidth, sidebarHeight, BorderRadius.all(7), sideBar);
        DrawUtil.drawRoundedBorder(
                ctx.getMatrices(),
                sidebarX, sidebarY,
                sidebarWidth, sidebarHeight,
                -0.1f,
                BorderRadius.all(7),
                theme.getForegroundStroke().mulAlpha(progress)
        );

        // --- ИКОНКИ ---
        float logoSize = 14;
        float logoX = sidebarX + (collapsedSidebarWidth - logoSize) / 2f;
        float logoY = sidebarY + 8;
        ctx.drawText(Fonts.ICONS.getFont(11), "5", logoX + 2, logoY + 3, Zenith.getInstance().getThemeManager().getColorCycleIcon().toGradient().mulAlpha(progress));


        //ctx.drawSprite(new CustomSprite("icons/logo.png"), logoX, logoY, logoSize, logoSize, primary);
        ctx.pushMatrix();
        ctx.enableScissor((int) sidebarX, (int) sidebarY,
                (int) (sidebarX + sidebarWidth), (int) (sidebarY + sidebarHeight));
        float textAlpha = Math.min(1f, sidebarProgress * 2f);
        textColor = textColor.mulAlpha(textAlpha);
        ColorRGBA textColorDisable = theme.getGrayLight().mulAlpha(progress * textAlpha);
        ColorRGBA iconColorDisable = theme.getGray().mulAlpha(progress);
        {
            Font logoFont = Fonts.MEDIUM.getFont(7);
            String clientName = "zenithdlc.net";

            ctx.drawText(logoFont, clientName, logoX + logoSize + 8, logoY + (logoSize - logoFont.height()) / 2f + 1, textColor);
        }

        final float expandedIconSize = 10f;
        final float collapsedIconSize = 7f;

        float iconSize = expandedIconSize + (collapsedIconSize - expandedIconSize) * sidebarProgress;
        float padding = 10.5f;
        float startY = sidebarY + 35;


        {
            //render border+back active category
            int index = 0;
            for (SideBarCategory sideBarCategory : categories) {
                if (selectedCategory == sideBarCategory.getCategory()) {
                    float categoryY = startY + index * (iconSize + padding);
                    float iconX = sidebarX + (collapsedSidebarWidth - iconSize) / 2f;
                    animRect = new Rect(MathUtil.interpolate(animRect.x(),sidebarX + 4,animationChange.getValue()), MathUtil.interpolate(animRect.y(),categoryY,animationChange.getValue()),sidebarWidth - 8, iconSize + 11);
                    sideBarCategory.render(ctx, animRect.x(), animRect.y(), sidebarWidth - 8, iconSize + 11, sidebarProgress, selectedCategory == sideBarCategory.getCategory(), textColor, textColorDisable, iconColorDisable, primary);
                    ctx.drawRoundedRect( animRect.x(), animRect.y(), sidebarWidth - 8, iconSize + 11, BorderRadius.all(4), theme.getForegroundLight().mulAlpha(progress));

                    DrawUtil.drawRoundedBorder(
                            ctx.getMatrices(),
                            animRect.x(), animRect.y(), sidebarWidth - 8, iconSize + 11,
                            -0.1f,
                            BorderRadius.all(4),
                            theme.getForegroundLightStroke().mulAlpha(progress)
                    );
                    break;
                }
                index++;
            }
        }
        animationChange.animateTo(1f);
        animationChange.update();
        int index = 0;
        for (SideBarCategory sideBarCategory : categories) {
            float categoryY = startY + index * (iconSize + padding);
            float iconX = sidebarX + (collapsedSidebarWidth - iconSize) / 2f;
            sideBarCategory.render(ctx, sidebarX + 4, categoryY, sidebarWidth - 8, iconSize + 11, sidebarProgress, selectedCategory == sideBarCategory.getCategory(), textColor, textColorDisable, iconColorDisable, primary);
            categoryBounds.put(sideBarCategory.getCategory(), new Rect(sidebarX + 4, categoryY, sidebarWidth - 8, iconSize + 11));
            index++;
        }

        // --- АВАТАР И КНОПКА СВОРАЧИВАНИЯ ---
        float avatarSize = 18;
        float avatarX = sidebarX + (collapsedSidebarWidth - avatarSize) / 2f;
        float avatarY = sidebarY + sidebarHeight - avatarSize - 8;
        float toggleX = avatarX + 5;
        float toggleY = avatarY - 19;
        float toggleW = 8;
        float toggleH = 8;

        Font iconFont = Fonts.ICONS.getFont(7);
        ctx.drawText(iconFont,"6",toggleX, toggleY,theme.getGray().mulAlpha(progress));
        sidebarToggleButtonBounds = new Rect(toggleX, toggleY, toggleW, toggleH);

        boolean hover = GuiUtil.isHovered(avatarX, avatarY, avatarSize, avatarSize, ctx);

        DrawUtil.drawRoundedTexture(ctx.getMatrices(), Zenith.id("icons/avatar.png"), avatarX, avatarY, avatarSize, avatarSize, BorderRadius.all(4), ColorRGBA.WHITE.mulAlpha(progress));
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), avatarX, avatarY, avatarSize, avatarSize, -0.1f, BorderRadius.all(3), new ColorRGBA(181, 162, 255, hover ? 200 : 190).mulAlpha(progress));

        {
            String playerName = UserInfo.getUsername();
            Font nameFont = Fonts.MEDIUM.getFont(6);
            ctx.drawText(nameFont, playerName, avatarX + avatarSize + 8, avatarY + (avatarSize - nameFont.height()) / 2f, textColor);
        }
        ctx.disableScissor();
        ctx.popMatrix();
    }

    public boolean handleMouseClicked(double mouseX, double mouseY) {
        if (sidebarToggleButtonBounds != null && sidebarToggleButtonBounds.contains(mouseX, mouseY)) {
            onSidebarToggle.run();
            return true;
        }

        for (Map.Entry<Category, Rect> entry : categoryBounds.entrySet()) {
            if (entry.getValue().contains(mouseX, mouseY)) {
                animationChange.animateTo(0);
                animationChange.setValue(0);

                onCategorySelect.accept(entry.getKey());
                return true;
            }
        }

        return false;
    }
}