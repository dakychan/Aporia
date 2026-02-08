package ru;

import com.ferra13671.cometrenderer.CometRenderer;
import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import ru.event.impl.EventSystemImpl;
import ru.event.impl.KeyPressEvent;
import ru.gui.GuiManager;
import ru.input.api.KeyboardKeys;
import ru.input.api.KeyBindings;
import ru.input.impl.UnifiedInputHandler;
import ru.input.impl.bind.KeybindListener;
import ru.input.impl.bind.KeybindManager;
import ru.mixin.render.IGlGpuBuffer;
import ru.render.MsdfFont;
import ru.render.MsdfTextRenderer;
import ru.ui.notify.NotificationManager;
import ru.ui.notify.NotificationRenderer;

public class Aporia implements ClientModInitializer {

    public static void initRender() {
        CometRenderer.init();
        MinecraftPlugin.init(glGpuBuffer -> ((IGlGpuBuffer) glGpuBuffer)._getId(), () -> 1);
    }

    @Override
    public void onInitializeClient() {
        UnifiedInputHandler.init();
        
        KeybindManager.getInstance().loadKeybinds();
        
        KeybindListener.init();
        
        NotificationManager.getInstance();
        
        initNotificationRenderer();
        
        KeyBindings.register();
        
        setupKeyBindings();
    }

    private static void initNotificationRenderer() {
        try {
            MsdfFont font = new MsdfFont(
                "assets/aporia/fonts/Inter_Medium.json",
                "assets/aporia/fonts/Inter_Medium.png"
            );
            MsdfTextRenderer textRenderer = new MsdfTextRenderer(font);
            NotificationRenderer.init(textRenderer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void setupKeyBindings() {
    }

    public static void render() {
        GuiManager.render();
    }
}

