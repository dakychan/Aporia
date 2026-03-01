package ru;

import aporia.cc.Logger;
import aporia.cc.chat.ChatUtils;
import aporia.cc.user.UserData;
import cc.apr.modules.visuals.BlurTest;
import cc.apr.modules.visuals.Interface;
import cc.apr.module.api.Module;
import cc.apr.module.api.ModuleManager;
import com.ferra13671.cometrenderer.CometRenderer;
import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin;
import aporia.cc.files.FilesManager;
import com.mojang.blaze3d.opengl.GlBuffer;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.Minecraft;
import ru.utils.ui.gui.GuiManager;
import ru.utils.discord.DiscordManager;
import ru.utils.input.api.KeyBindings;
import ru.utils.input.impl.UnifiedInputHandler;
import ru.utils.input.impl.bind.KeybindListener;
import ru.utils.input.impl.bind.KeybindManager;
import aporia.cc.manager.OsManager;
import ru.utils.render.RectRenderer;
import ru.ui.hud.HudManager;
import ru.utils.ui.notify.Notify;

public class Aporia {

    private static FilesManager filesManager;
    private static DiscordManager discordManager;
    
    public static DiscordManager getDiscordManager() {
        return discordManager;
    }

    public static void initRender() {
        CometRenderer.init();
        MinecraftPlugin.init(glGpuBuffer -> ((GlBuffer) glGpuBuffer).getHandle(), () -> 1);
    }

    public static void onInit() {
        initFileSystem();
        initOsManager();
        UnifiedInputHandler.init();
        KeybindManager.getInstance().loadKeybinds();
        KeybindListener.init();
        Notify.Manager.getInstance();
        KeyBindings.register();
        setupKeyBindings();
        initUserData();
        initRenderingSystems();
        initCommandSystem();
        initDiscordManager();
    }

    private static void initOsManager() {
        /**
         * Initialize OsManager logger with ASCII banner.
         * Logs system information (OS, CPU, RAM, GPU) without spam.
         */
        OsManager.INSTANCE.initLogger();
    }

    private static void initRenderingSystems() {
        RectRenderer.init();
        HudManager.INSTANCE.initialize();
    }

    private static void initCommandSystem() {
        ChatUtils.INSTANCE.initialize();
    }
    
    private static void initDiscordManager() {
        /**
         * Initialize Discord manager but don't start RPC automatically.
         * RPC will be started when DiscordRPC module is enabled.
         */
        discordManager = new DiscordManager();
    }

    private static void initFileSystem() {
        aporia.cc.Logger.INSTANCE.initialize();
        filesManager = new FilesManager();
        filesManager.initialize();
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                UserData.UserDataClass userData = UserData.INSTANCE.getUserData();
                filesManager.saveStats(userData);
            } catch (Exception e) {
                Logger.INSTANCE.error("Failed to save stats on shutdown: " + e.getMessage(), e);
            }
        }));
    }

    public static FilesManager getFilesManager() {
        return filesManager;
    }

    private static void setupKeyBindings() {}

    public static void render() {
        GuiManager.render();

        MinecraftPlugin plugin = MinecraftPlugin.getInstance();
        if (plugin == null) return;

        int width = plugin.getMainFramebufferWidth();
        int height = plugin.getMainFramebufferHeight();

        Module interfaceModule = ModuleManager.getInstance().getModuleByName("Interface");
        boolean interfaceEnabled = interfaceModule != null && interfaceModule.isEnabled();

        if (interfaceEnabled && interfaceModule instanceof Interface) {
            ((Interface) interfaceModule).render();
        }
    }

    /**
     * Вызывается ПОСЛЕ рендера мира и ентитов для правильного blur
     */
    public static void renderBlur() {
        MinecraftPlugin plugin = MinecraftPlugin.getInstance();
        if (plugin == null) return;

        int width = plugin.getMainFramebufferWidth();
        int height = plugin.getMainFramebufferHeight();

        Module blurTestModule = ModuleManager.getInstance().getModuleByName("BlurTest");
        if (blurTestModule != null && blurTestModule.isEnabled() && blurTestModule instanceof BlurTest) {
            ((BlurTest) blurTestModule).onRender2D();
        }
    }
    /**
     * Захватывает mainTarget для blur.
     * Вызывается из LevelRenderer.addBlurCapturePass() внутри FrameGraph.
     */
    public static void captureMainTargetForBlur(com.mojang.blaze3d.pipeline.RenderTarget mainTarget) {
        if (mainTarget == null) return;
        
        // Проверяем нужен ли blur
        Module blurTestModule = ModuleManager.getInstance().getModuleByName("BlurTest");
        if (blurTestModule == null || !blurTestModule.isEnabled()) {
            return;
        }
        
        // Применяем blur через BlurManager
        System.out.println("[APORIA] Applying blur in FrameGraph pass");
        ru.utils.render.BlurManager.getInstance().applyBlur(mainTarget);
    }
    
    public static boolean handleInterfaceClick(int mouseX, int mouseY, int button) {
        Module interfaceModule = ModuleManager.getInstance().getModuleByName("Interface");
        if (interfaceModule != null && interfaceModule.isEnabled() && interfaceModule instanceof Interface) {
            return ((Interface) interfaceModule).handleMouseClick(mouseX, mouseY, button);
        }
        return false;
    }
    
    public static void handleInterfaceDrag(int mouseX, int mouseY) {
        Module interfaceModule = ModuleManager.getInstance().getModuleByName("Interface");
        if (interfaceModule != null && interfaceModule.isEnabled() && interfaceModule instanceof Interface) {
            ((Interface) interfaceModule).handleMouseDrag(mouseX, mouseY);
        }
    }
    
    public static void handleInterfaceRelease() {
        Module interfaceModule = ModuleManager.getInstance().getModuleByName("Interface");
        if (interfaceModule != null && interfaceModule.isEnabled() && interfaceModule instanceof Interface) {
            ((Interface) interfaceModule).handleMouseRelease();
        }
    }

    public static void initUserData() {
        UserData.UserDataClass userData = UserData.INSTANCE.getUserData();
    }
}

