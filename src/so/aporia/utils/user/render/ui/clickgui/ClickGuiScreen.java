/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.utils.user.render.ui.clickgui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import so.aporia.module.Category;
import so.aporia.module.Module;
import so.aporia.module.ModuleManager;
import so.aporia.utils.files.FilesManager;
import so.aporia.utils.user.locale.LocaleManager;
import so.aporia.utils.user.logger.Logger;
import so.aporia.utils.user.render.animation.Animator;
import so.aporia.utils.user.render.color.ColorUtil;
import so.aporia.utils.user.render.core.AporiaRenderer;

import java.util.concurrent.CompletableFuture;

/**
 * Main ClickGui screen for rendering the GUI interface.
 * Handles topbar rendering with buttons (Config, Stats, Tasks),
 * second topbar with breadcrumb navigation and language selector.
 *
 * Все вычисления кэшируются — минимум аллокаций в render().
 */
//TODO: replace renderer to imgui
@OnlyIn(Dist.CLIENT)
public final class ClickGuiScreen extends Screen {
    float px, py, pw, ph;
    Category active = Category.values()[0];
    Animator guiOpenAnim = null;
    Identifier profileImageId = null;
    boolean profileImageTried = false;
    Identifier UNKNOWN_USER_RL = Identifier.fromNamespaceAndPath("aporia", "texture/unknown-user.png");

    // Кэшированные данные
    private String cachedTasksText;
    private String cachedStatsText;
    private String cachedConfigText;
    private String cachedBreadcrumb;
    private float cachedTasksWidth;
    private float cachedStatsWidth;
    private float cachedConfigWidth;
    private float cachedBreadcrumbWidth;
    private int cachedCategoryCount;
    private String[] cachedCatNames;

    // Кэшированные иконки
    private Identifier cachedStatsIconId;
    private Identifier cachedConfigIconId;
    private Identifier cachedSocialIconId;
    private Identifier cachedDocumIconId;

    // Цвета — константы
    private static final int MAIN_BG_COLOR     = 0xFF101010;
    private static final int TOPBAR_BG_COLOR   = 0xFF161618;
    private static final int SECOND_TOPBAR_COLOR = 0xFF202022;
    private static final int SIDEBAR_COLOR     = 0xFF101010;
    private static final int CONTENT_BG_COLOR  = 0xFF2D2D32;
    private static final int BUTTON_COLOR      = 0xFF111113;
    private static final int TEXT_COLOR        = 0x96969696; // RGBA: 150,150,150,150
    private static final int UUID_TEXT_COLOR   = 0x96969664; // RGBA: 150,150,150,100

    private static boolean avatarLoadAttempted = false;
    private static boolean avatarLoadComplete = false;
    private static Identifier cachedAvatarId = null;

    public ClickGuiScreen() {
        super(Component.literal("ClickGui"));
    }

    @Override
    protected void init() {
        if (pw == 0) {
            pw = Math.max(600, this.width  * 0.75f);
            ph = Math.max(200, this.height * 0.7f);
            px = (this.width  - pw) / 2f;
            py = (this.height - ph) / 2f;
            guiOpenAnim = Animator.slide(600);
            guiOpenAnim.play();
            AporiaRenderer.INSTANCE.invalidateBlurCache();
        }

        // Кэшируем локаль и тексты — один раз
        cacheTexts();
        cacheIcons();

        AporiaRenderer.INSTANCE.resetDebugFlags();
    }

    /**
     * Кэширует все тексты из LocaleManager.
     * Вызывается один раз при инициализации.
     */
    private void cacheTexts() {
        AporiaRenderer r = AporiaRenderer.INSTANCE;
        LocaleManager lm = LocaleManager.getInstance();

        cachedTasksText = lm.get("gui.tasks");
        cachedStatsText = lm.get("gui.stats");
        cachedConfigText = lm.get("gui.config");
        cachedCategoryCount = Category.values().length;
        cachedCatNames = new String[cachedCategoryCount];
        for (int i = 0; i < cachedCategoryCount; i++) {
            cachedCatNames[i] = Category.values()[i].name();
        }
        updateBreadcrumb();

        // Кэшируем ширину текста
        cachedTasksWidth = r.getTextWidth("bold", cachedTasksText, 8f);
        cachedStatsWidth = r.getTextWidth("bold", cachedStatsText, 8f);
        cachedConfigWidth = r.getTextWidth("bold", cachedConfigText, 8f);
    }

