package ru.ui;

import com.ferra13671.cometrenderer.plugins.minecraft.RectColors;
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import com.ferra13671.cometrenderer.plugins.minecraft.drawer.impl.RoundedRectDrawer;
import ru.render.MsdfTextRenderer;

/**
 * Individual item within a dropdown component.
 * Represents a single clickable option with a label, enabled state, and onClick callback.
 */
public class DropDownItem {
    private String label;
    private boolean enabled;
    private Runnable onClick;

    /**
     * Creates a new DropDownItem with the specified label and onClick callback.
     * The item is enabled by default.
     *
     * @param label The text label to display for this item
     * @param onClick The callback to execute when this item is clicked
     */
    public DropDownItem(String label, Runnable onClick) {
        this(label, true, onClick);
    }

    /**
     * Creates a new DropDownItem with the specified label, enabled state, and onClick callback.
     *
     * @param label The text label to display for this item
     * @param enabled Whether this item is enabled (clickable)
     * @param onClick The callback to execute when this item is clicked
     */
    public DropDownItem(String label, boolean enabled, Runnable onClick) {
        this.label = label;
        this.enabled = enabled;
        this.onClick = onClick;
    }

    /**
     * Renders this dropdown item at the specified position with the given dimensions and alpha.
     * Uses CometRenderer for all drawing operations.
     *
     * @param x The x-coordinate of the top-left corner
     * @param y The y-coordinate of the top-left corner
     * @param width The width of the item
     * @param height The height of the item
     * @param alpha The alpha transparency value (0.0 to 1.0)
     * @param textRenderer The text renderer to use for drawing the label (can be null)
     */
    public void render(int x, int y, int width, int height, float alpha, MsdfTextRenderer textRenderer) {
        // Clamp alpha to valid range
        alpha = Math.max(0.0f, Math.min(1.0f, alpha));
        
        // Calculate alpha as 0-255 integer
        int alphaInt = (int) (alpha * 255);
        
        // Choose background color based on enabled state
        RenderColor backgroundColor;
        if (enabled) {
            backgroundColor = RenderColor.of(60, 60, 70, (int) (150 * alpha));
        } else {
            backgroundColor = RenderColor.of(40, 40, 50, (int) (100 * alpha));
        }
        
        // Render background rectangle with rounded corners
        new RoundedRectDrawer()
                .rectSized(x, y, width, height, 3, RectColors.oneColor(backgroundColor))
                .build()
                .tryDraw()
                .close();
        
        // Render text label if textRenderer is available
        if (textRenderer != null && label != null && !label.isEmpty()) {
            // Choose text color based on enabled state
            RenderColor textColor;
            if (enabled) {
                textColor = RenderColor.of(255, 255, 255, alphaInt);
            } else {
                textColor = RenderColor.of(150, 150, 150, alphaInt);
            }
            
            // Calculate text position (centered vertically with left padding)
            int textX = x + 10;
            int textY = y + (height / 2) - 7; // Approximate vertical centering for 14px text
            
            textRenderer.drawText(textX, textY, 14, label, textColor);
        }
    }

    /**
     * Executes the onClick callback if this item is enabled.
     * Does nothing if the item is disabled or onClick is null.
     */
    public void click() {
        if (enabled && onClick != null) {
            onClick.run();
        }
    }

    // Getters and setters

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Runnable getOnClick() {
        return onClick;
    }

    public void setOnClick(Runnable onClick) {
        this.onClick = onClick;
    }
}
