/*
 * Copyright (c) 2025-2026 BEVoid Project
 * Distributed under the BEVoid Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.utils.user.render.core;

import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;
import so.aporia.Aporia;
import so.aporia.utils.files.FilesManager;
import so.aporia.utils.user.logger.Logger;
import so.aporia.utils.user.render.font.Fonts;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
/**
 * Основной OpenGL-рендерер Aporia.
 *
 * Рендерит прямоугольники, круги, градиенты и изображения
 * через кастомные шейдеры с SDF для скруглённых углов.
 *
 * <p>Вершины передаются в экранных координатах, шейдер сам
 * конвертирует их в NDC. Bounds также в экранных координатах
 * (совпадает с fragPos).</p>
 *
 * <p>FBO берётся из {@code MainTarget} Minecraft для рендера
 * в тот же буфер что и GUI.</p>
 *
 * Main OpenGL renderer for Aporia.
 *
 * Renders rectangles, circles, gradients and images
 * through custom SDF shaders with rounded corners support.
 *
 * <p>Vertices are passed in screen coordinates; the vertex shader
 * converts them to NDC. Bounds are also in screen coordinates
 * (matching fragPos).</p>
 *
 * <p>FBO is obtained from Minecraft's {@code MainTarget} so
 * rendering goes into the same framebuffer as the GUI.</p>
 */
public class AporiaRenderer {
    public static final AporiaRenderer INSTANCE = new AporiaRenderer();
    public static final int MODE_FILL         = 0;
    public static final int MODE_CIRCLE       = 1;
    public static final int MODE_ROUNDED_RECT = 2;

    /**
     * Извлекает OpenGL ID текстуры из объекта Minecraft DynamicTexture.
     * Extracts OpenGL texture ID from Minecraft DynamicTexture object.
     */
    private static int getGlTextureId(Object obj) {
        try {
            var field = obj.getClass().getSuperclass().getDeclaredField("id");
            field.setAccessible(true);
            return field.getInt(obj);
        } catch (Exception e) {
            return -1;
        }
    }

    private int shaderProgram;
    private int blitShaderProgram;
    private final Map<String, Identifier> imageIds = new HashMap<>();

    // Cached uniform locations (lookups are expensive)
    private int locScreenSize = -1;
    private int locBounds = -1;
    private int locParams = -1;
    private int locParams2 = -1;
    private int locScreen = -1;
    private int locBlurTex = -1;
    private int locImageTex = -1;
    private int locInputTex = -1;

    // Reusable VAO/VBO/EBO для рисования
    private int drawVAO, drawVBO, drawEBO;
    private int imageVAO, imageVBO, imageEBO;
    private int cachedScreenW = -1;
    private int cachedScreenH = -1;
    private int cachedMcFBO = -1;
    private int cachedFbW = -1;
    private int cachedFbH = -1;
    private boolean fboBound = false;

    // State save для onRenderHud
    private int[] savedViewport = new int[4];
    private int savedDrawFBO = 0;
    private int savedProgram = 0;
    private boolean savedBlend = false;
    private boolean savedDepth = false;
    private boolean savedCull = false;

    // Pre-allocated buffers (убираем memAlloc каждый кадр)
    private float[] vertexDataCache = new float[54]; // макс 6 вершин × 9
    private short[] indicesCache = new short[6];
    private ByteBuffer vertexByteBuffer = MemoryUtil.memAlloc(54 * 4); // reusable
    private ByteBuffer indexByteBuffer = MemoryUtil.memAlloc(6 * 2);   // reusable