    private void updateBreadcrumb() {
        cachedBreadcrumb = "Категория -> " + active.name() + " -> Aura";
        cachedBreadcrumbWidth = AporiaRenderer.INSTANCE.getTextWidth("bold", cachedBreadcrumb, 8f);
    }

    /**
     * Кэширует все Identifier'ы иконок.
     */
    private void cacheIcons() {
        cachedStatsIconId = Identifier.fromNamespaceAndPath("aporia", "texture/statsistic.png");
        cachedConfigIconId = Identifier.fromNamespaceAndPath("aporia", "texture/config.png");
        cachedSocialIconId = Identifier.fromNamespaceAndPath("aporia", "texture/social.png");
        cachedDocumIconId = Identifier.fromNamespaceAndPath("aporia", "texture/docum.png");
    }

    @Override
    public void render(GuiGraphics gfx, int mx, int my, float delta) {
        AporiaRenderer r = AporiaRenderer.INSTANCE;
        Minecraft mc = Minecraft.getInstance();

        px = Math.max(0, Math.min(px, this.width  - pw));
        py = Math.max(0, Math.min(py, this.height - ph));

        int ipx = (int)px, ipy = (int)py, ipw = (int)pw, iph = (int)ph;

        guiOpenAnim.update();
        float openProg = guiOpenAnim.value();

        float scale = openProg;
        int scaledW = (int)(ipw * scale);
        int scaledH = (int)(iph * scale);
        int scaledX = ipx + (ipw - scaledW) / 2;
        int scaledY = ipy + (iph - scaledH) / 2;

        int topbarH = 22;
        int secondTopbarH = 18;
        int scaledTopbarW = (int)(ipw * scale);
        int scaledTopbarX = ipx + (ipw - scaledTopbarW) / 2;
        int scaledTopbarY = ipy + (iph - scaledH) / 2;

        // Основной фон
        r.drawRect(scaledX, scaledY, scaledW, scaledH, 4, MAIN_BG_COLOR);
        r.drawRect(scaledTopbarX, scaledTopbarY, scaledTopbarW, topbarH, 4, TOPBAR_BG_COLOR, 3);

        if (!avatarLoadComplete && !avatarLoadAttempted) {
            tryLoadAvatar(r);
        }

        // Кнопки верхнего бара
        renderTopBarButtons(r, scaledTopbarX, scaledTopbarY, scaledTopbarW, topbarH);

        // Второй верхний бар
        int secondTopbarY = scaledTopbarY + topbarH;
        r.drawRect(scaledTopbarX, secondTopbarY, scaledTopbarW, secondTopbarH, 0, SECOND_TOPBAR_COLOR);

        // Соц иконка слева
        int categoryIconSize = 12;
        int categoryIconY = secondTopbarY + (secondTopbarH - categoryIconSize) / 2;
        r.drawImage(scaledX + 6, categoryIconY, categoryIconSize, categoryIconSize, cachedSocialIconId, 2);

        // Хлебные крошки по центру
        float breadcrumbX = scaledX + (scaledW - cachedBreadcrumbWidth) / 2f;
        float breadcrumbY = secondTopbarY + (secondTopbarH - 8f) / 2f;
        r.drawText("bold", cachedBreadcrumb, breadcrumbX, breadcrumbY, 8f, TEXT_COLOR);

        // Язык и документация справа
        int rightElementsX = scaledX + scaledW - 6;
        int langIconSize = 12;
        Identifier flagIconId = getFlagIcon();
        r.drawImage(rightElementsX - langIconSize - 2, categoryIconY, langIconSize, langIconSize, flagIconId, 2);
        r.drawImage(rightElementsX - langIconSize - 12 - 6, categoryIconY, 12, 12, cachedDocumIconId, 2);

        // Сайдбар
        int categoriesY = secondTopbarY + secondTopbarH + (scaledH - (secondTopbarY + secondTopbarH)) / 2 - (cachedCategoryCount * 16 + (cachedCategoryCount - 1) * 12) / 2;
        int sidebarW = 120;
        int sidebarH = (scaledY + scaledH) - (secondTopbarY + secondTopbarH);
        r.drawRect(scaledX, secondTopbarY + secondTopbarH, sidebarW, sidebarH, 0, SIDEBAR_COLOR);

        // Контент
        int contentW = scaledW - sidebarW;
        int contentH = (scaledY + scaledH) - (secondTopbarY + secondTopbarH);
        r.drawRect(scaledX + sidebarW, secondTopbarY + secondTopbarH, contentW, contentH, 0, CONTENT_BG_COLOR);

        // Категории
        renderCategories(r, scaledX, categoriesY);

        // Профиль
        renderProfile(r, scaledX, scaledY, scaledH);
    }

