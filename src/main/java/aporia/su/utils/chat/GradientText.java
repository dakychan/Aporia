package aporia.su.utils.chat;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Система градиентов для текста в чате.
 * Поддерживает различные стили градиентов.
 * 
 * @author Aporia
 */
public class GradientText {
    
    public enum GradientStyle {
        HALF_SPLIT,
        FULL_GRADIENT,
        ASTOLFO,
        TWO_COLOR_FADE
    }
    
    // Предустановленные цвета
    public static final int RED = new java.awt.Color(255, 64, 64).getRGB();
    public static final int GREEN = new java.awt.Color(64, 255, 64).getRGB();
    public static final int YELLOW = new java.awt.Color(255, 255, 64).getRGB();
    public static final int ORANGE = new java.awt.Color(255, 128, 32).getRGB();
    public static final int WHITE = new java.awt.Color(255, 255, 255).getRGB();
    public static final int BLACK = new java.awt.Color(26, 26, 26).getRGB();
    public static final int CUSTOM_PURPLE = new java.awt.Color(130, 100, 210).getRGB();
    public static final int CUSTOM_RECT = new java.awt.Color(91, 63, 212).getRGB();
    
    /**
     * Применить градиент к тексту.
     */
    public static Component applyGradient(String text, GradientStyle style, int color1, int color2, boolean bold) {
        switch (style) {
            case HALF_SPLIT:
                return halfSplitGradient(text, color1, color2, bold);
            case FULL_GRADIENT:
                return fullGradient(text, color1, color2, bold);
            case ASTOLFO:
                return astolfoGradient(text, bold);
            case TWO_COLOR_FADE:
                return twoColorFade(text, color1, color2, bold);
            default:
                return Component.literal(text).withStyle(s -> s.withColor(color1).withBold(bold));
        }
    }
    
    /**
     * Половина текста одним цветом, половина другим.
     */
    private static Component halfSplitGradient(String text, int color1, int color2, boolean bold) {
        MutableComponent result = Component.literal("");
        int midPoint = text.length() / 2;
        
        for (int i = 0; i < text.length(); i++) {
            int color = i < midPoint ? color1 : color2;
            result.append(Component.literal(String.valueOf(text.charAt(i)))
                .withStyle(s -> s.withColor(color).withBold(bold)));
        }
        
        return result;
    }
    
    /**
     * Плавный градиент от первого цвета ко второму.
     */
    private static Component fullGradient(String text, int color1, int color2, boolean bold) {
        MutableComponent result = Component.literal("");
        
        for (int i = 0; i < text.length(); i++) {
            float ratio = text.length() > 1 ? (float) i / (text.length() - 1) : 0;
            int color = ColorUtils.interpolateColor(color1, color2, ratio);
            result.append(Component.literal(String.valueOf(text.charAt(i)))
                .withStyle(s -> s.withColor(color).withBold(bold)));
        }
        
        return result;
    }
    
    /**
     * Радужный градиент Astolfo.
     */
    private static Component astolfoGradient(String text, boolean bold) {
        MutableComponent result = Component.literal("");
        
        for (int i = 0; i < text.length(); i++) {
            int color = ColorUtils.astolfo(10, i, 0.7f, 0.7f, 1.0f);
            result.append(Component.literal(String.valueOf(text.charAt(i)))
                .withStyle(s -> s.withColor(color).withBold(bold)));
        }
        
        return result;
    }
    
    /**
     * Плавный переход между двумя цветами.
     */
    private static Component twoColorFade(String text, int color1, int color2, boolean bold) {
        MutableComponent result = Component.literal("");
        
        for (int i = 0; i < text.length(); i++) {
            float ratio = text.length() > 1 ? (float) i / (text.length() - 1) : 0;
            int color = ColorUtils.interpolateColor(color1, color2, ratio);
            result.append(Component.literal(String.valueOf(text.charAt(i)))
                .withStyle(s -> s.withColor(color).withBold(bold)));
        }
        
        return result;
    }
    
