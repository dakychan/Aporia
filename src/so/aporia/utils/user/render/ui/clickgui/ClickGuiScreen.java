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
import so.aporia.utils.user.render.animation.Animator;
import so.aporia.utils.user.render.color.ColorUtil;
import so.aporia.utils.user.render.core.AporiaRenderer;

/**
 * Main ClickGui screen for rendering the GUI interface.
 * Handles topbar rendering with buttons (Config, Stats, Tasks),
 * second topbar with breadcrumb navigation and language selector.
 */
@OnlyIn(Dist.CLIENT)
public final class ClickGuiScreen extends Screen {
    float px, py, pw, ph;
    Category active = Category.values()[0];
    Animator guiOpenAnim = null;
    Identifier profileImageId = null;
    boolean profileImageTried = false;
    Identifier UNKNOWN_USER_RL = Identifier.fromNamespaceAndPath("aporia", "texture/unknown-user.png");
    
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
        LocaleManager.getInstance().loadLocale("en_EU");
        LocaleManager.getInstance().loadLocale("ru_RU");
        LocaleManager.getInstance().loadLocale("ch_CH");
        AporiaRenderer.INSTANCE.resetDebugFlags();
    }

    /**
     * Renders the main GUI with topbars and buttons.
     * First topbar: Config, Stats, Tasks buttons with avatar
     * Second topbar: Social icon, breadcrumb navigation, documentation and language selector
     */
    @Override
    public void render(GuiGraphics gfx, int mx, int my, float delta) {
        AporiaRenderer r = AporiaRenderer.INSTANCE;

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

        r.drawRect(scaledX, scaledY, scaledW, scaledH, 4, ColorUtil.rgba(16, 16, 16, 255));
        r.drawRect(scaledTopbarX, scaledTopbarY, scaledTopbarW, topbarH, 4, ColorUtil.rgba(22, 22, 24, 255), 3);

        int buttonColor = ColorUtil.rgba(17, 17, 19, 255);
        int textColor = ColorUtil.rgba(150, 150, 150, 150);

        if (!profileImageTried) {
            profileImageTried = true;

            try {
                Module discordRPC = ModuleManager.INSTANCE.get("Discord RPC");
                if (discordRPC != null && discordRPC.isEnabled()) {
                    var discordModule = (so.aporia.module.impl.misc.DiscordRPCModule) discordRPC;
                    discordModule.loadAvatarOnRenderThread();
                    Identifier avatarId = discordModule.getAvatarId();
                    if (avatarId != null) {
                        profileImageId = avatarId;
                    }
                }
            } catch (Exception ignored) {}

            if (profileImageId == null) {
                try {
                    java.nio.file.Path imagesDir = FilesManager.ROOT.resolve("images");
                    java.nio.file.Files.createDirectories(imagesDir);
                    for (String ext : new String[]{"png", "jpg", "jpeg"}) {
                        java.nio.file.Path p = imagesDir.resolve("profile." + ext);
                        if (java.nio.file.Files.exists(p)) {
                            if (ext.equals("png")) {
                                profileImageId = r.loadImage(p);
                            } else {
                                java.nio.file.Path pngPath = imagesDir.resolve("profile_converted.png");
                                convertJpgToPng(p, pngPath);
                                profileImageId = r.loadImage(pngPath);
                            }
                            break;
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        int currentX = scaledTopbarX + scaledTopbarW - 8;
        int buttonY = scaledTopbarY + 3;
        int buttonH = 16;
        int buttonGap = 4;
        int padding = 6;
        int gap = 4;
        int iconW = 10;
        int fontH = 8;

        String tasksText = LocaleManager.getInstance().get("gui.tasks");
        float tasksTextWidth = r.getTextWidth("bold", tasksText, 8f);
        int tasksPanelW = (int)(padding * 2 + tasksTextWidth) + 30;
        int tasksPanelX = currentX - tasksPanelW;
        int tasksPanelY = buttonY;

        r.drawRect(tasksPanelX, tasksPanelY, tasksPanelW, buttonH, 3, buttonColor);
        float tasksContentX = tasksPanelX + padding;
        float tasksContentY = tasksPanelY + (buttonH - fontH) / 2f;
        r.drawText("bold", tasksText, tasksContentX, tasksContentY, 8f, textColor);

        currentX = tasksPanelX - buttonGap;

        String statsText = LocaleManager.getInstance().get("gui.stats");
        float statsTextWidth = r.getTextWidth("bold", statsText, 8f);
        Identifier statsIconId = Identifier.fromNamespaceAndPath("aporia", "texture/statsistic.png");
        int statsPanelW = (int)(padding * 2 + iconW + gap + statsTextWidth);
        int statsPanelX = currentX - statsPanelW;
        int statsPanelY = buttonY;

        r.drawRect(statsPanelX, statsPanelY, statsPanelW, buttonH, 3, buttonColor);
        
        float statsContentX = statsPanelX + padding;
        float statsContentY = statsPanelY + (buttonH - fontH) / 2f;
        r.drawImage((int)statsContentX, statsPanelY + 3, iconW, buttonH - 6, statsIconId, 2);
        r.drawText("bold", statsText, statsContentX + iconW + gap, statsContentY, 8f, textColor);

        currentX = statsPanelX - buttonGap;

        String configText = LocaleManager.getInstance().get("gui.config");
        float configTextWidth = r.getTextWidth("bold", configText, 8f);
        Identifier configIconId = Identifier.fromNamespaceAndPath("aporia", "texture/config.png");
        int configPanelW = (int)(padding * 2 + iconW + gap + configTextWidth);
        int configPanelX = currentX - configPanelW;
        int configPanelY = buttonY;

        r.drawRect(configPanelX, configPanelY, configPanelW, buttonH, 3, buttonColor);
        float configContentX = configPanelX + padding;
        float configContentY = configPanelY + (buttonH - fontH) / 2f;
        r.drawImage((int)configContentX, configPanelY + 3, iconW, buttonH - 6, configIconId, 2);
        r.drawText("bold", configText, configContentX + iconW + gap, configContentY, 8f, textColor);

        int secondTopbarY = scaledTopbarY + topbarH;
        r.drawRect(scaledTopbarX, secondTopbarY, scaledTopbarW, secondTopbarH, 0, ColorUtil.rgba(32, 32, 34, 255));

        int categoryIconSize = 12;
        int categoryIconY = secondTopbarY + (secondTopbarH - categoryIconSize) / 2;
        int categoryStartX = scaledX + 6;
        Identifier socialIconId = Identifier.fromNamespaceAndPath("aporia", "texture/social.png");
        r.drawImage(categoryStartX, categoryIconY, categoryIconSize, categoryIconSize, socialIconId, 2);

        String breadcrumbText = "Категория -> " + active.name() + " -> Aura";
        float breadcrumbWidth = r.getTextWidth("bold", breadcrumbText, 8f);
        float breadcrumbX = scaledX + (scaledW - breadcrumbWidth) / 2f;
        float breadcrumbY = secondTopbarY + (secondTopbarH - 8) / 2f;
        r.drawText("bold", breadcrumbText, breadcrumbX, breadcrumbY, 8f, textColor);

        int rightElementsX = scaledX + scaledW - 6;
        int langIconSize = 12;
        String currentLang = LocaleManager.getInstance().getCurrentLang();
        String flagTexture = switch (currentLang) {
            case "ru_RU" -> "texture/russia.png";
            case "ch_CH" -> "texture/china.png";
            default -> "texture/usa.png";
        };
        Identifier flagIconId = Identifier.fromNamespaceAndPath("aporia", flagTexture);
        r.drawImage(rightElementsX - langIconSize - 2, categoryIconY, langIconSize, langIconSize, flagIconId, 2);

        Identifier documIconId = Identifier.fromNamespaceAndPath("aporia", "texture/docum.png");
        int documIconSize = 12;
        r.drawImage(rightElementsX - langIconSize - documIconSize - 6, categoryIconY, documIconSize, documIconSize, documIconId, 2);

        int categoriesY = secondTopbarY + secondTopbarH + (scaledH - (secondTopbarY + secondTopbarH)) / 2 - (Category.values().length * 16 + (Category.values().length - 1) * 12) / 2;
        int catIconSize = 16;
        int catTextSize = 8;
        int catGap = 12;
        int catIconColumnW = 20;
        int currentCatY = categoriesY;
        int catX = scaledX + 26;

        int sidebarW = 120;
        int sidebarH = (scaledY + scaledH) - (secondTopbarY + secondTopbarH);
        r.drawRect(scaledX, secondTopbarY + secondTopbarH, sidebarW, sidebarH, 0, ColorUtil.rgba(16, 16, 16, 255));

        int contentW = scaledW - sidebarW;
        int contentH = (scaledY + scaledH) - (secondTopbarY + secondTopbarH);
        r.drawRect(scaledX + sidebarW, secondTopbarY + secondTopbarH, contentW, contentH, 0, ColorUtil.rgba(45, 45, 50, 255));

        for (Category cat : Category.values()) {
            int catItemH = catIconSize;
            int catItemY = currentCatY;
            
            int iconCenterX = catX + catIconColumnW / 2;
            int iconX = iconCenterX - catIconSize / 2;
            
            int alpha = cat == active ? 255 : 100;
            int catColor = ColorUtil.rgba(150, 150, 150, alpha);
            
            r.drawImage(iconX, catItemY, catIconSize, catIconSize, cat.texture, 2);
            String catName = cat.name();
            float textY = catItemY + (catIconSize - catTextSize) / 2f - 1;
            r.drawText("bold", catName, (float)(catX + catIconColumnW + 6), textY, 8f, catColor);
            currentCatY += catItemH + catGap;
        }

        int profileY = scaledY + scaledH - 60;
        int profileX = scaledX + 6;
        int avatarSize = 48;

        Identifier avatarImg = profileImageId != null ? profileImageId : UNKNOWN_USER_RL;
        r.drawImage(profileX, profileY, avatarSize, avatarSize, avatarImg, 2);

        aporia.cc.UserData.UserDataClass userData = aporia.cc.UserData.getUserData();
        String username = userData.getUsername();
        String uuid = userData.getUuid();

        r.drawText("bold", username, (float)(profileX + avatarSize + 8), (float)(profileY + 4), 9f, textColor);
        r.drawText("bold", uuid, (float)(profileX + avatarSize + 8), (float)(profileY + 16), 7f, ColorUtil.rgba(150, 150, 150, 100));
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
