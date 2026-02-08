package ru.ui;

import com.ferra13671.cometrenderer.plugins.minecraft.RectColors;
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import com.ferra13671.cometrenderer.plugins.minecraft.drawer.impl.RoundedRectDrawer;
import net.minecraft.client.gui.DrawContext;
import ru.render.Animation;
import ru.render.AnimationSystem;
import ru.render.MsdfTextRenderer;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an expandable/collapsible GUI dropdown component.
 * Supports smooth animations for expand/collapse transitions.
 * 
 * Requirements: 4.2, 5.2
 */
public class DropDownComponent {
    private String title;
    private List<DropDownItem> items;
    private boolean expanded;
    private float animationProgress; // 0.0 to 1.0
    private int x, y, width, height;
    private AnimationSystem animationSystem;
    private String animationId;
    
    // Constants for rendering
    private static final int TITLE_HEIGHT = 30;
    private static final int ITEM_HEIGHT = 25;
    private static final float ANIMATION_DURATION = 0.3f; // seconds
    
    /**
     * Creates a new DropDownComponent with the specified parameters.
     * 
     * @param title The title text displayed in the header
     * @param x The x-coordinate of the top-left corner
     * @param y The y-coordinate of the top-left corner
     * @param width The width of the component
     * @param animationSystem The animation system to use for animations
     */
    public DropDownComponent(String title, int x, int y, int width, AnimationSystem animationSystem) {
        this.title = title;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = TITLE_HEIGHT;
        this.items = new ArrayList<>();
        this.expanded = false;
        this.animationProgress = 0.0f;
        this.animationSystem = animationSystem;
        this.animationId = "dropdown_" + title + "_" + System.nanoTime();
    }
    
    /**
     * Adds an item to this dropdown.
     * 
     * @param item The DropDownItem to add
     */
    public void addItem(DropDownItem item) {
        if (item != null) {
            items.add(item);
        }
    }
    
    /**
     * Toggles the expanded state of this dropdown and creates an animation.
     * Creates a smooth animation from current state to target state.
     */
    public void toggle() {
        expanded = !expanded;
        
        // Create animation for the state change
        if (animationSystem != null) {
            animationSystem.createAnimation(animationId, ANIMATION_DURATION, null);
        }
    }
    
    /**
     * Renders this dropdown component with animated expansion.
     * Uses CometRenderer for all drawing operations.
     * 
     * @param context The DrawContext for rendering (can be null for headless testing)
     * @param delta The time elapsed since last frame in seconds
     * @param textRenderer The text renderer to use for drawing text (can be null)
     */
    public void render(DrawContext context, float delta, MsdfTextRenderer textRenderer) {
        // Update animation progress
        if (animationSystem != null && animationSystem.hasAnimation(animationId)) {
            float rawProgress = animationSystem.getProgress(animationId);
            
            // Interpolate animation progress based on expanded state
            if (expanded) {
                // Expanding: progress goes from current to 1.0
                animationProgress = animationProgress + (rawProgress * (1.0f - animationProgress));
                if (rawProgress >= 1.0f) {
                    animationProgress = 1.0f;
                }
            } else {
                // Collapsing: progress goes from current to 0.0
                animationProgress = animationProgress * (1.0f - rawProgress);
                if (rawProgress >= 1.0f) {
                    animationProgress = 0.0f;
                }
            }
        } else {
            // No animation, snap to target state
            animationProgress = expanded ? 1.0f : 0.0f;
        }
        
        // Render title bar
        renderTitleBar(context, textRenderer);
        
        // Render items if expanded or animating
        if (animationProgress > 0.0f) {
            renderItems(context, textRenderer);
        }
    }
    
    /**
     * Renders the title bar of the dropdown.
     */
    private void renderTitleBar(DrawContext context, MsdfTextRenderer textRenderer) {
        // Skip rendering if context is null (headless testing)
        if (context == null) {
            return;
        }
        
        // Background color for title bar
        RenderColor titleBgColor = RenderColor.of(70, 70, 80, 200);
        
        // Render title background
        new RoundedRectDrawer()
                .rectSized(x, y, width, TITLE_HEIGHT, 5, RectColors.oneColor(titleBgColor))
                .build()
                .tryDraw()
                .close();
        
        // Render title text
        if (textRenderer != null && title != null && !title.isEmpty()) {
            RenderColor textColor = RenderColor.of(255, 255, 255, 255);
            int textX = x + 10;
            int textY = y + (TITLE_HEIGHT / 2) - 8; // Center vertically
            textRenderer.drawText(textX, textY, 16, title, textColor);
        }
        
        // Render expand/collapse indicator
        if (textRenderer != null) {
            String indicator = expanded ? "▼" : "▶";
            RenderColor indicatorColor = RenderColor.of(200, 200, 200, 255);
            int indicatorX = x + width - 25;
            int indicatorY = y + (TITLE_HEIGHT / 2) - 8;
            textRenderer.drawText(indicatorX, indicatorY, 16, indicator, indicatorColor);
        }
    }
    
