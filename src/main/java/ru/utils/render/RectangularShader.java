package ru.utils.render;

import com.ferra13671.cometrenderer.buffer.framebuffer.Framebuffer;
import com.ferra13671.cometrenderer.buffer.framebuffer.FramebufferImpl;
import com.ferra13671.cometrenderer.CometRenderer;
import com.ferra13671.cometrenderer.glsl.GlProgram;
import com.ferra13671.cometrenderer.glsl.uniform.UniformType;
import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin;
import com.ferra13671.cometrenderer.plugins.minecraft.RectColors;
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import com.ferra13671.cometrenderer.plugins.minecraft.drawer.impl.BasicTextureDrawer;
import com.ferra13671.cometrenderer.plugins.minecraft.drawer.impl.RoundedRectDrawer;
import com.ferra13671.cometrenderer.vertex.DrawMode;
import com.ferra13671.cometrenderer.vertex.element.VertexElementType;
import com.ferra13671.cometrenderer.vertex.mesh.Mesh;
import com.ferra13671.gltextureutils.atlas.TextureBorder;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.Minecraft;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;

import static com.ferra13671.cometrenderer.CometVertexFormats.POSITION_TEXTURE;
import static com.mojang.blaze3d.opengl.GlConst.GL_FRAMEBUFFER;
import static org.lwjgl.BufferUtils.createByteBuffer;
import static org.lwjgl.opengl.GL30.*;

