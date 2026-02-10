package ru.ui.clickgui.comp;

import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import ru.render.MsdfTextRenderer;
import ru.render.RectRenderer;
import ru.render.anim.Animation;
import ru.render.anim.Easings;
import ru.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

public class MultiButton {
    private String name;
    private List<String> options;
    private List<Boolean> selected;
    private int x, y, width, height;
    private Animation hoverAnimation = new Animation();
    private int hoveredIndex = -1;
    
    public MultiButton(String name, String... options) {
        this.name = name;
        this.options = new ArrayList<>();
        this.selected = new ArrayList<>();
        for (String option : options) {
            this.options.add(option);
            this.selected.add(false);
        }
        this.height = 25 + options.length * 22;
    }
    
    public void render(int x, int y, int width, MsdfTextRenderer textRenderer, int mouseX, int mouseY) {
        this.x = x;
        this.y = y;
        this.width = width;
        
        // Заголовок
        RenderColor headerColor = RenderColor.of(50, 50, 60, 200);
        RectRenderer.drawRoundedRect(x, y, width, 25, 5, headerColor);
        
        if (textRenderer != null) {
            textRenderer.drawText(x + 8, y + 8, 12, name, RenderColor.WHITE);
        }
        
        // Опции
        int optionY = y + 27;
        hoveredIndex = -1;
        
        for (int i = 0; i < options.size(); i++) {
            boolean hovered = mouseX >= x && mouseX <= x + width && 
                            mouseY >= optionY && mouseY <= optionY + 20;
            
            if (hovered) {
                hoveredIndex = i;
            }
            
            hoverAnimation.run(hovered ? 1.0 : 0.0, 0.2, Easings.CUBIC_OUT, false);
            hoverAnimation.update();
            
            float hoverProgress = hovered ? hoverAnimation.get() : 0;
            boolean isSelected = selected.get(i);
            
            // Фон опции
            RenderColor optionColor = RenderColor.of(
                (int) MathHelper.lerp(isSelected ? 60 : 40, isSelected ? 80 : 60, hoverProgress),
                (int) MathHelper.lerp(isSelected ? 120 : 40, isSelected ? 140 : 60, hoverProgress),
                (int) MathHelper.lerp(isSelected ? 245 : 50, isSelected ? 255 : 70, hoverProgress),
                (int) MathHelper.lerp(isSelected ? 200 : 160, isSelected ? 230 : 180, hoverProgress)
            );
            RectRenderer.drawRoundedRect(x + 5, optionY, width - 10, 20, 4, optionColor);
            
            // Чекбокс
            RenderColor checkColor = isSelected 
                ? RenderColor.of(80, 200, 120, 255) 
                : RenderColor.of(100, 100, 110, 200);
            RectRenderer.drawRoundedRect(x + 10, optionY + 5, 10, 10, 2, checkColor);
            
            // Текст опции
            if (textRenderer != null) {
                textRenderer.drawText(x + 25, optionY + 5, 11, options.get(i), RenderColor.WHITE);
            }
            
            optionY += 22;
        }
    }
    
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0 && hoveredIndex >= 0) {
            selected.set(hoveredIndex, !selected.get(hoveredIndex));
            return true;
        }
        return false;
    }
    
    public List<Boolean> getSelected() {
        return new ArrayList<>(selected);
    }
    
    public void setSelected(int index, boolean value) {
        if (index >= 0 && index < selected.size()) {
            selected.set(index, value);
        }
    }
}