    /**
     * Renders the dropdown items with animation.
     */
    private void renderItems(DrawContext context, MsdfTextRenderer textRenderer) {
        // Skip rendering if context is null (headless testing)
        if (context == null) {
            return;
        }
        
        if (items.isEmpty()) {
            return;
        }
        
        // Calculate total items height
        int totalItemsHeight = items.size() * ITEM_HEIGHT;
        
        // Apply animation progress to height
        int animatedHeight = (int) (totalItemsHeight * animationProgress);
        
        // Render background for items area
        RenderColor itemsBgColor = RenderColor.of(50, 50, 60, (int) (180 * animationProgress));
        
        if (animatedHeight > 0) {
            new RoundedRectDrawer()
                    .rectSized(x, y + TITLE_HEIGHT, width, animatedHeight, 5, RectColors.oneColor(itemsBgColor))
                    .build()
                    .tryDraw()
                    .close();
        }
        
        // Render individual items
        int itemY = y + TITLE_HEIGHT;
        for (int i = 0; i < items.size(); i++) {
            // Calculate if this item should be visible based on animation progress
            float itemVisibility = Math.max(0.0f, Math.min(1.0f, 
                (animationProgress * items.size()) - i));
            
            if (itemVisibility > 0.0f) {
                DropDownItem item = items.get(i);
                item.render(x + 5, itemY, width - 10, ITEM_HEIGHT, itemVisibility, textRenderer);
            }
            
            itemY += ITEM_HEIGHT;
            
            // Stop rendering if we've exceeded the animated height
            if (itemY - (y + TITLE_HEIGHT) >= animatedHeight) {
                break;
            }
        }
    }
    
    /**
     * Checks if the mouse is over this dropdown component.
     * 
     * @param mouseX The mouse x-coordinate
     * @param mouseY The mouse y-coordinate
     * @return true if the mouse is over the component, false otherwise
     */
    public boolean isMouseOver(double mouseX, double mouseY) {
        // Check if mouse is over title bar
        if (mouseX >= x && mouseX <= x + width && 
            mouseY >= y && mouseY <= y + TITLE_HEIGHT) {
            return true;
        }
        
        // Check if mouse is over expanded items area
        if (expanded && !items.isEmpty()) {
            int totalItemsHeight = items.size() * ITEM_HEIGHT;
            if (mouseX >= x && mouseX <= x + width && 
                mouseY >= y + TITLE_HEIGHT && mouseY <= y + TITLE_HEIGHT + totalItemsHeight) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Handles a mouse click event on this dropdown.
     * Toggles expansion if clicked on title bar, or triggers item click if clicked on an item.
     * 
     * @param mouseX The mouse x-coordinate
     * @param mouseY The mouse y-coordinate
     * @return true if the click was handled, false otherwise
     */
    public boolean handleClick(double mouseX, double mouseY) {
        // Check if clicked on title bar
        if (mouseX >= x && mouseX <= x + width && 
            mouseY >= y && mouseY <= y + TITLE_HEIGHT) {
            toggle();
            return true;
        }
        
        // Check if clicked on an item (only if expanded)
        if (expanded && !items.isEmpty()) {
            int itemY = y + TITLE_HEIGHT;
            for (DropDownItem item : items) {
                if (mouseX >= x && mouseX <= x + width && 
                    mouseY >= itemY && mouseY <= itemY + ITEM_HEIGHT) {
                    item.click();
                    return true;
                }
                itemY += ITEM_HEIGHT;
            }
        }
        
        return false;
    }
    
    // Getters and setters
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public List<DropDownItem> getItems() {
        return new ArrayList<>(items);
    }
    
    public boolean isExpanded() {
        return expanded;
    }
    
    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }
    
    public float getAnimationProgress() {
        return animationProgress;
    }
    
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
}
