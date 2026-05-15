package so.aporia.utils.user.render.core;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

public class AporiaRenderer {
    public static final AporiaRenderer INSTANCE = new AporiaRenderer();
    public static final int MODE_FILL         = 0;
    public static final int MODE_CIRCLE       = 1;
    public static final int MODE_ROUNDED_RECT = 2;

    private RenderPipeline pipeline;
    private CachedOrthoProjectionMatrixBuffer orthoProjection;
    private RenderPipeline kawaseDownPipeline;
    private RenderPipeline kawaseUpPipeline;
    private RenderPipeline blitPipeline;
    private RenderPipeline postPipeline;

    // Блюр-текстуры
    private TextureTarget kawaseDownTarget;  // half-res для downscale
    private TextureTarget blurTarget;        // full-res финальный блюр
    private TextureTarget blurTempTarget;    // full-res для апскейл-пинг-понга
    private int blurTargetW = -1, blurTargetH = -1;
    private boolean blurReady = false;

    // Пост-обработка сатурации
    private TextureTarget postTempTarget;
    private int postTempW = -1, postTempH = -1;

    private final Map<String, Identifier>      imageIds     = new HashMap<>();
    private final Map<String, DynamicTexture>  imageTextures = new HashMap<>();

    // Кэшированные буферы для всей 2D-геометрии (позиция + UV + цвет)
    private GpuBuffer cachedVertexBuffer;    // общий VBO (до 6 вершин)
    private GpuBuffer cachedShapeBuffer;     // UBO 128 байт для ShapeData
    private GpuBuffer cachedBlitVertexBuffer; // VBO для блита (POSITION_TEX)
    private GpuBuffer blurQuadVbo;  // VBO для полноэкранного квада
    private GpuBuffer blurUbo;      // UBO для KawaseData (32 байта)

    private float cachedBlurStrength = -1f;
    private float cachedBlurSaturation = -1f;
    private int blurFrameCounter = 0;
    private static final int BLUR_UPDATE_INTERVAL = 3;

    public void init() {
        pipeline = RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/aporia"))
                .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/aporia"))
                .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/aporia"))
                .withUniform("Projection", UniformType.UNIFORM_BUFFER)
                .withUniform("ShapeData",  UniformType.UNIFORM_BUFFER)
                .withSampler("BlurTextureSampler")
                .withSampler("ImageTextureSampler")
                .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.TRIANGLES)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withCull(false)
                .build();

        orthoProjection = new CachedOrthoProjectionMatrixBuffer("aporia", -1000f, 1000f, true);

        kawaseDownPipeline = RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/kawase_down"))
                .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/kawase_down"))
                .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/kawase_down"))
                .withSampler("InputTexture")
                .withUniform("KawaseData", UniformType.UNIFORM_BUFFER)
                .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.TRIANGLES)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withCull(false)
                .build();

        kawaseUpPipeline = RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/kawase_up"))
                .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/kawase_up"))
                .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/kawase_up"))
                .withSampler("InputTexture")
                .withUniform("KawaseData", UniformType.UNIFORM_BUFFER)
                .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.TRIANGLES)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withCull(false)
                .build();

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

        var device = RenderSystem.getDevice();
        // Кэшированные буферы: VBO на 6 вершин (позиция, uv, цвет) — 6 * 36 = 216 байт, округлим до 256
        cachedVertexBuffer = device.createBuffer(() -> "aporia:cached_vbo",
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, 256L);
        // UBO для ShapeData — 128 байт
        cachedShapeBuffer = device.createBuffer(() -> "aporia:cached_shape",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, 128L);
        // VBO для блита (POSITION_TEX) — 6 вершин * 20 байт = 120, округляем до 128
        cachedBlitVertexBuffer = device.createBuffer(() -> "aporia:cached_blit_vbo",
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, 128L);

        // Один квад на все блюр-проходы
        var tess = Tesselator.getInstance();
        var buf = tess.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX);
        buf.addVertex(-1f,  1f, 0f).setUv(0f, 1f);
        buf.addVertex(-1f, -1f, 0f).setUv(0f, 0f);
        buf.addVertex( 1f, -1f, 0f).setUv(1f, 0f);
        buf.addVertex(-1f,  1f, 0f).setUv(0f, 1f);
        buf.addVertex( 1f, -1f, 0f).setUv(1f, 0f);
        buf.addVertex( 1f,  1f, 0f).setUv(1f, 1f);
        var quadMesh = buf.buildOrThrow();
        blurQuadVbo = device.createBuffer(() -> "aporia:blur_quad",
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, quadMesh.vertexBuffer());
        quadMesh.close();

