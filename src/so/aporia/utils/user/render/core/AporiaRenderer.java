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
import com.mojang.blaze3d.textures.GpuTexture;
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
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Основной OpenGL-рендерер Aporia.
 * Оптимизировано: отсутствуют аллокации каждый кадр, переиспользуемые буферы,
 * стейт OpenGL меняется только 1 раз за кадр (в onRenderHud/postRenderHud).
 */
public class AporiaRenderer {
    public static final AporiaRenderer INSTANCE = new AporiaRenderer();
    public static final int MODE_FILL         = 0;
    public static final int MODE_CIRCLE       = 1;
    public static final int MODE_ROUNDED_RECT = 2;
    private final Map<String, Integer> loadedTextures = new HashMap<>();
    private final Map<String, DynamicTexture> atlasDynamicTextures = new HashMap<>();

    private int shaderProgram;
    private int blitShaderProgram;
    private final Map<String, Identifier> imageIds = new HashMap<>();

    // Cached uniform locations
    private int locScreenSize = -1, locBounds = -1, locParams = -1, locParams2 = -1, locScreen = -1, locBlurTex = -1;
    private int locInputTex = -1, locBlitScreenSize = -1;
    private int msdfShaderProgram;
    private int locMsdfSampler0 = -1, locMsdfScreenData = -1, locMsdfOutlineColor = -1, locMsdfAtlasData = -1;

    // Reusable VAO/VBO/EBO
    private int drawVAO, drawVBO, drawEBO;
    private int imageVAO, imageVBO, imageEBO;
    private int msdfVAO, msdfVBO, msdfEBO;

    private int cachedScreenW = -1, cachedScreenH = -1;
    private int cachedMcFBO = -1, cachedFbW = -1, cachedFbH = -1;
    private boolean fboBound = false;

    // State save для onRenderHud
    private int[] savedViewport = new int[4];
    private int savedDrawFBO = 0, savedProgram = 0;
    private boolean savedBlend = false, savedDepth = false, savedCull = false;

    // Pre-allocated buffers для фигур
    private float[] vertexDataCache = new float[54];
    private short[] indicesCache = new short[6];
    private ByteBuffer vertexByteBuffer = MemoryUtil.memAlloc(54 * 4);
    private ByteBuffer indexByteBuffer = MemoryUtil.memAlloc(6 * 2);

    // Pre-allocated buffers для Image (4 вершины)
    private float[] imageVertexCache = new float[4 * 5]; // 4 verts * (pos3 + uv2)
    private short[] imageIndexCache = {0, 2, 1, 0, 3, 2};
    private ByteBuffer imageVertexByteBuffer = MemoryUtil.memAlloc(4 * 5 * 4);
    private ByteBuffer imageIndexByteBuffer = MemoryUtil.memAlloc(6 * 2);

    // Pre-allocated buffers для MSDF (4 вершины)
    private float[] msdfVertexCache = new float[4 * 17]; // 4 verts * 17 floats
    private short[] msdfIndexCache = {0, 2, 1, 0, 3, 2};
    private ByteBuffer msdfVertexByteBuffer = MemoryUtil.memAlloc(4 * 17 * 4);
    private ByteBuffer msdfIndexByteBuffer = MemoryUtil.memAlloc(6 * 2);

    // msdf
    // MSDF Batch Rendering
    private static final int MAX_MSDF_CHARS = 128; // Лимит символов на один батч (16KB UBO limit!)
    private static final int UBO_SIZE = MAX_MSDF_CHARS * 4 * 16 + 16 + 16 + 16 + 16; // chars + screenData + outlineColor + atlasData + charCount
    private int msdfUBO;

    // Буфер для накопления символов
    private ByteBuffer msdfBatchBuffer = MemoryUtil.memAlloc(UBO_SIZE);
    private int currentBatchCount = 0;
    private float currentOutlineWidth = 0f;
    private int currentOutlineColor = 0;
    private float currentPxRange = 0f;
    private float currentAtlasWidth = 0f;
    private float currentAtlasHeight = 0f;
    private Identifier currentAtlasId = null;

