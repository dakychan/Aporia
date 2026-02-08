package ru;

import com.ferra13671.cometrenderer.CometRenderer;
import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import ru.event.EventSystem;
import ru.event.KeyPressEvent;
import ru.gui.GuiManager;
import ru.input.Keyboard;
import ru.input.KeyInputHandler;
import ru.mixin.IGlGpuBuffer;

public class Aporia implements ClientModInitializer {

    public static void initRender() {
        CometRenderer.init();
        MinecraftPlugin.init(glGpuBuffer -> ((IGlGpuBuffer) glGpuBuffer)._getId(), () -> 1);
    }

    @Override
    public void onInitializeClient() {
        KeyInputHandler.register();
        setupKeyBindings();
    }

    private static void setupKeyBindings() {
        // Биндинг обрабатывается через ScreenKeyPressMixin
    }

    public static void render() {
        GuiManager.render();
    }
}

