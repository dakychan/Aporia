package so.aporia.utils.user.render.core;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer;
import net.minecraft.resources.Identifier;
import so.aporia.utils.user.render.font.Fonts;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.OptionalInt;

public class AporiaRenderer {

    public static final AporiaRenderer INSTANCE = new AporiaRenderer();

    /** Mode constants — must match values in {@code aporia.fsh}. */
    public static final int MODE_FILL         = 0;
    public static final int MODE_CIRCLE       = 1;
    public static final int MODE_ROUNDED_RECT = 2;

    private RenderPipeline pipeline;
    private CachedOrthoProjectionMatrixBuffer orthoProjection;

    /* Blur pipeline fields */
    private RenderPipeline blurPipeline;
    private RenderPipeline blitPipeline;
    private TextureTarget blurTarget;
    private GpuBuffer blurUniformBuffer;
    
    /* Blur rectangle settings */
    private float blurRectW = 300f;
    private float blurRectH = 200f;
    private float blurRectRadius = 15f;
    private float blurRectSmoothness = 2f;
    private float blurRadius = 15f;

    public void init() {
        pipeline = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/aporia"))
            .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/aporia"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/aporia"))
            .withUniform("Projection", com.mojang.blaze3d.shaders.UniformType.UNIFORM_BUFFER)
            .withUniform("ShapeData",  com.mojang.blaze3d.shaders.UniformType.UNIFORM_BUFFER)
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.TRIANGLES)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .build();

        orthoProjection = new CachedOrthoProjectionMatrixBuffer("aporia", -1000f, 1000f, true);

        /* Init blur pipeline */
        blurPipeline = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/blur"))
            .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/blur"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/blur"))
            .withSampler("InputTexture")
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .build();

        /* Init blit pipeline - uses simple blit shader */
        blitPipeline = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/blur_blit"))
            .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/blit"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/blit"))
            .withSampler("InputTexture")
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .build();
    }

    /** Public drawing API. */

    /** Line from (x1,y1) to (x2,y2) with given pixel thickness */
    public void drawLine(float x1, float y1, float x2, float y2, float thickness, int color) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len == 0) return;
        float nx = -dy / len * thickness * 0.5f;
        float ny =  dx / len * thickness * 0.5f;

        /* Two triangles forming a quad along the line. */
        float[] vx = { x1+nx, x1-nx, x2-nx, x2+nx };
        float[] vy = { y1+ny, y1-ny, y2-ny, y2+ny };

        /* Bounds = line bounding box for SDF (unused in mode 0, but populated for consistency). */
        float bx = Math.min(x1, x2) - thickness;
        float by = Math.min(y1, y2) - thickness;
        float bw = Math.abs(dx) + thickness * 2;
        float bh = Math.abs(dy) + thickness * 2;

        draw(new float[][]{
            {vx[0], vy[0]}, {vx[1], vy[1]}, {vx[2], vy[2]},
            {vx[0], vy[0]}, {vx[2], vy[2]}, {vx[3], vy[3]}
        }, color, MODE_FILL, bx, by, bw, bh, 0f);
    }

    /** Filled circle */
    public void drawCircle(float cx, float cy, float radius, int color) {
        float x = cx - radius, y = cy - radius, d = radius * 2;
        drawShape(x, y, d, d, color, MODE_CIRCLE, x, y, d, d, radius, 0, 0f, 0f);
    }

    /** Filled triangle with 3 explicit vertices */
    public void drawTriangle(float x1, float y1, float x2, float y2, float x3, float y3, int color) {
        float bx = Math.min(x1, Math.min(x2, x3));
        float by = Math.min(y1, Math.min(y2, y3));
        float bw = Math.max(x1, Math.max(x2, x3)) - bx;
        float bh = Math.max(y1, Math.max(y2, y3)) - by;
        draw(new float[][]{{x1,y1},{x2,y2},{x3,y3}}, color, MODE_FILL, bx, by, bw, bh, 0f);
    }

    /** Filled rectangle, optionally rounded */
    public void drawRect(float x, float y, float w, float h, float radius, int color) {
        int mode = radius > 0 ? MODE_ROUNDED_RECT : MODE_FILL;
        drawShape(x, y, w, h, color, mode, x, y, w, h, radius, 0, 0f, 0f);
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

        /* ShapeData UBO layout: bounds(4) + params(4) + params2(4) = 48 bytes */
        var shapeBuf = device.createBuffer(() -> "aporia:shape", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, 48L);
        var bb = ByteBuffer.allocateDirect(48).order(ByteOrder.nativeOrder());
        bb.putFloat(bx); bb.putFloat(by); bb.putFloat(bw); bb.putFloat(bh);       /* bounds  */
        bb.putFloat(radius); bb.putFloat(1.0f); bb.putFloat(mode); bb.putFloat((float) borderMode); /* params  */
        bb.putFloat(thickness); bb.putFloat(fadeCorner); bb.putFloat(0f); bb.putFloat(0f);           /* params2 */
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

    /** Test render entry point — shapes are commented out until needed. */

    public void onRenderHud(Minecraft mc) {
    }

    /** Draws text via the shared {@link so.aporia.utils.user.render.font.FontRenderer}. */
    public void drawText(String font, String text, float x, float y, float size, int color) {
        so.aporia.Aporia.FONTS.drawText(font, text, x, y, size, color);
    }

    /** Draws text with an MSDF outline. */
    public void drawTextWithOutline(String font, String text, float x, float y, float size,
                                    int color, float outlineWidth, int outlineColor) {
        so.aporia.Aporia.FONTS.drawTextWithOutline(font, text, x, y, size, color, outlineWidth, outlineColor);
    }

    /** Returns pixel width of text at given size. */
    public float getTextWidth(String font, String text, float size) {
        return so.aporia.Aporia.FONTS.getTextWidth(font, text, size);
    }

    /**
     * Draws a single glyph from the big icon font by index.
     * Index maps to PUA codepoint {@code 0xE000 + index}.
     */
    public void drawGlyph(int index, float x, float y, float size, int color) {
        so.aporia.Aporia.FONTS.drawGlyph(Fonts.FONT, index, x, y, size, color);
    }

    public void onRenderWorld(Minecraft mc) {}

    /**
     * Applies blur effect to a rectangular region.
     * Called from Blur module each frame when enabled.
     */
    public void applyBlur(Minecraft mc, float blurRadius) {
        var mainTarget = mc.getMainRenderTarget();
        var colorView = mainTarget.getColorTextureView();
        if (colorView == null || blurPipeline == null) return;

        var device = RenderSystem.getDevice();
        var encoder = device.createCommandEncoder();

        /* Draw a quad with blur effect in center of screen */
        var tess = Tesselator.getInstance();
        var buf = tess.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        
        /* Center quad - NDC coordinates */
        float size = 0.4f;
        buf.addVertex(-size, size, 0f).setColor(1f, 1f, 1f, 1f);
        buf.addVertex(-size, -size, 0f).setColor(1f, 1f, 1f, 1f);
        buf.addVertex(size, -size, 0f).setColor(1f, 1f, 1f, 1f);
        buf.addVertex(-size, size, 0f).setColor(1f, 1f, 1f, 1f);
        buf.addVertex(size, -size, 0f).setColor(1f, 1f, 1f, 1f);
        buf.addVertex(size, size, 0f).setColor(1f, 1f, 1f, 1f);

        var mesh = buf.buildOrThrow();
        var vertexGpu = device.createBuffer(
            () -> "aporia:blur_vbo",
            GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
            mesh.vertexBuffer()
        );

        try (var pass = encoder.createRenderPass(() -> "aporia:blur_pass", mainTarget.getColorTextureView(), OptionalInt.empty())) {
            pass.setPipeline(blurPipeline);
            pass.bindTexture("InputTexture", colorView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            pass.setVertexBuffer(0, vertexGpu);
            pass.draw(0, 6);
        }

        mesh.close();
        vertexGpu.close();
    }

    /**
     * Renders blur effect for the Blur module.
     * Call this from RenderHudEvent when Blur is enabled.
     */
    public void renderBlur(Minecraft mc) {
        applyBlur(mc, 5f);
    }

    /**
     * Cleans up blur resources when module is disabled.
     */
    public void cleanupBlur() {
        if (blurTarget != null) {
            blurTarget.destroyBuffers();
            blurTarget = null;
        }
        if (blurUniformBuffer != null) {
            blurUniformBuffer.close();
            blurUniformBuffer = null;
        }
    }
}