// Один UBO под KawaseData (32 байта хватит на всё)
        blurUbo = device.createBuffer(() -> "aporia:blur_ubo",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, 32L);
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
        Minecraft mc = Minecraft.getInstance();
        if (!blurReady || pipeline == null || blurTarget == null) {
            drawRect(x, y, w, h, radius, color);
            return;
        }
        var mainTarget = mc.getMainRenderTarget();
        var window     = mc.getWindow();
        float sw = window.getGuiScaledWidth();
        float sh = window.getGuiScaledHeight();
        var colorView  = mainTarget.getColorTextureView();
        if (colorView == null) return;
        float ta = ((color >> 24) & 0xFF) / 255f;
        float tr = ((color >> 16) & 0xFF) / 255f;
        float tg = ((color >>  8) & 0xFF) / 255f;
        float tb = ((color ) & 0xFF) / 255f;
        var projSlice = orthoProjection.getBuffer(sw, sh);
        RenderSystem.setProjectionMatrix(projSlice, ProjectionType.ORTHOGRAPHIC);
        var tess = Tesselator.getInstance();
        var buf  = tess.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR);
        buf.addVertex(x,   y+h, 0f).setUv(0f,0f).setColor(tr,tg,tb,ta);
        buf.addVertex(x+w, y+h, 0f).setUv(1f,0f).setColor(tr,tg,tb,ta);
        buf.addVertex(x+w, y,   0f).setUv(1f,1f).setColor(tr,tg,tb,ta);
        buf.addVertex(x,   y+h, 0f).setUv(0f,0f).setColor(tr,tg,tb,ta);
        buf.addVertex(x+w, y,   0f).setUv(1f,1f).setColor(tr,tg,tb,ta);
        buf.addVertex(x,   y,   0f).setUv(0f,1f).setColor(tr,tg,tb,ta);
        var mesh = buf.buildOrThrow();
        var device  = RenderSystem.getDevice();
        var encoder = device.createCommandEncoder();
        encoder.writeToBuffer(cachedVertexBuffer.slice(), mesh.vertexBuffer());
        mesh.close();
        var shapeBB = ByteBuffer.allocateDirect(128).order(ByteOrder.nativeOrder());
        shapeBB.putFloat(x); shapeBB.putFloat(y); shapeBB.putFloat(w); shapeBB.putFloat(h);
        shapeBB.putFloat(radius); shapeBB.putFloat(1.0f); shapeBB.putFloat(MODE_ROUNDED_RECT); shapeBB.putFloat(0f);
        shapeBB.putFloat(0f); shapeBB.putFloat(0f); shapeBB.putFloat(1f); shapeBB.putFloat(0f);
        shapeBB.putFloat((float) mainTarget.width); shapeBB.putFloat((float) mainTarget.height); shapeBB.putFloat(0f); shapeBB.putFloat(0f);
        shapeBB.flip();
        encoder.writeToBuffer(cachedShapeBuffer.slice(), shapeBB);
        var indexBuf = RenderSystem.getSequentialBuffer(VertexFormat.Mode.TRIANGLES);
        try (var pass = encoder.createRenderPass(() -> "aporia:blur_rect", colorView, OptionalInt.empty())) {
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("ShapeData", cachedShapeBuffer.slice());
            pass.bindTexture("BlurTextureSampler", blurTarget.getColorTextureView(),
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            pass.setVertexBuffer(0, cachedVertexBuffer);
            pass.setIndexBuffer(indexBuf.getBuffer(6), indexBuf.type());
            pass.drawIndexed(0, 0, 6, 0);
        }
    }

    public void drawRectBlurred(float x, float y, float w, float h, float radius, int color) {
        drawRectBlurred(x, y, w, h, radius, color, 4f, 15);
    }

    public void drawRectBlurred(float x, float y, float w, float h, float radius, int color, float blurStrength, int cornerMask) {
        Minecraft mc = Minecraft.getInstance();
        if (!blurReady || pipeline == null || blurTarget == null) {
            drawRect(x, y, w, h, radius, color, cornerMask);
            return;
        }
        var mainTarget = mc.getMainRenderTarget();
        var window     = mc.getWindow();
        float sw = window.getGuiScaledWidth();
        float sh = window.getGuiScaledHeight();
        var colorView  = mainTarget.getColorTextureView();
        if (colorView == null) return;
        float ta = ((color >> 24) & 0xFF) / 255f;
        float tr = ((color >> 16) & 0xFF) / 255f;
        float tg = ((color >>  8) & 0xFF) / 255f;
        float tb = ((color ) & 0xFF) / 255f;
        var projSlice = orthoProjection.getBuffer(sw, sh);
        RenderSystem.setProjectionMatrix(projSlice, ProjectionType.ORTHOGRAPHIC);
        var tess = Tesselator.getInstance();
        var buf  = tess.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR);
        buf.addVertex(x,   y+h, 0f).setUv(0f,0f).setColor(tr,tg,tb,ta);
        buf.addVertex(x+w, y+h, 0f).setUv(1f,0f).setColor(tr,tg,tb,ta);
        buf.addVertex(x+w, y,   0f).setUv(1f,1f).setColor(tr,tg,tb,ta);
        buf.addVertex(x,   y+h, 0f).setUv(0f,0f).setColor(tr,tg,tb,ta);
        buf.addVertex(x+w, y,   0f).setUv(1f,1f).setColor(tr,tg,tb,ta);
        buf.addVertex(x,   y,   0f).setUv(0f,1f).setColor(tr,tg,tb,ta);
        var mesh = buf.buildOrThrow();
        var device  = RenderSystem.getDevice();
        var encoder = device.createCommandEncoder();
        encoder.writeToBuffer(cachedVertexBuffer.slice(), mesh.vertexBuffer());
        mesh.close();
        var shapeBB = ByteBuffer.allocateDirect(128).order(ByteOrder.nativeOrder());
        shapeBB.putFloat(x); shapeBB.putFloat(y); shapeBB.putFloat(w); shapeBB.putFloat(h);
        shapeBB.putFloat(radius); shapeBB.putFloat(1.0f); shapeBB.putFloat(MODE_ROUNDED_RECT); shapeBB.putFloat(0f);
        shapeBB.putFloat(0f); shapeBB.putFloat(0f); shapeBB.putFloat(1f); shapeBB.putFloat((float) cornerMask);
        shapeBB.putFloat(0f); shapeBB.putFloat(0f); shapeBB.putFloat(0f); shapeBB.putFloat(0f);
        shapeBB.putFloat((float) mainTarget.width); shapeBB.putFloat((float) mainTarget.height); shapeBB.putFloat(0f); shapeBB.putFloat(0f);
        shapeBB.flip();
        encoder.writeToBuffer(cachedShapeBuffer.slice(), shapeBB);
        var indexBuf = RenderSystem.getSequentialBuffer(VertexFormat.Mode.TRIANGLES);
        try (var pass = encoder.createRenderPass(() -> "aporia:blur_rect", colorView, OptionalInt.empty())) {
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("ShapeData", cachedShapeBuffer.slice());
            pass.bindTexture("BlurTextureSampler", blurTarget.getColorTextureView(),
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
                      int borderMode, float thickness, float fadeCorner) {
        draw(verts, color, mode, bx, by, bw, bh, radius, borderMode, thickness, fadeCorner, 15);
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
            buf.addVertex(v[0], v[1], 0f).setUv(0f, 0f).setColor(r, g, b, a);
        }
        var mesh = buf.buildOrThrow();
        var device  = RenderSystem.getDevice();
        var encoder = device.createCommandEncoder();
        encoder.writeToBuffer(cachedVertexBuffer.slice(), mesh.vertexBuffer());
        mesh.close();

        var bb = ByteBuffer.allocateDirect(128).order(ByteOrder.nativeOrder());
        bb.putFloat(bx); bb.putFloat(by); bb.putFloat(bw); bb.putFloat(bh);
        bb.putFloat(radius); bb.putFloat(1.0f); bb.putFloat(mode); bb.putFloat((float) borderMode);
        bb.putFloat(thickness); bb.putFloat(fadeCorner); bb.putFloat(0f); bb.putFloat((float) cornerMask);
        bb.putFloat(0f); bb.putFloat(0f); bb.putFloat(0f); bb.putFloat(0f);
        bb.flip();
        encoder.writeToBuffer(cachedShapeBuffer.slice(), bb);

        var indexBuf = RenderSystem.getSequentialBuffer(VertexFormat.Mode.TRIANGLES);
        int vertexCount = verts.length;
        try (var pass = encoder.createRenderPass(() -> "aporia:draw", colorView, OptionalInt.empty())) {
            pass.setPipeline(pipeline);
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
            var tess = Tesselator.getInstance();
            var buf = tess.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX);
            buf.addVertex(x,   y+h, 0f).setUv(0f, 1f);
            buf.addVertex(x+w, y+h, 0f).setUv(1f, 1f);
            buf.addVertex(x+w, y,   0f).setUv(1f, 0f);
            buf.addVertex(x,   y+h, 0f).setUv(0f, 1f);
            buf.addVertex(x+w, y,   0f).setUv(1f, 0f);
            buf.addVertex(x,   y,   0f).setUv(0f, 0f);
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

        // Скруглённое изображение – через основной пайплайн с ImageTextureSampler
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
        encoder.writeToBuffer(cachedVertexBuffer.slice(), mesh.vertexBuffer());
        mesh.close();
        var bb = ByteBuffer.allocateDirect(128).order(ByteOrder.nativeOrder());
        bb.putFloat(x); bb.putFloat(y); bb.putFloat(w); bb.putFloat(h);
        bb.putFloat(radius); bb.putFloat(1.0f); bb.putFloat(MODE_ROUNDED_RECT); bb.putFloat(0f);
        bb.putFloat(0f); bb.putFloat(0f); bb.putFloat(0f); bb.putFloat(15f);
        bb.putFloat(1.0f); bb.putFloat(0f); bb.putFloat(0f); bb.putFloat(0f);
        bb.putFloat(0f); bb.putFloat(0f); bb.putFloat(0f); bb.putFloat(0f);
        bb.flip();
        encoder.writeToBuffer(cachedShapeBuffer.slice(), bb);
        var indexBuf = RenderSystem.getSequentialBuffer(VertexFormat.Mode.TRIANGLES);
        try (var pass = encoder.createRenderPass(() -> "aporia:img_rounded_pass", colorView, OptionalInt.empty())) {
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("ShapeData", cachedShapeBuffer.slice());
            pass.bindTexture("ImageTextureSampler", tex.getTextureView(),
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            pass.setVertexBuffer(0, cachedVertexBuffer);
            pass.setIndexBuffer(indexBuf.getBuffer(6), indexBuf.type());
            pass.drawIndexed(0, 0, 6, 0);
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

    public void drawTextWithOutline(String font, String text, float x, float y, float size,
                                    int color, float outlineWidth, int outlineColor) {
        Aporia.FONTS.drawTextWithOutline(font, text, x, y, size, color, outlineWidth, outlineColor);
    }

    public float getTextWidth(String font, String text, float size) {
        return Aporia.FONTS.getTextWidth(font, text, size);
    }

    public void drawGlyph(int index, float x, float y, float size, int color) {
        Aporia.FONTS.drawGlyph(Fonts.FONT, index, x, y, size, color);
    }

    /* ===========================
       Kawase Blur (down + up)
       =========================== */

    public void onRenderHud(Minecraft mc) {
        if (saturation != 0.5f) applySaturation(mc, saturation);
    }

    public float saturation = 0.5f;

    public void prepareFrameBlur(Minecraft mc, float strength, float saturation) {
        blurFrameCounter++;
        if (blurFrameCounter >= BLUR_UPDATE_INTERVAL) {
            blurFrameCounter = 0;
            prepareBlur(mc, strength, saturation);
            cachedBlurStrength = strength;
            cachedBlurSaturation = saturation;
        }
    }

    public void invalidateBlurCache() {
        cachedBlurStrength = -1f;
        cachedBlurSaturation = -1f;
        blurReady = false;
    }

    private void ensureBlurTarget(Minecraft mc) {
        var main = mc.getMainRenderTarget();
        if (main.width == blurTargetW && main.height == blurTargetH) return;

        // Уничтожаем старые текстуры
        if (kawaseDownTarget != null) kawaseDownTarget.destroyBuffers();
        if (blurTarget       != null) blurTarget.destroyBuffers();
        if (blurTempTarget   != null) blurTempTarget.destroyBuffers();

        // half‑res текстура для downscale
        int halfW = Math.max(1, main.width / 2);
        int halfH = Math.max(1, main.height / 2);
        kawaseDownTarget = new TextureTarget("aporia:kawase_down", halfW, halfH, false);

        // full‑res текстуры для upscale пинг‑понга
        blurTarget     = new TextureTarget("aporia:blur_final", main.width, main.height, false);
        blurTempTarget = new TextureTarget("aporia:blur_temp",  main.width, main.height, false);

        blurTargetW = main.width;
        blurTargetH = main.height;
        blurReady   = false;
    }

    public void prepareBlur(Minecraft mc, float strength, float saturation) {
        ensureBlurTarget(mc);
        var mainTarget = mc.getMainRenderTarget();
        var device     = RenderSystem.getDevice();

        int mainW = mainTarget.width;
        int mainH = mainTarget.height;
        int halfW = Math.max(1, mainW / 2);
        int halfH = Math.max(1, mainH / 2);

        var encoder = device.createCommandEncoder();

        // ----- DOWNSCALE -----
        var bb = ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder());
        bb.putFloat((float) mainW);
        bb.putFloat((float) mainH);
        bb.putFloat(1.0f);
        bb.putFloat(saturation);
        bb.putFloat(0f); bb.putFloat(0f); bb.putFloat(0f); bb.putFloat(0f);
        bb.flip();
        encoder.writeToBuffer(blurUbo.slice(), bb);

        try (var pass = encoder.createRenderPass(() -> "aporia:kawase_down",
                kawaseDownTarget.getColorTextureView(), OptionalInt.empty())) {
            pass.setPipeline(kawaseDownPipeline);
            pass.bindTexture("InputTexture", mainTarget.getColorTextureView(),
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            pass.setUniform("KawaseData", blurUbo.slice());
            pass.setVertexBuffer(0, blurQuadVbo);
            pass.draw(0, 6);
        }

        // ----- UPSCALE -----
        int iterations = Math.max(1, (int) (strength / 2.0f));
        float offset = 1.0f;
        float offsetStep = Math.max(0.5f, strength / (iterations * 2.0f));

        TextureTarget src = kawaseDownTarget;
        TextureTarget dst = blurTempTarget;

        for (int i = 0; i < iterations; i++) {
            bb.clear();
            bb.putFloat((float) halfW);
            bb.putFloat((float) halfH);
            bb.putFloat(offset);
            bb.putFloat(0f);
            bb.putFloat(0f); bb.putFloat(0f); bb.putFloat(0f); bb.putFloat(0f);
            bb.flip();
            encoder.writeToBuffer(blurUbo.slice(), bb);

            try (var pass = encoder.createRenderPass(() -> "aporia:kawase_up",
                    dst.getColorTextureView(), OptionalInt.empty())) {
                pass.setPipeline(kawaseUpPipeline);
                pass.bindTexture("InputTexture", src.getColorTextureView(),
                        RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
                pass.setUniform("KawaseData", blurUbo.slice());
                pass.setVertexBuffer(0, blurQuadVbo);
                pass.draw(0, 6);
            }

            offset += offsetStep;
            // Пинг-понг
            var tmp = src;
            src = dst;
            dst = (tmp == blurTempTarget) ? blurTarget : blurTempTarget;
        }

        // Гарантируем, что результат в blurTarget
        if (src != blurTarget) {
            bb.clear();
            bb.putFloat((float) halfW);
            bb.putFloat((float) halfH);
            bb.putFloat(0f);
            bb.putFloat(0f);
            bb.putFloat(0f); bb.putFloat(0f); bb.putFloat(0f); bb.putFloat(0f);
            bb.flip();
            encoder.writeToBuffer(blurUbo.slice(), bb);

            try (var pass = encoder.createRenderPass(() -> "aporia:copy_to_final",
                    blurTarget.getColorTextureView(), OptionalInt.empty())) {
                pass.setPipeline(kawaseUpPipeline);
                pass.bindTexture("InputTexture", blurTempTarget.getColorTextureView(),
                        RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
                pass.setUniform("KawaseData", blurUbo.slice());
                pass.setVertexBuffer(0, blurQuadVbo);
                pass.draw(0, 6);
            }
        }

        blurReady = true;
    }

    public void prepareBlur(Minecraft mc, float strength) {
        prepareBlur(mc, strength, 0.5f);
    }

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

        var dataBuf = device.createBuffer(() -> "aporia:post_data",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, 16L);
        var bb = ByteBuffer.allocateDirect(16).order(ByteOrder.nativeOrder());
        bb.putFloat(saturation); bb.putFloat(0f); bb.putFloat(0f); bb.putFloat(0f);
        bb.flip();
        encoder.writeToBuffer(dataBuf.slice(), bb);

        try (var pass = encoder.createRenderPass(() -> "aporia:post_pass",
                postTempTarget.getColorTextureView(), OptionalInt.empty())) {
            pass.setPipeline(postPipeline);
            pass.bindTexture("InputTexture", mainTarget.getColorTextureView(),
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            pass.setUniform("PostData", dataBuf.slice());
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
        dataBuf.close();
    }

    public void cleanupBlur() {
        if (kawaseDownTarget != null) { kawaseDownTarget.destroyBuffers(); kawaseDownTarget = null; }
        if (blurTarget       != null) { blurTarget.destroyBuffers();       blurTarget       = null; }
        if (blurTempTarget   != null) { blurTempTarget.destroyBuffers();   blurTempTarget   = null; }
        blurTargetW = -1; blurTargetH = -1; blurReady = false;
    }
}