package ru.ui.clickgui.comp;

import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin;
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import ru.render.MsdfTextRenderer;
import ru.render.RectRenderer;
import ru.render.anim.Animation;
import ru.render.anim.Easings;
import ru.util.math.MathHelper;

public class TextField {
    private String name;
    private String text = "";
    private String placeholder = "";
    private boolean focused = false;
    private int x, y, width, height;
    private int cursorPosition = 0;
    private long lastBlinkTime = 0;
    private boolean cursorVisible = true;
    private Animation focusAnimation = new Animation();
    private Animation hoverAnimation = new Animation();
    
    public TextField(String name, String placeholder) {
        this.name = name;
        this.placeholder = placeholder;
        this.height = 45;
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
        
        focusAnimation.run(focused ? 1.0 : 0.0, 0.3, Easings.CUBIC_OUT, false);
        focusAnimation.update();
        
        float hoverProgress = hoverAnimation.get();
        float focusProgress = focusAnimation.get();

        RenderColor bgColor = RenderColor.of(
            (int) MathHelper.lerp(40, 50, Math.max(hoverProgress, focusProgress)),
            (int) MathHelper.lerp(40, 50, Math.max(hoverProgress, focusProgress)),
            (int) MathHelper.lerp(50, 60, Math.max(hoverProgress, focusProgress)),
            180
        );
        RectRenderer.drawRoundedRect(x, y, width, height, 5, bgColor);

        if (textRenderer != null) {
            textRenderer.drawText(x + 8, y + 6, 11, name, 
                RenderColor.of(180, 180, 190, 255));
        }

        int fieldY = y + 22;
        int fieldHeight = 18;
        
        RenderColor fieldColor = RenderColor.of(30, 30, 38, 200);
        RectRenderer.drawRoundedRect(x + 5, fieldY, width - 10, fieldHeight, 4, fieldColor);

        if (focusProgress > 0.01f) {
            RenderColor borderColor = RenderColor.of(60, 120, 245, (int) (focusProgress * 200));
            RectRenderer.drawRoundedRect(x + 5, fieldY, width - 10, fieldHeight, 4, borderColor);
            RectRenderer.drawRoundedRect(x + 6, fieldY + 1, width - 12, fieldHeight - 2, 3, fieldColor);
        }

        if (textRenderer != null) {
            String displayText = text.isEmpty() ? placeholder : text;
            RenderColor textColor = text.isEmpty() 
                ? RenderColor.of(100, 100, 110, 200) 
                : RenderColor.WHITE;

            float maxWidth = width - 20;
            float textWidth = textRenderer.measureWidth(displayText, 11);
            
            if (textWidth > maxWidth) {
                int startIndex = Math.max(0, displayText.length() - 20);
                displayText = displayText.substring(startIndex);
            }
            
            textRenderer.drawText(x + 10, fieldY + 4, 11, displayText, textColor);

            if (focused && cursorVisible) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastBlinkTime > 500) {
                    cursorVisible = !cursorVisible;
                    lastBlinkTime = currentTime;
                }
                
                float cursorX = x + 10 + textRenderer.measureWidth(
                    text.substring(0, Math.min(cursorPosition, text.length())), 11);
                RenderColor cursorColor = RenderColor.WHITE;
                RectRenderer.drawRoundedRect(cursorX, fieldY + 3, 1, 12, 0.5f, cursorColor);
            } else if (!focused) {
                lastBlinkTime = System.currentTimeMillis();
                cursorVisible = true;
            }
        }
    }
    
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0) {
            boolean wasInside = isHovered(mouseX, mouseY);
            focused = wasInside;
            
            if (focused) {
                cursorPosition = text.length();
            }
            
            return wasInside;
        }
        return false;
    }
    
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused) return false;
        
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!text.isEmpty() && cursorPosition > 0) {
                text = text.substring(0, cursorPosition - 1) + text.substring(cursorPosition);
                cursorPosition--;
            }
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (cursorPosition < text.length()) {
                text = text.substring(0, cursorPosition) + text.substring(cursorPosition + 1);
            }
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_LEFT) {
            if (cursorPosition > 0) {
                cursorPosition--;
            }
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            if (cursorPosition < text.length()) {
                cursorPosition++;
            }
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_HOME) {
            cursorPosition = 0;
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_END) {
            cursorPosition = text.length();
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_ENTER) {
            focused = false;
            return true;
        }
        
        return false;
    }
    
    public boolean charTyped(char chr, int modifiers) {
        if (!focused) return false;

        if (chr >= 32 && chr != 127) {
            text = text.substring(0, cursorPosition) + chr + text.substring(cursorPosition);
            cursorPosition++;
            return true;
        }
        
        return false;
    }
    
    private boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
        cursorPosition = Math.min(cursorPosition, text.length());
    }
    
    public boolean isFocused() {
        return focused;
    }
    
    public void setFocused(boolean focused) {
        this.focused = focused;
    }
}
