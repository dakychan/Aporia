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
    
    // State fields
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
        return (int) (HEADER_HEIGHT + dropdownHeight);
    }
    
    public boolean isExpanded() {
        return expanded;
    }
    
    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }
    
    public void render(int x, int y, int width, MsdfTextRenderer textRenderer, int mouseX, int mouseY, float alpha) {
        MinecraftPlugin plugin = MinecraftPlugin.getInstance();
        plugin.bindMainFramebuffer(true);
        this.x = x;
        this.y = y;
        this.width = width;
        
        // Update animation
        expandAnimation.run(expanded ? 1.0 : 0.0, 0.2, Easings.CUBIC_OUT, false);
        expandAnimation.update();
        float animProgress = expandAnimation.get();
        
        // Calculate dropdown height based on animation
        dropdownHeight = animProgress * options.size() * OPTION_HEIGHT;
        
        // Render collapsed state (header)
        renderHeader(textRenderer, mouseX, mouseY, alpha);
        
        // Render expanded state (options)
        if (animProgress > 0.01f) {
            renderOptions(textRenderer, alpha, animProgress);
        }
    }
    
    private void renderHeader(MsdfTextRenderer textRenderer, int mouseX, int mouseY, float alpha) {
        // Draw header background
        boolean hovered = isHeaderHovered(mouseX, mouseY);
        RenderColor bgColor = hovered 
            ? RenderColor.of(35, 35, 40, (int)(180 * alpha))
            : RenderColor.of(30, 30, 35, (int)(150 * alpha));
        
        RectRenderer.drawRoundedRect(x, y, width, HEADER_HEIGHT, 4, bgColor);
        
        // Draw setting name
        if (textRenderer != null) {
            textRenderer.drawText(x + 10, y + 8, 12, name, 
                RenderColor.of(200, 200, 210, (int)(255 * alpha)));
        }
        
        // Draw arrow indicator
        if (textRenderer != null) {
            String arrow = expanded ? "▼" : "▶";
            float arrowWidth = textRenderer.measureWidth(arrow, 12);
            textRenderer.drawText(x + width - arrowWidth - 10, y + 8, 12, 
                arrow, RenderColor.of(100, 200, 255, (int)(255 * alpha)));
        }
    }
    
    private void renderOptions(MsdfTextRenderer textRenderer, float alpha, float animProgress) {
        int optionY = y + HEADER_HEIGHT;
        
        for (int i = 0; i < options.size(); i++) {
            String option = options.get(i);
            boolean isSelected = selectedOptions.contains(option);
            
            // Calculate option alpha based on animation progress
            float optionAlpha = alpha * animProgress;
            
            // Draw option background if selected
            if (isSelected) {
                RectRenderer.drawRoundedRect(x + 5, optionY, width - 10, OPTION_HEIGHT - 2, 3,
                    RenderColor.of(40, 80, 40, (int)(150 * optionAlpha)));
            }
            
            // Draw checkbox
            drawCheckbox(x + 15, optionY + 7, isSelected, optionAlpha);
            
            // Draw option text
            if (textRenderer != null) {
                RenderColor textColor = isSelected 
                    ? RenderColor.of(80, 200, 120, (int)(255 * optionAlpha))
                    : RenderColor.of(150, 150, 160, (int)(255 * optionAlpha));
                
                textRenderer.drawText(x + 35, optionY + 8, 12, option, textColor);
            }
            
            optionY += OPTION_HEIGHT;
        }
    }
    
    private void drawCheckbox(int x, int y, boolean checked, float alpha) {
        int size = 10;
        
        // Draw checkbox border
        RenderColor borderColor = checked 
            ? RenderColor.of(80, 200, 120, (int)(255 * alpha))
            : RenderColor.of(100, 100, 110, (int)(200 * alpha));
        
        RectRenderer.drawRoundedRect(x, y, size, size, 2, borderColor);
        
        // Draw checkmark if checked
        if (checked) {
            RectRenderer.drawRoundedRect(x + 2, y + 2, size - 4, size - 4, 1,
                RenderColor.of(80, 200, 120, (int)(255 * alpha)));
        }
    }
    
    private boolean isHeaderHovered(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && 
               mouseY >= y && mouseY <= y + HEADER_HEIGHT;
    }
    
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0 && isHeaderHovered(mouseX, mouseY)) {
            // Toggle expanded state
            expanded = !expanded;
            return true;
        }
        
        // Check if clicking on an option checkbox
        if (button == 0 && expanded) {
            int optionY = y + HEADER_HEIGHT;
            for (String option : options) {
                if (mouseX >= x && mouseX <= x + width &&
                    mouseY >= optionY && mouseY <= optionY + OPTION_HEIGHT) {
                    // Toggle option selection
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
    
    // For testing purposes - allows setting bounds without rendering
    public void setBounds(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.width = width;
        // Update dropdown height for expanded state
        if (expanded) {
            dropdownHeight = options.size() * OPTION_HEIGHT;
        }
    }
}
