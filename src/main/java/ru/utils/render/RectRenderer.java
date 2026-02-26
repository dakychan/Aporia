package ru.utils.render;

import com.ferra13671.cometrenderer.plugins.minecraft.RectColors;
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import com.ferra13671.cometrenderer.plugins.minecraft.drawer.impl.RoundedRectDrawer;

public class RectRenderer {
    private static RectangularShader rectangularShader;
    private static BlurShader sharedBlurShader;

    public static void init() {
        if (rectangularShader == null) {
            rectangularShader = new RectangularShader();
        }
    }

    public static void setSharedBlurShader(BlurShader blurShader) {
        sharedBlurShader = blurShader;
        if (rectangularShader != null) {
            rectangularShader.setBlurShader(blurShader);
        }
    }

    private static void ensureInitialized() {
        if (rectangularShader == null) {
            rectangularShader = new RectangularShader();
            if (sharedBlurShader != null) {
                rectangularShader.setBlurShader(sharedBlurShader);
            }
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

    /**
     * Начинает batch рендеринг blur прямоугольников.
     * Захватывает экран ОДИН РАЗ для всех последующих drawRectangleWithBlur вызовов.
     * ВАЖНО: Вызывать ПОСЛЕ рендера мира, но ДО первого drawRectangleWithBlur!
     */
    public static void beginBlurBatch(int screenWidth, int screenHeight) {
        ensureInitialized();
        rectangularShader.beginBlurBatch(screenWidth, screenHeight);
    }

    /**
     * Заканчивает batch рендеринг blur прямоугольников.
     * Сбрасывает флаг захвата экрана.
     */
    public static void endBlurBatch() {
        ensureInitialized();
        rectangularShader.endBlurBatch();
    }
    
    /**
     * Захватывает экран в конце кадра для использования в следующем кадре.
     * Вызывается ПОСЛЕ guiRenderer.render() когда мир уже отрендерен.
     */
    public static void captureForNextFrame(int screenWidth, int screenHeight) {
        ensureInitialized();
        rectangularShader.captureForNextFrame(screenWidth, screenHeight);
    }
    
    /**
     * НОВЫЙ ПОДХОД: Устанавливает world-only texture для использования в blur эффекте.
     * Вызывается из Aporia.captureWorldOnlyTarget() когда LevelRenderer захватил мир БЕЗ UI.
     */
    public static void setWorldOnlyTexture(int textureId, int width, int height) {
        ensureInitialized();
        rectangularShader.setWorldOnlyTexture(textureId, width, height);
    }

    public static void cleanup() {
        if (rectangularShader != null) {
            rectangularShader.cleanup();
            rectangularShader = null;
        }
    }
}
