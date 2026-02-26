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
import com.mojang.blaze3d.textures.GpuTexture;
import ru.utils.ui.gui.GuiManager;
import ru.utils.discord.DiscordManager;
import ru.utils.input.api.KeyBindings;
import ru.utils.input.impl.UnifiedInputHandler;
import ru.utils.input.impl.bind.KeybindListener;
import ru.utils.input.impl.bind.KeybindManager;
import aporia.cc.manager.OsManager;
import ru.utils.render.BlurShader;
import ru.utils.render.RectRenderer;
import ru.ui.hud.HudManager;
import ru.utils.ui.notify.Notify;

public class Aporia {

    private static FilesManager filesManager;
    private static BlurShader blurShader;
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
        blurShader = new BlurShader();
        RectRenderer.init();
        RectRenderer.setSharedBlurShader(blurShader); // Используем один BlurShader
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
                
                if (blurShader != null) {
                    blurShader.cleanup();
                }
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

        if (interfaceEnabled && blurShader != null) {
            blurShader.renderToFramebuffer(width, height, () -> {
                if (interfaceModule instanceof Interface) {
                    ((Interface) interfaceModule).render();
                }
            });
        } else if (interfaceEnabled && interfaceModule instanceof Interface) {
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
     * Захватывает экран СРАЗУ после рендера мира для blur эффекта
     */
    public static void captureScreenForBlur() {
        MinecraftPlugin plugin = MinecraftPlugin.getInstance();
        if (plugin == null) return;

        int width = plugin.getMainFramebufferWidth();
        int height = plugin.getMainFramebufferHeight();

        Module blurTestModule = ModuleManager.getInstance().getModuleByName("BlurTest");
        if (blurTestModule != null && blurTestModule.isEnabled()) {
            RectRenderer.beginBlurBatch(width, height);
        }
    }
    
    /**
     * Захватывает экран в КОНЦЕ кадра для использования в СЛЕДУЮЩЕМ кадре.
     * Вызывается ПОСЛЕ guiRenderer.render() когда мир уже отрендерен.
     */
    public static void captureScreenForNextFrame() {
        MinecraftPlugin plugin = MinecraftPlugin.getInstance();
        if (plugin == null) return;

        int width = plugin.getMainFramebufferWidth();
        int height = plugin.getMainFramebufferHeight();

        Module blurTestModule = ModuleManager.getInstance().getModuleByName("BlurTest");
        if (blurTestModule != null && blurTestModule.isEnabled()) {
            RectRenderer.captureForNextFrame(width, height);
        }
    }
    
    /**
     * НОВЫЙ МЕТОД: Захватывает экран ВНУТРИ FramePass когда framebuffer гарантированно заполнен.
     * Вызывается из LevelRenderer.addBlurCapturePass() во время выполнения FrameGraph.
     */
    public static void captureScreenInFramePass(int width, int height) {
        Module blurTestModule = ModuleManager.getInstance().getModuleByName("BlurTest");
        if (blurTestModule != null && blurTestModule.isEnabled()) {
            RectRenderer.captureForNextFrame(width, height);
        }
    }
    
    /**
     * НОВЫЙ ПОДХОД: Захватывает world-only RenderTarget для blur эффекта.
     * Вызывается из LevelRenderer.addWorldOnlyCapturePass() когда мир отрендерен БЕЗ UI.
     */
    public static void captureWorldOnlyTarget(com.mojang.blaze3d.pipeline.RenderTarget worldTarget) {
        int width = worldTarget.width;
        int height = worldTarget.height;

        GpuTexture gpuTexture = worldTarget.getColorTexture();
        
        if (gpuTexture instanceof GlTexture) {
            int textureId = ((GlTexture)gpuTexture).glId();

            RectRenderer.setWorldOnlyTexture(textureId, width, height);
        }
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

