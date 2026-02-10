package ru.render;

import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin;
import net.minecraft.client.gui.DrawContext;

public class GuiRenderer {
    private final BlurRenderer blurRenderer;
    private final AnimationSystem animationSystem;
    private boolean initialized = false;
    public GuiRenderer() {
        this.blurRenderer = new BlurRenderer();
        this.animationSystem = new AnimationSystem();
    }
    public void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
    }
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!initialized) {
            initialize();
        }

        animationSystem.tick();

        if (BlurRenderer.isInitialized()) {
            int width = context.getScaledWindowWidth();
            int height = context.getScaledWindowHeight();
            blurRenderer.applyBlur(width, height);
        }
    }
    public void cleanup() {
        initialized = false;
    }
    public BlurRenderer getBlurRenderer() {
        return blurRenderer;
    }
    public AnimationSystem getAnimationSystem() {
        return animationSystem;
    }
    public boolean isInitialized() {
        return initialized;
    }
}
