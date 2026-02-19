package ru;

import aporia.cc.user.UserData;
import com.ferra13671.cometrenderer.CometRenderer;
import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin;
import lombok.Getter;
import ru.files.FilesManager;
import ru.gui.GuiManager;
import ru.help.discord.DiscordManager;
import ru.input.api.KeyBindings;
import ru.input.impl.UnifiedInputHandler;
import ru.input.impl.bind.KeybindListener;
import ru.input.impl.bind.KeybindManager;
import ru.render.BlurShader;
import ru.ui.hud.HudManager;
import ru.ui.notify.Notify;

public class Aporia {

    private static FilesManager filesManager;
    private static BlurShader blurShader;
    private static DiscordManager discordManager;
    
    public static DiscordManager getDiscordManager() {
        return discordManager;
    }

    public static void initRender() {
        CometRenderer.init();
        MinecraftPlugin.init(glGpuBuffer -> ((com.mojang.blaze3d.opengl.GlBuffer) glGpuBuffer).getHandle(), () -> 1);
    }

    public static void onInit() {
        initFileSystem();
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

    private static void initRenderingSystems() {
        blurShader = new BlurShader();
        HudManager.INSTANCE.initialize();
    }

    private static void initCommandSystem() {
        aporia.cc.chat.ChatUtils.INSTANCE.initialize();
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
                aporia.cc.Logger.INSTANCE.error("Failed to save stats on shutdown: " + e.getMessage(), e);
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
        
        ru.module.Module interfaceModule = ru.module.ModuleManager.getInstance().getModuleByName("Interface");
        boolean interfaceEnabled = interfaceModule != null && interfaceModule.isEnabled();
        
        if (interfaceEnabled && blurShader != null) {
            blurShader.renderToFramebuffer(width, height, () -> {
                if (interfaceModule instanceof ru.module.impl.visuals.Interface) {
                    ((ru.module.impl.visuals.Interface) interfaceModule).render();
                }
            });
        } else if (interfaceEnabled && interfaceModule instanceof ru.module.impl.visuals.Interface) {
            ((ru.module.impl.visuals.Interface) interfaceModule).render();
        }
    }
    
    public static boolean handleInterfaceClick(int mouseX, int mouseY, int button) {
        ru.module.Module interfaceModule = ru.module.ModuleManager.getInstance().getModuleByName("Interface");
        if (interfaceModule != null && interfaceModule.isEnabled() && interfaceModule instanceof ru.module.impl.visuals.Interface) {
            return ((ru.module.impl.visuals.Interface) interfaceModule).handleMouseClick(mouseX, mouseY, button);
        }
        return false;
    }
    
    public static void handleInterfaceDrag(int mouseX, int mouseY) {
        ru.module.Module interfaceModule = ru.module.ModuleManager.getInstance().getModuleByName("Interface");
        if (interfaceModule != null && interfaceModule.isEnabled() && interfaceModule instanceof ru.module.impl.visuals.Interface) {
            ((ru.module.impl.visuals.Interface) interfaceModule).handleMouseDrag(mouseX, mouseY);
        }
    }
    
    public static void handleInterfaceRelease() {
        ru.module.Module interfaceModule = ru.module.ModuleManager.getInstance().getModuleByName("Interface");
        if (interfaceModule != null && interfaceModule.isEnabled() && interfaceModule instanceof ru.module.impl.visuals.Interface) {
            ((ru.module.impl.visuals.Interface) interfaceModule).handleMouseRelease();
        }
    }

    public static void initUserData() {
        UserData.UserDataClass userData = UserData.INSTANCE.getUserData();
    }
}

