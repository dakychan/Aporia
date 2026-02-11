package ru.render;

import com.ferra13671.cometrenderer.plugins.minecraft.RectColors;
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import com.ferra13671.cometrenderer.plugins.minecraft.drawer.impl.RoundedRectDrawer;

public class RectRenderer {
    private static RectangularShader rectangularShader;
    
    private static void ensureInitialized() {
        if (rectangularShader == null) {
            rectangularShader = new RectangularShader();
        }
    }
    
    public static void drawRoundedRect(float x, float y, float width, float height, float radius, RenderColor color) {
        new RoundedRectDrawer()
                .rectSized(x, y, width, height, radius, RectColors.oneColor(color))
                .build()
                .tryDraw()
                .close();
    }
    
    public static void drawRoundedRect(float x, float y, float width, float height, float radius, RectColors colors) {
        new RoundedRectDrawer()
                .rectSized(x, y, width, height, radius, colors)
                .build()
                .tryDraw()
                .close();
    }
    
    public static void drawRectangle(float x, float y, float width, float height, int color, float cornerRadius) {
        ensureInitialized();
        rectangularShader.drawRectangle(x, y, width, height, color, cornerRadius);
    }
    
    public static void drawRectangle(float x, float y, float width, float height, RenderColor color, float cornerRadius) {
        ensureInitialized();
        rectangularShader.drawRectangle(x, y, width, height, color, cornerRadius);
    }
    
    public static void drawRectangleWithBlur(float x, float y, float width, float height, int color, float cornerRadius, float blurAmount) {
        ensureInitialized();
        rectangularShader.drawRectangleWithBlur(x, y, width, height, color, cornerRadius, blurAmount);
    }
    
    public static void drawRectangleWithBlur(float x, float y, float width, float height, RenderColor color, float cornerRadius, float blurAmount) {
        ensureInitialized();
        rectangularShader.drawRectangleWithBlur(x, y, width, height, color, cornerRadius, blurAmount);
    }
    
    public static void cleanup() {
        if (rectangularShader != null) {
            rectangularShader.cleanup();
            rectangularShader = null;
        }
    }
}