    private void tryLoadAvatar(AporiaRenderer r) {
        if (avatarLoadComplete) return;
        if (avatarLoadAttempted) return;

        avatarLoadAttempted = true;

        CompletableFuture.runAsync(() -> {
            try {
                Module discordRPC = ModuleManager.INSTANCE.get("Discord RPC");
                if (discordRPC != null && discordRPC.isEnabled()) {
                    var discordModule = (so.aporia.module.impl.misc.DiscordRPCModule) discordRPC;
                    discordModule.loadAvatarOnRenderThread();
                    Identifier avatarId = discordModule.getAvatarId();
                    if (avatarId != null) {
                        Minecraft.getInstance().execute(() -> {
                            profileImageId = avatarId;
                            avatarLoadComplete = true;
                        });
                        return;
                    }
                }
            } catch (Exception ignored) {}
            try {
                java.nio.file.Path imagesDir = FilesManager.ROOT.resolve("images");
                java.nio.file.Files.createDirectories(imagesDir);
                java.nio.file.Path pngPath = imagesDir.resolve("profile.png");

                if (java.nio.file.Files.exists(pngPath)) {
                    Minecraft.getInstance().execute(() -> {
                        profileImageId = r.loadImage(pngPath);
                        avatarLoadComplete = true;
                    });
                    return;
                }
            } catch (Exception ignored) {}
            Minecraft.getInstance().execute(() -> {
                profileImageId = UNKNOWN_USER_RL;
                avatarLoadComplete = true;
            });
        });
    }

    private void renderTopBarButtons(AporiaRenderer r, int topbarX, int topbarY, int topbarW, int topbarH) {
        int buttonY = topbarY + 3;
        int buttonH = 16;
        int buttonGap = 4;
        int padding = 6;
        int gap = 4;
        int iconW = 10;
        int fontH = 8;

        // Tasks
        int tasksPanelW = (int)(padding * 2 + cachedTasksWidth) + 30;
        int tasksPanelX = topbarX + topbarW - 8 - tasksPanelW;
        int tasksPanelY = buttonY;
        r.drawRect(tasksPanelX, tasksPanelY, tasksPanelW, buttonH, 3, BUTTON_COLOR);
        r.drawText("bold", cachedTasksText, tasksPanelX + padding, tasksPanelY + (buttonH - fontH) / 2f, 8f, TEXT_COLOR);

        // Stats
        int statsPanelW = (int)(padding * 2 + iconW + gap + cachedStatsWidth);
        int statsPanelX = tasksPanelX - buttonGap - statsPanelW;
        int statsPanelY = buttonY;
        r.drawRect(statsPanelX, statsPanelY, statsPanelW, buttonH, 3, BUTTON_COLOR);
        r.drawImage(statsPanelX + padding, statsPanelY + 3, iconW, buttonH - 6, cachedStatsIconId, 2);
        r.drawText("bold", cachedStatsText, statsPanelX + padding + iconW + gap, statsPanelY + (buttonH - fontH) / 2f, 8f, TEXT_COLOR);

        // Config
        int configPanelW = (int)(padding * 2 + iconW + gap + cachedConfigWidth);
        int configPanelX = statsPanelX - buttonGap - configPanelW;
        int configPanelY = buttonY;
        r.drawRect(configPanelX, configPanelY, configPanelW, buttonH, 3, BUTTON_COLOR);
        r.drawImage(configPanelX + padding, configPanelY + 3, iconW, buttonH - 6, cachedConfigIconId, 2);
        r.drawText("bold", cachedConfigText, configPanelX + padding + iconW + gap, configPanelY + (buttonH - fontH) / 2f, 8f, TEXT_COLOR);
    }

