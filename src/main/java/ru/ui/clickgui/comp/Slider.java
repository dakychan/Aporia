package ru.ui.clickgui.comp;

import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin;
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import ru.render.MsdfTextRenderer;
import ru.render.RectRenderer;
import ru.render.anim.Animation;
import ru.render.anim.Easings;
import ru.util.math.MathHelper;

public class Slider {
    private String name;
    private float value;
    private float min;
    private float max;
    private int x, y, width, height;
    private boolean dragging = false;
    private Animation hoverAnimation = new Animation();
    
    public Slider(String name, float value, float min, float max) {
        this.name = name;
        this.value = value;
        this.min = min;
        this.max = max;
        this.height = 25;
    }
    
    public void render(int x, int y, int width, MsdfTextRenderer textRenderer, int mouseX, int mouseY) {
        MinecraftPlugin plugin = MinecraftPlugin.getInstance();
        plugin.bindMainFramebuffer(true);
        
        // Only update bounds if not currently dragging to prevent coordinate desync
        if (!dragging) {
            this.x = x;
            this.y = y;
            this.width = width;
        }
        
        boolean hovered = isHovered(mouseX, mouseY);
        hoverAnimation.run(hovered ? 1.0 : 0.0, 0.2, Easings.CUBIC_OUT, false);
        hoverAnimation.update();
        
        float hoverProgress = hoverAnimation.get();

        if (textRenderer != null) {
            textRenderer.drawText(x, y + 6, 12, name, RenderColor.WHITE);
        }

        if (textRenderer != null) {
            String valueText = String.format("%.1f", value);
            float textWidth = textRenderer.measureWidth(valueText, 12);
            textRenderer.drawText(x + width - textWidth, y + 6, 12, valueText, 
                RenderColor.of(180, 180, 190, 255));
        }

        // Draw slider track (background)
        RenderColor trackColor = RenderColor.of(40, 40, 50, 255);
        RectRenderer.drawRoundedRect(x, y + height - 8, width, 6, 3f, trackColor);
        
        // Draw slider fill (progress)
        float percentage = (value - min) / (max - min);
        float barWidth = width * percentage;
        
        RenderColor barColor = RenderColor.of(60, 120, 245, 255);
        if (barWidth > 2) {
            RectRenderer.drawRoundedRect(x, y + height - 8, barWidth, 6, 3f, barColor);
        }
        
        // Draw slider handle (knob) - bigger and more visible
        float handleX = x + barWidth - 6;
        float handleY = y + height - 11;
        RenderColor handleColor = hovered || dragging 
            ? RenderColor.of(100, 160, 255, 255)
            : RenderColor.of(80, 140, 255, 255);
        RectRenderer.drawRoundedRect(handleX, handleY, 12, 12, 6f, handleColor);
    }
    
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        boolean hovered = isHovered(mouseX, mouseY);
        
        if (button == 0 && hovered) {
            dragging = true;
            updateValue(mouseX);
            return true;
        }
        return false;
    }
    
    public boolean mouseReleased(int mouseX, int mouseY, int button) {
        if (button == 0 && dragging) {
            dragging = false;
            return true;
        }
        return false;
    }
    
    public void mouseDragged(int mouseX, int mouseY) {
        if (dragging) {
            updateValue(mouseX);
        }
    }
    
    private void updateValue(int mouseX) {
        float percentage = MathHelper.clamp((float) (mouseX - x) / width, 0, 1);
        float newValue = min + percentage * (max - min);
        newValue = (float) MathHelper.round(newValue, 1);
        
        if (Math.abs(newValue - value) > 0.01f) {
            value = newValue;
        }
    }
    
    private boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
    
    public float getValue() {
        return value;
    }
    
    public void setValue(float value) {
        this.value = MathHelper.clamp(value, min, max);
    }
    
    public boolean isDragging() {
        return dragging;
    }
    
    // For testing purposes - allows setting bounds without rendering
    public void setBounds(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }
}
