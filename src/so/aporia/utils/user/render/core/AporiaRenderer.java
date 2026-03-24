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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

public class AporiaRenderer {

    public static final AporiaRenderer INSTANCE = new AporiaRenderer();

    /** Mode constants — must match values in {@code aporia.fsh}. */
    public static final int MODE_FILL         = 0;
    public static final int MODE_CIRCLE       = 1;
    public static final int MODE_ROUNDED_RECT = 2;
    private RenderPipeline pipeline;
    private CachedOrthoProjectionMatrixBuffer orthoProjection;
    private RenderPipeline blurPipeline;
    private RenderPipeline blitPipeline;
    private TextureTarget  blurTarget;
    private TextureTarget  blurTempTarget;
    private int blurTargetW = -1;
    private int blurTargetH = -1;
    private boolean blurReady = false;
    private RenderPipeline postPipeline;
    private TextureTarget  postTempTarget;
    private int postTempW = -1, postTempH = -1;

    private final Map<String, Identifier>      imageIds     = new HashMap<>();
    private final Map<String, DynamicTexture>  imageTextures = new HashMap<>();

    public void init() {
        pipeline = RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/aporia"))
                .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/aporia"))
                .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/aporia"))
                .withUniform("Projection", UniformType.UNIFORM_BUFFER)
                .withUniform("ShapeData",  UniformType.UNIFORM_BUFFER)
                .withSampler("BlurTextureSampler")
                .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.TRIANGLES)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withCull(false)
                .build();

        orthoProjection = new CachedOrthoProjectionMatrixBuffer("aporia", -1000f, 1000f, true);

