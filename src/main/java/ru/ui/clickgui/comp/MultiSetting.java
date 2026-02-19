package ru.ui.clickgui.comp;

import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin;
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import ru.render.MsdfTextRenderer;
import ru.render.RectRenderer;

import java.util.List;

/**
 * Multi-setting component that displays all options inline without expansion.
 * Shows header with setting name and all options as clickable buttons in a row.
 */
public class MultiSetting {
    private String name;
    private List<String> options;
    private List<String> selectedOptions;
    private int x, y, width, height;
    private MsdfTextRenderer textRenderer; // Store for width calculations
    
    private static final int HEADER_HEIGHT = 20;
    private static final int BUTTON_HEIGHT = 18;
    private static final int BUTTON_SPACING = 4;
    private static final int PADDING = 6;
    
    public MultiSetting(String name, List<String> options, List<String> selectedOptions) {
        this.name = name;
        this.options = options;
        this.selectedOptions = selectedOptions;
        this.height = HEADER_HEIGHT + BUTTON_HEIGHT + PADDING;
    }
    
    /**
     * Get total height of the component.
     * Calculates height based on number of rows needed for buttons.
     * 
     * @return Component height in pixels
     */
    public int getHeight() {
        // Calculate number of rows needed
        int totalRows = 1;
        int currentRowWidth = 0;
        int maxWidth = width > 0 ? width : 150; // Use stored width or default
        
        for (String option : options) {
            float textWidth = 40; // Default
            if (textRenderer != null) {
                try {
                    textWidth = textRenderer.measureWidth(option, 9);
                } catch (Exception e) {
                    textWidth = option.length() * 6;
                }
            }
            int buttonWidth = (int) (textWidth + 12);
            
            if (currentRowWidth + buttonWidth > maxWidth && currentRowWidth > 0) {
                totalRows++;
                currentRowWidth = 0;
            }
            currentRowWidth += buttonWidth + BUTTON_SPACING;
        }
        
        return HEADER_HEIGHT + (totalRows * (BUTTON_HEIGHT + BUTTON_SPACING)) + PADDING;
    }
    
    /**
     * Render the multi-setting component with inline options.
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @param width Component width
     * @param textRenderer Text renderer for drawing text
     * @param mouseX Mouse X coordinate for hover detection
     * @param mouseY Mouse Y coordinate for hover detection
     * @param alpha Parent alpha value (overridden to 1.0 for visibility)
     */
    public void render(int x, int y, int width, MsdfTextRenderer textRenderer, int mouseX, int mouseY, float alpha) {
        MinecraftPlugin plugin = MinecraftPlugin.getInstance();
        plugin.bindMainFramebuffer(true);
        
        this.x = x;
        this.y = y;
        this.width = width;
        this.textRenderer = textRenderer; // Store for later use
        
        // Force full alpha for visibility
        alpha = 1.0f;
        
        // Render header with setting name
        if (textRenderer != null) {
            textRenderer.drawText(x, y + 6, 10, name, RenderColor.of(220, 220, 230, 255));
        }
        
        // Render option buttons inline with wrapping
        int buttonX = x;
        int buttonY = y + HEADER_HEIGHT;
        int maxWidth = width; // Maximum width before wrapping
        int currentRowWidth = 0;
        
        for (String option : options) {
            boolean isSelected = selectedOptions.contains(option);
            
            // Calculate button width based on text
            float textWidth = textRenderer != null ? textRenderer.measureWidth(option, 9) : 40;
            int buttonWidth = (int) (textWidth + 12);
            
            // Check if button needs to wrap to next line
            if (currentRowWidth + buttonWidth > maxWidth && currentRowWidth > 0) {
                buttonX = x;
                buttonY += BUTTON_HEIGHT + BUTTON_SPACING;
                currentRowWidth = 0;
            }
            
            // Check if button is hovered
            boolean hovered = mouseX >= buttonX && mouseX <= buttonX + buttonWidth &&
                            mouseY >= buttonY && mouseY <= buttonY + BUTTON_HEIGHT;
            
            // Button background color
            RenderColor bgColor;
            if (isSelected) {
                bgColor = hovered 
                    ? RenderColor.of(70, 130, 255, 255)
                    : RenderColor.of(60, 120, 245, 230);
            } else {
                bgColor = hovered 
                    ? RenderColor.of(60, 60, 70, 220)
                    : RenderColor.of(50, 50, 60, 200);
            }
            
            RectRenderer.drawRoundedRect(buttonX, buttonY, buttonWidth, BUTTON_HEIGHT, 3, bgColor);
            
            // Button text - опущен еще ниже
            if (textRenderer != null) {
                RenderColor textColor = isSelected 
                    ? RenderColor.WHITE
                    : RenderColor.of(180, 180, 190, 255);
                textRenderer.drawText(buttonX + 6, buttonY + 8, 9, option, textColor);
            }
            
            buttonX += buttonWidth + BUTTON_SPACING;
            currentRowWidth += buttonWidth + BUTTON_SPACING;
        }
    }
    
    /**
     * Handle mouse click on option buttons.
     * 
     * @param mouseX Mouse X coordinate
     * @param mouseY Mouse Y coordinate
     * @param button Mouse button (0 = left click)
     * @return true if click was handled
     */
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (button != 0) return false;
        
        int buttonX = x;
        int buttonY = y + HEADER_HEIGHT;
        int maxWidth = width;
        int currentRowWidth = 0;
        
        for (String option : options) {
            // Calculate button width based on text - need textRenderer for accurate width
            float textWidth = 40; // Default fallback
            if (textRenderer != null) {
                // Create a temporary renderer instance to measure text
                try {
                    textWidth = textRenderer.measureWidth(option, 9);
                } catch (Exception e) {
                    textWidth = option.length() * 6; // Fallback calculation
                }
            }
            int buttonWidth = (int) (textWidth + 12);
            
            // Check if button needs to wrap to next line
            if (currentRowWidth + buttonWidth > maxWidth && currentRowWidth > 0) {
                buttonX = x;
                buttonY += BUTTON_HEIGHT + BUTTON_SPACING;
                currentRowWidth = 0;
            }
            
            if (mouseX >= buttonX && mouseX <= buttonX + buttonWidth &&
                mouseY >= buttonY && mouseY <= buttonY + BUTTON_HEIGHT) {
                toggleOption(option);
                return true;
            }
            
            buttonX += buttonWidth + BUTTON_SPACING;
            currentRowWidth += buttonWidth + BUTTON_SPACING;
        }
        
        return false;
    }
    
    /**
     * Toggle option selection state.
     * 
     * @param option Option to toggle
     */
    private void toggleOption(String option) {
        if (selectedOptions.contains(option)) {
            selectedOptions.remove(option);
        } else {
            selectedOptions.add(option);
        }
    }
    
    /**
     * Set component bounds.
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @param width Component width
     */
    public void setBounds(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }
    
    // Deprecated methods for compatibility
    public boolean isExpanded() { return false; }
    public void setExpanded(boolean expanded) { }
    public boolean isClickOutside(int mouseX, int mouseY) { return false; }
    public void collapse() { }
}