    /**
     * Применить предустановленный градиент по имени.
     */
    public static Component applyPredefinedGradient(String text, String gradientName, boolean bold) {
        switch (gradientName.toLowerCase()) {
            case "red_blue":
                return applyGradient(text, GradientStyle.HALF_SPLIT, RED, ColorUtils.toColor("#0000FF"), bold);
            case "green_purple":
                return applyGradient(text, GradientStyle.HALF_SPLIT, GREEN, ColorUtils.toColor("#800080"), bold);
            case "yellow_cyan":
                return applyGradient(text, GradientStyle.FULL_GRADIENT, YELLOW, ColorUtils.toColor("#00FFFF"), bold);
            case "orange_magenta":
                return applyGradient(text, GradientStyle.FULL_GRADIENT, ORANGE, ColorUtils.toColor("#FF00FF"), bold);
            case "astolfo":
                return applyGradient(text, GradientStyle.ASTOLFO, 0, 0, bold);
            case "blue_green_fade":
                return applyGradient(text, GradientStyle.TWO_COLOR_FADE, ColorUtils.toColor("#0000FF"), GREEN, bold);
            case "purple_red_fade":
                return applyGradient(text, GradientStyle.TWO_COLOR_FADE, ColorUtils.toColor("#800080"), RED, bold);
            case "cyan_orange_fade":
                return applyGradient(text, GradientStyle.TWO_COLOR_FADE, ColorUtils.toColor("#00FFFF"), ORANGE, bold);
            case "white_black":
                return applyGradient(text, GradientStyle.FULL_GRADIENT, WHITE, BLACK, bold);
            case "custom_purple":
                return applyGradient(text, GradientStyle.FULL_GRADIENT, CUSTOM_PURPLE, CUSTOM_RECT, bold);
            case "black_light_purple":
                return applyGradient(text, GradientStyle.FULL_GRADIENT, BLACK, ColorUtils.toColor("#DA70D6"), bold);
            case "dark_red_bright_red":
                return applyGradient(text, GradientStyle.FULL_GRADIENT, ColorUtils.toColor("#8B0000"), RED, bold);
            case "dark_red":
                return applyGradient(text, GradientStyle.HALF_SPLIT, ColorUtils.toColor("#8B0000"), ColorUtils.toColor("#8B0000"), bold);
            case "red_white":
                return applyGradient(text, GradientStyle.HALF_SPLIT, RED, WHITE, bold);
            case "purple_bright_pink":
                return applyGradient(text, GradientStyle.FULL_GRADIENT, ColorUtils.toColor("#800080"), ColorUtils.toColor("#FF69B4"), bold);
            case "pink_dark_pink":
                return applyGradient(text, GradientStyle.FULL_GRADIENT, ColorUtils.toColor("#FFC1CC"), ColorUtils.toColor("#C71585"), bold);
            case "bright_red":
                return applyGradient(text, GradientStyle.HALF_SPLIT, RED, RED, bold);
            case "dark_green_bright_green":
                return applyGradient(text, GradientStyle.FULL_GRADIENT, ColorUtils.toColor("#006400"), GREEN, bold);
            case "red_orange":
                return applyGradient(text, GradientStyle.FULL_GRADIENT, RED, ORANGE, bold);
            case "turquoise_blue":
                return applyGradient(text, GradientStyle.FULL_GRADIENT, ColorUtils.toColor("#40E0D0"), ColorUtils.toColor("#0000FF"), bold);
            case "fire":
            case "orange_white":
                return applyGradient(text, GradientStyle.FULL_GRADIENT, ORANGE, WHITE, bold);
            default:
                return Component.literal(text).withStyle(s -> s.withColor(WHITE).withBold(bold));
        }
    }
    
    /**
     * Создать префикс "Aporia.cc" с огненным градиентом.
     */
    public static MutableComponent createPrefix() {
        return (MutableComponent) applyPredefinedGradient("Aporia.cc", "fire", true);
    }
}
