package ru.render;

import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin;
import net.minecraft.client.gui.GuiGraphics;

public class GuiRenderer {
    private final BlurShader blurShader;
    private final AnimationSystem animationSystem;
    private boolean initialized = false;
    public GuiRenderer() {
        this.blurShader = new BlurShader();
        this.animationSystem = new AnimationSystem();
    }
    public void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
    }
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        if (!initialized) {
            initialize();
        }

        animationSystem.tick();
    }
    public void cleanup() {
        blurShader.cleanup();
        initialized = false;
    }
    public BlurShader getBlurShader() {
        return blurShader;
    }
    public AnimationSystem getAnimationSystem() {
        return animationSystem;
    }
    public boolean isInitialized() {
        return initialized;
    }
}
