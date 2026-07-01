/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.utils.user.render.core;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import so.aporia.Aporia;
import so.aporia.utils.user.logger.Logger;
import so.aporia.utils.user.render.font.Fonts;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public class AporiaRenderer {
    public static final AporiaRenderer INSTANCE = new AporiaRenderer();
    public static final int MODE_FILL         = 0;
    public static final int MODE_CIRCLE       = 1;
    public static final int MODE_ROUNDED_RECT = 2;

    /** World rendering projection matrix (with view bob, hurt bob, portal effect). Set by GameRenderer each frame. */
    public static Matrix4f worldProjMatrix = new Matrix4f();
    /** World rendering view matrix (camera rotation only). Set by GameRenderer each frame. */
    public static Matrix4f worldViewMatrix = new Matrix4f();

    private float currentDepth = 0f;

    private final List<DrawTask> taskQueue = new ArrayList<>();

    private static final class DrawTask {
        final float z;
        final Runnable action;
        DrawTask(float z, Runnable action) { this.z = z; this.action = action; }
    }

    public void depth(float z) {
        this.currentDepth = z;
    }

    public void flush() {
        taskQueue.sort(Comparator.comparingDouble((DrawTask t) -> t.z));
        for (DrawTask task : taskQueue) task.action.run();
        taskQueue.clear();
    }

    RenderPipeline pipeline;
    private RenderPipeline imagePipeline;
    private RenderPipeline roundedRectPipeline;
    CachedOrthoProjectionMatrixBuffer orthoProjection;
    private RenderPipeline blitPipeline;
    private RenderPipeline postPipeline;
    private RenderPipeline mainmenuPipeline;
    private RenderPipeline entityGlowPipeline;
    private GpuBuffer entityGlowIBO;

    private ByteBuffer cachedMainmenuBB;
    /** 128 байт для ShapeData / 128 bytes for ShapeData. */
    private ByteBuffer cachedShapeBB;
    /** 128 байт для drawImage / 128 bytes for drawImage. */
    private ByteBuffer cachedImageBB;
    /** 16 байт для post-process / 16 bytes for post-process. */
    private ByteBuffer cachedPostBB;

    /** Пост-обработка сатурации / Saturation post-processing. */
    private TextureTarget postTempTarget;
    private int postTempW = -1, postTempH = -1;

    private final Map<String, Identifier>      imageIds     = new HashMap<>();
    private final Map<String, DynamicTexture>  imageTextures = new HashMap<>();

    /** Кэшированные буферы для всей 2D-геометрии (позиция + UV + цвет).
     *  Cached buffers for all 2D geometry (position + UV + color). */
    private GpuBuffer cachedVertexBuffer;
    private GpuBuffer cachedBlitVertexBuffer;
    private GpuBuffer cachedImageVertexBuffer;

    private GpuBuffer cachedShapeBuffer;
    private GpuBuffer cachedImageShapeBuffer;
    /** UBO для mainmenu шейдера / UBO for mainmenu shader. */
    private GpuBuffer mainmenuUbo;
    /** UBO для post-process (16 байт) / UBO for post-process (16 bytes). */
    private GpuBuffer postUbo;

    /** VBO для entity glow / VBO for entity glow. */
    private GpuBuffer entityGlowVBO;
    /** UBO для ModelViewProj (3×mat4 = 192 байта) / UBO for ModelViewProj. */
    private GpuBuffer entityGlowMVP;
    /** UBO для DrawParams (2×vec4 = 32 байта) / UBO for DrawParams. */
    private GpuBuffer entityGlowParams;

    public void init() {
        pipeline = RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/aporia"))
                .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/aporia"))
                .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/aporia"))
                .withUniform("Projection", UniformType.UNIFORM_BUFFER)
                .withUniform("ShapeData",  UniformType.UNIFORM_BUFFER)
                .withSampler("BlurTextureSampler")
                .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.TRIANGLES)
                .withBlend(new BlendFunction(
                        com.mojang.blaze3d.platform.SourceFactor.SRC_ALPHA,
                        com.mojang.blaze3d.platform.DestFactor.ONE_MINUS_SRC_ALPHA,
                        com.mojang.blaze3d.platform.SourceFactor.SRC_ALPHA,
                        com.mojang.blaze3d.platform.DestFactor.ONE_MINUS_SRC_ALPHA
                ))
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withCull(false)
                .build();

        imagePipeline = RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/image"))
                .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/image"))
                .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/image"))
                .withUniform("Projection", UniformType.UNIFORM_BUFFER)
                .withUniform("ShapeData",  UniformType.UNIFORM_BUFFER)
                .withSampler("ImageTextureSampler")
                .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.TRIANGLES)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withCull(false)
                .build();

        roundedRectPipeline = RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/rounded_rect"))
                .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/rounded_rect"))
                .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/rounded_rect"))
                .withUniform("Projection", UniformType.UNIFORM_BUFFER)
                .withUniform("ShapeData",  UniformType.UNIFORM_BUFFER)
                .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.TRIANGLES)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withCull(false)
                .build();

        orthoProjection = new CachedOrthoProjectionMatrixBuffer("aporia", -1000f, 1000f, true);

        blitPipeline = RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/blur_blit"))
                .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/blit"))
                .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/blit"))
                .withSampler("InputTexture")
                .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.TRIANGLES)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withCull(false)
                .build();

        postPipeline = RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/postprocess"))
                .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/postprocess"))
                .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/postprocess"))
                .withSampler("InputTexture")
                .withUniform("PostData", UniformType.UNIFORM_BUFFER)
                .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.TRIANGLES)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withCull(false)
                .build();

        mainmenuPipeline = RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/mainmenu"))
                .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/mainmenu"))
                .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/mainmenu"))
                .withUniform("Time", UniformType.UNIFORM_BUFFER)
                .withUniform("Resolution", UniformType.UNIFORM_BUFFER)
                .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.TRIANGLES)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withCull(false)
                .build();

        entityGlowPipeline = RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/entity_glow"))
                .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/entity_glow"))
                .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/entity_glow"))
                .withUniform("ModelViewProj", UniformType.UNIFORM_BUFFER)
                .withUniform("DrawParams", UniformType.UNIFORM_BUFFER)
                .withVertexFormat(DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.TRIANGLES)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withCull(false)
                .build();

        var device = RenderSystem.getDevice();
        BlurRenderer.init(device);
        /** Кэшированные буферы: VBO на 6 вершин (позиция, uv, цвет) — 6 * 36 = 216 байт, округлим до 256.
         *  Cached buffers: VBO for 6 vertices (position, uv, color) — 6 * 36 = 216 bytes, rounded to 256. */
        cachedVertexBuffer = device.createBuffer(() -> "aporia:cached_vbo",
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, 256L);
        /** UBO для ShapeData — 128 байт / UBO for ShapeData — 128 bytes. */
        cachedShapeBuffer = device.createBuffer(() -> "aporia:cached_shape",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, 128L);
        /** VBO для блита (POSITION_TEX) — 6 вершин * 20 байт = 120, округляем до 128.
         *  VBO for blit (POSITION_TEX) — 6 vertices * 20 bytes = 120, rounded to 128. */
        cachedBlitVertexBuffer = device.createBuffer(() -> "aporia:cached_blit_vbo",
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, 128L);
        cachedImageVertexBuffer = device.createBuffer(() -> "aporia:cached_image_vbo",
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, 256L);
        cachedImageShapeBuffer = device.createBuffer(() -> "aporia:cached_image_shape",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, 128L);

        /** UBO для mainmenu шейдера (time + resolution = 24 байта).
         *  UBO for mainmenu shader (time + resolution = 24 bytes). */
        mainmenuUbo = device.createBuffer(() -> "aporia:mainmenu_ubo",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, 32L);

        /** UBO для post-process (saturation = 16 байт).
         *  UBO for post-process (saturation = 16 bytes). */
        postUbo = device.createBuffer(() -> "aporia:post_ubo",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, 16L);

        /** VBO для entity glow (достаточно для ~4000 вершин).
         *  VBO for entity glow (enough for ~4000 vertices). */
        entityGlowVBO = device.createBuffer(() -> "aporia:entity_glow_vbo",
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, 262144L);
        /** UBO ModelViewProj: 3×mat4 = 192 байт.
         *  UBO ModelViewProj: 3×mat4 = 192 bytes. */
        entityGlowMVP = device.createBuffer(() -> "aporia:entity_glow_mvp",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, 192L);
        /** UBO DrawParams: 2×vec4 = 32 байта.
         *  UBO DrawParams: 2×vec4 = 32 bytes. */
        entityGlowParams = device.createBuffer(() -> "aporia:entity_glow_params",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, 32L);

        int maxQuads = 2000; // 8000 vertices / 4
        int maxIndices = maxQuads * 6;
        entityGlowIBO = device.createBuffer(() -> "aporia:entity_glow_ibo",
                GpuBuffer.USAGE_INDEX | GpuBuffer.USAGE_COPY_DST, (long)maxIndices * 2L);
    }

    public void drawMainMenuBackground(float time, int width, int height) {
        if (mainmenuPipeline == null) return;

        var mainTarget = Minecraft.getInstance().getMainRenderTarget();
        var colorView = mainTarget.getColorTextureView();
        if (colorView == null) return;

        var device = RenderSystem.getDevice();
        var encoder = device.createCommandEncoder();

        if (cachedMainmenuBB == null) cachedMainmenuBB = ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder());
        cachedMainmenuBB.clear();
        cachedMainmenuBB.putFloat(time);
        cachedMainmenuBB.putFloat((float)width);
        cachedMainmenuBB.putFloat((float)height);
        cachedMainmenuBB.flip();
        encoder.writeToBuffer(mainmenuUbo.slice(), cachedMainmenuBB);

        var tess = Tesselator.getInstance();
        var buf = tess.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX);
        buf.addVertex(0f, height, 0f).setUv(0f, 1f);
        buf.addVertex(0f, 0f, 0f).setUv(0f, 0f);
        buf.addVertex(width, 0f, 0f).setUv(1f, 0f);
        buf.addVertex(0f, height, 0f).setUv(0f, 1f);
        buf.addVertex(width, 0f, 0f).setUv(1f, 0f);
        buf.addVertex(width, height, 0f).setUv(1f, 1f);
        var mesh = buf.buildOrThrow();
        var vbo = device.createBuffer(() -> "aporia:mainmenu_vbo",
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, mesh.vertexBuffer());
        mesh.close();

        var projSlice = orthoProjection.getBuffer(width, height);
        RenderSystem.setProjectionMatrix(projSlice, ProjectionType.ORTHOGRAPHIC);

        try (var pass = encoder.createRenderPass(() -> "aporia:mainmenu_bg", colorView, OptionalInt.empty())) {
            pass.setPipeline(mainmenuPipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("Time", mainmenuUbo.slice());
            pass.setVertexBuffer(0, vbo);
            pass.setIndexBuffer(RenderSystem.getSequentialBuffer(VertexFormat.Mode.TRIANGLES).getBuffer(6),
                    RenderSystem.getSequentialBuffer(VertexFormat.Mode.TRIANGLES).type());
            pass.drawIndexed(0, 0, 6, 0);
        }

        vbo.close();
    }

    /* ===========================
       Public drawing API
       =========================== */

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

    private static int lerp(int c0, int c1, float t) {
        int a = (int)(((c0>>24)&0xFF) + (((c1>>24)&0xFF) - ((c0>>24)&0xFF)) * t);
        int r = (int)(((c0>>16)&0xFF) + (((c1>>16)&0xFF) - ((c0>>16)&0xFF)) * t);
        int g = (int)(((c0>> 8)&0xFF) + (((c1>> 8)&0xFF) - ((c0>> 8)&0xFF)) * t);
        int b = (int)(((c0    )&0xFF) + (((c1    )&0xFF) - ((c0    )&0xFF)) * t);
        return (a<<24)|(r<<16)|(g<<8)|b;
    }

    public void drawCircle(float cx, float cy, float radius, int color) {
        float d = radius * 2;
        drawShape(cx - radius, cy - radius, d, d, color, MODE_CIRCLE,
                cx - radius, cy - radius, d, d, radius, 0, 0f, 0f);
    }

    public void drawTriangle(float x1, float y1, float x2, float y2, float x3, float y3, int color) {
        float bx = Math.min(x1, Math.min(x2, x3));
        float by = Math.min(y1, Math.min(y2, y3));
        float bw = Math.max(x1, Math.max(x2, x3)) - bx;
        float bh = Math.max(y1, Math.max(y2, y3)) - by;
        draw(new float[][]{{x1,y1},{x2,y2},{x3,y3}}, color, MODE_FILL, bx, by, bw, bh, 0f);
    }

    public void drawRectBlurred(float x, float y, float w, float h, float radius, int color, float blurStrength) {
        drawRectBlurred(x, y, w, h, radius, color, blurStrength, 15);
    }

    public void drawRectBlurred(float x, float y, float w, float h, float radius, int color) {
        drawRectBlurred(x, y, w, h, radius, color, 4f, 15);
    }

    public void drawRectBlurred(float x, float y, float w, float h, float radius, int color, float blurStrength, int cornerMask) {
        Minecraft mc = Minecraft.getInstance();
        boolean gui = BlurRenderer.useGuiBlur;
        if (gui ? (!BlurRenderer.guiBlurReady || pipeline == null || BlurRenderer.guiBlurTarget == null)
                : (!BlurRenderer.blurReady || pipeline == null || BlurRenderer.blurTarget == null)) {
            drawRect(x, y, w, h, radius, color);
            return;
        }
        var mainTarget = mc.getMainRenderTarget();
        var window     = mc.getWindow();
        float sw = window.getGuiScaledWidth();
        float sh = window.getGuiScaledHeight();
        var colorView  = mainTarget.getColorTextureView();
        if (colorView == null) return;

        /** Разбираем переданный цвет и передаём напрямую (без pre-multiply).
         *  Unpack the passed color and pass it directly (no pre-multiply). */
        float overlayA = ((color >> 24) & 0xFF) / 255f;
        float overlayR = ((color >> 16) & 0xFF) / 255f;
        float overlayG = ((color >>  8) & 0xFF) / 255f;
        float overlayB = ((color      ) & 0xFF) / 255f;

        var projSlice = orthoProjection.getBuffer(sw, sh);
        RenderSystem.setProjectionMatrix(projSlice, ProjectionType.ORTHOGRAPHIC);

        var tess = Tesselator.getInstance();
        var buf  = tess.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR);

        // Первый треугольник (Левый верх -> Правый верх -> Левый низ)
        buf.addVertex(x,   y,   0f).setUv(0f, 1f).setColor(overlayR, overlayG, overlayB, overlayA);
        buf.addVertex(x+w, y,   0f).setUv(1f, 1f).setColor(overlayR, overlayG, overlayB, overlayA);
        buf.addVertex(x,   y+h, 0f).setUv(0f, 0f).setColor(overlayR, overlayG, overlayB, overlayA);

        // Второй треугольник (Правый верх -> Правый низ -> Левый низ)
        buf.addVertex(x+w, y,   0f).setUv(1f, 1f).setColor(overlayR, overlayG, overlayB, overlayA);
        buf.addVertex(x+w, y+h, 0f).setUv(1f, 0f).setColor(overlayR, overlayG, overlayB, overlayA);
        buf.addVertex(x,   y+h, 0f).setUv(0f, 0f).setColor(overlayR, overlayG, overlayB, overlayA);


        var mesh = buf.buildOrThrow();
        var device  = RenderSystem.getDevice();
        var encoder = device.createCommandEncoder();
        encoder.writeToBuffer(cachedVertexBuffer.slice(), mesh.vertexBuffer());
        mesh.close();

        if (cachedShapeBB == null) cachedShapeBB = ByteBuffer.allocateDirect(128).order(ByteOrder.nativeOrder());
        cachedShapeBB.clear();
        cachedShapeBB.putFloat(x); cachedShapeBB.putFloat(y); cachedShapeBB.putFloat(w); cachedShapeBB.putFloat(h);
        cachedShapeBB.putFloat(radius); cachedShapeBB.putFloat(1.0f); cachedShapeBB.putFloat(MODE_ROUNDED_RECT); cachedShapeBB.putFloat(0f);
        cachedShapeBB.putFloat(0f); cachedShapeBB.putFloat(0f); cachedShapeBB.putFloat(1f); cachedShapeBB.putFloat((float) cornerMask);
        cachedShapeBB.putFloat((float) mainTarget.width); cachedShapeBB.putFloat((float) mainTarget.height); cachedShapeBB.putFloat(0f); cachedShapeBB.putFloat(0f);
        cachedShapeBB.flip();
        encoder.writeToBuffer(cachedShapeBuffer.slice(), cachedShapeBB);

        var indexBuf = RenderSystem.getSequentialBuffer(VertexFormat.Mode.TRIANGLES);
        try (var pass = encoder.createRenderPass(() -> "aporia:blur_rect", colorView, OptionalInt.empty())) {
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("ShapeData", cachedShapeBuffer.slice());
            pass.bindTexture("BlurTextureSampler",
                    BlurRenderer.useGuiBlur ? BlurRenderer.guiBlurTarget.getColorTextureView() : BlurRenderer.blurTarget.getColorTextureView(),
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            pass.setVertexBuffer(0, cachedVertexBuffer);
            pass.setIndexBuffer(indexBuf.getBuffer(6), indexBuf.type());
            pass.drawIndexed(0, 0, 6, 0);
        }
    }

    public void drawRect(float x, float y, float w, float h, float radius, int color) {
        drawRect(x, y, w, h, radius, color, 15);
    }

    public void drawRect(float x, float y, float w, float h, float radius, int color, int cornerMask) {
        int mode = radius > 0 ? MODE_ROUNDED_RECT : MODE_FILL;
        drawShape(x, y, w, h, color, mode, x, y, w, h, radius, 0, 0f, 0f, cornerMask);
    }

    public void drawStroke(float x, float y, float w, float h, float radius,
                           float thickness, int borderMode, float fadeCorner, int color) {
        drawShape(x, y, w, h, color, MODE_ROUNDED_RECT, x, y, w, h, radius,
                borderMode, thickness, fadeCorner);
    }

    /* ===========================
       Internal draw with caching
       =========================== */

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

    private void draw(float[][] verts, int color, int mode,
                      float bx, float by, float bw, float bh, float radius) {
        draw(verts, color, mode, bx, by, bw, bh, radius, 0, 0f, 0f, 15);
    }

    private void draw(float[][] verts, int color, int mode,
                      float bx, float by, float bw, float bh, float radius,
                      int borderMode, float thickness, float fadeCorner, int cornerMask) {
        Minecraft mc = Minecraft.getInstance();
        var window    = mc.getWindow();
        var colorView = mc.getMainRenderTarget().getColorTextureView();
        if (colorView == null || pipeline == null) return;
        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >>  8) & 0xFF) / 255f;
        float b = ((color      ) & 0xFF) / 255f;
        var projSlice = orthoProjection.getBuffer(window.getGuiScaledWidth(), window.getGuiScaledHeight());
        RenderSystem.setProjectionMatrix(projSlice, ProjectionType.ORTHOGRAPHIC);

        var tess = Tesselator.getInstance();
        var buf = tess.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (float[] v : verts) {
            buf.addVertex(v[0], v[1], currentDepth).setUv(0f, 0f).setColor(r, g, b, a);
        }
        var mesh = buf.buildOrThrow();
        var device  = RenderSystem.getDevice();
        var encoder = device.createCommandEncoder();
        encoder.writeToBuffer(cachedVertexBuffer.slice(), mesh.vertexBuffer());
        mesh.close();

        if (cachedShapeBB == null) cachedShapeBB = ByteBuffer.allocateDirect(128).order(ByteOrder.nativeOrder());
        cachedShapeBB.clear();
        cachedShapeBB.putFloat(bx); cachedShapeBB.putFloat(by); cachedShapeBB.putFloat(bw); cachedShapeBB.putFloat(bh);
        cachedShapeBB.putFloat(radius); cachedShapeBB.putFloat(1.0f); cachedShapeBB.putFloat(mode); cachedShapeBB.putFloat((float) borderMode);
        cachedShapeBB.putFloat(thickness); cachedShapeBB.putFloat(fadeCorner); cachedShapeBB.putFloat(0f); cachedShapeBB.putFloat((float) cornerMask);
        cachedShapeBB.putFloat(0f); cachedShapeBB.putFloat(0f); cachedShapeBB.putFloat(0f); cachedShapeBB.putFloat(0f);
        cachedShapeBB.flip();
        encoder.writeToBuffer(cachedShapeBuffer.slice(), cachedShapeBB);

        var indexBuf = RenderSystem.getSequentialBuffer(VertexFormat.Mode.TRIANGLES);
        int vertexCount = verts.length;
        try (var pass = encoder.createRenderPass(() -> "aporia:draw", colorView, OptionalInt.empty())) {
            pass.setPipeline(roundedRectPipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("ShapeData", cachedShapeBuffer.slice());
            pass.setVertexBuffer(0, cachedVertexBuffer);
            pass.setIndexBuffer(indexBuf.getBuffer(vertexCount), indexBuf.type());
            pass.drawIndexed(0, 0, vertexCount, 0);
        }
    }

    /* ===========================
       Images & text
       =========================== */

    public void drawImage(float x, float y, float w, float h, Identifier id) {
        drawImage(x, y, w, h, id, 0);
    }

    public void drawImage(float x, float y, float w, float h, Identifier id, float radius) {
        if (id == null) return;
        Minecraft mc = Minecraft.getInstance();
        var tex = mc.getTextureManager().getTexture(id);
        if (tex == null || tex.getTextureView() == null) return;
        var colorView = mc.getMainRenderTarget().getColorTextureView();
        if (colorView == null) return;

        if (radius <= 0) {
            float sw = mc.getWindow().getGuiScaledWidth();
            float sh = mc.getWindow().getGuiScaledHeight();
            float nx0 = -1f + 2f * x / sw;
            float ny0 =  1f - 2f * (y + h) / sh;
            float nx1 = -1f + 2f * (x + w) / sw;
            float ny1 =  1f - 2f * y / sh;
            var tess = Tesselator.getInstance();
            var buf = tess.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX);
            buf.addVertex(nx0, ny0, 0f).setUv(0f, 1f);
            buf.addVertex(nx1, ny0, 0f).setUv(1f, 1f);
            buf.addVertex(nx1, ny1, 0f).setUv(1f, 0f);
            buf.addVertex(nx0, ny0, 0f).setUv(0f, 1f);
            buf.addVertex(nx1, ny1, 0f).setUv(1f, 0f);
            buf.addVertex(nx0, ny1, 0f).setUv(0f, 0f);
            var mesh = buf.buildOrThrow();
            var device  = RenderSystem.getDevice();
            var encoder = device.createCommandEncoder();
            encoder.writeToBuffer(cachedBlitVertexBuffer.slice(), mesh.vertexBuffer());
            mesh.close();
            try (var pass = encoder.createRenderPass(() -> "aporia:img_pass", colorView, OptionalInt.empty())) {
                pass.setPipeline(blitPipeline);
                pass.bindTexture("InputTexture", tex.getTextureView(),
                        RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
                pass.setVertexBuffer(0, cachedBlitVertexBuffer);
                pass.draw(0, 6);
            }
            return;
        }

        /** Скруглённое изображение — через основной пайплайн с ImageTextureSampler.
         *  Rounded image — via main pipeline with ImageTextureSampler. */
        float sw = mc.getWindow().getGuiScaledWidth();
        float sh = mc.getWindow().getGuiScaledHeight();
        var projSlice = orthoProjection.getBuffer(sw, sh);
        RenderSystem.setProjectionMatrix(projSlice, ProjectionType.ORTHOGRAPHIC);
        var tess = Tesselator.getInstance();
        var buf = tess.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR);
        buf.addVertex(x,   y+h, 0f).setUv(0f, 1f).setColor(1f, 1f, 1f, 1f);
        buf.addVertex(x+w, y+h, 0f).setUv(1f, 1f).setColor(1f, 1f, 1f, 1f);
        buf.addVertex(x+w, y,   0f).setUv(1f, 0f).setColor(1f, 1f, 1f, 1f);
        buf.addVertex(x,   y+h, 0f).setUv(0f, 1f).setColor(1f, 1f, 1f, 1f);
        buf.addVertex(x+w, y,   0f).setUv(1f, 0f).setColor(1f, 1f, 1f, 1f);
        buf.addVertex(x,   y,   0f).setUv(0f, 0f).setColor(1f, 1f, 1f, 1f);
        var mesh = buf.buildOrThrow();
        var device  = RenderSystem.getDevice();
        var encoder = device.createCommandEncoder();
        encoder.writeToBuffer(cachedImageVertexBuffer.slice(), mesh.vertexBuffer());
        mesh.close();
        if (cachedImageBB == null) cachedImageBB = ByteBuffer.allocateDirect(128).order(ByteOrder.nativeOrder());
        cachedImageBB.clear();
        cachedImageBB.putFloat(x); cachedImageBB.putFloat(y); cachedImageBB.putFloat(w); cachedImageBB.putFloat(h);
        cachedImageBB.putFloat(radius); cachedImageBB.putFloat(1.0f); cachedImageBB.putFloat(MODE_ROUNDED_RECT); cachedImageBB.putFloat(0f);
        cachedImageBB.putFloat(0f); cachedImageBB.putFloat(0f); cachedImageBB.putFloat(0f); cachedImageBB.putFloat(15f);
        cachedImageBB.putFloat(1.0f); cachedImageBB.putFloat(0f); cachedImageBB.putFloat(0f); cachedImageBB.putFloat(0f);
        cachedImageBB.putFloat(0f); cachedImageBB.putFloat(0f); cachedImageBB.putFloat(0f); cachedImageBB.putFloat(0f);
        cachedImageBB.flip();
        encoder.writeToBuffer(cachedImageShapeBuffer.slice(), cachedImageBB);
        var indexBuf = RenderSystem.getSequentialBuffer(VertexFormat.Mode.TRIANGLES);
        try (var pass = encoder.createRenderPass(() -> "aporia:img_rounded_pass", colorView, OptionalInt.empty())) {
            pass.setPipeline(imagePipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("ShapeData", cachedImageShapeBuffer.slice());
            pass.bindTexture("ImageTextureSampler", tex.getTextureView(),
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            pass.setVertexBuffer(0, cachedImageVertexBuffer);
            pass.setIndexBuffer(indexBuf.getBuffer(6), indexBuf.type());
            pass.drawIndexed(0, 0, 6, 0);
        }
    }

    public void drawTexture(float x, float y, float w, float h, GpuTextureView view) {
        if (view == null) return;
        Minecraft mc = Minecraft.getInstance();
        var colorView = mc.getMainRenderTarget().getColorTextureView();
        if (colorView == null) return;

        float sw = mc.getWindow().getGuiScaledWidth();
        float sh = mc.getWindow().getGuiScaledHeight();
        float nx0 = -1f + 2f * x / sw;
        float ny0 =  1f - 2f * (y + h) / sh;
        float nx1 = -1f + 2f * (x + w) / sw;
        float ny1 =  1f - 2f * y / sh;
        var tess = Tesselator.getInstance();
        var buf = tess.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX);
        buf.addVertex(nx0, ny0, 0f).setUv(0f, 1f);
        buf.addVertex(nx1, ny0, 0f).setUv(1f, 1f);
        buf.addVertex(nx1, ny1, 0f).setUv(1f, 0f);
        buf.addVertex(nx0, ny0, 0f).setUv(0f, 1f);
        buf.addVertex(nx1, ny1, 0f).setUv(1f, 0f);
        buf.addVertex(nx0, ny1, 0f).setUv(0f, 0f);
        var mesh = buf.buildOrThrow();
        var device  = RenderSystem.getDevice();
        var encoder = device.createCommandEncoder();
        encoder.writeToBuffer(cachedBlitVertexBuffer.slice(), mesh.vertexBuffer());
        mesh.close();
        try (var pass = encoder.createRenderPass(() -> "aporia:webview_pass", colorView, OptionalInt.empty())) {
            pass.setPipeline(blitPipeline);
            pass.bindTexture("InputTexture", view,
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            pass.setVertexBuffer(0, cachedBlitVertexBuffer);
            pass.draw(0, 6);
        }
    }

    public void drawImageCropped(float x, float y, float w, float h, Identifier id, float radius, float u0, float v0, float u1, float v1) {
        if (id == null) return;
        Minecraft mc = Minecraft.getInstance();
        var tex = mc.getTextureManager().getTexture(id);
        if (tex == null || tex.getTextureView() == null) return;
        var colorView = mc.getMainRenderTarget().getColorTextureView();
        if (colorView == null) return;

        float sw = mc.getWindow().getGuiScaledWidth();
        float sh = mc.getWindow().getGuiScaledHeight();
        var projSlice = orthoProjection.getBuffer(sw, sh);
        RenderSystem.setProjectionMatrix(projSlice, ProjectionType.ORTHOGRAPHIC);

        var tess = Tesselator.getInstance();
        var buf = tess.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR);
        buf.addVertex(x,   y+h, 0f).setUv(u0, v1).setColor(1f, 1f, 1f, 1f);
        buf.addVertex(x+w, y+h, 0f).setUv(u1, v1).setColor(1f, 1f, 1f, 1f);
        buf.addVertex(x+w, y,   0f).setUv(u1, v0).setColor(1f, 1f, 1f, 1f);
        buf.addVertex(x,   y+h, 0f).setUv(u0, v1).setColor(1f, 1f, 1f, 1f);
        buf.addVertex(x+w, y,   0f).setUv(u1, v0).setColor(1f, 1f, 1f, 1f);
        buf.addVertex(x,   y,   0f).setUv(u0, v0).setColor(1f, 1f, 1f, 1f);
        var mesh = buf.buildOrThrow();

        var device = RenderSystem.getDevice();
        var encoder = device.createCommandEncoder();
        encoder.writeToBuffer(cachedImageVertexBuffer.slice(), mesh.vertexBuffer());
        mesh.close();

        if (cachedImageBB == null) cachedImageBB = ByteBuffer.allocateDirect(128).order(ByteOrder.nativeOrder());
        cachedImageBB.clear();
        cachedImageBB.putFloat(x); cachedImageBB.putFloat(y); cachedImageBB.putFloat(w); cachedImageBB.putFloat(h);
        cachedImageBB.putFloat(radius); cachedImageBB.putFloat(1.0f); cachedImageBB.putFloat(MODE_ROUNDED_RECT); cachedImageBB.putFloat(0f);
        cachedImageBB.putFloat(0f); cachedImageBB.putFloat(0f); cachedImageBB.putFloat(0f); cachedImageBB.putFloat(15f);
        cachedImageBB.putFloat(1.0f); cachedImageBB.putFloat(0f); cachedImageBB.putFloat(0f); cachedImageBB.putFloat(0f);
        cachedImageBB.putFloat(0f); cachedImageBB.putFloat(0f); cachedImageBB.putFloat(0f); cachedImageBB.putFloat(0f);
        cachedImageBB.flip();
        encoder.writeToBuffer(cachedImageShapeBuffer.slice(), cachedImageBB);

        var indexBuf = RenderSystem.getSequentialBuffer(VertexFormat.Mode.TRIANGLES);
        try (var pass = encoder.createRenderPass(() -> "aporia:img_cropped", colorView, OptionalInt.empty())) {
            pass.setPipeline(imagePipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("ShapeData", cachedImageShapeBuffer.slice());
            pass.bindTexture("ImageTextureSampler", tex.getTextureView(),
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            pass.setVertexBuffer(0, cachedImageVertexBuffer);
            pass.setIndexBuffer(indexBuf.getBuffer(6), indexBuf.type());
            pass.drawIndexed(0, 0, 6, 0);
        }
    }

    public void blitCropped(float x, float y, float w, float h, Identifier id, float u0, float v0, float u1, float v1) {
        if (id == null) return;
        Minecraft mc = Minecraft.getInstance();
        var tex = mc.getTextureManager().getTexture(id);
        if (tex == null || tex.getTextureView() == null) return;
        var colorView = mc.getMainRenderTarget().getColorTextureView();
        if (colorView == null) return;

        float sw = mc.getWindow().getGuiScaledWidth();
        float sh = mc.getWindow().getGuiScaledHeight();
        float nx0 = -1f + 2f * x / sw;
        float ny0 =  1f - 2f * (y + h) / sh;
        float nx1 = -1f + 2f * (x + w) / sw;
        float ny1 =  1f - 2f * y / sh;
        var tess = Tesselator.getInstance();
        var buf = tess.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX);
        buf.addVertex(nx0, ny0, 0f).setUv(u0, v1);
        buf.addVertex(nx1, ny0, 0f).setUv(u1, v1);
        buf.addVertex(nx1, ny1, 0f).setUv(u1, v0);
        buf.addVertex(nx0, ny0, 0f).setUv(u0, v1);
        buf.addVertex(nx1, ny1, 0f).setUv(u1, v0);
        buf.addVertex(nx0, ny1, 0f).setUv(u0, v0);
        var mesh = buf.buildOrThrow();
        var device  = RenderSystem.getDevice();
        var encoder = device.createCommandEncoder();
        encoder.writeToBuffer(cachedBlitVertexBuffer.slice(), mesh.vertexBuffer());
        mesh.close();
        try (var pass = encoder.createRenderPass(() -> "aporia:blit_cropped_pass", colorView, OptionalInt.empty())) {
            pass.setPipeline(blitPipeline);
            pass.bindTexture("InputTexture", tex.getTextureView(),
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            pass.setVertexBuffer(0, cachedBlitVertexBuffer);
            pass.draw(0, 6);
        }
    }

    public Identifier loadImage(Path path) {
        String key = path.toAbsolutePath().toString();
        if (imageIds.containsKey(key)) return imageIds.get(key);
        try (FileInputStream fis = new FileInputStream(path.toFile())) {
            NativeImage img = NativeImage.read(fis);
            DynamicTexture tex = new DynamicTexture(() -> key, img);
            String name = path.getFileName().toString().toLowerCase().replaceAll("[^a-z0-9_.-]", "_");
            Identifier id = Identifier.fromNamespaceAndPath("aporia", "user_image/" + name + "_" + Math.abs(key.hashCode()));
            Minecraft.getInstance().getTextureManager().register(id, tex);
            imageIds.put(key, id);
            imageTextures.put(key, tex);
            return id;
        } catch (IOException e) {
            Logger.warn("[loadImage] Failed to load: " + path + " — " + e.getMessage());
            return null;
        }
    }

    public Identifier loadImage(InputStream stream) {
        String key = "discord_avatar_" + System.currentTimeMillis();
        if (imageIds.containsKey(key)) return imageIds.get(key);
        try {
            NativeImage img = NativeImage.read(stream);
            DynamicTexture tex = new DynamicTexture(() -> key, img);
            Identifier id = Identifier.fromNamespaceAndPath("aporia", "user_image/discord_avatar_" + Math.abs(key.hashCode()));
            Minecraft.getInstance().getTextureManager().register(id, tex);
            imageIds.put(key, id);
            imageTextures.put(key, tex);
            return id;
        } catch (IOException e) {
            Logger.warn("[loadImage] Failed to load from stream: " + e.getMessage());
            return null;
        }
    }

    public void drawText(String font, String text, float x, float y, float size, int color) {
        Aporia.FONTS.drawText(font, text, x, y, size, color);
    }

    public float getTextWidth(String font, String text, float size) {
        return Aporia.FONTS.getTextWidth(font, text, size);
    }

    /* ===========================
       Player Glow (3D entity shader)
       =========================== */



    public void drawPlayerGlow(
            com.mojang.blaze3d.vertex.PoseStack poseStack,
            net.minecraft.client.model.EntityModel<?> model,
            int packedLight, int packedOverlay,
            float colorR, float colorG, float colorB, float colorA,
            float glowIntensity, float rimPower, float fresnelPower, float pulseSpeed
    ) {
        if (entityGlowPipeline == null) { Logger.warn("entityGlowPipeline is null"); return; }

        // Build vertex data NOW (during WorldRenderEvent)
        var bb = new com.mojang.blaze3d.vertex.ByteBufferBuilder(262144);
        var buf = new BufferBuilder(bb, VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
        model.renderToBuffer(poseStack, buf, packedLight, packedOverlay, -1);
        var mesh = buf.build();
        if (mesh == null) { bb.close(); return; }

        int totalBytes = mesh.vertexBuffer().remaining();
        var rawData = new byte[totalBytes];
        mesh.vertexBuffer().get(rawData);
        int vertexCount = mesh.drawState().vertexCount();
        mesh.close();
        bb.close();

        drawPlayerGlowSubmit(rawData, vertexCount, colorR, colorG, colorB, colorA,
                glowIntensity, rimPower, fresnelPower, pulseSpeed);
    }

    // Pre-allocated scratch buffers — ZERO allocations per frame
    private ByteBuffer scratchMVP = ByteBuffer.allocateDirect(192).order(ByteOrder.nativeOrder());
    private ByteBuffer scratchParams = ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder());
    private ByteBuffer scratchVertex = ByteBuffer.allocateDirect(262144).order(ByteOrder.nativeOrder());
    private ByteBuffer scratchIndexData = ByteBuffer.allocateDirect(12000).order(ByteOrder.nativeOrder());
    private final Matrix4f scratchProj = new Matrix4f();
    private final Matrix4f scratchIdentity = new Matrix4f();

    private final Matrix4f scratchCombined = new Matrix4f();

    public void drawPlayerGlowSubmit(
            byte[] vertexData, int vertexCount,
            float colorR, float colorG, float colorB, float colorA,
            float glowIntensity, float rimPower, float fresnelPower, float pulseSpeed
    ) {
        if (entityGlowPipeline == null) { Logger.warn("drawPlayerGlowSubmit: pipeline null"); return; }

        var mc2 = Minecraft.getInstance();
        var colorView = mc2.getMainRenderTarget().getColorTextureView();
        var depthView = mc2.getMainRenderTarget().getDepthTextureView();
        if (colorView == null) return;

        if (vertexCount == 0) { Logger.warn("drawPlayerGlowSubmit: vertexCount=0, skipping"); return; }

        if (scratchVertex.capacity() < vertexData.length) {
            scratchVertex = ByteBuffer.allocateDirect(vertexData.length).order(ByteOrder.nativeOrder());
        }
        scratchVertex.clear();
        scratchVertex.put(vertexData);
        scratchVertex.flip();

        // vertices are in world space → modelView = ViewRot * Trans(-camPos)
        var camPos = mc2.gameRenderer.getMainCamera().position();
        var mv = new Matrix4f(worldViewMatrix);
        mv.translate(-(float)camPos.x, -(float)camPos.y, -(float)camPos.z);
        scratchMVP.clear();
        worldProjMatrix.get(scratchMVP);
        mv.get(scratchMVP);
        scratchIdentity.get(scratchMVP);
        scratchMVP.flip();

        scratchParams.clear();
        scratchParams.putFloat(colorR).putFloat(colorG).putFloat(colorB).putFloat(colorA);
        scratchParams.putFloat(glowIntensity).putFloat(rimPower).putFloat(fresnelPower).putFloat(pulseSpeed);
        scratchParams.flip();

        int numQuads = vertexCount / 4;
        int indexCount = numQuads * 6;
        Logger.info("drawPlayerGlowSubmit: vtx=" + vertexCount + " quads=" + numQuads + " idx=" + indexCount + " rgba=" + colorR + "," + colorG + "," + colorB + "," + colorA);
        if (scratchIndexData.capacity() < indexCount * 2) {
            scratchIndexData = ByteBuffer.allocateDirect(indexCount * 2).order(ByteOrder.nativeOrder());
        }
        scratchIndexData.clear();
        for (int i = 0; i < numQuads; i++) {
            int base = i * 4;
            scratchIndexData.putShort((short)(base));
            scratchIndexData.putShort((short)(base + 1));
            scratchIndexData.putShort((short)(base + 2));
            scratchIndexData.putShort((short)(base));
            scratchIndexData.putShort((short)(base + 2));
            scratchIndexData.putShort((short)(base + 3));
        }
        scratchIndexData.flip();

        var device = RenderSystem.getDevice();
        var encoder = device.createCommandEncoder();

        encoder.writeToBuffer(entityGlowVBO.slice(), scratchVertex);
        encoder.writeToBuffer(entityGlowMVP.slice(), scratchMVP);
        encoder.writeToBuffer(entityGlowParams.slice(), scratchParams);
        encoder.writeToBuffer(entityGlowIBO.slice(), scratchIndexData);

        try (var pass = encoder.createRenderPass(() -> "aporia:entity_glow",
                colorView, OptionalInt.empty(), depthView, OptionalDouble.empty())) {
            pass.setPipeline(entityGlowPipeline);
            pass.setUniform("ModelViewProj", entityGlowMVP.slice());
            pass.setUniform("DrawParams", entityGlowParams.slice());
            pass.setVertexBuffer(0, entityGlowVBO);
            pass.setIndexBuffer(entityGlowIBO, VertexFormat.IndexType.SHORT);
            GL11.glGetError(); // clear
            pass.drawIndexed(0, 0, indexCount, 0);
            int glerr = GL11.glGetError();
            int progAfter = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
            if (glerr != 0) Logger.warn("drawPlayerGlowSubmit: GL error=" + glerr + " program=" + progAfter);
            if (progAfter == 0) Logger.warn("drawPlayerGlowSubmit: INVALID_PROGRAM after draw!");
        }
    }

    public void drawTestQuad(
            float colorR, float colorG, float colorB, float colorA,
            float glowIntensity, float rimPower, float fresnelPower, float pulseSpeed
    ) {
        if (entityGlowPipeline == null) { Logger.warn("drawTestQuad: pipeline null"); return; }

        var mc2 = Minecraft.getInstance();
        var colorView = mc2.getMainRenderTarget().getColorTextureView();
        var depthView = mc2.getMainRenderTarget().getDepthTextureView();
        if (colorView == null) return;

        int stride = DefaultVertexFormat.NEW_ENTITY.getVertexSize();
        int vCount = 4;
        int qCount = 1;
        int iCount = qCount * 6;

        scratchVertex.clear();
        if (scratchVertex.capacity() < vCount * stride) {
            scratchVertex = ByteBuffer.allocateDirect(vCount * stride).order(ByteOrder.nativeOrder());
        }
        // Write one quad at (0,0,0) size 2x2 in view space
        // Position (3 floats) + Color (4 bytes) + UV0 (2 floats) + UV1 (2 shorts) + UV2 (2 shorts) + Normal (3 bytes + pad)
        for (int i = 0; i < vCount; i++) {
            float x = (i == 0 || i == 3) ? -1.0f : 1.0f;
            float y = (i == 0 || i == 1) ? -1.0f : 1.0f;
            float z = -5.0f;
            scratchVertex.putFloat(x).putFloat(y).putFloat(z);           // Position
            scratchVertex.putInt(-1);                                      // Color (white)
            scratchVertex.putFloat(i * 0.333f).putFloat(i * 0.333f);      // UV0
            scratchVertex.putShort((short)15728880).putShort((short)15728880); // UV1 (light)
            scratchVertex.putShort((short)0).putShort((short)0);          // UV2 (overlay)
            scratchVertex.put((byte)0).put((byte)0).put((byte)127).put((byte)0); // Normal (0,0,1) + pad
        }
        scratchVertex.flip();

        // Use a known-good perspective projection (bypass worldProjMatrix for debug)
        float fov = (float)Math.toRadians(70.0);
        float aspect = (float)Minecraft.getInstance().getWindow().getWidth()
                     / (float)Minecraft.getInstance().getWindow().getHeight();
        scratchCombined.setPerspective(fov, aspect, 0.1f, 1000.0f);
        scratchMVP.clear();
        scratchCombined.get(scratchMVP);
        scratchIdentity.get(scratchMVP);
        scratchIdentity.get(scratchMVP);
        scratchMVP.flip();

        scratchParams.clear();
        scratchParams.putFloat(colorR).putFloat(colorG).putFloat(colorB).putFloat(colorA);
        scratchParams.putFloat(glowIntensity).putFloat(rimPower).putFloat(fresnelPower).putFloat(pulseSpeed);
        scratchParams.flip();

        if (scratchIndexData.capacity() < iCount * 2) {
            scratchIndexData = ByteBuffer.allocateDirect(iCount * 2).order(ByteOrder.nativeOrder());
        }
        scratchIndexData.clear();
        scratchIndexData.putShort((short)0).putShort((short)1).putShort((short)2);
        scratchIndexData.putShort((short)0).putShort((short)2).putShort((short)3);
        scratchIndexData.flip();

        var device = RenderSystem.getDevice();
        var encoder = device.createCommandEncoder();

        encoder.writeToBuffer(entityGlowVBO.slice(), scratchVertex);
        encoder.writeToBuffer(entityGlowMVP.slice(), scratchMVP);
        encoder.writeToBuffer(entityGlowParams.slice(), scratchParams);
        encoder.writeToBuffer(entityGlowIBO.slice(), scratchIndexData);

        int progAfter = 0;
        try (var pass = encoder.createRenderPass(() -> "aporia:test_quad",
                colorView, OptionalInt.empty(), depthView, OptionalDouble.empty())) {
            pass.setPipeline(entityGlowPipeline);
            pass.setUniform("ModelViewProj", entityGlowMVP.slice());
            pass.setUniform("DrawParams", entityGlowParams.slice());
            pass.setVertexBuffer(0, entityGlowVBO);
            pass.setIndexBuffer(entityGlowIBO, VertexFormat.IndexType.SHORT);
            pass.drawIndexed(0, 0, iCount, 0);
            progAfter = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        } catch (Exception e) {
            Logger.warn("drawTestQuad: EXCEPTION: " + e + " " + e.getMessage());
            for (var ste : e.getStackTrace()) Logger.warn("  at " + ste);
        }
        Logger.info("drawTestQuad: program=" + progAfter + " expected!=0");
    }

    /* ===========================
       Kawase Blur (down + up)
       =========================== */

    public float saturation = 0.5f;

    /* ===========================
       Post‑process saturation
       =========================== */

    public void applySaturation(Minecraft mc, float saturation) {
        if (postPipeline == null) return;
        var mainTarget = mc.getMainRenderTarget();
        if (mainTarget.width != postTempW || mainTarget.height != postTempH) {
            if (postTempTarget != null) postTempTarget.destroyBuffers();
            postTempTarget = new TextureTarget("aporia:post_temp", mainTarget.width, mainTarget.height, false);
            postTempW = mainTarget.width;
            postTempH = mainTarget.height;
        }
        var device  = RenderSystem.getDevice();
        var encoder = device.createCommandEncoder();
        var tess = Tesselator.getInstance();
        var buf = tess.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX);
        buf.addVertex(-1f,  1f, 0f).setUv(0f, 1f);
        buf.addVertex(-1f, -1f, 0f).setUv(0f, 0f);
        buf.addVertex( 1f, -1f, 0f).setUv(1f, 0f);
        buf.addVertex(-1f,  1f, 0f).setUv(0f, 1f);
        buf.addVertex( 1f, -1f, 0f).setUv(1f, 0f);
        buf.addVertex( 1f,  1f, 0f).setUv(1f, 1f);
        var mesh = buf.buildOrThrow();
        encoder.writeToBuffer(cachedBlitVertexBuffer.slice(), mesh.vertexBuffer());
        mesh.close();

        if (cachedPostBB == null) cachedPostBB = ByteBuffer.allocateDirect(16).order(ByteOrder.nativeOrder());
        cachedPostBB.clear();
        cachedPostBB.putFloat(saturation); cachedPostBB.putFloat(0f); cachedPostBB.putFloat(0f); cachedPostBB.putFloat(0f);
        cachedPostBB.flip();
        encoder.writeToBuffer(postUbo.slice(), cachedPostBB);

        try (var pass = encoder.createRenderPass(() -> "aporia:post_pass",
                postTempTarget.getColorTextureView(), OptionalInt.empty())) {
            pass.setPipeline(postPipeline);
            pass.bindTexture("InputTexture", mainTarget.getColorTextureView(),
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            pass.setUniform("PostData", postUbo.slice());
            pass.setVertexBuffer(0, cachedBlitVertexBuffer);
            pass.draw(0, 6);
        }
        try (var pass = encoder.createRenderPass(() -> "aporia:post_blit",
                mainTarget.getColorTextureView(), OptionalInt.empty())) {
            pass.setPipeline(blitPipeline);
            pass.bindTexture("InputTexture", postTempTarget.getColorTextureView(),
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            pass.setVertexBuffer(0, cachedBlitVertexBuffer);
            pass.draw(0, 6);
        }
    }

    /** Clears cached GPU textures for clean reconnect. Called from Minecraft.disconnect(). */
    public static void cleanupImages() {
        INSTANCE.imageIds.clear();
        INSTANCE.imageTextures.values().forEach(tex -> {
            try { tex.close(); } catch (Exception ignored) {}
        });
        INSTANCE.imageTextures.clear();
        if (INSTANCE.postTempTarget != null) {
            INSTANCE.postTempTarget.destroyBuffers();
            INSTANCE.postTempTarget = null;
        }
        INSTANCE.postTempW = -1;
        INSTANCE.postTempH = -1;
    }

}