package ru.render;

import com.ferra13671.cometrenderer.buffer.framebuffer.Framebuffer;
import com.ferra13671.cometrenderer.buffer.framebuffer.FramebufferImpl;
import com.ferra13671.cometrenderer.CometRenderer;
import com.ferra13671.cometrenderer.glsl.GlProgram;
import com.ferra13671.cometrenderer.plugins.minecraft.RectColors;
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import com.ferra13671.cometrenderer.plugins.minecraft.drawer.impl.RoundedRectDrawer;
import net.minecraft.client.Minecraft;

import java.awt.*;

import static com.mojang.blaze3d.opengl.GlConst.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;

public class RectangularShader {
    private BlurShader blurShader;
    private Framebuffer captureFramebuffer;
    private Framebuffer tempFramebuffer;
    private boolean screenCaptured = false;
    private boolean useNextFrameCapture = false; // Флаг для использования захвата из предыдущего кадра
    
    // НОВЫЙ ПОДХОД: world-only texture из LevelRenderer
    private int worldOnlyTextureId = -1;
    private int worldOnlyTextureWidth = 0;
    private int worldOnlyTextureHeight = 0;
    private boolean worldOnlyTextureReady = false;
    
    // DEBUG: Таймер для скриншотов (раз в 5 секунд)
    private long lastDebugTime = 0;
    private static final long DEBUG_INTERVAL = 5000; // 5 секунд
    
    // Переиспользуемый mesh для fullscreen quad
    private com.ferra13671.cometrenderer.vertex.mesh.Mesh fullscreenQuadMesh = null;

    public RectangularShader() {
        this.blurShader = new BlurShader();
    }

    public void setBlurShader(BlurShader blurShader) {
        this.blurShader = blurShader;
    }
    
    private void ensureFramebuffer(int width, int height) {
        if (captureFramebuffer == null || captureFramebuffer.getWidth() != width || captureFramebuffer.getHeight() != height) {
            if (captureFramebuffer != null) captureFramebuffer.delete();
            captureFramebuffer = new FramebufferImpl("capture", false, width, height, new Color(0, 0, 0, 0), 1.0);
        }
        if (tempFramebuffer == null || tempFramebuffer.getWidth() != width || tempFramebuffer.getHeight() != height) {
            if (tempFramebuffer != null) tempFramebuffer.delete();
            tempFramebuffer = new FramebufferImpl("temp", false, width, height, new Color(0, 0, 0, 0), 1.0);
        }
    }

    /**
     * Начинает batch рендеринг blur прямоугольников.
     * Захватывает экран ОДИН РАЗ для всех последующих drawRectangleWithBlur вызовов.
     * ВАЖНО: Вызывать ПОСЛЕ рендера мира, но ДО первого drawRectangleWithBlur!
     */
    public void beginBlurBatch(int screenWidth, int screenHeight) {
        System.out.println("[BLUR] === BEGIN BLUR BATCH ===");
        ensureFramebuffer(screenWidth, screenHeight);
        captureScreenContent(screenWidth, screenHeight);
        applyBlurToFramebuffer(screenWidth, screenHeight);
        screenCaptured = true;
        System.out.println("[BLUR] Screen captured and blurred for batch rendering");
    }

    /**
     * Заканчивает batch рендеринг blur прямоугольников.
     * Сбрасывает флаг захвата экрана.
     */
    public void endBlurBatch() {
        System.out.println("[BLUR] === END BLUR BATCH ===");
        screenCaptured = false;
    }
    
    /**
     * Захватывает экран в КОНЦЕ кадра для использования в СЛЕДУЮЩЕМ кадре.
     * Вызывается ПОСЛЕ guiRenderer.render() когда мир уже отрендерен, но ДО UI.
     */
    public void captureForNextFrame(int screenWidth, int screenHeight) {
        System.out.println("[BLUR] === CAPTURE FOR NEXT FRAME ===");
        ensureFramebuffer(screenWidth, screenHeight);
        captureScreenContent(screenWidth, screenHeight);
        applyBlurToFramebuffer(screenWidth, screenHeight);
        useNextFrameCapture = true;
        System.out.println("[BLUR] Screen captured and blurred for NEXT frame");
    }
    