    /**
     * Инициализация: компиляция шейдеров.
     * Initializes and compiles shader programs.
     */
    public void init() {
        shaderProgram = createShaderProgram(
                loadShaderFromResources("aporia:shaders/core/aporia.vsh"),
                loadShaderFromResources("aporia:shaders/core/aporia.fsh")
        );
        blitShaderProgram = createShaderProgram(
                loadShaderFromResources("aporia:shaders/core/blit.vsh"),
                loadShaderFromResources("aporia:shaders/core/blit.fsh")
        );

        // Кэшируем uniform locations — один раз, не каждый кадр
        locScreenSize = GL20.glGetUniformLocation(shaderProgram, "screenSize");
        locBounds     = GL20.glGetUniformLocation(shaderProgram, "bounds");
        locParams     = GL20.glGetUniformLocation(shaderProgram, "params");
        locParams2    = GL20.glGetUniformLocation(shaderProgram, "params2");
        locScreen     = GL20.glGetUniformLocation(shaderProgram, "screen");
        locBlurTex    = GL20.glGetUniformLocation(shaderProgram, "BlurTextureSampler");
        locImageTex   = GL20.glGetUniformLocation(shaderProgram, "ImageTextureSampler");
        locInputTex   = GL20.glGetUniformLocation(blitShaderProgram, "InputTexture");

        // Reusable VAO/VBO/EBO — один на все draw вызовы
        drawVAO = GL30.glGenVertexArrays();
        drawVBO = GL15.glGenBuffers();
        drawEBO = GL15.glGenBuffers();

        // Reusable VAO для изображений
        imageVAO = GL30.glGenVertexArrays();
        imageVBO = GL15.glGenBuffers();
        imageEBO = GL15.glGenBuffers();

        cachedScreenW = -1;
        cachedScreenH = -1;
    }

