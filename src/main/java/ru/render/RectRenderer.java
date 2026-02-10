package ru.render;

import com.ferra13671.cometrenderer.plugins.minecraft.RectColors;
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import com.ferra13671.cometrenderer.plugins.minecraft.drawer.impl.RoundedRectDrawer;

/**
 * Простая обертка для рендера прямоугольников через CometRenderer
 * Использует напрямую RoundedRectDrawer без лишних вызовов
 */
public class RectRenderer {
    
    /**
     * Рисует закругленный прямоугольник одного цвета
     */
    public static void drawRoundedRect(float x, float y, float width, float height, float radius, RenderColor color) {
        new RoundedRectDrawer()
                .rectSized(x, y, width, height, radius, RectColors.oneColor(color))
                .build()
                .tryDraw()
                .close();
    }
    
    /**
     * Рисует закругленный прямоугольник с градиентом
     */
    public static void drawRoundedRect(float x, float y, float width, float height, float radius, RectColors colors) {
        new RoundedRectDrawer()
                .rectSized(x, y, width, height, radius, colors)
                .build()
                .tryDraw()
                .close();
    }
}
