package ru.utils.render;

import net.minecraft.client.gui.GuiGraphics;

public class GuiRenderer {
    private final AnimationSystem animationSystem;
    private boolean initialized = false;
    
    public GuiRenderer() {
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
        initialized = false;
    }
    
    public AnimationSystem getAnimationSystem() {
        return animationSystem;
    }
    
    public boolean isInitialized() {
        return initialized;
    }
}
