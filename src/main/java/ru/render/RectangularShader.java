package ru.render;

import com.ferra13671.cometrenderer.plugins.minecraft.RectColors;
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import com.ferra13671.cometrenderer.plugins.minecraft.drawer.impl.RoundedRectDrawer;

public class RectangularShader {
    private final BlurShader blurShader;
    
    public RectangularShader() {
        this.blurShader = new BlurShader();
    }
    
    public void drawRectangle(float x, float y, float width, float height, RenderColor color, float cornerRadius) {
        new RoundedRectDrawer()
                .rectSized(x, y, width, height, cornerRadius, RectColors.oneColor(color))
                .build()
                .tryDraw()
                .close();
    }
    
    public void drawRectangle(float x, float y, float width, float height, int color, float cornerRadius) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        drawRectangle(x, y, width, height, RenderColor.of(r, g, b, a), cornerRadius);
    }
    
    public void drawRectangleWithBlur(float x, float y, float width, float height, RenderColor color, float cornerRadius, float blurAmount) {
        blurShader.setBlurRadius(blurAmount);
        drawRectangle(x, y, width, height, color, cornerRadius);
    }
    
    public void drawRectangleWithBlur(float x, float y, float width, float height, int color, float cornerRadius, float blurAmount) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        drawRectangleWithBlur(x, y, width, height, RenderColor.of(r, g, b, a), cornerRadius, blurAmount);
    }
    
    public void cleanup() {
        blurShader.cleanup();
    }
}
