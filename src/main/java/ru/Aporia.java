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
import ru.mixin.render.IGlGpuBuffer;

public class Aporia implements ClientModInitializer {

    public static void initRender() {
        CometRenderer.init();
        MinecraftPlugin.init(glGpuBuffer -> ((IGlGpuBuffer) glGpuBuffer)._getId(), () -> 1);
    }

    @Override
    public void onInitializeClient() {
        // Initialize unified input handler
        UnifiedInputHandler.init();
        
        // Register key bindings
        KeyBindings.register();
        
        setupKeyBindings();
    }

    private static void setupKeyBindings() {
        // Биндинг обрабатывается через ScreenKeyPressMixin
    }

    public static void render() {
        GuiManager.render();
    }
}

