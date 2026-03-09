package ru.utils.render;

import com.ferra13671.cometrenderer.plugins.minecraft.drawer.impl.BasicTextureDrawer;
import com.ferra13671.cometrenderer.plugins.minecraft.drawer.impl.RoundedRectDrawer;
import com.ferra13671.cometrenderer.plugins.minecraft.RectColors;
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import com.ferra13671.gltextureutils.atlas.TextureBorder;
import net.minecraft.client.Minecraft;

public class RectRenderer {
    
    public static void init() {
        // BlurManager is singleton, no need to init here
    }
    
    /**
     * Рисует прямоугольник с blur эффектом используя BlurManager.
     */
    public static void drawRectWithBlur(float x, float y, float width, float height, int color, float cornerRadius) {
        BlurManager blurManager = BlurManager.getInstance();
        
        if (!blurManager.isBlurAvailable()) {
            drawRect(x, y, width, height, color, cornerRadius);
            return;
        }
        
        int blurTexId = blurManager.getBlurredTextureId();
        
        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getWidth();
        int screenHeight = mc.getWindow().getHeight();
        
        float u0 = x / screenWidth;
        float v0 = 1.0f - (y / screenHeight);
        float u1 = (x + width) / screenWidth;
        float v1 = 1.0f - ((y + height) / screenHeight);
        
        TextureBorder border = new TextureBorder(u0, v0, u1, v1);
        
        try (var drawer = new BasicTextureDrawer()
            .setTexture(blurTexId)
            .rectSized(x, y, width, height, border)
            .build()) {
            drawer.tryDraw();
        }
        
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        
        float tintAlpha = Math.max(0.02f, a / 2550.0f);
        RenderColor tint = RenderColor.of(r, g, b, tintAlpha);
        
        try (var drawer = new RoundedRectDrawer()
            .rectSized(x, y, width, height, cornerRadius, RectColors.oneColor(tint))
            .build()) {
            drawer.tryDraw();
        }
    }
    
    /**
     * Рисует обычный прямоугольник.
     */
    public static void drawRect(float x, float y, float width, float height, int color, float cornerRadius) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        
        RenderColor renderColor = RenderColor.of(r, g, b, a);
        
        new RoundedRectDrawer()
            .rectSized(x, y, width, height, cornerRadius, RectColors.oneColor(renderColor))
            .build()
            .tryDraw()
            .close();
    }
    
    /**
     * Alias для совместимости.
     */
    public static void drawRoundedRect(float x, float y, float width, float height, float cornerRadius, int color) {
        drawRect(x, y, width, height, color, cornerRadius);
    }
    
    /**
     * Alias для совместимости с RectColors.
     */
    public static void drawRoundedRect(float x, float y, float width, float height, float cornerRadius, RectColors colors) {
        new RoundedRectDrawer()
            .rectSized(x, y, width, height, cornerRadius, colors)
            .build()
            .tryDraw()
            .close();
    }
    
    /**
     * Alias для совместимости с ClickGuiScreen.
     */
    public static void drawRectangleWithBlur(float x, float y, float width, float height, RenderColor color, float cornerRadius, float blurAmount) {
        float[] rgba = color.getColor();
        int colorInt = ((int)(rgba[3] * 255) << 24) | ((int)(rgba[0] * 255) << 16) | ((int)(rgba[1] * 255) << 8) | (int)(rgba[2] * 255);
        drawRectWithBlur(x, y, width, height, colorInt, cornerRadius);
    }
    
    /**
     * Overload для float,float,float,float,float,RenderColor.
     */
    public static void drawRoundedRect(float x, float y, float width, float height, float cornerRadius, RenderColor color) {
        drawRect(x, y, width, height, colorToInt(color), cornerRadius);
    }
    
    /**
     * Overload для int параметров.
     */
    public static void drawRoundedRect(int x, int y, int width, int height, float cornerRadius, RenderColor color) {
        drawRect((float)x, (float)y, (float)width, (float)height, colorToInt(color), cornerRadius);
    }
    
    /**
     * Overload для int параметров.
     */
    public static void drawRoundedRect(int x, int y, int width, int height, int cornerRadius, RenderColor color) {
        drawRect((float)x, (float)y, (float)width, (float)height, colorToInt(color), (float)cornerRadius);
    }
    
    /**
     * Overload для mixed параметров.
     */
    public static void drawRoundedRect(float x, float y, int width, int height, float cornerRadius, RenderColor color) {
        drawRect(x, y, (float)width, (float)height, colorToInt(color), cornerRadius);
    }
    
    /**
     * Overload для mixed параметров.
     */
    public static void drawRoundedRect(int x, int y, float width, int height, float cornerRadius, RenderColor color) {
        drawRect((float)x, y, width, (float)height, colorToInt(color), cornerRadius);
    }
    
    /**
     * Overload для mixed параметров.
     */
    public static void drawRoundedRect(float x, int y, int width, int height, float cornerRadius, RenderColor color) {
        drawRect(x, (float)y, (float)width, (float)height, colorToInt(color), cornerRadius);
    }
    
    private static int colorToInt(RenderColor color) {
        float[] rgba = color.getColor();
        return ((int)(rgba[3] * 255) << 24) | ((int)(rgba[0] * 255) << 16) | ((int)(rgba[1] * 255) << 8) | (int)(rgba[2] * 255);
    }
    
    public static void setBlurRadius(float radius) {
        BlurManager.getInstance().setBlurRadius(radius);
    }
    
    public static void setBlurIterations(int iterations) {
        BlurManager.getInstance().setBlurIterations(iterations);
    }
    
    public static void setBlurOffset(float offset) {
        BlurManager.getInstance().setBlurOffset(offset);
    }
    
    public static void cleanup() {
        BlurManager.getInstance().cleanup();
    }
}
