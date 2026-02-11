package ru;

import aporia.cc.user.UserData;
import aporia.cc.user.UserGenerator;
import com.ferra13671.cometrenderer.CometRenderer;
import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin;
import net.fabricmc.api.ClientModInitializer;
import ru.gui.GuiManager;
import ru.input.api.KeyBindings;
import ru.input.impl.UnifiedInputHandler;
import ru.input.impl.bind.KeybindListener;
import ru.input.impl.bind.KeybindManager;
import ru.mixin.render.IGlBuffer;
import ru.ui.notify.Notify;

public class Aporia implements ClientModInitializer {

    public static void initRender() {
        CometRenderer.init();
        MinecraftPlugin.init(glGpuBuffer -> ((IGlBuffer) glGpuBuffer)._getHandle(), () -> 1);
    }

    @Override
    public void onInitializeClient() {
        UnifiedInputHandler.init();
        KeybindManager.getInstance().loadKeybinds();
        KeybindListener.init();
        Notify.Manager.getInstance();
        KeyBindings.register();
        setupKeyBindings();
        initUserData();
    }

    private static void setupKeyBindings() {}

    public static void render() {
        GuiManager.render();
    }

    public static void initUserData() {
        UserData.UserDataClass userData = UserData.INSTANCE.getUserData();
        //                     TESTS
        System.out.println("§a[UserData] Username: " + userData.getUsername());
        System.out.println("§a[UserData] UUID: " + userData.getUuid());
        System.out.println("§a[UserData] Role: " + userData.getRole());
        System.out.println("§a[UserData] HardwareID: " + userData.getHardwareId());

        // Пример генерации рандомного ника
        String randomName = UserGenerator.INSTANCE.generateRandomUsername();
        System.out.println("§a[UserGenerator] Random: " + randomName);

        // Оффлайн UUID
        String offlineUUID = UserGenerator.INSTANCE.generateOfflineUUID("kotay");
        System.out.println("§a[UserGenerator] Offline UUID: " + offlineUUID);
    }
}

