package ru.ui.clickgui;

import com.ferra13671.cometrenderer.plugins.minecraft.RectColors;
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import com.ferra13671.cometrenderer.plugins.minecraft.drawer.impl.RoundedRectDrawer;
import net.minecraft.client.gui.DrawContext;
import ru.render.AnimationSystem;
import ru.render.MsdfTextRenderer;

/**
 * A toggle button GUI component that can be switched between two states.
 * Supports smooth animations for state transitions.
 * 
 * Requirements: 4.5, 4.6
 */
public class ToggleButton {
    private int x, y, width, height;
    private boolean state;
    private String labelOn;
    private String labelOff;
    private AnimationSystem animationSystem;
    private String animationId;
    private float animationProgress; // 0.0 to 1.0
    
    // Constants for rendering
    private static final int DEFAULT_WIDTH = 120;
    private static final int DEFAULT_HEIGHT = 30;
    private static final float ANIMATION_DURATION = 0.25f; // seconds
    
    /**
     * Creates a new ToggleButton with default labels "ON" and "OFF".
     * 
     * @param x The x-coordinate of the top-left corner
     * @param y The y-coordinate of the top-left corner
     * @param animationSystem The animation system to use for animations
     */
    public ToggleButton(int x, int y, AnimationSystem animationSystem) {
        this(x, y, DEFAULT_WIDTH, DEFAULT_HEIGHT, "ON", "OFF", animationSystem);
    }
    