    /**
     * НОВЫЙ ПОДХОД: Устанавливает world-only texture из LevelRenderer для blur эффекта.
     * Этот texture содержит ТОЛЬКО мир БЕЗ UI, захваченный в отдельный RenderTarget.
     * ВЫЗЫВАЕТСЯ КАЖДЫЙ КАДР!
     */
    public void setWorldOnlyTexture(int textureId, int width, int height) {
        this.worldOnlyTextureId = textureId;
        this.worldOnlyTextureWidth = width;
        this.worldOnlyTextureHeight = height;
        
        // ВСЕГДА копируем и применяем blur (каждый кадр)
        ensureFramebuffer(width, height);
        
        // Копируем world texture в TEMP framebuffer (не в capture!)
        copyWorldTextureToTemp(textureId, width, height);
        
        // Применяем blur из temp в capture
        applyBlurFromTempToCapture(width, height);
        
        this.worldOnlyTextureReady = true;
    }
    
    /**
     * Копирует world-only texture в TEMP framebuffer.
     */
    private void copyWorldTextureToTemp(int textureId, int width, int height) {
        // Создаем временный FBO для world texture
        int tempWorldFbo = org.lwjgl.opengl.GL30.glGenFramebuffers();
        org.lwjgl.opengl.GL30.glBindFramebuffer(org.lwjgl.opengl.GL30.GL_FRAMEBUFFER, tempWorldFbo);
        org.lwjgl.opengl.GL30.glFramebufferTexture2D(
            org.lwjgl.opengl.GL30.GL_FRAMEBUFFER,
            org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0,
            org.lwjgl.opengl.GL11.GL_TEXTURE_2D,
            textureId,
            0
        );
        
        // Получаем FBO для temp framebuffer
        tempFramebuffer.bind(false);
        int tempFbo = org.lwjgl.opengl.GL30.glGetInteger(org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_BINDING);
        
        // Используем glBlitFramebuffer для копирования
        org.lwjgl.opengl.GL30.glBindFramebuffer(org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER, tempWorldFbo);
        org.lwjgl.opengl.GL30.glBindFramebuffer(org.lwjgl.opengl.GL30.GL_DRAW_FRAMEBUFFER, tempFbo);
        
        org.lwjgl.opengl.GL30.glBlitFramebuffer(
            0, 0, width, height,
            0, 0, width, height,
            org.lwjgl.opengl.GL30.GL_COLOR_BUFFER_BIT,
            org.lwjgl.opengl.GL11.GL_NEAREST
        );
        
        // Удаляем временный FBO
        org.lwjgl.opengl.GL30.glDeleteFramebuffers(tempWorldFbo);
        
        // Восстанавливаем binding
        com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin.getInstance().bindMainFramebuffer(false);
    }
    
    /**
     * Копирует world-only texture в captureFramebuffer для последующего blur.
     */
    private void copyWorldOnlyTextureToFramebuffer(int textureId, int width, int height) {
        int captureTexId = captureFramebuffer.getColorTextureId();
        
        // Создаем временный FBO для world texture
        int tempWorldFbo = org.lwjgl.opengl.GL30.glGenFramebuffers();
        org.lwjgl.opengl.GL30.glBindFramebuffer(org.lwjgl.opengl.GL30.GL_FRAMEBUFFER, tempWorldFbo);
        org.lwjgl.opengl.GL30.glFramebufferTexture2D(
            org.lwjgl.opengl.GL30.GL_FRAMEBUFFER,
            org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0,
            org.lwjgl.opengl.GL11.GL_TEXTURE_2D,
            textureId,
            0
        );
        
        // DEBUG: Сохраняем world texture ДО копирования
        ru.debug.FramebufferDebug.saveCurrentFramebuffer("03_worldTexture_before_copy");
        
        // Получаем FBO для capture texture
        captureFramebuffer.bind(false);
        int captureFbo = org.lwjgl.opengl.GL30.glGetInteger(org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_BINDING);
        
        // Используем glBlitFramebuffer для копирования
        org.lwjgl.opengl.GL30.glBindFramebuffer(org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER, tempWorldFbo);
        org.lwjgl.opengl.GL30.glBindFramebuffer(org.lwjgl.opengl.GL30.GL_DRAW_FRAMEBUFFER, captureFbo);
        
        org.lwjgl.opengl.GL30.glBlitFramebuffer(
            0, 0, width, height,
            0, 0, width, height,
            org.lwjgl.opengl.GL30.GL_COLOR_BUFFER_BIT,
            org.lwjgl.opengl.GL11.GL_NEAREST
        );
        
        // DEBUG: Сохраняем capture framebuffer ПОСЛЕ копирования
        ru.debug.FramebufferDebug.saveCurrentFramebuffer("04_captureFramebuffer_after_copy");
        
        // Удаляем временный FBO
        org.lwjgl.opengl.GL30.glDeleteFramebuffers(tempWorldFbo);
        
        // Восстанавливаем binding
        com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin.getInstance().bindMainFramebuffer(false);
    }
    
