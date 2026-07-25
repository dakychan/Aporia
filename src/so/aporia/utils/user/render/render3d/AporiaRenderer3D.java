/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.utils.user.render.render3d;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4fc;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import com.chaos.annotation.ChaosNative;

/**
 * 3D-рендерер Aporia — рисует примитивы в мировых координатах.
 * <p>
 * Aporia 3D renderer — draws primitives in world coordinates.
 * <p>
 * Features / Возможности:
 * <ul>
 *   <li>Solid/wireframe boxes, spheres, cylinders / Закрашенные/каркасные боксы, сферы, цилиндры</li>
 *   <li>3D rectangles at arbitrary positions / 3D прямоугольники в произвольных позициях</li>
 *   <li>Nearby entity rendering with zone selection / Рендер ближайших сущностей с выбором зоны</li>
 *   <li>Blurred rectangles from current frame / Размытые прямоугольники из текущего фрейма</li>
 *   <li>Lines, grids, coordinate axes / Линии, сетки, оси координат</li>
 * </ul>
 */
@ChaosNative
public class AporiaRenderer3D {

    /** Синглтон / Singleton. */
    public static final AporiaRenderer3D INSTANCE = new AporiaRenderer3D();

    /** Тип зоны для drawNear.
     *  Zone shape type for drawNear. */
    @ChaosNative
public enum ZoneShape {
        /** Сфера вокруг игрока / Sphere around player. */
        SPHERE,
        /** Куб (AABB) вокруг игрока / Cube (AABB) around player. */
        BOX,
        /** Цилиндр вокруг игрока / Cylinder around player. */
        CYLINDER
    }

    private RenderPipeline solidPipeline;
    private RenderPipeline wirePipeline;
    private RenderPipeline unlitPipeline;
    private RenderPipeline blurredPipeline;

    /** Mega pipelines — unified shader for all 3D rendering. */
    private RenderPipeline megaTriPipeline;
    private RenderPipeline megaLinePipeline;
    private RenderPipeline megaTriOverlayPipeline;
    private RenderPipeline megaLineOverlayPipeline;

    private float depthOffset = 0f;

    public void depthOffset(float offset) {
        this.depthOffset = offset;
    }

    private final List<DrawTask3D> taskQueue = new ArrayList<>();

    private static final class DrawTask3D {
        final float depth;
        final byte[] vertexData;
        final int vertexCount;
        final boolean lines;
        final Matrix4f viewProj;
        final Matrix4f model;
        final int drawMode;
        final float glowInt, glowRad;
        final float cornerR, edgeSoft, strokeW, strokeOnly;
        final float rimPower, rimIntens, fresnelP;
        final float panelCx, panelCy, panelHx, panelHy;
        final boolean overlay;
        DrawTask3D(float depth, byte[] vertexData, int vertexCount, boolean lines,
                   Matrix4f viewProj, Matrix4f model, int drawMode,
                   float glowInt, float glowRad, float cornerR, float edgeSoft,
                   float strokeW, float strokeOnly, float rimPower, float rimIntens,
                   float fresnelP, float panelCx, float panelCy, float panelHx,
                   float panelHy, boolean overlay) {
            this.depth = depth; this.vertexData = vertexData;
            this.vertexCount = vertexCount; this.lines = lines;
            this.viewProj = new Matrix4f(viewProj); this.model = new Matrix4f(model);
            this.drawMode = drawMode;
            this.glowInt = glowInt; this.glowRad = glowRad;
            this.cornerR = cornerR; this.edgeSoft = edgeSoft;
            this.strokeW = strokeW; this.strokeOnly = strokeOnly;
            this.rimPower = rimPower; this.rimIntens = rimIntens;
            this.fresnelP = fresnelP;
            this.panelCx = panelCx; this.panelCy = panelCy;
            this.panelHx = panelHx; this.panelHy = panelHy;
            this.overlay = overlay;
        }
    }

    public void flush3D() {
        taskQueue.sort(Comparator.comparingDouble((DrawTask3D t) -> t.depth));
        for (DrawTask3D task : taskQueue) executeTask(task);
        taskQueue.clear();
    }

    private void executeTask(DrawTask3D task) {
        var mainTarget = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        var colorView = mainTarget.getColorTextureView();
        var depthView = mainTarget.getDepthTextureView();
        if (colorView == null) return;

        var pipeline = task.overlay
                ? (task.lines ? megaLineOverlayPipeline : megaTriOverlayPipeline)
                : (task.lines ? megaLinePipeline : megaTriPipeline);
        if (pipeline == null) return;

        uploadMvp(task.viewProj, task.model);
        uploadDrawParams(task.drawMode, task.glowInt, task.glowRad,
                task.cornerR, task.edgeSoft, task.strokeW, task.strokeOnly,
                task.rimPower, task.rimIntens, task.fresnelP,
                task.panelCx, task.panelCy, task.panelHx, task.panelHy);

        ByteBuffer vertexBuf = ByteBuffer.wrap(task.vertexData);
        var device = RenderSystem.getDevice();
        var encoder = device.createCommandEncoder();
        encoder.writeToBuffer(mvpUbo.slice(), cachedMvpBB);
        encoder.writeToBuffer(drawParamsUbo.slice(), cachedDrawParamsBB);
        encoder.writeToBuffer(cachedVbo.slice(), vertexBuf);

        try (var pass = encoder.createRenderPass(() -> "aporia:render3d_mega",
                colorView, Optional.<Vector4fc>empty(), depthView, OptionalDouble.empty())) {
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("ModelViewProj", mvpUbo.slice());
            pass.setUniform("DrawParams", drawParamsUbo.slice());
            pass.setVertexBuffer(0, cachedVbo.slice());
            pass.draw(0, task.vertexCount, 0, 0);
        }
    }

    private GpuBuffer mvpUbo;
    private GpuBuffer drawParamsUbo;
    private GpuBuffer cachedVbo;

    private ByteBuffer cachedMvpBB;
    private ByteBuffer cachedDrawParamsBB;

    private boolean initialized = false;

    /** Максимальный размер VBO — 1 МБ (хватает на ~1000 сущностей).
     *  Maximum VBO size — 1 MB (enough for ~1000 entities). */
    private static final int VBO_SIZE = 1024 * 1024;

    /** Pre-allocated matrices to avoid per-frame GC pressure. */
    private final Matrix4f tempMvp = new Matrix4f();
    private final Matrix4f tempNormal = new Matrix4f();

    /**
     * Инициализация GPU-пайплайнов и буферов.
     * <p>
     * Initialize GPU pipelines and buffers.
     */
    public void init() {
        if (initialized) return;

        solidPipeline = RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/render3d"))
                .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/render3d"))
                .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/render3d"))
                .withBindGroupLayout(BindGroupLayout.builder()
                        .withUniform("ModelViewProj", UniformType.UNIFORM_BUFFER)
                        .build())
                .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_NORMAL)
                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
                .withCull(true)
                .build();

