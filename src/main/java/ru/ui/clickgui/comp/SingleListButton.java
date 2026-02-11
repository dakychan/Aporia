package ru.ui.clickgui.comp;

import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin;
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import ru.render.MsdfTextRenderer;
import ru.render.RectRenderer;
import ru.render.anim.Animation;
import ru.render.anim.Easings;
import ru.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

public class SingleListButton {
    private String name;
    private List<String> options;
    private int selectedIndex = 0;
    private boolean expanded = false;
    private int x, y, width, height;
    private Animation expandAnimation = new Animation();
    private Animation hoverAnimation = new Animation();
    private int hoveredIndex = -1;

    private static final String ARROW_DOWN = "V";
    
    public SingleListButton(String name, String... options) {
        this.name = name;
        this.options = new ArrayList<>();
        for (String option : options) {
            this.options.add(option);
        }
        this.height = 25;
    }
    
    public void render(int x, int y, int width, MsdfTextRenderer textRenderer, int mouseX, int mouseY) {
        MinecraftPlugin plugin = MinecraftPlugin.getInstance();
        plugin.bindMainFramebuffer(true);
        this.x = x;
        this.y = y;
        this.width = width;
        
        expandAnimation.run(expanded ? 1.0 : 0.0, 0.3, Easings.CUBIC_OUT, false);
        expandAnimation.update();
        
        float expandProgress = expandAnimation.get();

        boolean headerHovered = mouseX >= x && mouseX <= x + width && 
                               mouseY >= y && mouseY <= y + 25;
        
        hoverAnimation.run(headerHovered ? 1.0 : 0.0, 0.2, Easings.CUBIC_OUT, false);
        hoverAnimation.update();
        
        float hoverProgress = hoverAnimation.get();
        
        RenderColor headerColor = RenderColor.of(
            (int) MathHelper.lerp(50, 60, hoverProgress),
            (int) MathHelper.lerp(50, 60, hoverProgress),
            (int) MathHelper.lerp(60, 70, hoverProgress),
            200
        );
        RectRenderer.drawRoundedRect(x, y, width, 25, 5, headerColor);

        if (textRenderer != null) {
            textRenderer.drawText(x + 8, y + 8, 12, name, RenderColor.WHITE);
            
            String selectedText = options.get(selectedIndex);
            float selectedWidth = textRenderer.measureWidth(selectedText, 11);
            textRenderer.drawText(x + width - selectedWidth - 25, y + 8, 11, selectedText, 
                RenderColor.of(180, 180, 190, 255));
            textRenderer.drawText(x + width - 20, y + 8, 12, ARROW_DOWN, 
                RenderColor.of(180, 180, 190, 255));
        }
        

        if (expandProgress > 0.01f) {
            int listHeight = (int) (options.size() * 22 * expandProgress);
            int optionY = y + 27;
            hoveredIndex = -1;
            
            for (int i = 0; i < options.size(); i++) {
                float itemAlpha = Math.min(1.0f, expandProgress * 2 - i * 0.1f);
                if (itemAlpha <= 0) continue;
                
                boolean hovered = mouseX >= x && mouseX <= x + width && 
                                mouseY >= optionY && mouseY <= optionY + 20;
                
                if (hovered && expanded) {
                    hoveredIndex = i;
                }
                
                boolean isSelected = i == selectedIndex;

                RenderColor optionColor = RenderColor.of(
                    isSelected ? 60 : (hovered ? 50 : 40),
                    isSelected ? 120 : (hovered ? 50 : 40),
                    isSelected ? 245 : (hovered ? 60 : 50),
                    (int) (itemAlpha * (isSelected ? 200 : 160))
                );
                RectRenderer.drawRoundedRect(x + 5, optionY, width - 10, 20, 4, optionColor);
                if (isSelected) {
                    RenderColor indicatorColor = RenderColor.of(80, 200, 120, (int) (itemAlpha * 255));
                    RectRenderer.drawRoundedRect(x + 8, optionY + 7, 3, 6, 1.5f, indicatorColor);
                }
                if (textRenderer != null) {
                    RenderColor textColor = RenderColor.of(255, 255, 255, (int) (itemAlpha * 255));
                    textRenderer.drawText(x + 15, optionY + 5, 11, options.get(i), textColor);
                }
                optionY += 22;
            }
            
            this.height = (int) (25 + listHeight + 2);
        } else {
            this.height = 25;
        }
    }
    
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (button != 0) return false;

        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 25) {
            expanded = !expanded;
            return true;
        }

        if (expanded && hoveredIndex >= 0) {
            selectedIndex = hoveredIndex;
            expanded = false;
            return true;
        }
        
        return false;
    }
    
    public int getHeight() {
        return height;
    }
    
    public int getSelectedIndex() {
        return selectedIndex;
    }
    
    public String getSelectedValue() {
        return options.get(selectedIndex);
    }
    
    public void setSelectedIndex(int index) {
        if (index >= 0 && index < options.size()) {
            selectedIndex = index;
        }
    }
}