    private void captureScreenContent(int screenWidth, int screenHeight) {
        System.out.println("[BLUR] === captureScreenContent START ===");
        
        // Получаем MC main render target
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        com.mojang.blaze3d.pipeline.RenderTarget mainTarget = mc.getMainRenderTarget();
        com.mojang.blaze3d.textures.GpuTexture mcGpuTexture = mainTarget.getColorTexture();
        
        if (mcGpuTexture == null) {
            System.err.println("[BLUR] Main render target color texture is null!");
            return;
        }
        
        // Получаем OpenGL texture ID из MC texture
        int mcTexId = ((com.mojang.blaze3d.opengl.GlTexture)mcGpuTexture).glId();
        int captureTexId = captureFramebuffer.getColorTextureId();
        
        System.out.println("[BLUR] MC texture ID: " + mcTexId + ", Capture texture ID: " + captureTexId);
        System.out.println("[BLUR] Size: " + screenWidth + "x" + screenHeight);
        
        // Создаем временный FBO для MC текстуры
        int tempMcFbo = org.lwjgl.opengl.GL30.glGenFramebuffers();
        org.lwjgl.opengl.GL30.glBindFramebuffer(org.lwjgl.opengl.GL30.GL_FRAMEBUFFER, tempMcFbo);
        org.lwjgl.opengl.GL30.glFramebufferTexture2D(
            org.lwjgl.opengl.GL30.GL_FRAMEBUFFER,
            org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0,
            org.lwjgl.opengl.GL11.GL_TEXTURE_2D,
            mcTexId,
            0
        );
        
        // Проверяем статус FBO
        int status = org.lwjgl.opengl.GL30.glCheckFramebufferStatus(org.lwjgl.opengl.GL30.GL_FRAMEBUFFER);
        System.out.println("[BLUR] Temp MC FBO status: " + (status == org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_COMPLETE ? "COMPLETE" : "INCOMPLETE " + status));
        
        // Получаем FBO для capture texture
        captureFramebuffer.bind(false);
        int captureFbo = org.lwjgl.opengl.GL30.glGetInteger(org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_BINDING);
        
        System.out.println("[BLUR] Temp MC FBO: " + tempMcFbo + ", Capture FBO: " + captureFbo);
        
        // Используем glBlitFramebuffer для копирования
        org.lwjgl.opengl.GL30.glBindFramebuffer(org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER, tempMcFbo);
        org.lwjgl.opengl.GL30.glBindFramebuffer(org.lwjgl.opengl.GL30.GL_DRAW_FRAMEBUFFER, captureFbo);
        
        org.lwjgl.opengl.GL30.glBlitFramebuffer(
            0, 0, screenWidth, screenHeight,
            0, 0, screenWidth, screenHeight,
            org.lwjgl.opengl.GL30.GL_COLOR_BUFFER_BIT,
            org.lwjgl.opengl.GL11.GL_NEAREST
        );
        
        int err = org.lwjgl.opengl.GL11.glGetError();
        System.out.println("[BLUR] glBlitFramebuffer: " + (err == 0 ? "OK" : "ERROR " + err));
        
        // Удаляем временный FBO
        org.lwjgl.opengl.GL30.glDeleteFramebuffers(tempMcFbo);
        
        // Восстанавливаем binding
        com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin.getInstance().bindMainFramebuffer(false);
        
        System.out.println("[BLUR] === captureScreenContent END ===");
    }

    /**
     * Применяет blur из tempFramebuffer в captureFramebuffer.
     */
    private void applyBlurFromTempToCapture(int width, int height) {
        GlProgram combinedProgram = blurShader.getCombinedProgram();

        if (combinedProgram == null) {
            return;
        }

        float radius = blurShader.getBlurRadius();
        int previousFbo = org.lwjgl.opengl.GL30.glGetInteger(org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_BINDING);
        
        // DEBUG: Проверяем нужно ли делать скриншоты
        long currentTime = System.currentTimeMillis();
        boolean shouldDebug = (currentTime - lastDebugTime) >= DEBUG_INTERVAL;
        if (shouldDebug) {
            lastDebugTime = currentTime;
            System.out.println("[BLUR] === DEBUG SCREENSHOTS (every 5 sec) ===");
        }

        // DEBUG: Сохраняем tempFramebuffer ДО blur (source)
        if (shouldDebug) {
            tempFramebuffer.bind(false);
            ru.debug.FramebufferDebug.saveCurrentFramebuffer("05a_temp_before_blur");
        }

        // SINGLE PASS: Combined 2D blur из temp в capture
        if (shouldDebug) System.out.println("[BLUR] === COMBINED BLUR PASS (temp -> capture) ===");
        
        // Рисуем blur НАПРЯМУЮ в captureFramebuffer
        captureFramebuffer.bind(false);
        org.lwjgl.opengl.GL11.glViewport(0, 0, width, height);
        org.lwjgl.opengl.GL11.glClear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT);

