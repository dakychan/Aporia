package so.aporia.utils.user.render.core;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/**
 * Batch renderer — accumulates draw tasks and flushes them in a single draw call.
 * Reduces GPU state changes and encoder overhead.
 *
 * Usage:
 *   DrawBatch batch = new DrawBatch();
 *   batch.drawRect(x, y, w, h, radius, color);
 *   batch.drawRect(x2, y2, w2, h2, radius2, color2);
 *   batch.flush(); // single draw call
 */
public class DrawBatch {

    public static final DrawBatch INSTANCE = new DrawBatch();

    private static final int MAX_QUADS = 4096;
    private static final int FLOATS_PER_VERTEX = 9; // x, y, z, u, v, r, g, b, a
    private static final int VERTICES_PER_QUAD = 6;

    private final List<float[]> quads = new ArrayList<>(256);
    private RenderPipeline activePipeline;

    private GpuBuffer vbo;
    private ByteBuffer vertexData;

    public DrawBatch() {
        vertexData = ByteBuffer.allocateDirect(MAX_QUADS * VERTICES_PER_QUAD * FLOATS_PER_VERTEX * 4)
                .order(ByteOrder.nativeOrder());
    }

    public void drawRect(float x, float y, float w, float h, float radius, int color) {
        switchPipeline(AporiaRenderer.INSTANCE.pipeline);
        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >>  8) & 0xFF) / 255f;
        float b = ((color      ) & 0xFF) / 255f;

        quads.add(new float[]{
                x, y+h, 0f,  0f, 1f, r, g, b, a,
                x+w, y+h, 0f,  1f, 1f, r, g, b, a,
                x+w, y, 0f,  1f, 0f, r, g, b, a,
                x, y+h, 0f,  0f, 1f, r, g, b, a,
                x+w, y, 0f,  1f, 0f, r, g, b, a,
                x, y, 0f,  0f, 0f, r, g, b, a,
        });

        if (quads.size() >= MAX_QUADS) {
            flush();
        }
    }

    public void drawRectBlurred(float x, float y, float w, float h, float radius, int color) {
        AporiaRenderer.INSTANCE.drawRectBlurred(x, y, w, h, radius, color);
    }

    public void drawStroke(float x, float y, float w, float h, float radius, float thickness, int color) {
        AporiaRenderer.INSTANCE.drawStroke(x, y, w, h, radius, thickness, 15, 0f, color);
    }

    public void flush() {
        if (quads.isEmpty()) return;

        var mc = Minecraft.getInstance();
        var colorView = mc.getMainRenderTarget().getColorTextureView();
        if (colorView == null) { quads.clear(); return; }

        var tess = Tesselator.getInstance();
        var buf = tess.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR);

        for (float[] quad : quads) {
            for (int v = 0; v < VERTICES_PER_QUAD; v++) {
                int off = v * FLOATS_PER_VERTEX;
                buf.addVertex(quad[off], quad[off+1], quad[off+2])
                   .setUv(quad[off+3], quad[off+4])
                   .setColor(quad[off+5], quad[off+6], quad[off+7], quad[off+8]);
            }
        }

        var mesh = buf.buildOrThrow();
        var device = RenderSystem.getDevice();
        var encoder = device.createCommandEncoder();

        if (vbo == null) {
            vbo = device.createBuffer(() -> "aporia:batch_vbo",
                    GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, MAX_QUADS * VERTICES_PER_QUAD * FLOATS_PER_VERTEX * 4L);
        }

        vertexData.clear();
        encoder.writeToBuffer(vbo.slice(), mesh.vertexBuffer());
        mesh.close();

        var projSlice = AporiaRenderer.INSTANCE.orthoProjection.getBuffer(
                mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
        RenderSystem.setProjectionMatrix(projSlice, com.mojang.blaze3d.ProjectionType.ORTHOGRAPHIC);

        var indexBuf = RenderSystem.getSequentialBuffer(VertexFormat.Mode.TRIANGLES);
        int vertexCount = quads.size() * VERTICES_PER_QUAD;

        try (var pass = encoder.createRenderPass(() -> "aporia:batch_flush", colorView, OptionalInt.empty())) {
            pass.setPipeline(AporiaRenderer.INSTANCE.pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setVertexBuffer(0, vbo);
            pass.setIndexBuffer(indexBuf.getBuffer(vertexCount), indexBuf.type());
            pass.drawIndexed(0, 0, vertexCount, 0);
        }

        quads.clear();
    }

    private void switchPipeline(RenderPipeline target) {
        if (activePipeline != target) {
            flush();
            activePipeline = target;
        }
    }

    public void close() {
        flush();
        if (vbo != null) {
            vbo.close();
            vbo = null;
        }
    }

    public int pendingQuads() {
        return quads.size();
    }
}
