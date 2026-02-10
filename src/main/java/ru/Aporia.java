package ru;

import com.ferra13671.cometrenderer.CometRenderer;
import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin;
import net.fabricmc.api.ClientModInitializer;
import ru.gui.GuiManager;
import ru.input.api.KeyBindings;
import ru.input.impl.UnifiedInputHandler;
import ru.input.impl.bind.KeybindListener;
import ru.input.impl.bind.KeybindManager;
import ru.mixin.render.IGlGpuBuffer;
import ru.ui.notify.Notify;

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

        Notify.Manager.getInstance();
        
        KeyBindings.register();
        
        setupKeyBindings();
    }

    private static void setupKeyBindings() {
    }

    public static void render() {
        GuiManager.render();
    }
}