        CometRenderer.setGlobalProgram(combinedProgram);
        
        combinedProgram.getUniform("uTexelSize", com.ferra13671.cometrenderer.glsl.uniform.UniformType.VEC2).set(new org.joml.Vector2f(1.0f / width, 1.0f / height));
        combinedProgram.getUniform("uRadius", com.ferra13671.cometrenderer.glsl.uniform.UniformType.FLOAT).set(radius);

        int tempTexId = tempFramebuffer.getColorTextureId();
        combinedProgram.getSampler(0).set(tempTexId);
        org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
        org.lwjgl.opengl.GL11.glBindTexture(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, tempTexId);

        // Создаем НОВЫЙ mesh каждый раз (не переиспользуем)
        com.ferra13671.cometrenderer.vertex.mesh.Mesh blurMesh = CometRenderer.createMesh(
            com.ferra13671.cometrenderer.vertex.DrawMode.QUADS,
            com.ferra13671.cometrenderer.CometVertexFormats.POSITION_TEXTURE,
            builder -> {
                builder.vertex(-1.0f, -1.0f, 0.0f)
                    .element("Texture", com.ferra13671.cometrenderer.vertex.element.VertexElementType.FLOAT, 0.0f, 0.0f);
                builder.vertex(1.0f, -1.0f, 0.0f)
                    .element("Texture", com.ferra13671.cometrenderer.vertex.element.VertexElementType.FLOAT, 1.0f, 0.0f);
                builder.vertex(1.0f, 1.0f, 0.0f)
                    .element("Texture", com.ferra13671.cometrenderer.vertex.element.VertexElementType.FLOAT, 1.0f, 1.0f);
                builder.vertex(-1.0f, 1.0f, 0.0f)
                    .element("Texture", com.ferra13671.cometrenderer.vertex.element.VertexElementType.FLOAT, 0.0f, 1.0f);
            }
        );
        
        if (blurMesh != null) {
            CometRenderer.draw(blurMesh); // Закрывается автоматически
        }
        
        // DEBUG: Сохраняем captureFramebuffer после blur
        if (shouldDebug) {
            ru.debug.FramebufferDebug.saveCurrentFramebuffer("05b_capture_after_blur");
        }