    private void renderCategories(AporiaRenderer r, int sidebarX, int startY) {
        int catIconSize = 16;
        int catTextSize = 8;
        int catGap = 12;
        int catIconColumnW = 20;
        int currentCatY = startY;
        int catX = sidebarX + 26;

        Category[] cats = Category.values();
        for (int i = 0; i < cats.length; i++) {
            Category cat = cats[i];
            int iconCenterX = catX + catIconColumnW / 2;
            int iconX = iconCenterX - catIconSize / 2;
            int alpha = cat == active ? 255 : 100;
            int catColor = ColorUtil.rgba(150, 150, 150, alpha);

            r.drawImage(iconX, currentCatY, catIconSize, catIconSize, cat.texture, 2);
            r.drawText("bold", cachedCatNames[i], (float)(catX + catIconColumnW + 6), currentCatY + (catIconSize - catTextSize) / 2f - 1, 8f, catColor);
            currentCatY += catIconSize + catGap;
        }
    }

    private void renderProfile(AporiaRenderer r, int sidebarX, int screenY, int screenH) {
        int profileY = screenY + screenH - 60;
        int profileX = sidebarX + 6;
        int avatarSize = 48;

        Identifier avatarImg = (profileImageId != null) ? profileImageId : UNKNOWN_USER_RL;
        r.drawImage(profileX, profileY, avatarSize, avatarSize, avatarImg, 2);

        aporia.cc.UserData.UserDataClass userData = aporia.cc.UserData.getUserData();
        String username = userData.getUsername();
        String uuid = userData.getUuid();

        r.drawText("bold", username, (float)(profileX + avatarSize + 8), (float)(profileY + 4), 9f, TEXT_COLOR);
        r.drawText("bold", uuid, (float)(profileX + avatarSize + 8), (float)(profileY + 16), 7f, UUID_TEXT_COLOR);
    }

    private Identifier getFlagIcon() {
        String flag = switch (LocaleManager.getInstance().getCurrentLang()) {
            case "ru_RU" -> "texture/russia.png";
            case "ch_CH" -> "texture/china.png";
            default -> "texture/usa.png";
        };
        return Identifier.fromNamespaceAndPath("aporia", flag);
    }

    /**
     * Gets the currently active category index.
     */
    public int getActiveCategory() {
        return active.ordinal();
    }

    /**
     * Converts JPG image to PNG format.
     */
    private void convertJpgToPng(java.nio.file.Path jpgPath, java.nio.file.Path pngPath) {
        try {
            java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(jpgPath.toFile());
            javax.imageio.ImageIO.write(image, "png", pngPath.toFile());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets the active category by index.
     */
    public void setActiveCategory(int categoryIndex) {
        Category[] cats = Category.values();
        if (categoryIndex >= 0 && categoryIndex < cats.length) {
            active = cats[categoryIndex];
            updateBreadcrumb(); // Обновить кэш breadcrumb
        }
    }

    @Override public boolean isPauseScreen()     { return false; }
    @Override public boolean isAllowedInPortal() { return true; }
    @Override public void renderBackground(GuiGraphics gfx, int mx, int my, float d) {}

    @Override
    public void onClose() {
        AporiaRenderer.INSTANCE.invalidateBlurCache();
        super.onClose();
    }
}