    public void init() {
        shaderProgram = createShaderProgram(
                loadShaderFromResources("aporia:shaders/core/aporia.vsh"),
                loadShaderFromResources("aporia:shaders/core/aporia.fsh")
        );
        blitShaderProgram = createShaderProgram(
                loadShaderFromResources("aporia:shaders/core/blit.vsh"),
                loadShaderFromResources("aporia:shaders/core/blit.fsh")
        );
        msdfShaderProgram = createShaderProgram(
                loadShaderFromResources("aporia:shaders/core/msdf.vsh"),
                loadShaderFromResources("aporia:shaders/core/msdf.fsh")
        );

        locScreenSize = GL20.glGetUniformLocation(shaderProgram, "screenSize");
        locBounds     = GL20.glGetUniformLocation(shaderProgram, "bounds");
        locParams     = GL20.glGetUniformLocation(shaderProgram, "params");
        locParams2    = GL20.glGetUniformLocation(shaderProgram, "params2");
        locScreen     = GL20.glGetUniformLocation(shaderProgram, "screen");
        locBlurTex    = GL20.glGetUniformLocation(shaderProgram, "BlurTextureSampler");

        locInputTex   = GL20.glGetUniformLocation(blitShaderProgram, "InputTexture");
        locBlitScreenSize = GL20.glGetUniformLocation(blitShaderProgram, "screenSize");

        locMsdfSampler0 = GL20.glGetUniformLocation(msdfShaderProgram, "Sampler0");
        locMsdfScreenData = GL20.glGetUniformLocation(msdfShaderProgram, "ScreenData");
        locMsdfOutlineColor = GL20.glGetUniformLocation(msdfShaderProgram, "OutlineColor");
        locMsdfAtlasData = GL20.glGetUniformLocation(msdfShaderProgram, "AtlasData");

        drawVAO = GL30.glGenVertexArrays(); drawVBO = GL15.glGenBuffers(); drawEBO = GL15.glGenBuffers();
        imageVAO = GL30.glGenVertexArrays(); imageVBO = GL15.glGenBuffers(); imageEBO = GL15.glGenBuffers();
        msdfVAO = GL30.glGenVertexArrays();
        msdfVBO = GL15.glGenBuffers();
        GL30.glBindVertexArray(msdfVAO);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, msdfVBO);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, 4L, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(0, 1, GL15.GL_FLOAT, false, 0, 0);
        GL20.glEnableVertexAttribArray(0);
        GL30.glBindVertexArray(0);