        glBindFramebuffer(GL_FRAMEBUFFER, previousFbo);
        com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin.getInstance().bindMainFramebuffer(false);
    }

    private void applyBlurToFramebuffer(int width, int height) {
        // DEPRECATED: Эта функция больше не используется
        // Используется applyBlurFromTempToCapture() вместо нее
        System.err.println("[BLUR] WARNING: applyBlurToFramebuffer() is deprecated!");
    }

    private void renderFullscreenQuad() {
        // Создаем mesh ОДИН РАЗ и переиспользуем
        if (fullscreenQuadMesh == null) {
            fullscreenQuadMesh = CometRenderer.createMesh(
                com.ferra13671.cometrenderer.vertex.DrawMode.QUADS,
                com.ferra13671.cometrenderer.CometVertexFormats.POSITION_TEXTURE,
                builder -> {
                    builder.vertex(-1.0f, -1.0f, 0.0f)
                        .element("Texture", com.ferra13671.cometrenderer.vertex.element.VertexElementType.FLOAT, 0.0f, 0.0f);
                    builder.vertex(1.0f, -1.0f, 0.0f)
                        .element("Texture", com.ferra13671.cometrenderer.vertex.element.VertexElementType.FLOAT, 1.0f, 0.0f);
                    builder.vertex(1.0f, 1.0f, 0.0f)
                        .element("Texture", com.ferra13671.cometrenderer.vertex.element.VertexElementType.FLOAT, 1.0f, 1.0f);
                    builder.vertex(-1.0f, 1.0f, 0.0f)
                        .element("Texture", com.ferra13671.cometrenderer.vertex.element.VertexElementType.FLOAT, 0.0f, 1.0f);
                }
            );
        }
        
        if (fullscreenQuadMesh != null) {
            // НЕ закрываем mesh (close=false) чтобы переиспользовать
            CometRenderer.draw(fullscreenQuadMesh, false);
        }
    }

    private void renderBlurredBackground(float x, float y, float width, float height) {
        // Используем BasicTextureDrawer из CometRenderer
        int texId = captureFramebuffer.getColorTextureId();
        
        // DEBUG
        long currentTime = System.currentTimeMillis();
        if ((currentTime - lastDebugTime) < 1000) {
            System.out.println("[BLUR] renderBlurredBackground: Using BasicTextureDrawer with texture ID " + texId);
        }
        
        int screenWidth = Minecraft.getInstance().getWindow().getWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getHeight();

        // UV координаты для текстуры - берем часть экрана соответствующую прямоугольнику
        float u0 = x / screenWidth;
        float v0 = y / screenHeight;
        float u1 = (x + width) / screenWidth;
        float v1 = (y + height) / screenHeight;
        
        // Создаем TextureBorder с нашими UV координатами
        com.ferra13671.gltextureutils.atlas.TextureBorder textureBorder = 
            new com.ferra13671.gltextureutils.atlas.TextureBorder(u0, v0, u1, v1);
        
        // Используем BasicTextureDrawer для рисования текстуры
        new com.ferra13671.cometrenderer.plugins.minecraft.drawer.impl.BasicTextureDrawer()
            .setTexture(texId)
            .rectSized(x, y, width, height, textureBorder)
            .build()
            .tryDraw()
            .close();
    }

    private void saveTextureToFile(int textureId, String filename) {
        try {
            int width = captureFramebuffer.getWidth();
            int height = captureFramebuffer.getHeight();
            
            java.nio.ByteBuffer buffer = org.lwjgl.BufferUtils.createByteBuffer(width * height * 4);
            
            org.lwjgl.opengl.GL11.glBindTexture(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, textureId);
            org.lwjgl.opengl.GL11.glGetTexImage(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, 0, 
                org.lwjgl.opengl.GL11.GL_RGBA, org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE, buffer);
            
            java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(width, height, 
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
            
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int i = (y * width + x) * 4;
                    int r = buffer.get(i) & 0xFF;
                    int g = buffer.get(i + 1) & 0xFF;
                    int b = buffer.get(i + 2) & 0xFF;
                    int a = buffer.get(i + 3) & 0xFF;
                    image.setRGB(x, height - 1 - y, (a << 24) | (r << 16) | (g << 8) | b);
                }
            }
            
            javax.imageio.ImageIO.write(image, "PNG", new java.io.File(filename));
            System.out.println("[BLUR] Saved texture to " + filename);
        } catch (Exception e) {
            System.err.println("[BLUR] Failed to save texture: " + e.getMessage());
        }
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
        // НОВЫЙ ПОДХОД: Используем world-only texture если доступен
        if (worldOnlyTextureReady && captureFramebuffer != null) {
            // DEBUG: Проверяем texture ID
            int texId = captureFramebuffer.getColorTextureId();
            long currentTime = System.currentTimeMillis();
            boolean shouldDebug = (currentTime - lastDebugTime) < 1000;
            if (shouldDebug) {
                System.out.println("[BLUR] drawRectangleWithBlur: captureFramebuffer texture ID = " + texId);
            }
            
            // Рисуем размытый фон из world-only texture
            renderBlurredBackground(x, y, width, height);
            
            // DEBUG: Сохраняем что нарисовалось
            if (shouldDebug) {
                com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin.getInstance().bindMainFramebuffer(false);
                ru.debug.FramebufferDebug.saveCurrentFramebuffer("06_after_renderBlurredBackground");
            }
            
            // Рисуем ОЧЕНЬ прозрачный цветной оверлей для тинта (alpha уменьшаем в 4 раза)
            float[] rgba = color.getColor();
            float originalAlpha = rgba[3]; // Alpha в диапазоне 0-1
            float tintAlpha = Math.max(0.08f, originalAlpha / 4.0f); // Минимум 0.08 (20/255), максимум originalAlpha/4
            RenderColor tintColor = RenderColor.of(rgba[0], rgba[1], rgba[2], tintAlpha);
            
            new RoundedRectDrawer()
                .rectSized(x, y, width, height, cornerRadius, RectColors.oneColor(tintColor))
                .build()
                .tryDraw()
                .close();
        } else {
            // Fallback: просто рисуем обычный прямоугольник
            drawRectangle(x, y, width, height, color, cornerRadius);
        }
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
        if (captureFramebuffer != null) { captureFramebuffer.delete(); captureFramebuffer = null; }
        if (tempFramebuffer != null) { tempFramebuffer.delete(); tempFramebuffer = null; }
        if (fullscreenQuadMesh != null) { fullscreenQuadMesh.close(); fullscreenQuadMesh = null; }
    }
}


