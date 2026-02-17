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
        this.x = x;
        this.y = y;
        this.width = width;
        
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

        float percentage = (value - min) / (max - min);
        float barWidth = width * percentage;
        
        RenderColor barColor = RenderColor.of(60, 120, 245, 200);
        if (barWidth > 0) {
            RectRenderer.drawRoundedRect(x, y + height - 8, barWidth, 3, 1.5f, barColor);
        }
    }
    
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0 && isHovered(mouseX, mouseY)) {
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
        value = min + percentage * (max - min);
        value = (float) MathHelper.round(value, 1);
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
    
    // For testing purposes - allows setting bounds without rendering
    public void setBounds(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }
}