public class RectangularShader {
    private BlurShader blurShader;
    private Framebuffer captureFramebuffer;
    private Framebuffer tempFramebuffer;
    private boolean screenCaptured = false;
    private boolean useNextFrameCapture = false;
    private int worldOnlyTextureId = -1;
    private int worldOnlyTextureWidth = 0;
    private int worldOnlyTextureHeight = 0;
    private boolean worldOnlyTextureReady = false;
    private long lastDebugTime = 0;
    private static final long DEBUG_INTERVAL = 5000; // 5 секунд
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
        ensureFramebuffer(screenWidth, screenHeight);
        captureScreenContent(screenWidth, screenHeight);
        screenCaptured = true;
    }

    /**
     * Заканчивает batch рендеринг blur прямоугольников.
     * Сбрасывает флаг захвата экрана.
     */
    public void endBlurBatch() {
        screenCaptured = false;
    }
    
    /**
     * Захватывает экран в КОНЦЕ кадра для использования в СЛЕДУЮЩЕМ кадре.
     * Вызывается ПОСЛЕ guiRenderer.render() когда мир уже отрендерен, но ДО UI.
     */
    public void captureForNextFrame(int screenWidth, int screenHeight) {
        ensureFramebuffer(screenWidth, screenHeight);
        captureScreenContent(screenWidth, screenHeight);
        useNextFrameCapture = true;
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

        ensureFramebuffer(width, height);

        copyWorldTextureToTemp(textureId, width, height);

        applyBlurFromTempToCapture(width, height);
        
        this.worldOnlyTextureReady = true;
    }
    
    /**
     * Копирует world-only texture в TEMP framebuffer.
     */
    private void copyWorldTextureToTemp(int textureId, int width, int height) {
        int tempWorldFbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, tempWorldFbo);
        glFramebufferTexture2D(
            GL_FRAMEBUFFER,
            GL_COLOR_ATTACHMENT0,
            GL_TEXTURE_2D,
            textureId,
            0
        );

        tempFramebuffer.bind(false);
        int tempFbo = glGetInteger(GL_FRAMEBUFFER_BINDING);

        glBindFramebuffer(GL_READ_FRAMEBUFFER, tempWorldFbo);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, tempFbo);
        glBlitFramebuffer(
            0, 0, width, height,
            0, 0, width, height,
            GL_COLOR_BUFFER_BIT,
            GL_NEAREST
        );
        glDeleteFramebuffers(tempWorldFbo);
        MinecraftPlugin.getInstance().bindMainFramebuffer(false);
    }
    
    /**
     * Копирует world-only texture в captureFramebuffer для последующего blur.
     */
    private void copyWorldOnlyTextureToFramebuffer(int textureId, int width, int height) {
        int captureTexId = captureFramebuffer.getColorTextureId();
        int tempWorldFbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, tempWorldFbo);
        glFramebufferTexture2D(
            GL_FRAMEBUFFER,
            GL_COLOR_ATTACHMENT0,
            GL_TEXTURE_2D,
            textureId,
            0
        );

        captureFramebuffer.bind(false);
        int captureFbo = glGetInteger(GL_FRAMEBUFFER_BINDING);

        glBindFramebuffer(GL_READ_FRAMEBUFFER, tempWorldFbo);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, captureFbo);
        glBlitFramebuffer(
            0, 0, width, height,
            0, 0, width, height,
            GL_COLOR_BUFFER_BIT,
            GL_NEAREST
        );
       glDeleteFramebuffers(tempWorldFbo);
       MinecraftPlugin.getInstance().bindMainFramebuffer(false);
    }
    
    private void captureScreenContent(int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        RenderTarget mainTarget = mc.getMainRenderTarget();
        GpuTexture mcGpuTexture = mainTarget.getColorTexture();
        
        if (mcGpuTexture == null) {
            return;
        }

        int mcTexId = ((GlTexture)mcGpuTexture).glId();
        int captureTexId = captureFramebuffer.getColorTextureId();

        int tempMcFbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, tempMcFbo);
        glFramebufferTexture2D(
            GL_FRAMEBUFFER,
            GL_COLOR_ATTACHMENT0,
            GL_TEXTURE_2D,
            mcTexId,
            0
        );

        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);

        captureFramebuffer.bind(false);
        int captureFbo = glGetInteger(GL_FRAMEBUFFER_BINDING);

        glBindFramebuffer(GL_READ_FRAMEBUFFER, tempMcFbo);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, captureFbo);
        glBlitFramebuffer(
            0, 0, screenWidth, screenHeight,
            0, 0, screenWidth, screenHeight,
            GL_COLOR_BUFFER_BIT,
            GL_NEAREST
        );
        
        int err = glGetError();

        glDeleteFramebuffers(tempMcFbo);
        MinecraftPlugin.getInstance().bindMainFramebuffer(false);
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
        int previousFbo = glGetInteger(GL_FRAMEBUFFER_BINDING);
      
        long currentTime = System.currentTimeMillis();
        boolean shouldDebug = (currentTime - lastDebugTime) >= DEBUG_INTERVAL;
        
        captureFramebuffer.bind(false);
        glViewport(0, 0, width, height);
        glClear(GL_COLOR_BUFFER_BIT);

        CometRenderer.setGlobalProgram(combinedProgram);
        
        combinedProgram.getUniform("uTexelSize", UniformType.VEC2).set(new org.joml.Vector2f(1.0f / width, 1.0f / height));
        combinedProgram.getUniform("uRadius", UniformType.FLOAT).set(radius);

        int tempTexId = tempFramebuffer.getColorTextureId();
        combinedProgram.getSampler(0).set(tempTexId);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, tempTexId);

        Mesh blurMesh = CometRenderer.createMesh(
            DrawMode.QUADS,
            POSITION_TEXTURE,
            builder -> {
                builder.vertex(-1.0f, -1.0f, 0.0f)
                    .element("Texture", VertexElementType.FLOAT, 0.0f, 0.0f);
                builder.vertex(1.0f, -1.0f, 0.0f)
                    .element("Texture", VertexElementType.FLOAT, 1.0f, 0.0f);
                builder.vertex(1.0f, 1.0f, 0.0f)
                    .element("Texture", VertexElementType.FLOAT, 1.0f, 1.0f);
                builder.vertex(-1.0f, 1.0f, 0.0f)
                    .element("Texture", VertexElementType.FLOAT, 0.0f, 1.0f);
            }
        );
        
        if (blurMesh != null) {
            CometRenderer.draw(blurMesh); 
        }

        glBindFramebuffer(GL_FRAMEBUFFER, previousFbo);
        MinecraftPlugin.getInstance().bindMainFramebuffer(false);
    }
    
    private void renderFullscreenQuad() {
        if (fullscreenQuadMesh == null) {
            fullscreenQuadMesh = CometRenderer.createMesh(
                DrawMode.QUADS,
                POSITION_TEXTURE,
                builder -> {
                    builder.vertex(-1.0f, -1.0f, 0.0f)
                        .element("Texture", VertexElementType.FLOAT, 0.0f, 0.0f);
                    builder.vertex(1.0f, -1.0f, 0.0f)
                        .element("Texture", VertexElementType.FLOAT, 1.0f, 0.0f);
                    builder.vertex(1.0f, 1.0f, 0.0f)
                        .element("Texture", VertexElementType.FLOAT, 1.0f, 1.0f);
                    builder.vertex(-1.0f, 1.0f, 0.0f)
                        .element("Texture", VertexElementType.FLOAT, 0.0f, 1.0f);
                }
            );
        }
        if (fullscreenQuadMesh != null) {
            CometRenderer.draw(fullscreenQuadMesh, false);
        }
    }

    private void renderBlurredBackground(float x, float y, float width, float height) {
        int texId = captureFramebuffer.getColorTextureId();
        
        long currentTime = System.currentTimeMillis();
        
        int screenWidth = Minecraft.getInstance().getWindow().getWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getHeight();
        
        float u0 = x / screenWidth;
        float v0 = y / screenHeight;
        float u1 = (x + width) / screenWidth;
        float v1 = (y + height) / screenHeight;
     
        TextureBorder textureBorder = 
            new TextureBorder(u0, v0, u1, v1);
       
        new BasicTextureDrawer()
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
            
            ByteBuffer buffer = createByteBuffer(width * height * 4);
            
            glBindTexture(GL_TEXTURE_2D, textureId);
            glGetTexImage(GL_TEXTURE_2D, 0, 
                GL_RGBA, GL_UNSIGNED_BYTE, buffer);
            
            BufferedImage image = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_ARGB);
            
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
            ImageIO.write(image, "PNG", new File(filename));
        } catch (Exception e) {
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
        if (worldOnlyTextureReady && captureFramebuffer != null) {
            int texId = captureFramebuffer.getColorTextureId();
            long currentTime = System.currentTimeMillis();
            boolean shouldDebug = (currentTime - lastDebugTime) < 1000;

            renderBlurredBackground(x, y, width, height);

            float[] rgba = color.getColor();
            float originalAlpha = rgba[3];
            float tintAlpha = Math.max(0.08f, originalAlpha / 4.0f);
            RenderColor tintColor = RenderColor.of(rgba[0], rgba[1], rgba[2], tintAlpha);
            
            new RoundedRectDrawer()
                .rectSized(x, y, width, height, cornerRadius, RectColors.oneColor(tintColor))
                .build()
                .tryDraw()
                .close();
        } else {
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