        // Предзаливаем индексные буферы (они никогда не меняются для квада)
        imageIndexByteBuffer.clear().limit(12);
        imageIndexByteBuffer.asShortBuffer().put(imageIndexCache).flip();
        GL30.glBindVertexArray(imageVAO);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, imageEBO);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, imageIndexByteBuffer, GL15.GL_STATIC_DRAW);

        msdfIndexByteBuffer.clear().limit(12);
        msdfIndexByteBuffer.asShortBuffer().put(msdfIndexCache).flip();
        GL30.glBindVertexArray(msdfVAO);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, msdfEBO);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, msdfIndexByteBuffer, GL15.GL_STATIC_DRAW);

        GL30.glBindVertexArray(0);
        cachedScreenW = -1;
        cachedScreenH = -1;

        // UBO (Uniform Buffer Object)
        int UBO_SIZE = 64 + (MAX_MSDF_CHARS * 4 * 16); // 64 байта хедер + 128 * 64 байта символы = 8256 байт
        msdfUBO = GL30.glGenBuffers();
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, msdfUBO);
        GL15.glBufferData(GL31.GL_UNIFORM_BUFFER, UBO_SIZE, GL15.GL_DYNAMIC_DRAW);
        GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, 0, msdfUBO);
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);

        // Привязываем блок из шейдера к точке 0
        int blockIndex = GL31.glGetUniformBlockIndex(msdfShaderProgram, "FontData");
        if (blockIndex != -1) {
            GL31.glUniformBlockBinding(msdfShaderProgram, blockIndex, 0);
            Logger.info("MSDF FontData block bound to binding 0");
        }
    }

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

    private String loadShaderFromResources(String path) {
        try {
            String relPath = path.replace("aporia:", "aporia/");
            Path shaderPath = FilesManager.ROOT.resolve(".assets").resolve(relPath);
            if (Files.exists(shaderPath)) return Files.readString(shaderPath);
            Logger.error("Shader not found: " + shaderPath);
            return "";
        } catch (Exception e) {
            Logger.error("Failed to load shader: " + path + " - " + e.getMessage());
            return "";
        }
    }

    // =========================================================================
    //  Shape drawing methods
    // =========================================================================

    public void drawLine(float x1, float y1, float x2, float y2, float thickness, int color) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len == 0) return;
        float nx = -dy / len * thickness * 0.5f, ny = dx / len * thickness * 0.5f;
        float[] vx = { x1+nx, x1-nx, x2-nx, x2+nx }, vy = { y1+ny, y1-ny, y2-ny, y2+ny };
        draw(new float[][]{{vx[0], vy[0]}, {vx[1], vy[1]}, {vx[2], vy[2]}, {vx[0], vy[0]}, {vx[2], vy[2]}, {vx[3], vy[3]}},
                color, MODE_FILL, Math.min(x1, x2) - thickness, Math.min(y1, y2) - thickness, Math.abs(dx) + thickness * 2, Math.abs(dy) + thickness * 2, 0f);
    }

    public void drawFadeHLine(float centerX, float centerY, float halfLen, float thickness, float progress, int color) {
        float len = halfLen * progress;
        if (len < 1f) return;
        int segments = 20; float segW = len * 2f / segments; int a = (color >> 24) & 0xFF;
        for (int i = 0; i < segments; i++) {
            float x0 = centerX - len + i * segW, x1 = x0 + segW;
            int a0 = (int)(a * (1f - (Math.abs(x0 - centerX) / len) * (Math.abs(x0 - centerX) / len)));
            drawLine(x0, centerY, x1, centerY, thickness, (a0 << 24) | (color & 0x00FFFFFF));
        }
    }

    private static int lerp(int c0, int c1, float t) {
        return (((int)(((c0>>24)&0xFF) + (((c1>>24)&0xFF) - ((c0>>24)&0xFF)) * t)) << 24) |
                (((int)(((c0>>16)&0xFF) + (((c1>>16)&0xFF) - ((c0>>16)&0xFF)) * t)) << 16) |
                (((int)(((c0>> 8)&0xFF) + (((c1>> 8)&0xFF) - ((c0>> 8)&0xFF)) * t)) << 8)  |
                ((int)(((c0    )&0xFF) + (((c1    )&0xFF) - ((c0    )&0xFF)) * t));
    }

    public void drawCircle(float cx, float cy, float radius, int color) { float d = radius * 2; drawShape(cx - radius, cy - radius, d, d, color, MODE_CIRCLE, cx - radius, cy - radius, d, d, radius, 0, 0f, 0f); }
    public void drawTriangle(float x1, float y1, float x2, float y2, float x3, float y3, int color) { float bx = Math.min(x1, Math.min(x2, x3)), by = Math.min(y1, Math.min(y2, y3)); draw(new float[][]{{x1,y1},{x2,y2},{x3,y3}}, color, MODE_FILL, bx, by, Math.max(x1, Math.max(x2, x3)) - bx, Math.max(y1, Math.max(y2, y3)) - by, 0f); }
    public void resetDebugFlags() {}
    public void drawRectBlurred(float x, float y, float w, float h, float radius, int color) { drawRect(x, y, w, h, radius, color, 15); }
    public void drawRectBlurred(float x, float y, float w, float h, float radius, int color, float blurStrength, int cornerMask) { drawRect(x, y, w, h, radius, color, cornerMask); }
    public void drawRect(float x, float y, float w, float h, float radius, int color) { drawRect(x, y, w, h, radius, color, 15); }
    public void drawRect(float x, float y, float w, float h, float radius, int color, int cornerMask) { drawShape(x, y, w, h, color, radius > 0 ? MODE_ROUNDED_RECT : MODE_FILL, x, y, w, h, radius, 0, 0f, 0f, cornerMask); }

    public void drawRectGradient(float x, float y, float w, float h, float radius, int c1, int c2, int dir) {
        int steps = Math.max(2, (int)(dir==1 ? h : w) / 2);
        for (int i = 0; i < steps; i++) {
            float t0 = (float) i / steps, t1 = (float)(i + 1) / steps; int ca = lerp(c1, c2, (t0 + t1) * 0.5f);
            if (dir == 1) drawRect(x, y + t0 * h, w, (t1 - t0) * h, i==0||i==steps-1 ? radius : 0, ca);
            else if (dir == 0) drawRect(x + t0 * w, y, (t1 - t0) * w, h, i==0||i==steps-1 ? radius : 0, ca);
            else drawCircle(x + w/2f, y + h/2f, Math.min(w, h) * 0.5f * t1, lerp(c2, c1, t0));
        }
    }

    public void drawStroke(float x, float y, float w, float h, float radius, float thickness, int borderMode, float fadeCorner, int color) { drawShape(x, y, w, h, color, MODE_ROUNDED_RECT, x, y, w, h, radius, borderMode, thickness, fadeCorner); }
    private void drawShape(float x, float y, float w, float h, int color, int mode, float bx, float by, float bw, float bh, float radius, int borderMode, float thickness, float fadeCorner) { drawShape(x, y, w, h, color, mode, bx, by, bw, bh, radius, borderMode, thickness, fadeCorner, 15); }
    private void drawShape(float x, float y, float w, float h, int color, int mode, float bx, float by, float bw, float bh, float radius, int borderMode, float thickness, float fadeCorner, int cornerMask) { draw(new float[][]{{x, y+h}, {x+w, y+h}, {x+w, y}, {x, y+h}, {x+w, y}, {x, y}}, color, mode, bx, by, bw, bh, radius, borderMode, thickness, fadeCorner, cornerMask); }

    private void draw(float[][] verts, int color, int mode, float bx, float by, float bw, float bh, float radius) { draw(verts, color, mode, bx, by, bw, bh, radius, 0, 0f, 0f, 15); }
    private void draw(float[][] verts, int color, int mode, float bx, float by, float bw, float bh, float radius, int borderMode, float thickness, float fadeCorner) { draw(verts, color, mode, bx, by, bw, bh, radius, borderMode, thickness, fadeCorner, 15); }

    private void draw(float[][] verts, int color, int mode, float bx, float by, float bw, float bh, float radius, int borderMode, float thickness, float fadeCorner, int cornerMask) {
        Minecraft mc = Minecraft.getInstance();
        int screenW = mc.getWindow().getGuiScaledWidth(), screenH = mc.getWindow().getGuiScaledHeight();
        int fbW = mc.getWindow().getWidth(), fbH = mc.getWindow().getHeight();
        float a = ((color >> 24) & 0xFF) / 255f, r = ((color >> 16) & 0xFF) / 255f, g = ((color >> 8) & 0xFF) / 255f, b = (color & 0xFF) / 255f;
        int vertexCount = verts.length;

        for (int i = 0; i < vertexCount; i++) {
            int idx = i * 9;
            vertexDataCache[idx] = verts[i][0]; vertexDataCache[idx + 1] = verts[i][1]; vertexDataCache[idx + 2] = 0f;
            vertexDataCache[idx + 3] = 0f; vertexDataCache[idx + 4] = 0f;
            vertexDataCache[idx + 5] = r; vertexDataCache[idx + 6] = g; vertexDataCache[idx + 7] = b; vertexDataCache[idx + 8] = a;
            indicesCache[i] = (short) i;
        }

        vertexByteBuffer.clear().limit(vertexCount * 36);
        vertexByteBuffer.asFloatBuffer().put(vertexDataCache, 0, vertexCount * 9).flip();
        indexByteBuffer.clear().limit(vertexCount * 2);
        indexByteBuffer.asShortBuffer().put(indicesCache, 0, vertexCount).flip();

        // IF/ELSE: Проверяем, настроен ли уже стейт хуком onRenderHud
        if (!fboBound) {
            // Если мы тут, значит мы рисуем вне HUD (например, в чате/табе).
            // Берём контроль в свои руки! Выделяем буфер и пишем в него.

            // Обновляем кэш FBO
            if (cachedMcFBO == -1 || cachedScreenW != screenW || cachedScreenH != screenH) {
                var mainTarget = mc.getMainRenderTarget();
                var colorTex = mainTarget.getColorTexture(); var depthTex = mainTarget.getDepthTexture();
                if (colorTex != null) {
                    GlDevice glDevice = (GlDevice) RenderSystem.getDevice();
                    cachedMcFBO = ((GlTexture) colorTex).getFbo(glDevice.directStateAccess(), depthTex != null ? (GlTexture) depthTex : null);
                }
                cachedFbW = fbW; cachedFbH = fbH; cachedScreenW = screenW; cachedScreenH = screenH;
            }

            // Биндим FBO и настраиваем вьюпорт
            if (cachedMcFBO != -1) {
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, cachedMcFBO);
                GL11.glViewport(0, 0, cachedFbW, cachedFbH);
            }

            // Включаем бленд и отключаем глубину
            GL11.glEnable(GL11.GL_BLEND);
            GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_CULL_FACE);
        }
        // Если fboBound == true, то всё уже настроено, пропускаем дорогие вызовы!

        GL30.glBindVertexArray(drawVAO);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, drawVBO);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexByteBuffer, GL15.GL_DYNAMIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, drawEBO);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indexByteBuffer, GL15.GL_DYNAMIC_DRAW);

        GL20.glVertexAttribPointer(0, 3, GL15.GL_FLOAT, false, 36, 0); GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(1, 2, GL15.GL_FLOAT, false, 36, 12); GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(2, 4, GL15.GL_FLOAT, false, 36, 20); GL20.glEnableVertexAttribArray(2);

        GL20.glUseProgram(shaderProgram);
        if (locScreenSize >= 0) GL20.glUniform2f(locScreenSize, screenW, screenH);
        if (locBounds >= 0) GL20.glUniform4f(locBounds, bx, by, bw, bh);
        if (locParams >= 0) GL20.glUniform4f(locParams, radius, 1.0f, (float)mode, (float)borderMode);
        if (locParams2 >= 0) GL20.glUniform4f(locParams2, thickness, fadeCorner, 0f, 0.0f);
        if (locScreen >= 0) GL20.glUniform4f(locScreen, fbW, fbH, 0f, 0f);

        GL11.glDrawElements(GL11.GL_TRIANGLES, vertexCount, GL11.GL_UNSIGNED_SHORT, 0);
    }

    // =========================================================================
    //  HUD / Text methods
    // =========================================================================

    public void onRenderHud(Minecraft mc) {
        int screenW = mc.getWindow().getGuiScaledWidth(), screenH = mc.getWindow().getGuiScaledHeight();
        int fbW = mc.getWindow().getWidth(), fbH = mc.getWindow().getHeight();

        if (cachedMcFBO == -1 || cachedScreenW != screenW || cachedScreenH != screenH) {
            var mainTarget = mc.getMainRenderTarget();
            var colorTex = mainTarget.getColorTexture(); var depthTex = mainTarget.getDepthTexture();
            if (colorTex != null) {
                GlDevice glDevice = (GlDevice) RenderSystem.getDevice();
                cachedMcFBO = ((GlTexture) colorTex).getFbo(glDevice.directStateAccess(), depthTex != null ? (GlTexture) depthTex : null);
            }
            cachedFbW = fbW; cachedFbH = fbH; cachedScreenW = screenW; cachedScreenH = screenH;
        }

        GL11.glGetIntegerv(GL11.GL_VIEWPORT, savedViewport);
        savedDrawFBO = GL30.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        savedBlend = GL11.glIsEnabled(GL11.GL_BLEND);
        savedDepth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        savedCull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        savedProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);

        if (cachedMcFBO != -1) { GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, cachedMcFBO); GL11.glViewport(0, 0, cachedFbW, cachedFbH); }

        GL11.glEnable(GL11.GL_BLEND);
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        fboBound = true;
    }

    public void prepareFrameBlur(Minecraft mc, float strength, float saturation) {}
    public void invalidateBlurCache() { cachedMcFBO = -1; fboBound = false; }

    public void postRenderHud() {
        flushMsdfBatch();
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

    public void drawText(String font, String text, float x, float y, float size, int color) { Aporia.FONTS.drawText(font, text, x, y, size, color); }
    public void drawTextWithOutline(String font, String text, float x, float y, float size, int color, float outlineWidth, int outlineColor) { Aporia.FONTS.drawTextWithOutline(font, text, x, y, size, color, outlineWidth, outlineColor); }
    public float getTextWidth(String font, String text, float size) { return Aporia.FONTS.getTextWidth(font, text, size); }
    public void drawGlyph(int index, float x, float y, float size, int color) { Aporia.FONTS.drawGlyph(Fonts.FONT, index, x, y, size, color); }
    public void onRenderWorld(Minecraft mc) {}

    // =========================================================================
    //  Image loading
    // =========================================================================

    private static int getGlTextureId(DynamicTexture texture) {
        try { GpuTexture gpuTexture = texture.getTexture(); if (gpuTexture instanceof GlTexture glTexture) return glTexture.glId(); }
        catch (Exception e) { Logger.error("Failed to get GL texture id: " + e.getMessage()); }
        return -1;
    }

    public boolean isTextureLoaded(Identifier id) { return loadedTextures.containsKey(id.toString()); }

    public Identifier loadImage(InputStream stream) {
        try {
            NativeImage img = NativeImage.read(stream);
            return registerNativeImage(img, "stream_" + System.nanoTime());
        } catch (IOException e) {
            Logger.error("[loadImage] " + e.getMessage());
            return null;
        }
    }
    public Identifier loadImage(Path path) {
        String key = path.toAbsolutePath().toString();
        if (imageIds.containsKey(key)) return imageIds.get(key);
        try (FileInputStream fis = new FileInputStream(path.toFile())) { NativeImage img = NativeImage.read(fis); Identifier id = registerNativeImage(img, path.getFileName().toString()); imageIds.put(key, id); return id; }
        catch (IOException e) { Logger.warn("[loadImage] Failed to load: " + path + " — " + e.getMessage()); return null; }
    }

    private Identifier registerNativeImage(NativeImage img, String name) {
        DynamicTexture dynamicTexture = new DynamicTexture(() -> "aporia_image", img);
        int glId = getGlTextureId(dynamicTexture);
        if (glId == -1) { Logger.error("[loadImage] Failed to get GL texture ID!"); dynamicTexture.close(); return null; }
        String safeName = name.toLowerCase().replaceAll("[^a-z0-9_.-]", "_");
        Identifier id = Identifier.fromNamespaceAndPath("aporia", "user_image/" + safeName + "_" + Math.abs(name.hashCode()));
        loadedTextures.put(id.toString(), glId);
        return id;
    }

    // =========================================================================
    //  Image rendering (Оптимизировано: без аллокаций, без save/restore стейта)
    // =========================================================================

    public void drawImage(float x, float y, float w, float h, Identifier id) { drawImage(x, y, w, h, id, 0, 0f, 1f, 1f, 0f); }
    public void drawImage(float x, float y, float w, float h, Identifier id, float radius) { drawImage(x, y, w, h, id, radius, 0f, 1f, 1f, 0f); }
    public void drawSubImage(Identifier id, float x, float y, float w, float h, float u0, float v0, float u1, float v1) { drawImage(x, y, w, h, id, 0, u0, v0, u1, v1); }

    public void drawImage(float x, float y, float w, float h, Identifier id, float radius, float u0, float v0, float u1, float v1) {
        if (id == null) return;
        Integer glId = loadedTextures.get(id.toString());
        if (glId == null || glId == 0) return;

        Minecraft mc = Minecraft.getInstance();
        int sw = mc.getWindow().getGuiScaledWidth(), sh = mc.getWindow().getGuiScaledHeight();
        int fbW = mc.getWindow().getWidth(), fbH = mc.getWindow().getHeight(); // ОБЯЗАТЕЛЬНО получаем реальный FB

        // Обновляем FBO кэш (без аллокаций, как в onRenderHud)
        if (cachedMcFBO == -1 || cachedScreenW != sw || cachedScreenH != sh) {
            var mainTarget = mc.getMainRenderTarget();
            var colorTex = mainTarget.getColorTexture(); var depthTex = mainTarget.getDepthTexture();
            if (colorTex != null) {
                GlDevice glDevice = (GlDevice) RenderSystem.getDevice();
                cachedMcFBO = ((GlTexture) colorTex).getFbo(glDevice.directStateAccess(), depthTex != null ? (GlTexture) depthTex : null);
            }
            cachedFbW = fbW; cachedFbH = fbH; cachedScreenW = sw; cachedScreenH = sh;
        }

        // Биндим свой FBO и Viewport (чтобы рисовать прямо на экран)
        if (cachedMcFBO != -1) {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, cachedMcFBO);
            GL11.glViewport(0, 0, cachedFbW, cachedFbH);
        }

        // Заполняем кэш без аллокаций
        imageVertexCache[0] = x;     imageVertexCache[1] = y;     imageVertexCache[2] = 0f; imageVertexCache[3] = u0; imageVertexCache[4] = v1;
        imageVertexCache[5] = x + w; imageVertexCache[6] = y;     imageVertexCache[7] = 0f; imageVertexCache[8] = u1; imageVertexCache[9] = v1;
        imageVertexCache[10]= x + w; imageVertexCache[11]= y + h; imageVertexCache[12]= 0f; imageVertexCache[13]= u1; imageVertexCache[14]= v0;
        imageVertexCache[15]= x;     imageVertexCache[16]= y + h; imageVertexCache[17]= 0f; imageVertexCache[18]= u0; imageVertexCache[19]= v0;

        imageVertexByteBuffer.clear().limit(4 * 5 * 4);
        imageVertexByteBuffer.asFloatBuffer().put(imageVertexCache).flip();

        GL30.glBindVertexArray(imageVAO);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, imageVBO);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, imageVertexByteBuffer, GL15.GL_DYNAMIC_DRAW);

        GL20.glVertexAttribPointer(0, 3, GL15.GL_FLOAT, false, 20, 0); GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(1, 2, GL15.GL_FLOAT, false, 20, 12); GL20.glEnableVertexAttribArray(1);
        GL20.glDisableVertexAttribArray(2);

        // Бленд и стейт (обязательно, если вызываем вне onRenderHud)
        GL11.glEnable(GL11.GL_BLEND);
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);

        int prevProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM); // Сохраняем только шейдер

        GL20.glUseProgram(blitShaderProgram);
        if (locBlitScreenSize >= 0) GL20.glUniform2f(locBlitScreenSize, sw, sh);
        if (locInputTex >= 0) GL20.glUniform1i(locInputTex, 0);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, glId);

        GL11.glDrawElements(GL11.GL_TRIANGLES, 6, GL11.GL_UNSIGNED_SHORT, 0);

        // Восстанавливаем только шейдер, чтобы не сломать последующую отрисовку ванилы
        GL20.glUseProgram(prevProgram);
    }

    /**
     * Сбрасывает накопленный батч на экран.
     */
    private void flushMsdfBatch() {
        if (currentBatchCount == 0) return;

        // ОБЯЗАТЕЛЬНО: обновляем количество символов перед отправкой на GPU
        msdfBatchBuffer.putInt(48, currentBatchCount);

        Minecraft mc = Minecraft.getInstance();
        int sw = mc.getWindow().getGuiScaledWidth(), sh = mc.getWindow().getGuiScaledHeight();
        int fbW = mc.getWindow().getWidth(), fbH = mc.getWindow().getHeight();

        if (!fboBound) {
            if (cachedMcFBO == -1 || cachedScreenW != sw || cachedScreenH != sh) {
                var mainTarget = mc.getMainRenderTarget();
                var colorTex = mainTarget.getColorTexture(); var depthTex = mainTarget.getDepthTexture();
                if (colorTex != null) {
                    GlDevice glDevice = (GlDevice) RenderSystem.getDevice();
                    cachedMcFBO = ((GlTexture) colorTex).getFbo(glDevice.directStateAccess(), depthTex != null ? (GlTexture) depthTex : null);
                }
                cachedFbW = fbW; cachedFbH = fbH; cachedScreenW = sw; cachedScreenH = sh;
            }
            if (cachedMcFBO != -1) { GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, cachedMcFBO); GL11.glViewport(0, 0, cachedFbW, cachedFbH); }
            GL11.glEnable(GL11.GL_BLEND);
            GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDisable(GL11.GL_DEPTH_TEST); GL11.glDisable(GL11.GL_CULL_FACE);
        }

        msdfBatchBuffer.flip();

        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, msdfUBO);
        GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 0, msdfBatchBuffer);
        GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, 0, msdfUBO);

        int prevProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        GL20.glUseProgram(msdfShaderProgram);

        if (locMsdfSampler0 >= 0) GL20.glUniform1i(locMsdfSampler0, 0);

        Integer glId = loadedTextures.get(currentAtlasId.toString());
        if (glId != null && glId != 0) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, glId);
        } else {
            // ЕСЛИ ЭТО ВЫЛЕЗЕТ В КОНСОЛИ — ЗНАЧИТ ТЕКСТУРА ВСЁ ЕЩЁ НЕ НАЙДЕНА
            Logger.error("MSDF FLUSH ERROR: Texture not found for " + currentAtlasId);
            return; // Прерываем, чтобы не рисовать черноту
        }

        GL30.glBindVertexArray(msdfVAO);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, currentBatchCount * 6);

        GL20.glUseProgram(prevProgram);
        GL30.glBindVertexArray(0);

        currentBatchCount = 0;
        msdfBatchBuffer.clear();
        prepareMsdfUBOHeader();
    }

    private void prepareMsdfUBOHeader() {
        Minecraft mc = Minecraft.getInstance();
        int sw = mc.getWindow().getGuiScaledWidth(), sh = mc.getWindow().getGuiScaledHeight();
        float oR = ((currentOutlineColor >> 16) & 0xFF) / 255f;
        float oG = ((currentOutlineColor >> 8) & 0xFF) / 255f;
        float oB = (currentOutlineColor & 0xFF) / 255f;
        float oA = ((currentOutlineColor >> 24) & 0xFF) / 255f;

        msdfBatchBuffer.putFloat(0, sw);
        msdfBatchBuffer.putFloat(4, sh);
        msdfBatchBuffer.putFloat(8, mc.getWindow().getGuiScale());
        msdfBatchBuffer.putFloat(12, currentOutlineWidth);

        msdfBatchBuffer.putFloat(16, oR);
        msdfBatchBuffer.putFloat(20, oG);
        msdfBatchBuffer.putFloat(24, oB);
        msdfBatchBuffer.putFloat(28, oA);

        msdfBatchBuffer.putFloat(32, currentAtlasWidth);
        msdfBatchBuffer.putFloat(36, currentAtlasHeight);
        msdfBatchBuffer.putFloat(40, currentPxRange);
        msdfBatchBuffer.putFloat(44, 0);

        // ВОЗВРАЩАЕМ putInt! Шейдер ожидает бинарный int!
        msdfBatchBuffer.putInt(48, currentBatchCount);
        msdfBatchBuffer.putInt(52, 0);
        msdfBatchBuffer.putInt(56, 0);
        msdfBatchBuffer.putInt(60, 0);

        msdfBatchBuffer.position(64);
    }

    public void drawMsdfGlyph(Identifier texId, float x, float y, float w, float h,
                              float u0, float v0, float u1, float v1,
                              int color, float outlineWidth, int outlineColor,
                              float pxRange, float atlasWidth, float atlasHeight) {

        if (currentBatchCount > 0 && (currentAtlasId != texId || currentOutlineWidth != outlineWidth || currentOutlineColor != outlineColor || currentPxRange != pxRange)) {
            flushMsdfBatch();
        }

        currentAtlasId = texId;
        currentOutlineWidth = outlineWidth;
        currentOutlineColor = outlineColor;
        currentPxRange = pxRange;
        currentAtlasWidth = atlasWidth;
        currentAtlasHeight = atlasHeight;

        if (currentBatchCount == 0) {
            prepareMsdfUBOHeader();
        }

        msdfBatchBuffer.putFloat(x);
        msdfBatchBuffer.putFloat(y);
        msdfBatchBuffer.putFloat(w);
        msdfBatchBuffer.putFloat(h);

        msdfBatchBuffer.putFloat(u0);
        msdfBatchBuffer.putFloat(v0);
        msdfBatchBuffer.putFloat(u1);
        msdfBatchBuffer.putFloat(v1);

        msdfBatchBuffer.putFloat(((color >> 16) & 0xFF) / 255f);
        msdfBatchBuffer.putFloat(((color >> 8) & 0xFF) / 255f);
        msdfBatchBuffer.putFloat((color & 0xFF) / 255f);
        msdfBatchBuffer.putFloat(((color >> 24) & 0xFF) / 255f);

        msdfBatchBuffer.putFloat(0f);
        msdfBatchBuffer.putFloat(0f);
        msdfBatchBuffer.putFloat(0f);
        msdfBatchBuffer.putFloat(0f);

        currentBatchCount++;

        if (currentBatchCount >= MAX_MSDF_CHARS) {
            flushMsdfBatch();
        }
    }

    public void loadAtlasTexture(Identifier id, Path path) {
        if (loadedTextures.containsKey(id.toString())) return;

        try (InputStream stream = Files.newInputStream(path)) {
            NativeImage img = NativeImage.read(stream);
            DynamicTexture dynamicTexture = new DynamicTexture(() -> "aporia_msdf", img);
            int glId = getGlTextureId(dynamicTexture);
            if (glId == -1) {
                dynamicTexture.close();
                return;
            }
            loadedTextures.put(id.toString(), glId);
            atlasDynamicTextures.put(id.toString(), dynamicTexture);
            Logger.info("[loadAtlasTexture] OK glId=" + glId + " for " + id);
        } catch (IOException e) {
            Logger.error("[loadAtlasTexture] Failed: " + e.getMessage());
        }
    }

    // =========================================================================
    //  Cleanup
    // =========================================================================

    /**
     * Освобождает ВСЕ ресурсы: шейдеры, буферы, текстуры и память.
     * Вызывать при закрытии/перезагрузке!
     */
    public void cleanup() {
        // 1. Удаляем шейдерные программы
        if (shaderProgram != 0) { GL20.glDeleteProgram(shaderProgram); shaderProgram = 0; }
        if (blitShaderProgram != 0) { GL20.glDeleteProgram(blitShaderProgram); blitShaderProgram = 0; }
        if (msdfShaderProgram != 0) { GL20.glDeleteProgram(msdfShaderProgram); msdfShaderProgram = 0; }

        // 2. Удаляем Vertex Arrays и Buffers (Фигуры)
        if (drawVAO != 0) { GL30.glDeleteVertexArrays(drawVAO); drawVAO = 0; }
        if (drawVBO != 0) { GL15.glDeleteBuffers(drawVBO); drawVBO = 0; }
        if (drawEBO != 0) { GL15.glDeleteBuffers(drawEBO); drawEBO = 0; }

        // 3. Удаляем Vertex Arrays и Buffers (Картинки)
        if (imageVAO != 0) { GL30.glDeleteVertexArrays(imageVAO); imageVAO = 0; }
        if (imageVBO != 0) { GL15.glDeleteBuffers(imageVBO); imageVBO = 0; }
        if (imageEBO != 0) { GL15.glDeleteBuffers(imageEBO); imageEBO = 0; }

        // 4. Удаляем Vertex Arrays и Buffers (MSDF Текст)
        if (msdfVAO != 0) { GL30.glDeleteVertexArrays(msdfVAO); msdfVAO = 0; }
        if (msdfVBO != 0) { GL15.glDeleteBuffers(msdfVBO); msdfVBO = 0; }
        if (msdfEBO != 0) { GL15.glDeleteBuffers(msdfEBO); msdfEBO = 0; }

        // 5. ОЧЕНЬ ВАЖНО: Удаляем загруженные текстуры из видеопамяти (VRAM)!
        for (int glId : loadedTextures.values()) {
            if (glId != 0) {
                GL11.glDeleteTextures(glId);
            }
        }
        loadedTextures.clear();
        imageIds.clear();

        // 6. Освобождаем Java-память (Direct ByteBuffer)
        if (vertexByteBuffer != null) { MemoryUtil.memFree(vertexByteBuffer); vertexByteBuffer = null; }
        if (indexByteBuffer != null) { MemoryUtil.memFree(indexByteBuffer); indexByteBuffer = null; }
        if (imageVertexByteBuffer != null) { MemoryUtil.memFree(imageVertexByteBuffer); imageVertexByteBuffer = null; }
        if (imageIndexByteBuffer != null) { MemoryUtil.memFree(imageIndexByteBuffer); imageIndexByteBuffer = null; }
        if (msdfVertexByteBuffer != null) { MemoryUtil.memFree(msdfVertexByteBuffer); msdfVertexByteBuffer = null; }
        if (msdfIndexByteBuffer != null) { MemoryUtil.memFree(msdfIndexByteBuffer); msdfIndexByteBuffer = null; }

        // Сбрасываем кэши
        cachedMcFBO = -1;
        cachedScreenW = -1;
        cachedScreenH = -1;
        fboBound = false;
    }
}