package ru.ui.clickgui.comp;

import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import ru.render.MsdfTextRenderer;
import ru.render.RectRenderer;
import ru.render.anim.Animation;
import ru.render.anim.Easings;
import ru.util.math.MathHelper;

public class FuncButton {
    private String name;
    private int x, y, width, height;
    private Animation hoverAnimation = new Animation();
    private Runnable action;
    
    public FuncButton(String name, Runnable action) {
        this.name = name;
        this.action = action;
        this.height = 25;
    }
    
    public void render(int x, int y, int width, MsdfTextRenderer textRenderer, int mouseX, int mouseY) {
        this.x = x;
        this.y = y;
        this.width = width;
        
        boolean hovered = isHovered(mouseX, mouseY);
        hoverAnimation.run(hovered ? 1.0 : 0.0, 0.2, Easings.CUBIC_OUT, false);
        hoverAnimation.update();
        
        float hoverProgress = hoverAnimation.get();
        
        // Фон
        RenderColor bgColor = RenderColor.of(
            (int) MathHelper.lerp(50, 70, hoverProgress),
            (int) MathHelper.lerp(50, 70, hoverProgress),
            (int) MathHelper.lerp(60, 80, hoverProgress),
            (int) MathHelper.lerp(180, 220, hoverProgress)
        );
        RectRenderer.drawRoundedRect(x, y, width, height, 5, bgColor);
        
        // Текст по центру
        if (textRenderer != null) {
            float textWidth = textRenderer.measureWidth(name, 12);
            textRenderer.drawText(x + (width - textWidth) / 2, y + 8, 12, name, RenderColor.WHITE);
        }
    }
    
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0 && isHovered(mouseX, mouseY)) {
            if (action != null) {
                action.run();
            }
            return true;
        }
        return false;
    }
    
    private boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