    /**
     * Creates a new ToggleButton with custom labels.
     * 
     * @param x The x-coordinate of the top-left corner
     * @param y The y-coordinate of the top-left corner
     * @param width The width of the button
     * @param height The height of the button
     * @param labelOn The label to display when the button is in the ON state
     * @param labelOff The label to display when the button is in the OFF state
     * @param animationSystem The animation system to use for animations
     */
    public ToggleButton(int x, int y, int width, int height, String labelOn, String labelOff, AnimationSystem animationSystem) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.state = false;
        this.labelOn = labelOn;
        this.labelOff = labelOff;
        this.animationSystem = animationSystem;
        this.animationId = "toggle_button_" + System.nanoTime();
        this.animationProgress = 0.0f;
    }
    
    /**
     * Toggles the state of this button and creates an animation for the transition.
     * Creates a smooth animation from the current state to the new state.
     */
    public void toggle() {
        state = !state;
        
        // Create animation for the state change
        if (animationSystem != null) {
            animationSystem.createAnimation(animationId, ANIMATION_DURATION, null);
        }
    }
    
    /**
     * Updates the animation progress for this toggle button.
     * Should be called each frame to update animation state.
     * 
     * @param delta The time elapsed since last frame in seconds
     */
    public void updateAnimation(float delta) {
        // Update animation progress
        if (animationSystem != null && animationSystem.hasAnimation(animationId)) {
            float rawProgress = animationSystem.getProgress(animationId);
            
            // Interpolate animation progress based on state
            if (state) {
                // Transitioning to ON: progress goes from current to 1.0
                animationProgress = animationProgress + (rawProgress * (1.0f - animationProgress));
                if (rawProgress >= 1.0f) {
                    animationProgress = 1.0f;
                }
            } else {
                // Transitioning to OFF: progress goes from current to 0.0
                animationProgress = animationProgress * (1.0f - rawProgress);
                if (rawProgress >= 1.0f) {
                    animationProgress = 0.0f;
                }
            }
        } else {
            // No animation, snap to target state
            animationProgress = state ? 1.0f : 0.0f;
        }
    }
    
    /**
     * Renders this toggle button with animated state transitions.
     * Uses CometRenderer for all drawing operations.
     * 
     * @param context The DrawContext for rendering (can be null for headless testing)
     * @param delta The time elapsed since last frame in seconds
     * @param textRenderer The text renderer to use for drawing text (can be null)
     */
    public void render(DrawContext context, float delta, MsdfTextRenderer textRenderer) {
        // Update animation progress
        updateAnimation(delta);
        
        // Skip rendering if context is null (headless testing)
        if (context == null) {
            return;
        }
        
        // Render button background
        renderBackground();
        
        // Render toggle indicator (sliding circle)
        renderToggleIndicator();
        
        // Render label text
        renderLabel(textRenderer);
    }
    
    /**
     * Renders the background of the toggle button.
     */
    private void renderBackground() {
        // Interpolate background color based on animation progress
        // OFF state: darker gray, ON state: brighter blue/green
        int r = (int) (60 + (100 * animationProgress));
        int g = (int) (60 + (140 * animationProgress));
        int b = (int) (70 + (80 * animationProgress));
        int a = 200;
        
        RenderColor backgroundColor = RenderColor.of(r, g, b, a);
        
        // Render rounded rectangle background
        new RoundedRectDrawer()
                .rectSized(x, y, width, height, height / 2, RectColors.oneColor(backgroundColor))
                .build()
                .tryDraw()
                .close();
    }
    
    /**
     * Renders the sliding toggle indicator (circle).
     */
    private void renderToggleIndicator() {
        // Calculate indicator position based on animation progress
        int indicatorSize = height - 8; // Slightly smaller than button height
        int indicatorPadding = 4;
        
        // Calculate x position: slides from left to right
        int maxTravel = width - indicatorSize - (indicatorPadding * 2);
        int indicatorX = x + indicatorPadding + (int) (maxTravel * animationProgress);
        int indicatorY = y + indicatorPadding;
        
        // Indicator color (white)
        RenderColor indicatorColor = RenderColor.of(255, 255, 255, 255);
        
        // Render circular indicator
        new RoundedRectDrawer()
                .rectSized(indicatorX, indicatorY, indicatorSize, indicatorSize, 
                          indicatorSize / 2, RectColors.oneColor(indicatorColor))
                .build()
                .tryDraw()
                .close();
    }
    
    /**
     * Renders the label text on the button.
     */
    private void renderLabel(MsdfTextRenderer textRenderer) {
        if (textRenderer == null) {
            return;
        }
        
        // Choose label based on current state
        String currentLabel = state ? labelOn : labelOff;
        
        if (currentLabel == null || currentLabel.isEmpty()) {
            return;
        }
        
        // Calculate text position (centered)
        int textX = x + (width / 2) - (currentLabel.length() * 4); // Approximate centering
        int textY = y + (height / 2) - 8; // Center vertically
        
        // Text color (white)
        RenderColor textColor = RenderColor.of(255, 255, 255, 255);
        
        textRenderer.drawText(textX, textY, 16, currentLabel, textColor);
    }
    
    /**
     * Checks if the mouse is over this toggle button.
     * 
     * @param mouseX The mouse x-coordinate
     * @param mouseY The mouse y-coordinate
     * @return true if the mouse is over the button, false otherwise
     */
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && 
               mouseY >= y && mouseY <= y + height;
    }
    
    // Getters and setters
    
    public int getX() {
        return x;
    }
    
    public void setX(int x) {
        this.x = x;
    }
    
    public int getY() {
        return y;
    }
    
    public void setY(int y) {
        this.y = y;
    }
    
    public int getWidth() {
        return width;
    }
    
    public void setWidth(int width) {
        this.width = width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public void setHeight(int height) {
        this.height = height;
    }
    
    public boolean getState() {
        return state;
    }
    
    public void setState(boolean state) {
        this.state = state;
    }
    
    public String getLabelOn() {
        return labelOn;
    }
    
    public void setLabelOn(String labelOn) {
        this.labelOn = labelOn;
    }
    
    public String getLabelOff() {
        return labelOff;
    }
    
    public void setLabelOff(String labelOff) {
        this.labelOff = labelOff;
    }
    
    public float getAnimationProgress() {
        return animationProgress;
    }
}
