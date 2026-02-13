package ru;

import aporia.cc.user.UserData;
import com.ferra13671.cometrenderer.CometRenderer;
import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin;
import ru.files.FilesManager;
import ru.gui.GuiManager;
import ru.input.api.KeyBindings;
import ru.input.impl.UnifiedInputHandler;
import ru.input.impl.bind.KeybindListener;
import ru.input.impl.bind.KeybindManager;
import ru.ui.notify.Notify;


public class Aporia {

    private static FilesManager filesManager;

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
    }

    private static void initFileSystem() {
        filesManager = new FilesManager();
        filesManager.initialize();
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                UserData.UserDataClass userData = UserData.INSTANCE.getUserData();
                filesManager.saveStats(userData);
            } catch (Exception e) {
                System.err.println("Failed to save stats on shutdown: " + e.getMessage());
            }
        }));
    }

    public static FilesManager getFilesManager() {
        return filesManager;
    }

    private static void setupKeyBindings() {}

    public static void render() {
        GuiManager.render();
        
        // Render Interface module if enabled
        ru.module.Module interfaceModule = ru.module.ModuleManager.getInstance().getModuleByName("Interface");
        if (interfaceModule != null && interfaceModule.isEnabled() && interfaceModule instanceof ru.module.impl.visuals.Interface) {
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