    /**
     * Компилирует и линкует шейдерную программу.
     * Compiles and links a shader program.
     */
    private int createShaderProgram(String vertexSource, String fragmentSource) {
        int vertexShader = compileShader(GL20.GL_VERTEX_SHADER, vertexSource);
        int fragmentShader = compileShader(GL20.GL_FRAGMENT_SHADER, fragmentSource);
        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vertexShader);
        GL20.glAttachShader(program, fragmentShader);
        GL20.glLinkProgram(program);
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == 0) {
            String log = GL20.glGetProgramInfoLog(program, 4096);
            Logger.error("Shader program link failed: " + log);
            throw new RuntimeException("Shader program link failed: " + log);
        }
        Logger.success("Shader program linked OK, ID=" + program);
        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);
        return program;
    }

    /**
     * Компилирует один шейдер.
     * Compiles a single shader.
     */
    private int compileShader(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == 0) {
            String log = GL20.glGetShaderInfoLog(shader, 1024);
            throw new RuntimeException("Shader compilation failed: " + log);
        }
        return shader;
    }

    /**
     * Загружает шейдер из файлов .assets/aporia.
     * Loads shader from .assets/aporia folder.
     */
    private String loadShaderFromResources(String path) {
        try {
            String relPath = path.replace("aporia:", "aporia/");
            Path shaderPath = FilesManager.ROOT.resolve(".assets").resolve(relPath);
            if (Files.exists(shaderPath)) {
                return Files.readString(shaderPath);
            }
            Logger.error("Shader not found: " + shaderPath);
            return "";
        } catch (Exception e) {
            Logger.error("Failed to load shader: " + path + " - " + e.getMessage());
            return "";
        }
    }

    // =========================================================================
    //  Shape drawing methods / Методы рисования фигур
    // =========================================================================

    /**
     * Рисует линию между двумя точками.
     * Draws a line between two points.
     */
    public void drawLine(float x1, float y1, float x2, float y2, float thickness, int color) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len == 0) return;
        float nx = -dy / len * thickness * 0.5f;
        float ny =  dx / len * thickness * 0.5f;
        float[] vx = { x1+nx, x1-nx, x2-nx, x2+nx };
        float[] vy = { y1+ny, y1-ny, y2-ny, y2+ny };
        float bx = Math.min(x1, x2) - thickness;
        float by = Math.min(y1, y2) - thickness;
        float bw = Math.abs(dx) + thickness * 2;
        float bh = Math.abs(dy) + thickness * 2;
        draw(new float[][]{
                {vx[0], vy[0]}, {vx[1], vy[1]}, {vx[2], vy[2]},
                {vx[0], vy[0]}, {vx[2], vy[2]}, {vx[3], vy[3]}
        }, color, MODE_FILL, bx, by, bw, bh, 0f);
    }

    /**
     * Горизонтальная линия с затуханием по краям.
     * Horizontal line with edge fade-out.
     */
    public void drawFadeHLine(float centerX, float centerY, float halfLen, float thickness, float progress, int color) {
        float len = halfLen * progress;
        if (len < 1f) return;
        float startX = centerX - len;
        float endX = centerX + len;
        int segments = 20;
        float segW = len * 2f / segments;
        int a = (color >> 24) & 0xFF;
        for (int i = 0; i < segments; i++) {
            float x0 = startX + i * segW;
            float x1 = startX + (i + 1) * segW;
            float t0 = Math.abs(x0 - centerX) / len;
            float t1 = Math.abs(x1 - centerX) / len;
            float fade0 = (1f - t0 * t0);
            float fade1 = (1f - t1 * t1);
            int a0 = (int)(a * fade0);
            int a1 = (int)(a * fade1);
            int c0 = (a0 << 24) | (color & 0x00FFFFFF);
            int c1 = (a1 << 24) | (color & 0x00FFFFFF);
            drawLine(x0, centerY, x1, centerY, thickness, c0);
        }
    }

    /**
     * Линейная интерполяция цвета.
     * Linear color interpolation.
     */
    private static int lerp(int c0, int c1, float t) {
        int a = (int)(((c0>>24)&0xFF) + (((c1>>24)&0xFF) - ((c0>>24)&0xFF)) * t);
        int r = (int)(((c0>>16)&0xFF) + (((c1>>16)&0xFF) - ((c0>>16)&0xFF)) * t);
        int g = (int)(((c0>> 8)&0xFF) + (((c1>> 8)&0xFF) - ((c0>> 8)&0xFF)) * t);
        int b = (int)(((c0    )&0xFF) + (((c1    )&0xFF) - ((c0    )&0xFF)) * t);
        return (a<<24)|(r<<16)|(g<<8)|b;
    }

    /**
     * Рисует круг.
     * Draws a circle.
     */
    public void drawCircle(float cx, float cy, float radius, int color) {
        float x = cx - radius, y = cy - radius, d = radius * 2;
        drawShape(x, y, d, d, color, MODE_CIRCLE, x, y, d, d, radius, 0, 0f, 0f);
    }

    /**
     * Рисует треугольник.
     * Draws a triangle.
     */
    public void drawTriangle(float x1, float y1, float x2, float y2, float x3, float y3, int color) {
        float bx = Math.min(x1, Math.min(x2, x3));
        float by = Math.min(y1, Math.min(y2, y3));
        float bw = Math.max(x1, Math.max(x2, x3)) - bx;
        float bh = Math.max(y1, Math.max(y2, y3)) - by;
        draw(new float[][]{{x1,y1},{x2,y2},{x3,y3}}, color, MODE_FILL, bx, by, bw, bh, 0f);
    }

    public void resetDebugFlags() {}

    /**
     * Прямоугольник с размытыми краями (алиас на drawRect).
     * Rectangle with blurred edges (alias for drawRect).
     */
    public void drawRectBlurred(float x, float y, float w, float h, float radius, int color) {
        drawRect(x, y, w, h, radius, color, 15);
    }

    public void drawRectBlurred(float x, float y, float w, float h, float radius, int color, float blurStrength, int cornerMask) {
        drawRect(x, y, w, h, radius, color, cornerMask);
    }

    /**
     * Прямоугольник (fill).
     * Filled rectangle.
     */
    public void drawRect(float x, float y, float w, float h, float radius, int color) {
        drawRect(x, y, w, h, radius, color, 15);
    }

    /**
     * Прямоугольник с маской углов.
     * Rectangle with corner mask (bitmask).
     */
    public void drawRect(float x, float y, float w, float h, float radius, int color, int cornerMask) {
        int mode = radius > 0 ? MODE_ROUNDED_RECT : MODE_FILL;
        drawShape(x, y, w, h, color, mode, x, y, w, h, radius, 0, 0f, 0f, cornerMask);
    }

    /**
     * Градиентный прямоугольник (вертикальный/горизонтальный/радиальный).
     * Gradient rectangle (vertical/horizontal/radial).
     */
    public void drawRectGradient(float x, float y, float w, float h, float radius, int c1, int c2, int dir) {
        int steps = Math.max(2, (int)(dir==1 ? h : w) / 2);
        for (int i = 0; i < steps; i++) {
            float t0 = (float) i / steps;
            float t1 = (float)(i + 1) / steps;
            int ca = lerp(c1, c2, (t0 + t1) * 0.5f);
            if (dir == 1) {
                float sy = y + t0 * h, sh = (t1 - t0) * h;
                drawRect(x, sy, w, sh, i==0||i==steps-1 ? radius : 0, ca);
            } else if (dir == 0) {
                float sx = x + t0 * w, sw = (t1 - t0) * w;
                drawRect(sx, y, sw, h, i==0||i==steps-1 ? radius : 0, ca);
            } else {
                float cx2 = x + w/2f, cy2 = y + h/2f;
                float r2 = Math.min(w, h) * 0.5f * t1;
                drawCircle(cx2, cy2, r2, lerp(c2, c1, t0));
            }
        }
    }

    /**
     * Обводка прямоугольника (stroke).
     * Rectangle stroke (outline).
     */
    public void drawStroke(float x, float y, float w, float h, float radius,
                           float thickness, int borderMode, float fadeCorner, int color) {
        drawShape(x, y, w, h, color, MODE_ROUNDED_RECT, x, y, w, h, radius,
                borderMode, thickness, fadeCorner);
    }

    private void drawShape(float x, float y, float w, float h, int color,
                           int mode, float bx, float by, float bw, float bh,
                           float radius, int borderMode, float thickness, float fadeCorner) {
        drawShape(x, y, w, h, color, mode, bx, by, bw, bh, radius, borderMode, thickness, fadeCorner, 15);
    }

    private void drawShape(float x, float y, float w, float h, int color,
                           int mode, float bx, float by, float bw, float bh,
                           float radius, int borderMode, float thickness, float fadeCorner, int cornerMask) {
        draw(new float[][]{
                {x,   y+h}, {x+w, y+h}, {x+w, y},
                {x,   y+h}, {x+w, y  }, {x,   y}
        }, color, mode, bx, by, bw, bh, radius, borderMode, thickness, fadeCorner, cornerMask);
    }

    // =========================================================================
    //  Internal draw methods / Внутренние методы рисования
    // =========================================================================

    private void draw(float[][] verts, int color, int mode,
                      float bx, float by, float bw, float bh, float radius) {
        draw(verts, color, mode, bx, by, bw, bh, radius, 0, 0f, 0f, 15);
    }

    private void draw(float[][] verts, int color, int mode,
                      float bx, float by, float bw, float bh, float radius,
                      int borderMode, float thickness, float fadeCorner) {
        draw(verts, color, mode, bx, by, bw, bh, radius, borderMode, thickness, fadeCorner, 15);
    }

    /**
     * Основной метод рендера. Полностью самодостаточный — FBO, blend, state save/restore.
     *
     * Main rendering method. Fully self-contained — FBO, blend, state save/restore.
     */
    private void draw(float[][] verts, int color, int mode,
                      float bx, float by, float bw, float bh, float radius,
                      int borderMode, float thickness, float fadeCorner, int cornerMask) {
        Minecraft mc = Minecraft.getInstance();
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        int fbW = mc.getWindow().getWidth();
        int fbH = mc.getWindow().getHeight();

        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >>  8) & 0xFF) / 255f;
        float b = ((color      ) & 0xFF) / 255f;

        int vertexCount = verts.length;

        // Заполняем кэш массивы без аллокаций
        for (int i = 0; i < vertexCount; i++) {
            int idx = i * 9;
            vertexDataCache[idx + 0] = verts[i][0];
            vertexDataCache[idx + 1] = verts[i][1];
            vertexDataCache[idx + 2] = 0f;
            vertexDataCache[idx + 3] = 0f;
            vertexDataCache[idx + 4] = 0f;
            vertexDataCache[idx + 5] = r;
            vertexDataCache[idx + 6] = g;
            vertexDataCache[idx + 7] = b;
            vertexDataCache[idx + 8] = a;
            indicesCache[i] = (short) i;
        }

        int dataBytes = vertexCount * 9 * 4;
        int indexBytes = vertexCount * 2;

        // Reusable ByteBuffer'ы — без аллокаций каждый кадр
        vertexByteBuffer.clear().limit(dataBytes);
        vertexByteBuffer.asFloatBuffer().put(vertexDataCache, 0, vertexCount * 9).flip();
        indexByteBuffer.clear().limit(indexBytes);
        indexByteBuffer.asShortBuffer().put(indicesCache, 0, vertexCount).flip();

        // Сохраняем состояние
        int[] prevViewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, prevViewport);
        int prevDrawFBO = GL30.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int prevProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        boolean wasBlend = GL11.glIsEnabled(GL11.GL_BLEND);

        // FBO — сбрасываем при ресайзе
        if (cachedMcFBO == -1 || cachedScreenW != screenW || cachedScreenH != screenH) {
            cachedMcFBO = -1;
            var mainTarget = mc.getMainRenderTarget();
            var colorTex = mainTarget.getColorTexture();
            var depthTex = mainTarget.getDepthTexture();
            if (colorTex != null) {
                GlDevice glDevice = (GlDevice) RenderSystem.getDevice();
                cachedMcFBO = ((GlTexture) colorTex).getFbo(
                        glDevice.directStateAccess(),
                        depthTex != null ? (GlTexture) depthTex : null
                );
                cachedScreenW = screenW;
                cachedScreenH = screenH;
                cachedFbW = fbW;
                cachedFbH = fbH;
            }
        }

        if (cachedMcFBO != -1) {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, cachedMcFBO);
            GL11.glViewport(0, 0, cachedFbW, cachedFbH);
        }

        GL30.glBindVertexArray(drawVAO);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, drawVBO);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexByteBuffer, GL15.GL_DYNAMIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, drawEBO);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indexByteBuffer, GL15.GL_DYNAMIC_DRAW);
        GL20.glVertexAttribPointer(0, 3, GL15.GL_FLOAT, false, 36, 0);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(1, 2, GL15.GL_FLOAT, false, 36, 12);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(2, 4, GL15.GL_FLOAT, false, 36, 20);
        GL20.glEnableVertexAttribArray(2);
        GL20.glUseProgram(shaderProgram);
        if (locScreenSize >= 0) GL20.glUniform2f(locScreenSize, screenW, screenH);
        if (locBounds >= 0) GL20.glUniform4f(locBounds, bx, by, bw, bh);
        if (locParams >= 0) GL20.glUniform4f(locParams, radius, 1.0f, (float)mode, (float)borderMode);
        if (locParams2 >= 0) GL20.glUniform4f(locParams2, thickness, fadeCorner, 0f, 0.0f);
        if (locScreen >= 0) GL20.glUniform4f(locScreen, fbW, fbH, 0f, 0f);

        GL11.glEnable(GL11.GL_BLEND);
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);

        GL11.glDrawElements(GL11.GL_TRIANGLES, vertexCount, GL11.GL_UNSIGNED_SHORT, 0);

        // Восстанавливаем
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDrawFBO);
        GL11.glViewport(prevViewport[0], prevViewport[1], prevViewport[2], prevViewport[3]);
        GL20.glUseProgram(prevProgram);
        if (!wasBlend) GL11.glDisable(GL11.GL_BLEND);
    }

    // =========================================================================
    //  HUD / Text methods / Методы HUD и текста
    // =========================================================================

    /**
     * Вызывается один раз за кадр ПЕРЕД рендером.
     * Биндит FBO, сохраняет состояние, настраивает blend — больше не на каждый draw.
     */
    public void onRenderHud(Minecraft mc) {
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        int fbW = mc.getWindow().getWidth();
        int fbH = mc.getWindow().getHeight();

        // Кэшируем FBO — он не меняется в течение кадра
        if (cachedMcFBO == -1 || cachedScreenW != screenW || cachedScreenH != screenH) {
            var mainTarget = mc.getMainRenderTarget();
            var colorTex = mainTarget.getColorTexture();
            var depthTex = mainTarget.getDepthTexture();
            if (colorTex != null) {
                GlDevice glDevice = (GlDevice) RenderSystem.getDevice();
                cachedMcFBO = ((GlTexture) colorTex).getFbo(
                        glDevice.directStateAccess(),
                        depthTex != null ? (GlTexture) depthTex : null
                );
            }
            cachedFbW = fbW;
            cachedFbH = fbH;
            cachedScreenW = screenW;
            cachedScreenH = screenH;
        }

        // Сохраняем состояние ОДИН раз
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, savedViewport);
        savedDrawFBO = GL30.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        savedBlend = GL11.glIsEnabled(GL11.GL_BLEND);
        savedDepth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        savedCull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        savedProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);

        // Биндим FBO ОДИН раз
        if (cachedMcFBO != -1) {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, cachedMcFBO);
            GL11.glViewport(0, 0, cachedFbW, cachedFbH);
        }

        // Blend setup — один раз
        GL11.glEnable(GL11.GL_BLEND);
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);

        fboBound = true;
    }

    public void prepareFrameBlur(Minecraft mc, float strength, float saturation) {}

    public void invalidateBlurCache() {
        cachedMcFBO = -1;
        fboBound = false;
    }

    /**
     * Восстанавливает состояние после рендера. Вызывается ПОСЛЕ всех draw.
     */
    public void postRenderHud() {
        if (fboBound) {
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, savedDrawFBO);
            GL11.glViewport(savedViewport[0], savedViewport[1], savedViewport[2], savedViewport[3]);
            if (!savedBlend) GL11.glDisable(GL11.GL_BLEND);
            if (savedDepth) GL11.glEnable(GL11.GL_DEPTH_TEST);
            if (savedCull) GL11.glEnable(GL11.GL_CULL_FACE);
            GL20.glUseProgram(savedProgram);
            fboBound = false;
        }
    }

    /**
     * Рисует текст через встроенный шрифтовый движок.
     * Draws text via built-in font engine.
     */
    public void drawText(String font, String text, float x, float y, float size, int color) {
        Aporia.FONTS.drawText(font, text, x, y, size, color);
    }

    /**
     * Текст с обводкой.
     * Text with outline.
     */
    public void drawTextWithOutline(String font, String text, float x, float y, float size,
                                    int color, float outlineWidth, int outlineColor) {
        Aporia.FONTS.drawTextWithOutline(font, text, x, y, size, color, outlineWidth, outlineColor);
    }

    /**
     * Ширина текста.
     * Text width measurement.
     */
    public float getTextWidth(String font, String text, float size) {
        return Aporia.FONTS.getTextWidth(font, text, size);
    }

    public void drawGlyph(int index, float x, float y, float size, int color) {
        Aporia.FONTS.drawGlyph(Fonts.FONT, index, x, y, size, color);
    }

    public void onRenderWorld(Minecraft mc) {}

    // =========================================================================
    //  Image loading / Загрузка изображений
    // =========================================================================

    /**
     * Загружает изображение из файла и регистрирует как текстуру.
     * Loads image from file and registers as texture.
     */
    public Identifier loadImage(Path path) {
        String key = path.toAbsolutePath().toString();
        if (imageIds.containsKey(key)) {
            return imageIds.get(key);
        }
        try (FileInputStream fis = new FileInputStream(path.toFile())) {
            NativeImage img = NativeImage.read(fis);
            DynamicTexture tex = new DynamicTexture(() -> key, img);
            String name = path.getFileName().toString()
                    .toLowerCase().replaceAll("[^a-z0-9_.-]", "_");
            Identifier id = Identifier.fromNamespaceAndPath("aporia", "user_image/" + name + "_" + Math.abs(key.hashCode()));
            Minecraft.getInstance().getTextureManager().register(id, tex);
            imageIds.put(key, id);
            return id;
        } catch (IOException e) {
            Logger.warn("[loadImage] Failed to load: " + path + " — " + e.getMessage());
            return null;
        }
    }

    public Identifier loadImage(java.io.InputStream stream) {
        String key = "discord_avatar_" + System.currentTimeMillis();
        if (imageIds.containsKey(key)) {
            return imageIds.get(key);
        }
        try {
            NativeImage img = NativeImage.read(stream);
            DynamicTexture tex = new DynamicTexture(() -> key, img);
            Identifier id = Identifier.fromNamespaceAndPath("aporia", "user_image/discord_avatar_" + Math.abs(key.hashCode()));
            Minecraft.getInstance().getTextureManager().register(id, tex);
            imageIds.put(key, id);
            return id;
        } catch (IOException e) {
            Logger.warn("[loadImage] Failed to load from stream: " + e.getMessage());
            return null;
        }
    }

    // =========================================================================
    //  Image rendering / Рендер изображений
    // =========================================================================

    /**
     * Рисует изображение.
     * Draws an image.
     */
    public void drawImage(float x, float y, float w, float h, Identifier id) {
        drawImage(x, y, w, h, id, 0);
    }

    /**
     * Рисует изображение с опциональными скруглёнными углами.
     * Draws an image with optional rounded corners.
     */
    public void drawImage(float x, float y, float w, float h, Identifier id, float radius) {
        if (id == null) return;

        Minecraft mc = Minecraft.getInstance();
        var tex = mc.getTextureManager().getTexture(id);
        if (tex == null || tex.getTextureView() == null) return;

        int glTextureId = getGlTextureId(tex.getTextureView());
        if (glTextureId == -1) return;

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        int fbW = mc.getWindow().getWidth();
        int fbH = mc.getWindow().getHeight();

        float[] vertices = {
                x,     y + h, 0f,  0f, 1f,
                x + w, y + h, 0f,  1f, 1f,
                x + w, y,     0f,  1f, 0f,
                x,     y,     0f,  0f, 0f
        };
        short[] indices = {0, 1, 2, 0, 2, 3};

        // Сохраняем
        int[] prevVP = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, prevVP);
        int prevFBO = GL30.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int prevProg = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int prevVAO = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);

        // FBO
        var mainTarget = mc.getMainRenderTarget();
        var colorTex = mainTarget.getColorTexture();
        var depthTex = mainTarget.getDepthTexture();
        if (colorTex != null) {
            GlDevice glDevice = (GlDevice) RenderSystem.getDevice();
            int mcFBO = ((GlTexture) colorTex).getFbo(
                    glDevice.directStateAccess(),
                    depthTex != null ? (GlTexture) depthTex : null
            );
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, mcFBO);
            GL11.glViewport(0, 0, fbW, fbH);
        }

        // Upload в reusable VBO
        ByteBuffer vb = MemoryUtil.memAlloc(vertices.length * 4);
        vb.asFloatBuffer().put(vertices).flip();
        ByteBuffer ib = MemoryUtil.memAlloc(indices.length * 2);
        ib.asShortBuffer().put(indices).flip();

        GL30.glBindVertexArray(imageVAO);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, imageVBO);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vb, GL15.GL_DYNAMIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, imageEBO);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, ib, GL15.GL_DYNAMIC_DRAW);
        GL20.glVertexAttribPointer(0, 3, GL15.GL_FLOAT, false, 20, 0);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(1, 2, GL15.GL_FLOAT, false, 20, 12);
        GL20.glEnableVertexAttribArray(1);

        MemoryUtil.memFree(vb);
        MemoryUtil.memFree(ib);

        // Выбор шейдера
        if (radius <= 0) {
            GL20.glUseProgram(blitShaderProgram);
            GL20.glUniform1i(locInputTex, 0);
        } else {
            GL20.glUseProgram(shaderProgram);
            GL20.glUniform2f(locScreenSize, screenW, screenH);
            GL20.glUniform4f(locBounds, x, y, w, h);
            GL20.glUniform4f(locParams, radius, 1.0f, MODE_ROUNDED_RECT, 0);
            GL20.glUniform4f(locParams2, 0f, 0f, 0f, 16f);
            GL20.glUniform4f(locScreen, fbW, fbH, 0f, 0f);
            GL20.glUniform1i(locImageTex, 0);
        }

        GL11.glEnable(GL11.GL_BLEND);
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, glTextureId);

        GL11.glDrawElements(GL11.GL_TRIANGLES, 6, GL11.GL_UNSIGNED_SHORT, 0);

        // Восстанавливаем
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevFBO);
        GL11.glViewport(prevVP[0], prevVP[1], prevVP[2], prevVP[3]);
        GL30.glBindVertexArray(prevVAO);
        GL20.glUseProgram(prevProg);
    }

    // =========================================================================
    //  Cleanup
    // =========================================================================

    /**
     * Освобождает ресурсы шейдеров.
     * Releases shader resources.
     */
    public void cleanup() {
        if (shaderProgram != 0) {
            GL20.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
        if (blitShaderProgram != 0) {
            GL20.glDeleteProgram(blitShaderProgram);
            blitShaderProgram = 0;
        }
        if (drawVAO != 0) {
            GL30.glDeleteVertexArrays(drawVAO);
            drawVAO = 0;
        }
        if (drawVBO != 0) {
            GL15.glDeleteBuffers(drawVBO);
            drawVBO = 0;
        }
        if (drawEBO != 0) {
            GL15.glDeleteBuffers(drawEBO);
            drawEBO = 0;
        }
        if (imageVAO != 0) {
            GL30.glDeleteVertexArrays(imageVAO);
            imageVAO = 0;
        }
        if (imageVBO != 0) {
            GL15.glDeleteBuffers(imageVBO);
            imageVBO = 0;
        }
        if (imageEBO != 0) {
            GL15.glDeleteBuffers(imageEBO);
            imageEBO = 0;
        }
        if (vertexByteBuffer != null) {
            MemoryUtil.memFree(vertexByteBuffer);
            vertexByteBuffer = null;
        }
        if (indexByteBuffer != null) {
            MemoryUtil.memFree(indexByteBuffer);
            indexByteBuffer = null;
        }
    }
}