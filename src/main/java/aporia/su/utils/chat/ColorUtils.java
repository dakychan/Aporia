package aporia.su.utils.chat;

import java.awt.Color;

/**
 * Утилиты для работы с цветами и градиентами.
 * 
 * @author Aporia
 */
public class ColorUtils {
    
    public static int red(int c) {
        return (c >> 16) & 0xFF;
    }
    
    public static int green(int c) {
        return (c >> 8) & 0xFF;
    }
    
    public static int blue(int c) {
        return c & 0xFF;
    }
    
    public static int alpha(int c) {
        return (c >> 24) & 0xFF;
    }
    
    public static int rgba(int red, int green, int blue, int alpha) {
        return ((clamp(alpha, 0, 255) << 24) |
                (clamp(red, 0, 255) << 16) |
                (clamp(green, 0, 255) << 8) |
                clamp(blue, 0, 255));
    }
    
    public static int toColor(String hexColor) {
        int argb = Integer.parseInt(hexColor.substring(1), 16);
        return setAlpha(argb, 255);
    }
    
    public static int setAlpha(int color, int alpha) {
        return (color & 0x00ffffff) | (alpha << 24);
    }
    
    public static int interpolateColor(int color1, int color2, float amount) {
        amount = Math.min(1, Math.max(0, amount));
        
        int r1 = red(color1), g1 = green(color1), b1 = blue(color1), a1 = alpha(color1);
        int r2 = red(color2), g2 = green(color2), b2 = blue(color2), a2 = alpha(color2);
        
        int r = (int) (r1 + (r2 - r1) * amount);
        int g = (int) (g1 + (g2 - g1) * amount);
        int b = (int) (b1 + (b2 - b1) * amount);
        int a = (int) (a1 + (a2 - a1) * amount);
        
        return rgba(r, g, b, a);
    }
    
    public static int astolfo(int speed, int index, float saturation, float brightness, float alpha) {
        float hueStep = 360.0f / 4.0f;
        long currentTime = System.currentTimeMillis();
        float baseHue = (float) ((currentTime / speed + index) % 360L);
        float hue = (baseHue + index * hueStep) % 360.0f;
        hue = hue / 360.0f;
        
        saturation = clamp(saturation, 0.0f, 1.0f);
        brightness = clamp(brightness, 0.0f, 1.0f);
        
        int rgb = Color.HSBtoRGB(hue, saturation, brightness);
        int alphaInt = Math.max(0, Math.min(255, (int) (alpha * 255.0F)));
        
        return (alphaInt << 24) | (rgb & 0x00ffffff);
    }
    
    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
    
    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
