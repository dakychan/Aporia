package ru;

import aporia.cc.user.UserData;
import com.ferra13671.cometrenderer.CometRenderer;
import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin;
import net.fabricmc.api.ClientModInitializer;
import ru.files.FilesManager;
import ru.gui.GuiManager;
import ru.input.api.KeyBindings;
import ru.input.impl.UnifiedInputHandler;
import ru.input.impl.bind.KeybindListener;
import ru.input.impl.bind.KeybindManager;
import ru.mixin.render.IGlBuffer;
import ru.ui.notify.Notify;

public class Aporia implements ClientModInitializer {

    private static FilesManager filesManager;

    public static void initRender() {
        CometRenderer.init();
        MinecraftPlugin.init(glGpuBuffer -> ((IGlBuffer) glGpuBuffer)._getHandle(), () -> 1);
    }

    @Override
    public void onInitializeClient() {
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
    }

    public static void initUserData() {
        UserData.UserDataClass userData = UserData.INSTANCE.getUserData();
    }
}

