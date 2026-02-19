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

        RenderColor trackColor = RenderColor.of(40, 40, 50, 255);
        RectRenderer.drawRoundedRect(x, y + height - 8, width, 6, 3f, trackColor);

        float percentage = (value - min) / (max - min);
        float barWidth = width * percentage;
        
        RenderColor barColor = RenderColor.of(60, 120, 245, 255);
        if (barWidth > 2) {
            RectRenderer.drawRoundedRect(x, y + height - 8, barWidth, 6, 3f, barColor);
        }

        float handleX = x + barWidth - 6;
        float handleY = y + height - 11;
        RenderColor handleColor = hovered || dragging 
            ? RenderColor.of(100, 160, 255, 255)
            : RenderColor.of(80, 140, 255, 255);
        RectRenderer.drawRoundedRect(handleX, handleY, 12, 12, 6f, handleColor);
    }
    
    /**
     * Handle mouse click with precise hit detection.
     * Only captures clicks on the slider circle (handle) or slider line (track).
     * 
     * @param mouseX Mouse X coordinate
     * @param mouseY Mouse Y coordinate
     * @param button Mouse button
     * @return true if click was handled
     */
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (button != 0) return false;
        
        boolean circleHit = isCircleHovered(mouseX, mouseY);
        boolean lineHit = isLineHovered(mouseX, mouseY);
        
        if (circleHit || lineHit) {
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
    
    /**
     * Check if mouse is over the slider circle (handle).
     * Uses radius-based distance calculation for precise hit detection.
     * 
     * @param mouseX Mouse X coordinate
     * @param mouseY Mouse Y coordinate
     * @return true if mouse is over the circle
     */
    private boolean isCircleHovered(int mouseX, int mouseY) {
        float percentage = (value - min) / (max - min);
        float barWidth = width * percentage;

        float handleX = x + barWidth - 6 + 6;
        float handleY = y + height - 11 + 6;
        float handleRadius = 6;
        
        float dx = mouseX - handleX;
        float dy = mouseY - handleY;
        float distanceSquared = dx * dx + dy * dy;
        
        return distanceSquared <= handleRadius * handleRadius;
    }
    
    /**
     * Check if mouse is over the slider line (track).
     * Checks if click is within the track rectangle bounds.
     * 
     * @param mouseX Mouse X coordinate
     * @param mouseY Mouse Y coordinate
     * @return true if mouse is over the line
     */
    private boolean isLineHovered(int mouseX, int mouseY) {
        int lineY = y + height - 8;
        int lineHeight = 6;
        
        return mouseX >= x && mouseX <= x + width &&
               mouseY >= lineY && mouseY <= lineY + lineHeight;
    }
    
    private boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
    
    public float getValue() {
        return value;
    }
    
    /**
     * Update slider value from external source.
     * Only applies when not actively dragging to prevent external updates
     * from overwriting user drag operations.
     * 
     * @param value New value
     */
    public void setValue(float value) {
        if (!dragging) {
            this.value = MathHelper.clamp(value, min, max);
        }
    }
    
    public boolean isDragging() {
        return dragging;
    }

    public void setBounds(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }
}