        blurPipeline = RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/blur"))
                .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/blur"))
                .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/blur"))
                .withSampler("InputTexture")
                .withUniform("BlurData", UniformType.UNIFORM_BUFFER)
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
    }

    /** Public drawing API. */

    /**
     * Line from (x1,y1) to (x2,y2) with given pixel thickness.
     * <p>
     * Линия от (x1,y1) до (x2,y2) с заданной толщиной в пикселях.
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
     * Horizontal line that fades to transparent at both ends.
     * <p>
     * Горизонтальная линия с затуханием к краям.
     * <ul>
     *   <li>centerX/centerY — center of the line</li>
     *   <li>halfLen — half-length</li>
     *   <li>thickness — height</li>
     *   <li>progress 0..1 — animated width scale (line grows from center)</li>
     * </ul>
     */
    public void drawFadeHLine(float centerX, float centerY, float halfLen, float thickness, float progress, int color) {
        float len = halfLen * progress;
        if (len < 1f) return;
        int segments = 12;
        float segW = len / segments;
        int a = (color >> 24) & 0xFF;
        float startX = centerX - len;
        for (int i = 0; i < segments; i++) {
            float t0 = (float) i / segments;
            float t1 = (float)(i + 1) / segments;
            float fade0 = 1f - Math.abs(t0 * 2f - 1f);
            float fade1 = 1f - Math.abs(t1 * 2f - 1f);
            int a0 = (int)(a * fade0 * fade0);
            int a1 = (int)(a * fade1 * fade1);
            int c0 = (a0 << 24) | (color & 0x00FFFFFF);
            int c1 = (a1 << 24) | (color & 0x00FFFFFF);
            float x0 = startX + i * segW;
            float x1 = startX + (i + 1) * segW;
            drawLine(x0, centerY, x1, centerY, thickness, lerp(c0, c1, 0.5f));
        }
    }

    private static int lerp(int c0, int c1, float t) {
        int a = (int)(((c0>>24)&0xFF) + (((c1>>24)&0xFF) - ((c0>>24)&0xFF)) * t);
        int r = (int)(((c0>>16)&0xFF) + (((c1>>16)&0xFF) - ((c0>>16)&0xFF)) * t);
        int g = (int)(((c0>> 8)&0xFF) + (((c1>> 8)&0xFF) - ((c0>> 8)&0xFF)) * t);
        int b = (int)(((c0    )&0xFF) + (((c1    )&0xFF) - ((c0    )&0xFF)) * t);
        return (a<<24)|(r<<16)|(g<<8)|b;
    }

    /**
     * Filled circle.
     * <p>
     * Заполненный круг.
     */
    public void drawCircle(float cx, float cy, float radius, int color) {
        float x = cx - radius, y = cy - radius, d = radius * 2;
        drawShape(x, y, d, d, color, MODE_CIRCLE, x, y, d, d, radius, 0, 0f, 0f);
    }

    /**
     * Filled triangle with 3 explicit vertices.
     * <p>
     * Заполненный треугольник с 3 явными вершинами.
     */
    public void drawTriangle(float x1, float y1, float x2, float y2, float x3, float y3, int color) {
        float bx = Math.min(x1, Math.min(x2, x3));
        float by = Math.min(y1, Math.min(y2, y3));
        float bw = Math.max(x1, Math.max(x2, x3)) - bx;
        float bh = Math.max(y1, Math.max(y2, y3)) - by;
        draw(new float[][]{{x1,y1},{x2,y2},{x3,y3}}, color, MODE_FILL, bx, by, bw, bh, 0f);
    }

    private boolean blurLoggedOnce = false;
    private boolean blurPreparedLoggedOnce = false;

    public void resetDebugFlags() {
        blurLoggedOnce = false;
        blurPreparedLoggedOnce = false;
    }

    /**
     * Blurred rect — uses aporia pipeline with gl_FragCoord sampling from blurTarget.
     * SDF rounded corners clip correctly — same shader as drawRect.
     * <p>
     * Размытый прямоугольник — использует aporia pipeline с gl_FragCoord из blurTarget.
     * SDF скруглённые углы корректно обрезаются — тот же шейдер что и drawRect.
     */
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
        float tb = ((color      ) & 0xFF) / 255f;
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
        var mesh      = buf.buildOrThrow();
        var device  = RenderSystem.getDevice();
        var encoder = device.createCommandEncoder();
        var vertexGpu = device.createBuffer(() -> "aporia:blur_vbo2",
        GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, mesh.vertexBuffer());
        var shapeBuf = device.createBuffer(() -> "aporia:blur_shape2", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, 64L);
        var bb = ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder());
        bb.putFloat(x); bb.putFloat(y); bb.putFloat(w); bb.putFloat(h);
        bb.putFloat(radius); bb.putFloat(1.0f); bb.putFloat(MODE_ROUNDED_RECT); bb.putFloat(0f);
        bb.putFloat(0f); bb.putFloat(0f); bb.putFloat(1f); bb.putFloat(0f);
        bb.putFloat((float) mainTarget.width); bb.putFloat((float) mainTarget.height); bb.putFloat(0f); bb.putFloat(0f);
        bb.flip();
        encoder.writeToBuffer(shapeBuf.slice(), bb);
        var indexBuf = RenderSystem.getSequentialBuffer(VertexFormat.Mode.TRIANGLES);
        try (var pass = encoder.createRenderPass(() -> "aporia:blur_rect", colorView, OptionalInt.empty())) {
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("ShapeData", shapeBuf.slice());
            pass.bindTexture("BlurTextureSampler", blurTarget.getColorTextureView(),
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            pass.setVertexBuffer(0, vertexGpu);
            pass.setIndexBuffer(indexBuf.getBuffer(6), indexBuf.type());
            pass.drawIndexed(0, 0, 6, 0);
        }
        mesh.close();
        vertexGpu.close();
        shapeBuf.close();
    }

    /** Overload with default blur strength. */
    public void drawRectBlurred(float x, float y, float w, float h, float radius, int color) {
        drawRectBlurred(x, y, w, h, radius, color, 4f);
    }

    /** Filled rectangle, optionally rounded */
    public void drawRect(float x, float y, float w, float h, float radius, int color) {
        int mode = radius > 0 ? MODE_ROUNDED_RECT : MODE_FILL;
        drawShape(x, y, w, h, color, mode, x, y, w, h, radius, 0, 0f, 0f);
    }

    /**
     * Gradient rectangle — linearly interpolates between c1 and c2.
     * dir: 0=horizontal (left→right), 1=vertical (top→bottom), 2=radial (center→edge).
     */
    public void drawRectGradient(float x, float y, float w, float h, float radius, int c1, int c2, int dir) {
        int steps = Math.max(2, (int)(dir==1 ? h : w) / 2);
        for (int i = 0; i < steps; i++) {
            float t0 = (float) i / steps;
            float t1 = (float)(i + 1) / steps;
            int ca = lerp(c1, c2, (t0 + t1) * 0.5f);
            if (dir == 1) {
                float sy = y + t0 * h, sh = (t1 - t0) * h;
                float r0 = (i == 0 && radius > 0) ? radius : 0;
                float r1 = (i == steps-1 && radius > 0) ? radius : 0;
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
     * Rounded rectangle stroke.
     * {@code borderMode} — 1=full outline, 2=corners-only.
     * {@code thickness}  — stroke width in pixels.
     * {@code fadeCorner} — 0..1, how much straight edges fade (only for mode 2).
     */
    public void drawStroke(float x, float y, float w, float h, float radius,
                           float thickness, int borderMode, float fadeCorner, int color) {
        drawShape(x, y, w, h, color, MODE_ROUNDED_RECT, x, y, w, h, radius,
                borderMode, thickness, fadeCorner);
    }

    /** Internal helpers. */

    private void drawShape(float x, float y, float w, float h, int color,
                           int mode, float bx, float by, float bw, float bh,
                           float radius, int borderMode, float thickness, float fadeCorner) {
        draw(new float[][]{
                {x,   y+h}, {x+w, y+h}, {x+w, y},
                {x,   y+h}, {x+w, y  }, {x,   y}
        }, color, mode, bx, by, bw, bh, radius, borderMode, thickness, fadeCorner);
    }

    private void draw(float[][] verts, int color, int mode,
                      float bx, float by, float bw, float bh, float radius) {
        draw(verts, color, mode, bx, by, bw, bh, radius, 0, 0f, 0f);
    }

    private void draw(float[][] verts, int color, int mode,
                      float bx, float by, float bw, float bh, float radius,
                      int borderMode, float thickness, float fadeCorner) {
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
        var buf  = tess.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (float[] v : verts) {
            buf.addVertex(v[0], v[1], 0f).setUv(0f, 0f).setColor(r, g, b, a);
        }
        var mesh = buf.buildOrThrow();

        var device  = RenderSystem.getDevice();
        var encoder = device.createCommandEncoder();

        var vertexGpu = device.createBuffer(
                () -> "aporia:vbo",
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
                mesh.vertexBuffer()
        );

        /* ShapeData UBO: bounds(16) + params(16) + params2(16) + screen(16) = 64 bytes */
        var shapeBuf = device.createBuffer(() -> "aporia:shape", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, 64L);
        var bb = ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder());
        bb.putFloat(bx); bb.putFloat(by); bb.putFloat(bw); bb.putFloat(bh);
        bb.putFloat(radius); bb.putFloat(1.0f); bb.putFloat(mode); bb.putFloat((float) borderMode);
        bb.putFloat(thickness); bb.putFloat(fadeCorner); bb.putFloat(0f); bb.putFloat(0f);
        bb.putFloat(0f); bb.putFloat(0f); bb.putFloat(0f); bb.putFloat(0f);
        bb.flip();
        encoder.writeToBuffer(shapeBuf.slice(), bb);

        var indexBuf = RenderSystem.getSequentialBuffer(VertexFormat.Mode.TRIANGLES);

        try (var pass = encoder.createRenderPass(() -> "aporia:draw", colorView, OptionalInt.empty())) {
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("ShapeData", shapeBuf.slice());
            pass.setVertexBuffer(0, vertexGpu);
            pass.setIndexBuffer(indexBuf.getBuffer(verts.length), indexBuf.type());
            pass.drawIndexed(0, 0, verts.length / 3 * 3, 0);
        }
        mesh.close();
        vertexGpu.close();
        shapeBuf.close();
    }

    /** Saturation for the postprocess pass. 0=grayscale, 0.5=normal, 1.0=enhanced. */
    public float saturation = 0.5f;

    /** Test render entry point — shapes are commented out until needed. */
    public void onRenderHud(Minecraft mc) {
        if (saturation != 0.5f) applySaturation(mc, saturation);
    }

    /** Draws text via the shared {@link so.aporia.utils.user.render.font.FontRenderer}. */
    public void drawText(String font, String text, float x, float y, float size, int color) {
        Aporia.FONTS.drawText(font, text, x, y, size, color);
    }

    /** Draws text with an MSDF outline. */
    public void drawTextWithOutline(String font, String text, float x, float y, float size,
                                    int color, float outlineWidth, int outlineColor) {
        Aporia.FONTS.drawTextWithOutline(font, text, x, y, size, color, outlineWidth, outlineColor);
    }

    /** Returns pixel width of text at given size. */
    public float getTextWidth(String font, String text, float size) {
        return Aporia.FONTS.getTextWidth(font, text, size);
    }

    /**
     * Draws a single glyph from the big icon font by index.
     * Index maps to PUA codepoint {@code 0xE000 + index}.
     */
    public void drawGlyph(int index, float x, float y, float size, int color) {
        Aporia.FONTS.drawGlyph(Fonts.FONT, index, x, y, size, color);
    }

    public void onRenderWorld(Minecraft mc) {}

    /**
     * Ensures blur targets match current framebuffer size. Lazy + resize-aware.
     */
    private void ensureBlurTarget(Minecraft mc) {
        var main = mc.getMainRenderTarget();
        if (main.width == blurTargetW && main.height == blurTargetH) return;
        if (blurTarget     != null) blurTarget.destroyBuffers();
        if (blurTempTarget != null) blurTempTarget.destroyBuffers();
        blurTarget     = new TextureTarget("aporia:blur_final", main.width, main.height, false);
        blurTempTarget = new TextureTarget("aporia:blur_temp",  main.width, main.height, false);
        blurTargetW = main.width;
        blurTargetH = main.height;
        blurReady   = false;
    }

    /**
     * Two-pass separable gaussian: mainTarget → blurTempTarget (H) → blurTarget (V).
     * For strong blur, runs multiple iterations to avoid sampling artifacts.
     * @param saturation 0=grayscale, 0.5=normal, 1.0=enhanced colors in the blurred result
     */
    public void prepareBlur(Minecraft mc, float strength, float saturation) {
        ensureBlurTarget(mc);
        blurReady = false;
        var mainTarget = mc.getMainRenderTarget();
        var device     = RenderSystem.getDevice();
        float fw = mainTarget.width, fh = mainTarget.height;
        int   iterations   = Math.max(1, (int)(strength / 6f));
        float passStrength = strength / iterations;
        var tess = Tesselator.getInstance();
        var buf  = tess.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX);
        buf.addVertex(-1f,  1f, 0f).setUv(0f, 1f);
        buf.addVertex(-1f, -1f, 0f).setUv(0f, 0f);
        buf.addVertex( 1f, -1f, 0f).setUv(1f, 0f);
        buf.addVertex(-1f,  1f, 0f).setUv(0f, 1f);
        buf.addVertex( 1f, -1f, 0f).setUv(1f, 0f);
        buf.addVertex( 1f,  1f, 0f).setUv(1f, 1f);
        var mesh      = buf.buildOrThrow();
        var vertexGpu = device.createBuffer(() -> "aporia:blur_vbo",
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, mesh.vertexBuffer());
        var encoder = device.createCommandEncoder();
        for (int iter = 0; iter < iterations; iter++) {
            var srcH = (iter == 0) ? mainTarget.getColorTextureView()
                    : blurTarget.getColorTextureView();
            float sat = (iter == 0) ? saturation : 0.5f;
            var hBuf = device.createBuffer(() -> "aporia:blur_h", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, 32L);
            var hBB  = ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder());
            hBB.putFloat(fw); hBB.putFloat(fh); hBB.putFloat(passStrength); hBB.putFloat(0f);
            hBB.putFloat(sat); hBB.putFloat(0f); hBB.putFloat(0f); hBB.putFloat(0f);
            hBB.flip();
            encoder.writeToBuffer(hBuf.slice(), hBB);
            var vBuf = device.createBuffer(() -> "aporia:blur_v", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, 32L);
            var vBB  = ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder());
            vBB.putFloat(fw); vBB.putFloat(fh); vBB.putFloat(passStrength); vBB.putFloat(1f);
            vBB.putFloat(0.5f); vBB.putFloat(0f); vBB.putFloat(0f); vBB.putFloat(0f);
            vBB.flip();
            encoder.writeToBuffer(vBuf.slice(), vBB);
            try (var pass = encoder.createRenderPass(() -> "aporia:blur_h",
                    blurTempTarget.getColorTextureView(), OptionalInt.empty())) {
                pass.setPipeline(blurPipeline);
                pass.bindTexture("InputTexture", srcH,
                        RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
                pass.setUniform("BlurData", hBuf.slice());
                pass.setVertexBuffer(0, vertexGpu);
                pass.draw(0, 6);
            }
            try (var pass = encoder.createRenderPass(() -> "aporia:blur_v",
                    blurTarget.getColorTextureView(), OptionalInt.empty())) {
                pass.setPipeline(blurPipeline);
                pass.bindTexture("InputTexture", blurTempTarget.getColorTextureView(),
                        RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
                pass.setUniform("BlurData", vBuf.slice());
                pass.setVertexBuffer(0, vertexGpu);
                pass.draw(0, 6);
            }
            hBuf.close();
            vBuf.close();
        }
        mesh.close();
        vertexGpu.close();
        blurReady = true;
    }

    /** Overload with default saturation (normal). */
    public void prepareBlur(Minecraft mc, float strength) {
        prepareBlur(mc, strength, 0.5f);
    }

    /**
     * Applies saturation grading to the entire framebuffer.
     * @param saturation 0.0 = grayscale, 0.5 = original colors, 1.0 = enhanced saturation
     */
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
        var buf  = tess.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX);
        buf.addVertex(-1f,  1f, 0f).setUv(0f, 1f);
        buf.addVertex(-1f, -1f, 0f).setUv(0f, 0f);
        buf.addVertex( 1f, -1f, 0f).setUv(1f, 0f);
        buf.addVertex(-1f,  1f, 0f).setUv(0f, 1f);
        buf.addVertex( 1f, -1f, 0f).setUv(1f, 0f);
        buf.addVertex( 1f,  1f, 0f).setUv(1f, 1f);
        var mesh      = buf.buildOrThrow();
        var vertexGpu = device.createBuffer(() -> "aporia:post_vbo",
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, mesh.vertexBuffer());
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
            pass.setVertexBuffer(0, vertexGpu);
            pass.draw(0, 6);
        }
        try (var pass = encoder.createRenderPass(() -> "aporia:post_blit",
                mainTarget.getColorTextureView(), OptionalInt.empty())) {
            pass.setPipeline(blitPipeline);
            pass.bindTexture("InputTexture", postTempTarget.getColorTextureView(),
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            pass.setVertexBuffer(0, vertexGpu);
            pass.draw(0, 6);
        }
        mesh.close();
        vertexGpu.close();
        dataBuf.close();
    }

    /**
     * Loads a PNG/JPG from disk and registers it with TextureManager.
     * Returns the Identifier, or null on failure.
     */
    public Identifier loadImage(Path path) {
        String key = path.toAbsolutePath().toString();
        if (imageIds.containsKey(key)) return imageIds.get(key);
        try (FileInputStream fis = new FileInputStream(path.toFile())) {
            NativeImage img = NativeImage.read(fis);
            DynamicTexture tex = new DynamicTexture(() -> key, img);
            String name = path.getFileName().toString()
                .toLowerCase().replaceAll("[^a-z0-9_.-]", "_");
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

    /**
     * Draws a previously loaded image (by Identifier) into screen-space rect [x,y,w,h].
     * Uses blitPipeline with screen-space POSITION_TEX quad.
     */
    public void drawImage(float x, float y, float w, float h, Identifier id) {
        if (blitPipeline == null || id == null) return;
        Minecraft mc = Minecraft.getInstance();
        var tm = mc.getTextureManager();
        var tex = tm.getTexture(id);
        if (tex == null) return;
        var texView = tex.getTextureView();
        if (texView == null) return;
        var colorView = mc.getMainRenderTarget().getColorTextureView();
        if (colorView == null) return;

        float sw = mc.getWindow().getGuiScaledWidth();
        float sh = mc.getWindow().getGuiScaledHeight();
        float nx  = x / sw * 2f - 1f;
        float ny  = 1f - y / sh * 2f;
        float nx2 = (x + w) / sw * 2f - 1f;
        float ny2 = 1f - (y + h) / sh * 2f;

        var tess = Tesselator.getInstance();
        var buf  = tess.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX);
        buf.addVertex(nx,  ny2, 0f).setUv(0f, 1f);
        buf.addVertex(nx,  ny,  0f).setUv(0f, 0f);
        buf.addVertex(nx2, ny,  0f).setUv(1f, 0f);
        buf.addVertex(nx,  ny2, 0f).setUv(0f, 1f);
        buf.addVertex(nx2, ny,  0f).setUv(1f, 0f);
        buf.addVertex(nx2, ny2, 0f).setUv(1f, 1f);
        var mesh = buf.buildOrThrow();

        var device  = RenderSystem.getDevice();
        var encoder = device.createCommandEncoder();
        var vertexGpu = device.createBuffer(() -> "aporia:img_vbo",
            GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, mesh.vertexBuffer());

        try (var pass = encoder.createRenderPass(() -> "aporia:img_pass", colorView, OptionalInt.empty())) {
            pass.setPipeline(blitPipeline);
            pass.bindTexture("InputTexture", texView,
                RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            pass.setVertexBuffer(0, vertexGpu);
            pass.draw(0, 6);
        }
        mesh.close();
        vertexGpu.close();
    }

    public void cleanupBlur() {
        if (blurTarget     != null) { blurTarget.destroyBuffers();     blurTarget     = null; }
        if (blurTempTarget != null) { blurTempTarget.destroyBuffers(); blurTempTarget = null; }
        blurTargetW = -1; blurTargetH = -1; blurReady = false;
    }
}