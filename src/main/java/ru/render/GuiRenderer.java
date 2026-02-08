package ru.render;

import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin;
import net.minecraft.client.gui.DrawContext;

/**
 * Central rendering coordinator that manages all GUI drawing operations.
 * Coordinates BlurRenderer and AnimationSystem to provide a unified rendering interface.
 * 
 * Requirements: 4.1, 4.2, 4.3
 */
public class GuiRenderer {
    private final BlurRenderer blurRenderer;
    private final AnimationSystem animationSystem;
    private boolean initialized = false;
    
    /**
     * Creates a new GuiRenderer with BlurRenderer and AnimationSystem.
     */
    public GuiRenderer() {
        this.blurRenderer = new BlurRenderer();
        this.animationSystem = new AnimationSystem();
    }
    
    /**
     * Initializes the GuiRenderer and its subsystems.
     * This should be called before any rendering operations.
     */
    public void initialize() {
        if (initialized) {
            return;
        }
        
        // BlurRenderer initializes itself statically
        // AnimationSystem doesn't require initialization
        
        initialized = true;
    }
    
    /**
     * Renders all GUI components with blur and animations.
     * This method coordinates the rendering pipeline:
     * 1. Updates animations
     * 2. Applies blur effects
     * 3. Renders GUI components
     * 
     * @param context The DrawContext for rendering
     * @param mouseX The mouse X coordinate
     * @param mouseY The mouse Y coordinate
     * @param delta The time elapsed since last frame in seconds
     */
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!initialized) {
            initialize();
        }
        
        // Update all active animations
        animationSystem.update(delta);
        
        // Bind the main framebuffer for rendering
        MinecraftPlugin.getInstance().bindMainFramebuffer(true);
        
        // Apply blur effect if BlurRenderer is initialized
        if (BlurRenderer.isInitialized()) {
            // Get window dimensions from context
            int width = context.getScaledWindowWidth();
            int height = context.getScaledWindowHeight();
            blurRenderer.applyBlur(width, height);
        }
    }
    
    /**
     * Cleans up resources used by the GuiRenderer.
     * This should be called when the GUI is closed.
     */
    public void cleanup() {
        // Clear all active animations
        animationSystem.clear();
        initialized = false;
    }
    
    /**
     * Gets the BlurRenderer instance.
     * 
     * @return The BlurRenderer
     */
    public BlurRenderer getBlurRenderer() {
        return blurRenderer;
    }
    
    /**
     * Gets the AnimationSystem instance.
     * 
     * @return The AnimationSystem
     */
    public AnimationSystem getAnimationSystem() {
        return animationSystem;
    }
    
    /**
     * Checks if the GuiRenderer is initialized.
     * 
     * @return true if initialized, false otherwise
     */
    public boolean isInitialized() {
        return initialized;
    }
}
