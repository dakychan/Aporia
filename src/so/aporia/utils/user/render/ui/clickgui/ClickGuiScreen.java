package so.aporia.utils.user.render.ui.clickgui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
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
import so.aporia.utils.user.render.animation.Easing;
import so.aporia.utils.user.render.color.ColorUtil;
import so.aporia.utils.user.render.core.AporiaRenderer;

import java.io.File;

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

    @Override
    public void render(GuiGraphics gfx, int mx, int my, float delta) {
        AporiaRenderer r = AporiaRenderer.INSTANCE;
        r.prepareFrameBlur(Minecraft.getInstance(), 30f, 0.75f);

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

        r.drawRect(scaledX, scaledY, scaledW, scaledH, 16, ColorUtil.rgba(30, 30, 35, 255));

        // Topbar background with rounded top corners and animation
        int topbarH = 28;
        int scaledTopbarW = (int)(ipw * scale);
        int scaledTopbarX = ipx + (ipw - scaledTopbarW) / 2;
        int scaledTopbarY = ipy + (iph - scaledH) / 2;
        // cornerMask: bit 0=top-left, 1=top-right, 2=bottom-right, 3=bottom-left
        // For top corners only: 0b0011 = 3
        r.drawRect(scaledTopbarX, scaledTopbarY, scaledTopbarW, topbarH, 16, ColorUtil.rgba(20, 20, 25, 255), 3);

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
                            profileImageId = r.loadImage(p);
                            break;
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        int tasksPanelW = 75;
        int tasksPanelH = 20;
        int tasksPanelX = scaledTopbarX + scaledTopbarW - tasksPanelW - 14;
        int tasksPanelY = scaledTopbarY + 4;

        r.drawRect(tasksPanelX, tasksPanelY, tasksPanelW, tasksPanelH, 3, ColorUtil.rgba(40, 40, 45, 255));

        String tasksText = LocaleManager.getInstance().get("gui.tasks");
        r.drawText("regular", tasksText, (float)tasksPanelX + 2, (float)tasksPanelY + (tasksPanelH - 9) / 2f, 9f, ColorUtil.rgba(255, 255, 255, 200));

        Identifier avatarImg = profileImageId != null ? profileImageId : UNKNOWN_USER_RL;
        int avatarW = 33;
        r.drawImage(tasksPanelX + tasksPanelW - avatarW + 1, tasksPanelY, avatarW, tasksPanelH, avatarImg, 2);

        int statsPanelW = 75;
        int statsPanelH = 20;
        int statsPanelX = tasksPanelX - statsPanelW - 10;
        int statsPanelY = scaledTopbarY + 4;

        r.drawRect(statsPanelX, statsPanelY, statsPanelW, statsPanelH, 3, ColorUtil.rgba(40, 40, 45, 255));

        String statsText = LocaleManager.getInstance().get("gui.stats");
        float statsTextWidth = r.getTextWidth("regular", statsText, 9f);
        float statsTextX = statsPanelX + (statsPanelW - statsTextWidth) / 2f + 6;
        r.drawText("regular", statsText, statsTextX, (float)statsPanelY + (tasksPanelH - 9) / 2f, 9f, ColorUtil.rgba(255, 255, 255, 200));

        Identifier statsIconId = Identifier.fromNamespaceAndPath("aporia", "texture/statsistic.png");
        int statsIconW = 14;
        r.drawImage(statsPanelX + 3, statsPanelY + 3, statsIconW, statsPanelH - 6, statsIconId, 2);

        int configPanelW = 90;
        int configPanelH = 20;
        int configPanelX = statsPanelX - configPanelW - 10;
        int configPanelY = scaledTopbarY + 4;

        r.drawRect(configPanelX, configPanelY, configPanelW, configPanelH, 3, ColorUtil.rgba(40, 40, 45, 255));

        String configText = LocaleManager.getInstance().get("gui.config");
        float configTextWidth = r.getTextWidth("regular", configText, 9f);
        float configTextX = configPanelX + (configPanelW - configTextWidth) / 2f + 5;
        r.drawText("regular", configText, configTextX, (float)configPanelY + (configPanelH - 9) / 2f, 9f, ColorUtil.rgba(255, 255, 255, 200));

        Identifier configIconId = Identifier.fromNamespaceAndPath("aporia", "texture/config.png");
        int configIconW = 14;
        r.drawImage(configPanelX + 2, configPanelY + 3, configIconW, configPanelH - 6, configIconId, 2);
    }

    public int getActiveCategory() {
        return active.ordinal();
    }

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
