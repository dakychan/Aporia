package ru.ui.clickgui.comp;

import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import ru.render.MsdfTextRenderer;
import ru.render.RectRenderer;
import ru.render.anim.Animation;
import ru.render.anim.Easings;
import ru.util.math.MathHelper;

public class Button {
    private String name;
    private boolean enabled;
    private int x, y, width, height;
    private Animation hoverAnimation = new Animation();
    private Animation toggleAnimation = new Animation();
    private Runnable onClick;
    
    public Button(String name, boolean enabled) {
        this.name = name;
        this.enabled = enabled;
        this.height = 25;
    }
    
    public void setOnClick(Runnable onClick) {
        this.onClick = onClick;
    }
    
    public void render(int x, int y, int width, MsdfTextRenderer textRenderer, int mouseX, int mouseY) {
        this.x = x;
        this.y = y;
        this.width = width;
        
        boolean hovered = isHovered(mouseX, mouseY);
        hoverAnimation.run(hovered ? 1.0 : 0.0, 0.2, Easings.CUBIC_OUT, false);
        hoverAnimation.update();
        
        toggleAnimation.run(enabled ? 1.0 : 0.0, 0.3, Easings.CUBIC_OUT, false);
        toggleAnimation.update();
        
        float hoverProgress = hoverAnimation.get();
        float toggleProgress = toggleAnimation.get();
        
        // Фон
        RenderColor bgColor = RenderColor.of(
            (int) MathHelper.lerp(40, 60, toggleProgress),
            (int) MathHelper.lerp(40, 120, toggleProgress),
            (int) MathHelper.lerp(50, 245, toggleProgress),
            (int) MathHelper.lerp(180, 230, hoverProgress)
        );
        RectRenderer.drawRoundedRect(x, y, width, height, 5, bgColor);
        
        // Текст названия
        if (textRenderer != null) {
            textRenderer.drawText(x + 8, y + 8, 12, name, RenderColor.WHITE);
        }
        
        // Статус справа
        if (textRenderer != null) {
            String status = enabled ? "Да" : "Нет";
            RenderColor statusColor = enabled 
                ? RenderColor.of(80, 200, 120, 255) 
                : RenderColor.of(180, 180, 190, 255);
            float textWidth = textRenderer.measureWidth(status, 12);
            textRenderer.drawText(x + width - textWidth - 8, y + 8, 12, status, statusColor);
        }
    }
    
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0 && isHovered(mouseX, mouseY)) {
            enabled = !enabled;
            if (onClick != null) {
                onClick.run();
            }
            return true;
        }
        return false;
    }
    
    private boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