        wirePipeline = RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/render3d_wire"))
                .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/render3d"))
                .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/render3d"))
                .withBindGroupLayout(BindGroupLayout.builder()
                        .withUniform("ModelViewProj", UniformType.UNIFORM_BUFFER)
                        .build())
                .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_NORMAL)
                .withPrimitiveTopology(PrimitiveTopology.LINES)
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
                .withCull(false)
                .build();

        unlitPipeline = RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/render3d_unlit"))
                .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/render3d"))
                .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/render3d"))
                .withBindGroupLayout(BindGroupLayout.builder()
                        .withUniform("ModelViewProj", UniformType.UNIFORM_BUFFER)
                        .build())
                .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_NORMAL)
                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
                .withCull(true)
                .withShaderDefine("NO_LIGHTING")
                .build();

        /** Пайплайн для блюренных прямоугольников (берёт текстуру экрана и размывает).
         *  Pipeline for blurred rectangles (takes screen texture and blurs it). */
        blurredPipeline = RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/render3d_blur"))
                .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/render3d_blur"))
                .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/render3d_blur"))
                .withBindGroupLayout(BindGroupLayout.builder()
                        .withUniform("ModelViewProj", UniformType.UNIFORM_BUFFER)
                        .withSampler("ScreenTexture")
                        .build())
                .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
                .withCull(true)
                .build();

        /** Mega pipelines — unified shader for all 3D rendering. */
        megaTriPipeline = RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/render3d_mega"))
                .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/render3d_mega"))
                .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/render3d_mega"))
                .withBindGroupLayout(BindGroupLayout.builder()
                        .withUniform("ModelViewProj", UniformType.UNIFORM_BUFFER)
                        .withUniform("DrawParams", UniformType.UNIFORM_BUFFER)
                        .build())
                .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_NORMAL)
                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
                .withCull(true)
                .build();

        megaLinePipeline = RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/render3d_mega_wire"))
                .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/render3d_mega"))
                .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/render3d_mega"))
                .withBindGroupLayout(BindGroupLayout.builder()
                        .withUniform("ModelViewProj", UniformType.UNIFORM_BUFFER)
                        .withUniform("DrawParams", UniformType.UNIFORM_BUFFER)
                        .build())
                .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_NORMAL)
                .withPrimitiveTopology(PrimitiveTopology.LINES)
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
                .withCull(false)
                .build();

        megaTriOverlayPipeline = RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/render3d_mega_overlay"))
                .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/render3d_mega"))
                .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/render3d_mega"))
                .withBindGroupLayout(BindGroupLayout.builder()
                        .withUniform("ModelViewProj", UniformType.UNIFORM_BUFFER)
                        .withUniform("DrawParams", UniformType.UNIFORM_BUFFER)
                        .build())
                .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_NORMAL)
                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
                .withCull(true)
                .build();

        megaLineOverlayPipeline = RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/render3d_mega_wire_overlay"))
                .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/render3d_mega"))
                .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/render3d_mega"))
                .withBindGroupLayout(BindGroupLayout.builder()
                        .withUniform("ModelViewProj", UniformType.UNIFORM_BUFFER)
                        .withUniform("DrawParams", UniformType.UNIFORM_BUFFER)
                        .build())
                .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_NORMAL)
                .withPrimitiveTopology(PrimitiveTopology.LINES)
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
                .withCull(false)
                .build();

        var device = RenderSystem.getDevice();
        mvpUbo = device.createBuffer(() -> "aporia:render3d_mvp",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                3L * 64 + 16);

        cachedVbo = device.createBuffer(() -> "aporia:render3d_vbo",
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, VBO_SIZE);

        /** DrawParams UBO — 3 × vec4 = 48 bytes, round to 64 for alignment.
         *  modeParams (drawMode, glowIntensity, glowRadius, 0)
         *  shapeParams (cornerRadius, edgeSoftness, strokeWidth, strokeOnly)
         *  colorParams (rimPower, rimIntensity, fresnelPower, 0) */
        drawParamsUbo = device.createBuffer(() -> "aporia:render3d_draw_params",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, 64L);

        cachedMvpBB = ByteBuffer.allocateDirect(3 * 64 + 16).order(ByteOrder.nativeOrder());
        cachedDrawParamsBB = ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder());

        initialized = true;
    }

    private void uploadMvp(Matrix4f viewProj, Matrix4f model) {
        cachedMvpBB.clear();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            tempMvp.set(viewProj).mul(model);
            tempNormal.set(model).invert().transpose();
            Std140Builder.onStack(stack, cachedMvpBB.remaining())
                    .putMat4f(tempMvp)
                    .putMat4f(model)
                    .putMat4f(tempNormal)
                    .get();
        }
        cachedMvpBB.rewind();
        cachedMvpBB.limit((int)(3L * 64 + 16));
    }

    private void drawWithVertices(ByteBuffer vertexData, int vertexCount, boolean lines,
                                   Matrix4f viewProj, Matrix4f model) {
        if (vertexCount == 0) return;

        var mainTarget = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        var colorView = mainTarget.getColorTextureView();
        var depthView = mainTarget.getDepthTextureView();
        if (colorView == null) return;

        var pipeline = lines ? wirePipeline : solidPipeline;
        if (pipeline == null) return;

        uploadMvp(viewProj, model);

        vertexData.flip();
        var device = RenderSystem.getDevice();
        var encoder = device.createCommandEncoder();
        encoder.writeToBuffer(mvpUbo.slice(), cachedMvpBB);
        encoder.writeToBuffer(cachedVbo.slice(), vertexData);

        try (var pass = encoder.createRenderPass(() -> "aporia:render3d",
                colorView, Optional.<Vector4fc>empty(), depthView, OptionalDouble.empty())) {
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("ModelViewProj", mvpUbo.slice());
            pass.setVertexBuffer(0, cachedVbo.slice());
            pass.draw(0, vertexCount, 0, 0);
        }
    }

    // ==================== Mega Pipeline ====================

    /** Draw mode constants — match shader DrawParams.x */
    public static final int MODE_BASIC_LIT     = 0;
    public static final int MODE_WIRE_GLOW     = 1;
    public static final int MODE_SDF_PANEL     = 2;
    public static final int MODE_SOLID_RIM     = 3;
    public static final int MODE_UNLIT         = 4;
    public static final int MODE_UNLIT_GLOW    = 5;

    private void uploadDrawParams(float drawMode, float glowInt, float glowRad,
                                   float cornerR, float edgeSoft, float strokeW, float strokeOnly,
                                   float rimPower, float rimIntens, float fresnelP,
                                   float panelCx, float panelCy, float panelHx, float panelHy) {
        cachedDrawParamsBB.clear();
        cachedDrawParamsBB.putFloat(drawMode).putFloat(glowInt).putFloat(glowRad).putFloat(0f);
        cachedDrawParamsBB.putFloat(cornerR).putFloat(edgeSoft).putFloat(strokeW).putFloat(strokeOnly);
        cachedDrawParamsBB.putFloat(rimPower).putFloat(rimIntens).putFloat(fresnelP).putFloat(0f);
        cachedDrawParamsBB.putFloat(panelCx).putFloat(panelCy).putFloat(panelHx).putFloat(panelHy);
        cachedDrawParamsBB.flip();
    }

    /**
     * Core mega draw — submits pre-built vertex data with draw params.
     */
    private void drawMega(ByteBuffer vertexData, int vertexCount, boolean lines,
                          Matrix4f viewProj, Matrix4f model, int drawMode,
                          float glowInt, float glowRad,
                          float cornerR, float edgeSoft, float strokeW, float strokeOnly,
                          float rimPower, float rimIntens, float fresnelP,
                          float panelCx, float panelCy, float panelHx, float panelHy) {
        if (vertexCount == 0 || vertexData == null) return;

        byte[] dataCopy = new byte[vertexData.remaining()];
        vertexData.get(dataCopy);
        vertexData.rewind();

        boolean overlay = depthOffset != 0f;
        float depth = depthOffset;
        depthOffset = 0f;

        taskQueue.add(new DrawTask3D(depth, dataCopy, vertexCount, lines,
                viewProj, model, drawMode,
                glowInt, glowRad, cornerR, edgeSoft, strokeW, strokeOnly,
                rimPower, rimIntens, fresnelP,
                panelCx, panelCy, panelHx, panelHy, overlay));
    }

    /**
     * Draw a solid box with optional rim glow.
     * CPU just builds 36 vertices (6 faces × 2 triangles × 3 verts), GPU does lighting + rim.
     */
    public void drawSolidBox(float cx, float cy, float cz, float sx, float sy, float sz,
                              int color, float rimPower, float rimIntensity,
                              Matrix4f viewProj, Matrix4f model) {
        var buf = build(PrimitiveTopology.TRIANGLES);
        emitSolidBox(buf, cx, cy, cz, sx, sy, sz, color);
        var mesh = buf.buildOrThrow();
        drawMega(mesh.vertexBuffer(), 36, false, viewProj, model,
                MODE_SOLID_RIM, rimIntensity, 0, 0, 0, 0, 0, rimPower, rimIntensity, 0, 0, 0, 0, 0);
        mesh.close();
    }

    /**
     * Draw a wireframe box with glow.
     * CPU builds 24 vertices (12 edges × 2 verts), GPU does glow + fresnel.
     */
    public void drawWireBox(float cx, float cy, float cz, float sx, float sy, float sz,
                             int color, float glowIntensity,
                             Matrix4f viewProj, Matrix4f model) {
        var buf = build(PrimitiveTopology.LINES);
        emitWireBox(buf, cx, cy, cz, sx, sy, sz, color);
        var mesh = buf.buildOrThrow();
        drawMega(mesh.vertexBuffer(), 24, true, viewProj, model,
                MODE_WIRE_GLOW, glowIntensity, 2.0f, 0, 0, 0, 0, 3.0f, glowIntensity, 0, 0, 0, 0, 0);
        mesh.close();
    }

    /**
     * Draw an SDF rounded rect panel in 3D.
     * CPU builds 6 vertices (1 quad), GPU does SDF clipping + glow.
     */
    public void drawPanel(float cx, float cy, float cz, float w, float h,
                           int color, float cornerRadius, float edgeSoftness,
                           Matrix4f viewProj, Matrix4f model) {
        float hw = w / 2f, hh = h / 2f;
        int a = (color >> 24) & 0xFF, r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        var buf = build(PrimitiveTopology.TRIANGLES);

        buf.addVertex(cx - hw, cy - hh, cz).setColor(r, g, b, a).setNormal(0, 0, 1);
        buf.addVertex(cx + hw, cy - hh, cz).setColor(r, g, b, a).setNormal(0, 0, 1);
        buf.addVertex(cx + hw, cy + hh, cz).setColor(r, g, b, a).setNormal(0, 0, 1);
        buf.addVertex(cx - hw, cy - hh, cz).setColor(r, g, b, a).setNormal(0, 0, 1);
        buf.addVertex(cx + hw, cy + hh, cz).setColor(r, g, b, a).setNormal(0, 0, 1);
        buf.addVertex(cx - hw, cy + hh, cz).setColor(r, g, b, a).setNormal(0, 0, 1);

        var mesh = buf.buildOrThrow();
        drawMega(mesh.vertexBuffer(), 6, false, viewProj, model,
                MODE_SDF_PANEL, 0, 0, cornerRadius, edgeSoftness, 0, 0, 0, 0, 0,
                cx, cy, hw, hh);
        mesh.close();
    }

    /**
     * Draw an SDF rounded rect outline (stroke only) in 3D.
     */
    public void drawPanelOutline(float cx, float cy, float cz, float w, float h,
                                  int color, float cornerRadius, float strokeWidth,
                                  Matrix4f viewProj, Matrix4f model) {
        float hw = w / 2f, hh = h / 2f;
        int a = (color >> 24) & 0xFF, r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        var buf = build(PrimitiveTopology.TRIANGLES);

        buf.addVertex(cx - hw, cy - hh, cz).setColor(r, g, b, a).setNormal(0, 0, 1);
        buf.addVertex(cx + hw, cy - hh, cz).setColor(r, g, b, a).setNormal(0, 0, 1);
        buf.addVertex(cx + hw, cy + hh, cz).setColor(r, g, b, a).setNormal(0, 0, 1);
        buf.addVertex(cx - hw, cy - hh, cz).setColor(r, g, b, a).setNormal(0, 0, 1);
        buf.addVertex(cx + hw, cy + hh, cz).setColor(r, g, b, a).setNormal(0, 0, 1);
        buf.addVertex(cx - hw, cy + hh, cz).setColor(r, g, b, a).setNormal(0, 0, 1);

        var mesh = buf.buildOrThrow();
        drawMega(mesh.vertexBuffer(), 6, false, viewProj, model,
                MODE_SDF_PANEL, 0, 0, cornerRadius, 0.5f, strokeWidth, 1, 0, 0, 0,
                cx, cy, hw, hh);
        mesh.close();
    }

    /** Build vertex data for a solid box (36 vertices, 6 faces). */
    private static void emitSolidBox(BufferBuilder buf, float cx, float cy, float cz,
                                      float sx, float sy, float sz, int color) {
        float hx = sx / 2f, hy = sy / 2f, hz = sz / 2f;
        int a = (color >> 24) & 0xFF, r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;

        float[][] faceNormals = {{0,0,-1},{0,0,1},{-1,0,0},{1,0,0},{0,-1,0},{0,1,0}};
        float[][] v0 = {{-1,-1,-1},{1,-1,1},{-1,-1,1},{1,-1,-1},{-1,-1,1},{-1,1,-1}};
        float[][] v1 = {{1,-1,-1},{-1,-1,1},{-1,1,-1},{1,-1,1},{1,-1,1},{1,1,-1}};
        float[][] v2 = {{1,1,-1},{-1,1,1},{-1,1,-1},{1,1,1},{1,1,1},{1,1,1}};
        float[][] v3 = {{-1,1,-1},{1,1,1},{-1,1,1},{1,1,-1},{-1,1,-1},{-1,1,1}};

        for (int f = 0; f < 6; f++) {
            float nx = faceNormals[f][0], ny = faceNormals[f][1], nz = faceNormals[f][2];
            addTri(buf, cx, cy, cz, hx, hy, hz, v0[f], v1[f], v2[f], r, g, b, a, nx, ny, nz);
            addTri(buf, cx, cy, cz, hx, hy, hz, v0[f], v2[f], v3[f], r, g, b, a, nx, ny, nz);
        }
    }

    private static void addTri(BufferBuilder buf, float cx, float cy, float cz,
                                float hx, float hy, float hz, float[] a, float[] b, float[] c,
                                int r, int g, int bl, int al, float nx, float ny, float nz) {
        buf.addVertex(cx + a[0]*hx, cy + a[1]*hy, cz + a[2]*hz).setColor(r, g, bl, al).setNormal(nx, ny, nz);
        buf.addVertex(cx + b[0]*hx, cy + b[1]*hy, cz + b[2]*hz).setColor(r, g, bl, al).setNormal(nx, ny, nz);
        buf.addVertex(cx + c[0]*hx, cy + c[1]*hy, cz + c[2]*hz).setColor(r, g, bl, al).setNormal(nx, ny, nz);
    }

    /** Build vertex data for a wireframe box (24 vertices, 12 edges). */
    private static void emitWireBox(BufferBuilder buf, float cx, float cy, float cz,
                                     float sx, float sy, float sz, int color) {
        float hx = sx / 2f, hy = sy / 2f, hz = sz / 2f;
        int a = (color >> 24) & 0xFF, r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;

        float[][] edges = {
                {-1,-1,-1, 1,-1,-1}, {-1,-1, 1, 1,-1, 1},
                {-1, 1,-1, 1, 1,-1}, {-1, 1, 1, 1, 1, 1},
                {-1,-1,-1, -1, 1,-1}, { 1,-1,-1, 1, 1,-1},
                {-1,-1, 1, -1, 1, 1}, { 1,-1, 1, 1, 1, 1},
                {-1,-1,-1, -1,-1, 1}, { 1,-1,-1, 1,-1, 1},
                {-1, 1,-1, -1, 1, 1}, { 1, 1,-1, 1, 1, 1},
        };
        for (float[] e : edges) {
            buf.addVertex(cx + e[0]*hx, cy + e[1]*hy, cz + e[2]*hz).setColor(r, g, b, a).setNormal(0, 0, 0);
            buf.addVertex(cx + e[3]*hx, cy + e[4]*hy, cz + e[5]*hz).setColor(r, g, b, a).setNormal(0, 0, 0);
        }
    }

    private BufferBuilder build(PrimitiveTopology topology) {
        return new BufferBuilder(ByteBufferBuilder.exactlySized(4096), topology, DefaultVertexFormat.POSITION_COLOR_NORMAL);
    }

    // ==================== Batched draw API ====================

    /**
     * Batch-draw solid boxes from pre-built vertex data.
     * One upload, one render pass, one draw call for all boxes.
     */
    public void drawBoxBatched(ByteBuffer vertexData, int vertexCount,
                                Matrix4f viewProj, Matrix4f model) {
        if (vertexCount == 0) return;
        drawWithVertices(vertexData, vertexCount, false, viewProj, model);
    }

    /**
     * Batch-draw wireframe boxes from pre-built vertex data.
     */
    public void drawWireBoxBatched(ByteBuffer vertexData, int vertexCount,
                                    Matrix4f viewProj, Matrix4f model) {
        if (vertexCount == 0) return;
        drawWithVertices(vertexData, vertexCount, true, viewProj, model);
    }

    // ==================== Legacy per-object API ====================

    /**
     * Рисует закрашенный 3D-бокс ( параллелепипед ).
     * <p>
     * Draws a solid 3D box (parallelepiped).
     *
     * @param cx,cy,cz центр бокса / box center
     * @param sx,sy,sz размеры бокса / box dimensions
     * @param color    ARGB цвет / ARGB color
     * @param viewProj матрица вида-проекции / view-projection matrix
     * @param model    модельная матрица / model matrix
     */
    public void drawBox(float cx, float cy, float cz, float sx, float sy, float sz,
                        int color, Matrix4f viewProj, Matrix4f model) {
        float hx = sx / 2f, hy = sy / 2f, hz = sz / 2f;
        int a = (color >> 24) & 0xFF, r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        var buf = build(PrimitiveTopology.TRIANGLES);

        /* Front face (Z-) */
        buf.addVertex(cx - hx, cy - hy, cz - hz).setColor(r, g, b, a).setNormal(0, 0, -1);
        buf.addVertex(cx + hx, cy - hy, cz - hz).setColor(r, g, b, a).setNormal(0, 0, -1);
        buf.addVertex(cx + hx, cy + hy, cz - hz).setColor(r, g, b, a).setNormal(0, 0, -1);
        buf.addVertex(cx - hx, cy - hy, cz - hz).setColor(r, g, b, a).setNormal(0, 0, -1);
        buf.addVertex(cx + hx, cy + hy, cz - hz).setColor(r, g, b, a).setNormal(0, 0, -1);
        buf.addVertex(cx - hx, cy + hy, cz - hz).setColor(r, g, b, a).setNormal(0, 0, -1);

        /* Back face (Z+) */
        buf.addVertex(cx + hx, cy - hy, cz + hz).setColor(r, g, b, a).setNormal(0, 0, 1);
        buf.addVertex(cx - hx, cy - hy, cz + hz).setColor(r, g, b, a).setNormal(0, 0, 1);
        buf.addVertex(cx - hx, cy + hy, cz + hz).setColor(r, g, b, a).setNormal(0, 0, 1);
        buf.addVertex(cx + hx, cy - hy, cz + hz).setColor(r, g, b, a).setNormal(0, 0, 1);
        buf.addVertex(cx - hx, cy + hy, cz + hz).setColor(r, g, b, a).setNormal(0, 0, 1);
        buf.addVertex(cx + hx, cy + hy, cz + hz).setColor(r, g, b, a).setNormal(0, 0, 1);

        /* Left face (X-) */
        buf.addVertex(cx - hx, cy - hy, cz + hz).setColor(r, g, b, a).setNormal(-1, 0, 0);
        buf.addVertex(cx - hx, cy - hy, cz - hz).setColor(r, g, b, a).setNormal(-1, 0, 0);
        buf.addVertex(cx - hx, cy + hy, cz - hz).setColor(r, g, b, a).setNormal(-1, 0, 0);
        buf.addVertex(cx - hx, cy - hy, cz + hz).setColor(r, g, b, a).setNormal(-1, 0, 0);
        buf.addVertex(cx - hx, cy + hy, cz - hz).setColor(r, g, b, a).setNormal(-1, 0, 0);
        buf.addVertex(cx - hx, cy + hy, cz + hz).setColor(r, g, b, a).setNormal(-1, 0, 0);

        /* Right face (X+) */
        buf.addVertex(cx + hx, cy - hy, cz - hz).setColor(r, g, b, a).setNormal(1, 0, 0);
        buf.addVertex(cx + hx, cy - hy, cz + hz).setColor(r, g, b, a).setNormal(1, 0, 0);
        buf.addVertex(cx + hx, cy + hy, cz + hz).setColor(r, g, b, a).setNormal(1, 0, 0);
        buf.addVertex(cx + hx, cy - hy, cz - hz).setColor(r, g, b, a).setNormal(1, 0, 0);
        buf.addVertex(cx + hx, cy + hy, cz + hz).setColor(r, g, b, a).setNormal(1, 0, 0);
        buf.addVertex(cx + hx, cy + hy, cz - hz).setColor(r, g, b, a).setNormal(1, 0, 0);

        /* Bottom face (Y-) */
        buf.addVertex(cx - hx, cy - hy, cz + hz).setColor(r, g, b, a).setNormal(0, -1, 0);
        buf.addVertex(cx + hx, cy - hy, cz + hz).setColor(r, g, b, a).setNormal(0, -1, 0);
        buf.addVertex(cx + hx, cy - hy, cz - hz).setColor(r, g, b, a).setNormal(0, -1, 0);
        buf.addVertex(cx - hx, cy - hy, cz + hz).setColor(r, g, b, a).setNormal(0, -1, 0);
        buf.addVertex(cx + hx, cy - hy, cz - hz).setColor(r, g, b, a).setNormal(0, -1, 0);
        buf.addVertex(cx - hx, cy - hy, cz - hz).setColor(r, g, b, a).setNormal(0, -1, 0);

        /* Top face (Y+) */
        buf.addVertex(cx - hx, cy + hy, cz - hz).setColor(r, g, b, a).setNormal(0, 1, 0);
        buf.addVertex(cx + hx, cy + hy, cz - hz).setColor(r, g, b, a).setNormal(0, 1, 0);
        buf.addVertex(cx + hx, cy + hy, cz + hz).setColor(r, g, b, a).setNormal(0, 1, 0);
        buf.addVertex(cx - hx, cy + hy, cz - hz).setColor(r, g, b, a).setNormal(0, 1, 0);
        buf.addVertex(cx + hx, cy + hy, cz + hz).setColor(r, g, b, a).setNormal(0, 1, 0);
        buf.addVertex(cx - hx, cy + hy, cz + hz).setColor(r, g, b, a).setNormal(0, 1, 0);

        var mesh = buf.buildOrThrow();
        drawWithVertices(mesh.vertexBuffer(), 36, false, viewProj, model);
        mesh.close();
    }

    /**
     * Рисует 3D прямоугольник (плоская поверхность) на заданных координатах.
     * <p>
     * Draws a 3D rectangle (flat surface) at the given coordinates.
     * <p>
     * Прямоугольник располагается на плоскости Y по умолчанию (горизонтально).
     * Для вертикального расположения используйте модельную матрицу с поворотом.
     * <p>
     * The rectangle is placed on the Y plane by default (horizontal).
     * For vertical placement, use a model matrix with rotation.
     *
     * @param cx,cy,cz центр прямоугольника / rectangle center
     * @param w,d      ширина (X) и глубина (Z) / width (X) and depth (Z)
     * @param color    ARGB цвет / ARGB color
     * @param viewProj матрица вида-проекции / view-projection matrix
     * @param model    модельная матрица / model matrix
     */
    public void drawRect(float cx, float cy, float cz, float w, float d,
                         int color, Matrix4f viewProj, Matrix4f model) {
        float hw = w / 2f, hd = d / 2f;
        int a = (color >> 24) & 0xFF, r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        var buf = build(PrimitiveTopology.TRIANGLES);

        /* Верхняя грань (Y+) — единственная грань для плоского прямоугольника.
         * Top face (Y+) — the only face for a flat rectangle. */
        buf.addVertex(cx - hw, cy, cz - hd).setColor(r, g, b, a).setNormal(0, 1, 0);
        buf.addVertex(cx + hw, cy, cz - hd).setColor(r, g, b, a).setNormal(0, 1, 0);
        buf.addVertex(cx + hw, cy, cz + hd).setColor(r, g, b, a).setNormal(0, 1, 0);
        buf.addVertex(cx - hw, cy, cz - hd).setColor(r, g, b, a).setNormal(0, 1, 0);
        buf.addVertex(cx + hw, cy, cz + hd).setColor(r, g, b, a).setNormal(0, 1, 0);
        buf.addVertex(cx - hw, cy, cz + hd).setColor(r, g, b, a).setNormal(0, 1, 0);

        /* Нижняя грань (Y-) — для видимости снизу.
         * Bottom face (Y-) — for visibility from below. */
        buf.addVertex(cx - hw, cy, cz + hd).setColor(r, g, b, a).setNormal(0, -1, 0);
        buf.addVertex(cx + hw, cy, cz + hd).setColor(r, g, b, a).setNormal(0, -1, 0);
        buf.addVertex(cx + hw, cy, cz - hd).setColor(r, g, b, a).setNormal(0, -1, 0);
        buf.addVertex(cx - hw, cy, cz + hd).setColor(r, g, b, a).setNormal(0, -1, 0);
        buf.addVertex(cx + hw, cy, cz - hd).setColor(r, g, b, a).setNormal(0, -1, 0);
        buf.addVertex(cx - hw, cy, cz - hd).setColor(r, g, b, a).setNormal(0, -1, 0);

        var mesh = buf.buildOrThrow();
        drawWithVertices(mesh.vertexBuffer(), 12, false, viewProj, model);
        mesh.close();
    }

    /**
     * Рисует 3D прямоугольник с поворотом по yaw и pitch.
     * <p>
     * Draws a 3D rectangle with yaw and pitch rotation.
     *
     * @param cx,cy,cz центр / center
     * @param w,h      ширина и высота / width and height
     * @param yaw      поворот по Y (градусы) / yaw rotation (degrees)
     * @param pitch    поворот по X (градусы) / pitch rotation (degrees)
     * @param color    ARGB цвет / ARGB color
     * @param viewProj матрица вида-проекции / view-projection matrix
     * @param model    модельная матрица / model matrix
     */
    public void drawRectRotated(float cx, float cy, float cz, float w, float h,
                                 float yaw, float pitch,
                                 int color, Matrix4f viewProj, Matrix4f model) {
        float hw = w / 2f, hh = h / 2f;
        int a = (color >> 24) & 0xFF, r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        var buf = build(PrimitiveTopology.TRIANGLES);

        /* Локальные вершины: (-hw, -hh, 0) ... (+hw, +hh, 0) — плоскость XY.
         * Local vertices: (-hw, -hh, 0) ... (+hw, +hh, 0) — XY plane. */
        float cosY = (float) Math.cos(Math.toRadians(yaw));
        float sinY = (float) Math.sin(Math.toRadians(yaw));
        float cosP = (float) Math.cos(Math.toRadians(pitch));
        float sinP = (float) Math.sin(Math.toRadians(pitch));

        float[] px = new float[4], py = new float[4], pz = new float[4];
        float[][] local = {{-hw, -hh}, {hw, -hh}, {hw, hh}, {-hw, hh}};
        for (int i = 0; i < 4; i++) {
            float lx = local[i][0], ly = local[i][1];
            /* Поворот по pitch (вокруг X), затем по yaw (вокруг Y).
             * Rotate by pitch (around X), then by yaw (around Y). */
            float y1 = ly * cosP;
            float z1 = ly * sinP;
            float x1 = lx;
            float x2 = x1 * cosY + z1 * sinY;
            float z2 = -x1 * sinY + z1 * cosY;
            px[i] = cx + x2;
            py[i] = cy + y1;
            pz[i] = cz + z2;
        }

        /* Нормаль перпендикулярна плоскости прямоугольника.
         * Normal perpendicular to the rectangle plane. */
        float nx = sinY * cosP;
        float ny = sinP;
        float nz = cosY * cosP;

        buf.addVertex(px[0], py[0], pz[0]).setColor(r, g, b, a).setNormal(nx, ny, nz);
        buf.addVertex(px[1], py[1], pz[1]).setColor(r, g, b, a).setNormal(nx, ny, nz);
        buf.addVertex(px[2], py[2], pz[2]).setColor(r, g, b, a).setNormal(nx, ny, nz);
        buf.addVertex(px[0], py[0], pz[0]).setColor(r, g, b, a).setNormal(nx, ny, nz);
        buf.addVertex(px[2], py[2], pz[2]).setColor(r, g, b, a).setNormal(nx, ny, nz);
        buf.addVertex(px[3], py[3], pz[3]).setColor(r, g, b, a).setNormal(nx, ny, nz);

        var mesh = buf.buildOrThrow();
        drawWithVertices(mesh.vertexBuffer(), 6, false, viewProj, model);
        mesh.close();
    }

    /**
     * Рисует 3D прямоугольник-каркас (только рёбра).
     * <p>
     * Draws a 3D wireframe rectangle (edges only).
     *
     * @param cx,cy,cz центр / center
     * @param w,d      ширина и глубина / width and depth
     * @param color    ARGB цвет / ARGB color
     * @param viewProj матрица вида-проекции / view-projection matrix
     * @param model    модельная матрица / model matrix
     */
    public void drawWireRect(float cx, float cy, float cz, float w, float d,
                             int color, Matrix4f viewProj, Matrix4f model) {
        float hw = w / 2f, hd = d / 2f;
        int a = (color >> 24) & 0xFF, r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        var buf = build(PrimitiveTopology.LINES);

        /* 4 ребра прямоугольника на Y-плоскости.
         * 4 edges of the rectangle on the Y plane. */
        buf.addVertex(cx - hw, cy, cz - hd).setColor(r, g, b, a).setNormal(0, 0, 0);
        buf.addVertex(cx + hw, cy, cz - hd).setColor(r, g, b, a).setNormal(0, 0, 0);

        buf.addVertex(cx + hw, cy, cz - hd).setColor(r, g, b, a).setNormal(0, 0, 0);
        buf.addVertex(cx + hw, cy, cz + hd).setColor(r, g, b, a).setNormal(0, 0, 0);

        buf.addVertex(cx + hw, cy, cz + hd).setColor(r, g, b, a).setNormal(0, 0, 0);
        buf.addVertex(cx - hw, cy, cz + hd).setColor(r, g, b, a).setNormal(0, 0, 0);

        buf.addVertex(cx - hw, cy, cz + hd).setColor(r, g, b, a).setNormal(0, 0, 0);
        buf.addVertex(cx - hw, cy, cz - hd).setColor(r, g, b, a).setNormal(0, 0, 0);

        var mesh = buf.buildOrThrow();
        drawWithVertices(mesh.vertexBuffer(), 8, true, viewProj, model);
        mesh.close();
    }

    /**
     * Рисует закрашенную сферу.
     * <p>
     * Draws a solid sphere.
     *
     * @param cx,cy,cz  центр сферы / sphere center
     * @param radius    радиус / radius
     * @param color     ARGB цвет / ARGB color
     * @param viewProj  матрица вида-проекции / view-projection matrix
     * @param model     модельная матрица / model matrix
     * @param segments  количество сегментов (больше = детальнее) / segment count (more = more detailed)
     */
    public void drawSphere(float cx, float cy, float cz, float radius, int color,
                           Matrix4f viewProj, Matrix4f model, int segments) {
        int a = (color >> 24) & 0xFF, r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        var buf = build(PrimitiveTopology.TRIANGLES);

        for (int lat = 0; lat < segments; lat++) {
            float theta0 = (float) Math.PI * (-0.5f + (float) lat / segments);
            float theta1 = (float) Math.PI * (-0.5f + (float) (lat + 1) / segments);
            float cosT0 = (float) Math.cos(theta0), sinT0 = (float) Math.sin(theta0);
            float cosT1 = (float) Math.cos(theta1), sinT1 = (float) Math.sin(theta1);

            for (int lon = 0; lon < segments; lon++) {
                float phi0 = (float) (2 * Math.PI) * (float) lon / segments;
                float phi1 = (float) (2 * Math.PI) * (float) (lon + 1) / segments;
                float cosP0 = (float) Math.cos(phi0), sinP0 = (float) Math.sin(phi0);
                float cosP1 = (float) Math.cos(phi1), sinP1 = (float) Math.sin(phi1);

                float x00 = cx + radius * cosT0 * cosP0; float y00 = cy + radius * sinT0; float z00 = cz + radius * cosT0 * sinP0;
                float x01 = cx + radius * cosT0 * cosP1; float y01 = cy + radius * sinT0; float z01 = cz + radius * cosT0 * sinP1;
                float x10 = cx + radius * cosT1 * cosP0; float y10 = cy + radius * sinT1; float z10 = cz + radius * cosT1 * sinP0;
                float x11 = cx + radius * cosT1 * cosP1; float y11 = cy + radius * sinT1; float z11 = cz + radius * cosT1 * sinP1;

                buf.addVertex(x00, y00, z00).setColor(r, g, b, a).setNormal(cosT0 * cosP0, sinT0, cosT0 * sinP0);
                buf.addVertex(x10, y10, z10).setColor(r, g, b, a).setNormal(cosT1 * cosP0, sinT1, cosT1 * sinP0);
                buf.addVertex(x01, y01, z01).setColor(r, g, b, a).setNormal(cosT0 * cosP1, sinT0, cosT0 * sinP1);
                buf.addVertex(x01, y01, z01).setColor(r, g, b, a).setNormal(cosT0 * cosP1, sinT0, cosT0 * sinP1);
                buf.addVertex(x10, y10, z10).setColor(r, g, b, a).setNormal(cosT1 * cosP0, sinT1, cosT1 * sinP0);
                buf.addVertex(x11, y11, z11).setColor(r, g, b, a).setNormal(cosT1 * cosP1, sinT1, cosT1 * sinP1);
            }
        }

        var mesh = buf.buildOrThrow();
        int vertexCount = segments * segments * 6;
        drawWithVertices(mesh.vertexBuffer(), vertexCount, false, viewProj, model);
        mesh.close();
    }

    /**
     * Рисует каркасную сферу (только линии широт/долгот).
     * <p>
     * Draws a wireframe sphere (latitude/longitude lines only).
     */
    public void drawWireSphere(float cx, float cy, float cz, float radius, int color,
                               Matrix4f viewProj, Matrix4f model, int segments) {
        int a = (color >> 24) & 0xFF, r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        var buf = build(PrimitiveTopology.LINES);

        /* Линии широт / Latitude lines. */
        for (int lat = 0; lat < segments; lat++) {
            float theta = (float) Math.PI * (-0.5f + (float) lat / segments);
            float cosT = (float) Math.cos(theta), sinT = (float) Math.sin(theta);
            for (int lon = 0; lon < segments; lon++) {
                float phi0 = (float) (2 * Math.PI) * lon / segments;
                float phi1 = (float) (2 * Math.PI) * (lon + 1) / segments;
                float x0 = cx + radius * cosT * (float) Math.cos(phi0);
                float y0 = cy + radius * sinT;
                float z0 = cz + radius * cosT * (float) Math.sin(phi0);
                float x1 = cx + radius * cosT * (float) Math.cos(phi1);
                float y1 = cy + radius * sinT;
                float z1 = cz + radius * cosT * (float) Math.sin(phi1);
                buf.addVertex(x0, y0, z0).setColor(r, g, b, a).setNormal(0, 0, 0);
                buf.addVertex(x1, y1, z1).setColor(r, g, b, a).setNormal(0, 0, 0);
            }
        }

        /* Линии долгот / Longitude lines. */
        for (int lon = 0; lon < segments; lon++) {
            float phi = (float) (2 * Math.PI) * lon / segments;
            float cosP = (float) Math.cos(phi), sinP = (float) Math.sin(phi);
            for (int lat = 0; lat < segments; lat++) {
                float theta0 = (float) Math.PI * (-0.5f + (float) lat / segments);
                float theta1 = (float) Math.PI * (-0.5f + (float) (lat + 1) / segments);
                float x0 = cx + radius * (float) Math.cos(theta0) * cosP;
                float y0 = cy + radius * (float) Math.sin(theta0);
                float z0 = cz + radius * (float) Math.cos(theta0) * sinP;
                float x1 = cx + radius * (float) Math.cos(theta1) * cosP;
                float y1 = cy + radius * (float) Math.sin(theta1);
                float z1 = cz + radius * (float) Math.cos(theta1) * sinP;
                buf.addVertex(x0, y0, z0).setColor(r, g, b, a).setNormal(0, 0, 0);
                buf.addVertex(x1, y1, z1).setColor(r, g, b, a).setNormal(0, 0, 0);
            }
        }

        var mesh = buf.buildOrThrow();
        drawWithVertices(mesh.vertexBuffer(), segments * segments * 4, true, viewProj, model);
        mesh.close();
    }

    /**
     * Рисует цилиндр.
     * <p>
     * Draws a cylinder.
     */
    public void drawCylinder(float cx, float cz, float y0, float y1, float radius, int color,
                             Matrix4f viewProj, Matrix4f model, int segments) {
        int a = (color >> 24) & 0xFF, r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        var buf = build(PrimitiveTopology.TRIANGLES);

        for (int i = 0; i < segments; i++) {
            float a0 = (float) (2 * Math.PI) * i / segments;
            float a1 = (float) (2 * Math.PI) * (i + 1) / segments;
            float cosA0 = (float) Math.cos(a0), sinA0 = (float) Math.sin(a0);
            float cosA1 = (float) Math.cos(a1), sinA1 = (float) Math.sin(a1);
            float nx = cosA0, nz = sinA0;

            buf.addVertex(cx + radius * cosA0, y0, cz + radius * sinA0).setColor(r, g, b, a).setNormal(nx, 0, nz);
            buf.addVertex(cx + radius * cosA1, y0, cz + radius * sinA1).setColor(r, g, b, a).setNormal(nx, 0, nz);
            buf.addVertex(cx + radius * cosA1, y1, cz + radius * sinA1).setColor(r, g, b, a).setNormal(nx, 0, nz);
            buf.addVertex(cx + radius * cosA0, y0, cz + radius * sinA0).setColor(r, g, b, a).setNormal(nx, 0, nz);
            buf.addVertex(cx + radius * cosA1, y1, cz + radius * sinA1).setColor(r, g, b, a).setNormal(nx, 0, nz);
            buf.addVertex(cx + radius * cosA0, y1, cz + radius * sinA0).setColor(r, g, b, a).setNormal(nx, 0, nz);
        }

        var mesh = buf.buildOrThrow();
        drawWithVertices(mesh.vertexBuffer(), segments * 6, false, viewProj, model);
        mesh.close();
    }

    /**
     * Рисует 3D-линию между двумя точками.
     * <p>
     * Draws a 3D line between two points.
     */
    public void drawLine3D(float x1, float y1, float z1, float x2, float y2, float z2,
                           int color, float width, Matrix4f viewProj, Matrix4f model) {
        int a = (color >> 24) & 0xFF, r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.001f) return;
        float nx = dx / len, ny = dy / len, nz = dz / len;

        var buf = build(PrimitiveTopology.LINES);
        buf.addVertex(x1, y1, z1).setColor(r, g, b, a).setNormal(nx, ny, nz);
        buf.addVertex(x2, y2, z2).setColor(r, g, b, a).setNormal(nx, ny, nz);
        var mesh = buf.buildOrThrow();
        drawWithVertices(mesh.vertexBuffer(), 2, true, viewProj, model);
        mesh.close();
    }

    /**
     * Рисует каркасный бокс (только рёбра).
     * <p>
     * Draws a wireframe box (edges only).
     */
    public void drawWireBox(float cx, float cy, float cz, float sx, float sy, float sz,
                            int color, Matrix4f viewProj, Matrix4f model) {
        float hx = sx / 2f, hy = sy / 2f, hz = sz / 2f;
        int a = (color >> 24) & 0xFF, r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        var buf = build(PrimitiveTopology.LINES);

        /* Передняя грань / Front face */
        buf.addVertex(cx - hx, cy - hy, cz - hz).setColor(r, g, b, a).setNormal(0, 0, 0);
        buf.addVertex(cx + hx, cy - hy, cz - hz).setColor(r, g, b, a).setNormal(0, 0, 0);
        buf.addVertex(cx + hx, cy - hy, cz - hz).setColor(r, g, b, a).setNormal(0, 0, 0);
        buf.addVertex(cx + hx, cy + hy, cz - hz).setColor(r, g, b, a).setNormal(0, 0, 0);
        buf.addVertex(cx + hx, cy + hy, cz - hz).setColor(r, g, b, a).setNormal(0, 0, 0);
        buf.addVertex(cx - hx, cy + hy, cz - hz).setColor(r, g, b, a).setNormal(0, 0, 0);
        buf.addVertex(cx - hx, cy + hy, cz - hz).setColor(r, g, b, a).setNormal(0, 0, 0);
        buf.addVertex(cx - hx, cy - hy, cz - hz).setColor(r, g, b, a).setNormal(0, 0, 0);

        /* Задняя грань / Back face */
        buf.addVertex(cx - hx, cy - hy, cz + hz).setColor(r, g, b, a).setNormal(0, 0, 0);
        buf.addVertex(cx + hx, cy - hy, cz + hz).setColor(r, g, b, a).setNormal(0, 0, 0);
        buf.addVertex(cx + hx, cy - hy, cz + hz).setColor(r, g, b, a).setNormal(0, 0, 0);
        buf.addVertex(cx + hx, cy + hy, cz + hz).setColor(r, g, b, a).setNormal(0, 0, 0);
        buf.addVertex(cx + hx, cy + hy, cz + hz).setColor(r, g, b, a).setNormal(0, 0, 0);
        buf.addVertex(cx - hx, cy + hy, cz + hz).setColor(r, g, b, a).setNormal(0, 0, 0);
        buf.addVertex(cx - hx, cy + hy, cz + hz).setColor(r, g, b, a).setNormal(0, 0, 0);
        buf.addVertex(cx - hx, cy - hy, cz + hz).setColor(r, g, b, a).setNormal(0, 0, 0);

        /* Соединяющие рёбра / Connecting edges */
        buf.addVertex(cx - hx, cy - hy, cz - hz).setColor(r, g, b, a).setNormal(0, 0, 0);
        buf.addVertex(cx - hx, cy - hy, cz + hz).setColor(r, g, b, a).setNormal(0, 0, 0);
        buf.addVertex(cx + hx, cy - hy, cz - hz).setColor(r, g, b, a).setNormal(0, 0, 0);
        buf.addVertex(cx + hx, cy - hy, cz + hz).setColor(r, g, b, a).setNormal(0, 0, 0);
        buf.addVertex(cx - hx, cy + hy, cz - hz).setColor(r, g, b, a).setNormal(0, 0, 0);
        buf.addVertex(cx - hx, cy + hy, cz + hz).setColor(r, g, b, a).setNormal(0, 0, 0);
        buf.addVertex(cx + hx, cy + hy, cz - hz).setColor(r, g, b, a).setNormal(0, 0, 0);
        buf.addVertex(cx + hx, cy + hy, cz + hz).setColor(r, g, b, a).setNormal(0, 0, 0);

        var mesh = buf.buildOrThrow();
        drawWithVertices(mesh.vertexBuffer(), 24, true, viewProj, model);
        mesh.close();
    }

    /**
     * Рисует горизонтальную сетку.
     * <p>
     * Draws a horizontal grid.
     */
    public void drawGrid(float cx, float cz, float size, int divisions, int color,
                         Matrix4f viewProj, Matrix4f model, float y) {
        int a = (color >> 24) & 0xFF, r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        var buf = build(PrimitiveTopology.LINES);
        float half = size / 2f;

        for (int i = 0; i <= divisions; i++) {
            float t = -half + size * (float) i / divisions;
            buf.addVertex(cx + t, y, cz - half).setColor(r, g, b, a).setNormal(0, 1, 0);
            buf.addVertex(cx + t, y, cz + half).setColor(r, g, b, a).setNormal(0, 1, 0);
            buf.addVertex(cx - half, y, cz + t).setColor(r, g, b, a).setNormal(0, 1, 0);
            buf.addVertex(cx + half, y, cz + t).setColor(r, g, b, a).setNormal(0, 1, 0);
        }

        var mesh = buf.buildOrThrow();
        drawWithVertices(mesh.vertexBuffer(), (divisions + 1) * 4, true, viewProj, model);
        mesh.close();
    }

    /**
     * Рисует оси координат (X=красная, Z=синяя, Y=зелёная).
     * <p>
     * Draws coordinate axes (X=red, Z=blue, Y=green).
     */
    public void drawAxes(float length, Matrix4f viewProj, Matrix4f model) {
        var buf = build(PrimitiveTopology.LINES);
        buf.addVertex(0, 0, 0).setColor(255, 0, 0, 255).setNormal(0, 0, 0);
        buf.addVertex(length, 0, 0).setColor(255, 0, 0, 255).setNormal(0, 0, 0);
        buf.addVertex(0, 0, 0).setColor(0, 255, 0, 255).setNormal(0, 0, 0);
        buf.addVertex(0, length, 0).setColor(0, 255, 0, 255).setNormal(0, 0, 0);
        buf.addVertex(0, 0, 0).setColor(0, 0, 255, 255).setNormal(0, 0, 0);
        buf.addVertex(0, 0, length).setColor(0, 0, 255, 255).setNormal(0, 0, 0);
        var mesh = buf.buildOrThrow();
        drawWithVertices(mesh.vertexBuffer(), 6, true, viewProj, model);
        mesh.close();
    }

    // ==================== drawNear ====================

    /**
     * Рисует все объекты (сущности) вокруг игрока в заданной зоне.
     * <p>
     * Draws all entities around the player within the specified zone.
     * <p>
     * Поддерживаемые типы зон / Supported zone shapes:
     * <ul>
     *   <li>{@link ZoneShape#SPHERE} — сфера радиуса {@code range} / sphere of radius {@code range}</li>
     *   <li>{@link ZoneShape#BOX} — куб {@code range x range x range} / cube {@code range x range x range}</li>
     *   <li>{@link ZoneShape#CYLINDER} — цилиндр радиуса {@code range}, высота {@code height}
     *       / cylinder of radius {@code range}, height {@code height}</li>
     * </ul>
     *
     * @param range    радиус/размер зоны / zone radius/size
     * @param shape    тип зоны / zone shape type
     * @param color    ARGB цвет рамок / ARGB color for outlines
     * @param viewProj матрица вида-проекции / view-projection matrix
     * @param model    модельная матрица / model matrix
     * @param height   высота цилиндра (только для CYLINDER) / cylinder height (CYLINDER only)
     * @param segments количество сегментов для сферы/цилиндра / segment count for sphere/cylinder
     */
    public void drawNear(float range, ZoneShape shape, int color,
                         Matrix4f viewProj, Matrix4f model,
                         float height, int segments) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Vec3 playerPos = mc.player.position();

        /** Собираем все сущности в зоне / Collect all entities in zone. */
        List<Entity> nearby = new ArrayList<>();
        AABB searchBox = new AABB(
            playerPos.x - range, playerPos.y - range, playerPos.z - range,
            playerPos.x + range, playerPos.y + range, playerPos.z + range
        );
        for (Entity entity : mc.level.getEntitiesOfClass(Entity.class, searchBox, e -> e != mc.player)) {
            nearby.add(entity);
        }

        /** Рисуем зону (рамку) / Draw zone (outline). */
        switch (shape) {
            case SPHERE -> drawWireSphere((float) playerPos.x, (float) playerPos.y, (float) playerPos.z,
                    range, color, viewProj, model, segments);
            case BOX -> drawWireBox((float) playerPos.x, (float) playerPos.y, (float) playerPos.z,
                    range * 2, range * 2, range * 2, color, viewProj, model);
            case CYLINDER -> drawCylinder((float) playerPos.x, (float) playerPos.z,
                    (float) playerPos.y - height / 2f, (float) playerPos.y + height / 2f,
                    range, color, viewProj, model, segments);
        }

        /** Рисуем框 Around каждую сущность / Draw box around each entity. */
        int entityColor = 0x80FFFFFF & color | 0x40000000; /* Полупрозрачный / Semi-transparent */
        for (Entity entity : nearby) {
            if (entity instanceof Player) {
                /* Игроки — жёлтая рамка / Players — yellow box. */
                drawWireBox((float) entity.getX(), (float) entity.getY() + (float) entity.getBbHeight() / 2f,
                        (float) entity.getZ(),
                        (float) entity.getBbWidth() * 1.2f, (float) entity.getBbHeight() * 1.1f,
                        (float) entity.getBbWidth() * 1.2f,
                        0xFFFFDD00, viewProj, model);
            } else {
                /* Мобы — красноватая рамка / Mobs — reddish box. */
                drawWireBox((float) entity.getX(), (float) entity.getY() + (float) entity.getBbHeight() / 2f,
                        (float) entity.getZ(),
                        (float) entity.getBbWidth() * 1.2f, (float) entity.getBbHeight() * 1.1f,
                        (float) entity.getBbWidth() * 1.2f,
                        0x80FF4444, viewProj, model);
            }
        }
    }

    /**
     * Упрощённый вызов drawNear — сфера 10 блоков вокруг игрока.
     * <p>
     * Simplified drawNear call — 10-block sphere around the player.
     */
    public void drawNear(float range, int color, Matrix4f viewProj, Matrix4f model) {
        drawNear(range, ZoneShape.SPHERE, color, viewProj, model, range * 2, 16);
    }

    // ==================== drawBlurredRect ====================

    /**
     * Рисует размытый прямоугольник, беря текущий кадр как текстуру.
     * <p>
     * Draws a blurred rectangle using the current frame as texture.
     * <p>
     * Берёт текущий фрейм буфера экрана, применяет размытие и рисует
     * прямоугольник с этим содержимым. Аналог drawRectBlurred из AporiaRenderer,
     * но работает в 3D пространстве.
     * <p>
     * Takes the current screen frame buffer, applies blur and draws
     * a rectangle with that content. Similar to drawRectBlurred from AporiaRenderer,
     * but works in 3D space.
     *
     * @param cx,cy,cz  центр прямоугольника / rectangle center
     * @param w,h       ширина и высота / width and height
     * @param color     ARGB цвет тинта / ARGB tint color
     * @param viewProj  матрица вида-проекции / view-projection matrix
     * @param model     модельная матрица / model matrix
     */
    public void drawBlurredRect(float cx, float cy, float cz, float w, float h,
                                int color, Matrix4f viewProj, Matrix4f model) {
        Minecraft mc = Minecraft.getInstance();
        var mainTarget = mc.gameRenderer.mainRenderTarget();
        var colorView = mainTarget.getColorTextureView();
        var depthView = mainTarget.getDepthTextureView();
        if (colorView == null) return;

        float hw = w / 2f, hh = h / 2f;
        int a = (color >> 24) & 0xFF, r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        float ta = a / 255f, tr = r / 255f, tg = g / 255f, tb = b / 255f;

        var bb = ByteBufferBuilder.exactlySized(4096);
        var buf = new BufferBuilder(bb, PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR);
        /* Плоский quad на Y-плоскости с UV координатами экрана.
         * Flat quad on Y plane with screen UV coordinates. */
        buf.addVertex(cx - hw, cy, cz - hh).setUv(0f, 0f).setColor(tr, tg, tb, ta);
        buf.addVertex(cx + hw, cy, cz - hh).setUv(1f, 0f).setColor(tr, tg, tb, ta);
        buf.addVertex(cx + hw, cy, cz + hh).setUv(1f, 1f).setColor(tr, tg, tb, ta);
        buf.addVertex(cx - hw, cy, cz - hh).setUv(0f, 0f).setColor(tr, tg, tb, ta);
        buf.addVertex(cx + hw, cy, cz + hh).setUv(1f, 1f).setColor(tr, tg, tb, ta);
        buf.addVertex(cx - hw, cy, cz + hh).setUv(0f, 1f).setColor(tr, tg, tb, ta);

        var mesh = buf.buildOrThrow();
        uploadMvp(viewProj, model);
        mesh.vertexBuffer().flip();

        var device = RenderSystem.getDevice();
        var encoder = device.createCommandEncoder();
        encoder.writeToBuffer(mvpUbo.slice(), cachedMvpBB);
        encoder.writeToBuffer(cachedVbo.slice(), mesh.vertexBuffer());
        mesh.close();

        var sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);

        try (var pass = encoder.createRenderPass(() -> "aporia:blurred_rect_3d",
                colorView, Optional.<Vector4fc>empty(), depthView, OptionalDouble.empty())) {
            pass.setPipeline(blurredPipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("ModelViewProj", mvpUbo.slice());
            pass.bindTexture("ScreenTexture", colorView, sampler);
            pass.setVertexBuffer(0, cachedVbo.slice());
            pass.draw(0, 6, 0, 0);
        }
    }

    // ==================== Matrix helpers ====================

    /**
     * Создаёт матрицу перспективной проекции.
     * <p>
     * Creates a perspective projection matrix.
     */
    public Matrix4f createPerspective(float fovDeg, float aspect, float zNear, float zFar) {
        return new Matrix4f().perspective(fovDeg * (float) Math.PI / 180f, aspect, zNear, zFar);
    }

    /**
     * Создаёт матрицу вида из позиции и углов поворота.
     * <p>
     * Creates a view matrix from position and rotation angles.
     */
    public Matrix4f createViewMatrix(Vec3 pos, float yaw, float pitch) {
        return new Matrix4f().rotationX(pitch * (float) Math.PI / 180f)
                .rotateY(yaw * (float) Math.PI / 180f)
                .translate(-(float) pos.x, -(float) pos.y, -(float) pos.z);
    }

    /**
     * Создаёт модельную матрицу из трансформаций.
     * <p>
     * Creates a model matrix from transforms.
     */
    public Matrix4f createModelMatrix(float tx, float ty, float tz, float rx, float ry, float rz, float s) {
        return new Matrix4f().translation(tx, ty, tz)
                .rotateZ(rz * (float) Math.PI / 180f)
                .rotateY(ry * (float) Math.PI / 180f)
                .rotateX(rx * (float) Math.PI / 180f)
                .scale(s);
    }

    /**
     * Освобождает GPU-ресурсы.
     * <p>
     * Releases GPU resources.
     */
    public void cleanup() {
        if (mvpUbo != null) { mvpUbo.close(); mvpUbo = null; }
        if (drawParamsUbo != null) { drawParamsUbo.close(); drawParamsUbo = null; }
        if (cachedVbo != null) { cachedVbo.close(); cachedVbo = null; }
        initialized = false;
    }
}
