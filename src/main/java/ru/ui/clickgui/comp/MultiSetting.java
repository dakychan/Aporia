package ru.ui.clickgui.comp;

import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin;
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import ru.render.MsdfTextRenderer;
import ru.render.RectRenderer;
import ru.render.anim.Animation;
import ru.render.anim.Easings;

import java.util.List;

public class MultiSetting {
    private String name;
    private List<String> options;
    private List<String> selectedOptions;
    private int x, y, width, height;

    private boolean expanded = false;
    private Animation expandAnimation = new Animation();
    private float dropdownHeight = 0;
    
    private static final int HEADER_HEIGHT = 25;
    private static final int OPTION_HEIGHT = 25;
    
    public MultiSetting(String name, List<String> options, List<String> selectedOptions) {
        this.name = name;
        this.options = options;
        this.selectedOptions = selectedOptions;
        this.height = HEADER_HEIGHT;
    }
    
    public int getHeight() {
        if (expanded && dropdownHeight < options.size() * OPTION_HEIGHT * 0.5f) {
            return HEADER_HEIGHT + options.size() * OPTION_HEIGHT;
        }
        return (int) (HEADER_HEIGHT + dropdownHeight);
    }
    
    public boolean isExpanded() {
        return expanded;
    }
    
    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }
    
    /**
     * Render the multi-setting component with all options.
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
        
        /**
         * Force full alpha for visibility regardless of parent animation state.
         * This ensures MultiSetting components remain visible even when parent
         * containers are animating or have reduced opacity.
         * Validates Requirements 6.1, 6.2, 6.3.
         */
        alpha = 1.0f;

        expandAnimation.run(expanded ? 1.0 : 0.0, 0.2, Easings.CUBIC_OUT, false);
        expandAnimation.update();
        float animProgress = expandAnimation.get();

        dropdownHeight = animProgress * options.size() * OPTION_HEIGHT;

        renderHeader(textRenderer, mouseX, mouseY, alpha);

        if (animProgress > 0.01f) {
            renderOptions(textRenderer, alpha, animProgress);
        }
    }
    
    private void renderHeader(MsdfTextRenderer textRenderer, int mouseX, int mouseY, float alpha) {
        boolean hovered = isHeaderHovered(mouseX, mouseY);
        RenderColor bgColor = hovered 
            ? RenderColor.of(60, 60, 70, 220)
            : RenderColor.of(50, 50, 60, 200);
        
        RectRenderer.drawRoundedRect(x, y, width, HEADER_HEIGHT, 4, bgColor);

        if (textRenderer != null) {
            textRenderer.drawText(x + 10, y + 8, 12, name, 
                RenderColor.of(220, 220, 230, 255));
        }

        if (textRenderer != null) {
            String arrow = expanded ? "▼" : "▶";
            float arrowWidth = textRenderer.measureWidth(arrow, 12);
            textRenderer.drawText(x + width - arrowWidth - 10, y + 8, 12, 
                arrow, RenderColor.of(100, 200, 255, (int)(255 * alpha)));
        }
    }
    
    /**
     * Render multi-setting options with full opacity.
     * Uses full alpha values to ensure options are clearly visible
     * regardless of animation state.
     * 
     * @param textRenderer Text renderer for drawing text
     * @param alpha Parent alpha value (used at full opacity)
     * @param animProgress Animation progress for height calculation
     */
    private void renderOptions(MsdfTextRenderer textRenderer, float alpha, float animProgress) {
        int optionY = y + HEADER_HEIGHT;
        
        for (int i = 0; i < options.size(); i++) {
            String option = options.get(i);
            boolean isSelected = selectedOptions.contains(option);
            
            /**
             * Use full alpha for option rendering to ensure clear visibility.
             * Animation progress affects height/position only, not opacity.
             * Validates Requirements 6.1, 6.2, 6.3.
             */

            if (isSelected) {
                RectRenderer.drawRoundedRect(x + 5, optionY, width - 10, OPTION_HEIGHT - 2, 3,
                    RenderColor.of(40, 80, 40, 150));
            }

            drawCheckbox(x + 15, optionY + 7, isSelected, alpha);

            if (textRenderer != null) {
                RenderColor textColor = isSelected 
                    ? RenderColor.of(80, 200, 120, 255)
                    : RenderColor.of(150, 150, 160, 255);
                
                textRenderer.drawText(x + 35, optionY + 8, 12, option, textColor);
            }
            
            optionY += OPTION_HEIGHT;
        }
    }
    
    /**
     * Draw checkbox with full opacity for clear visibility.
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @param checked Whether checkbox is checked
     * @param alpha Parent alpha value (used at full opacity)
     */
    private void drawCheckbox(int x, int y, boolean checked, float alpha) {
        int size = 10;
        
        /**
         * Use full alpha for checkbox rendering to ensure clear visibility.
         * Validates Requirements 6.1, 6.2, 6.3.
         */

        RenderColor borderColor = checked 
            ? RenderColor.of(80, 200, 120, 255)
            : RenderColor.of(100, 100, 110, 200);
        
        RectRenderer.drawRoundedRect(x, y, size, size, 2, borderColor);

        if (checked) {
            RectRenderer.drawRoundedRect(x + 2, y + 2, size - 4, size - 4, 1,
                RenderColor.of(80, 200, 120, 255));
        }
    }
    
    private boolean isHeaderHovered(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && 
               mouseY >= y && mouseY <= y + HEADER_HEIGHT;
    }
    
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0 && isHeaderHovered(mouseX, mouseY)) {
            expanded = !expanded;
            return true;
        }

        if (button == 0 && expanded) {
            int optionY = y + HEADER_HEIGHT;
            for (String option : options) {
                if (mouseX >= x && mouseX <= x + width &&
                    mouseY >= optionY && mouseY <= optionY + OPTION_HEIGHT) {
                    toggleOption(option);
                    return true;
                }
                optionY += OPTION_HEIGHT;
            }
        }
        
        return false;
    }
    
    private void toggleOption(String option) {
        if (selectedOptions.contains(option)) {
            selectedOptions.remove(option);
        } else {
            selectedOptions.add(option);
        }
    }
    
    public boolean isClickOutside(int mouseX, int mouseY) {
        if (!expanded) {
            return false;
        }
        
        int totalHeight = HEADER_HEIGHT + (int) dropdownHeight;
        return !(mouseX >= x && mouseX <= x + width && 
                 mouseY >= y && mouseY <= y + totalHeight);
    }
    
    public void collapse() {
        expanded = false;
    }

    public void setBounds(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.width = width;
        if (expanded) {
            dropdownHeight = options.size() * OPTION_HEIGHT;
        }
    }
}
