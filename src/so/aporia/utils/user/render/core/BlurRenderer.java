package so.aporia.utils.user.render.core;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.OptionalInt;

public final class BlurRenderer {
    public static RenderPipeline kawaseDownPipeline;
    public static RenderPipeline kawaseUpPipeline;
    public static RenderPipeline blitPipeline;

    public static TextureTarget[] kawaseDownTargets;
    public static TextureTarget blurTarget;
    public static TextureTarget guiBlurTarget;
    public static boolean guiBlurReady = false;
    public static boolean guiBlurTargetsDirty = true;

    public static TextureTarget prePlayerBlurTarget;
    public static boolean prePlayerBlurReady = false;

    public static int blurTargetW = -1, blurTargetH = -1;
    public static boolean blurReady = false;
    public static boolean blurTargetsDirty = true;

    public static GpuBuffer blurQuadVbo;
    public static GpuBuffer blurUbo;

    public static float cachedBlurStrength = -1f;
    public static float cachedBlurSaturation = -1f;

    /** Глобальный флаг: брать блюр из guiBlurTarget (захват 3D+2D) вместо blurTarget (только мир). */
    public static boolean useGuiBlur = false;

    private static ByteBuffer cachedBlurBB;
    private static int blurFrameCounter = 0;
    private static final int BLUR_UPDATE_INTERVAL = 3;

    public static void init(GpuDevice device) {
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

        var tess = Tesselator.getInstance();
        var buf = tess.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX);

        buf.addVertex(-1f,  1f, 0f).setUv(0f, 0f);
        buf.addVertex(-1f, -1f, 0f).setUv(0f, 1f);
        buf.addVertex( 1f, -1f, 0f).setUv(1f, 1f);

        buf.addVertex(-1f,  1f, 0f).setUv(0f, 0f);
        buf.addVertex( 1f, -1f, 0f).setUv(1f, 1f);
        buf.addVertex( 1f,  1f, 0f).setUv(1f, 0f);

        var quadMesh = buf.buildOrThrow();
        blurQuadVbo = device.createBuffer(() -> "aporia:blur_quad",
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, quadMesh.vertexBuffer());
        quadMesh.close();

        blurUbo = device.createBuffer(() -> "aporia:blur_ubo",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, 32L);

        blitPipeline = RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/blit"))
                .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/blit"))
                .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/blit"))
                .withSampler("InputTexture")
                .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.TRIANGLES)
                .withBlend(new BlendFunction(SourceFactor.ONE, DestFactor.ZERO, SourceFactor.ONE, DestFactor.ZERO))
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withCull(false)
                .build();
    }

    public static void ensureBlurTarget(Minecraft mc) {
        var mainTarget = mc.getMainRenderTarget();
        int mainW = mainTarget.width;
        int mainH = mainTarget.height;

        if (kawaseDownTargets != null && kawaseDownTargets.length > 0 && kawaseDownTargets[0] != null) {
            if (blurTargetW == mainW && blurTargetH == mainH) {
                return;
            }
        }

        if (kawaseDownTargets == null || kawaseDownTargets.length != 5) {
            kawaseDownTargets = new TextureTarget[5];
        }

        for (int i = 0; i < kawaseDownTargets.length; i++) {
            kawaseDownTargets[i] = null;
        }
        blurTarget = null;

        blurTargetW = mainW;
        blurTargetH = mainH;
        blurTargetsDirty = true;

        blurTarget = new TextureTarget("aporia_blur_final", mainW, mainH, false);

        int currentW = mainW;
        int currentH = mainH;

        for (int i = 0; i < kawaseDownTargets.length; i++) {
            currentW = Math.max(1, currentW / 2);
            currentH = Math.max(1, currentH / 2);

            kawaseDownTargets[i] = new TextureTarget("aporia_blur_down_" + i, currentW, currentH, false);
        }
    }

    public static void ensureGuiBlurTarget(Minecraft mc) {
        ensureBlurTarget(mc);
        var mainTarget = mc.getMainRenderTarget();
        int mainW = mainTarget.width;
        int mainH = mainTarget.height;

        if (guiBlurTarget != null) {
            if (blurTargetW == mainW && blurTargetH == mainH) return;
        }

        guiBlurTarget = null;
        guiBlurTarget = new TextureTarget("aporia_gui_blur", mainW, mainH, false);
        guiBlurTargetsDirty = true;
    }

    public static void ensurePrePlayerBlurTarget(Minecraft mc) {
        var mainTarget = mc.getMainRenderTarget();
        int mainW = mainTarget.width;
        int mainH = mainTarget.height;

        if (prePlayerBlurTarget != null && prePlayerBlurTarget.width == mainW && prePlayerBlurTarget.height == mainH) {
            return;
        }

        prePlayerBlurTarget = null;
        prePlayerBlurTarget = new TextureTarget("aporia_pre_player_blur", mainW, mainH, false);
        prePlayerBlurReady = false;
    }

    public static void prepareGuiBlur(Minecraft mc, float strength, float saturation) {
        ensureGuiBlurTarget(mc);
        if (guiBlurTargetsDirty) {
            guiBlurTargetsDirty = false;
            guiBlurReady = false;
            return;
        }

        var mainTarget = mc.getMainRenderTarget();
        var device = RenderSystem.getDevice();

        if (cachedBlurBB == null) {
            cachedBlurBB = ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder());
        }

        var encoder = device.createCommandEncoder();
        int maxSteps = Math.max(1, Math.min(5, Math.round(strength / 6.0f)));
        RenderTarget currentSrc = mainTarget;

        for (int i = maxSteps - 1; i >= 0; i--) {
            TextureTarget currentDst = (i == 0) ? guiBlurTarget : kawaseDownTargets[i - 1];
            float offset = 0.5f + (float)(maxSteps - 1 - i) * 0.25f;

            cachedBlurBB.clear();
            cachedBlurBB.putFloat((float) currentDst.width);
            cachedBlurBB.putFloat((float) currentDst.height);
            cachedBlurBB.putFloat(offset);
            cachedBlurBB.putFloat(0f);
            cachedBlurBB.putFloat(0f); cachedBlurBB.putFloat(0f); cachedBlurBB.putFloat(0f); cachedBlurBB.putFloat(0f);
            cachedBlurBB.flip();
            encoder.writeToBuffer(blurUbo.slice(), cachedBlurBB);

            final int stepIdx = i;
            try (var pass = encoder.createRenderPass(() -> "aporia:gui_kawase_down_" + stepIdx,
                    currentDst.getColorTextureView(), OptionalInt.of(0))) {
                pass.setPipeline(kawaseUpPipeline);
                pass.bindTexture("InputTexture", currentSrc.getColorTextureView(),
                        RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
                pass.setUniform("KawaseData", blurUbo.slice());
                pass.setVertexBuffer(0, blurQuadVbo);
                pass.draw(0, 6);
            }
            currentSrc = currentDst;
        }

        for (int i = maxSteps - 1; i >= 0; i--) {
            TextureTarget currentDst = (i == 0) ? guiBlurTarget : kawaseDownTargets[i - 1];
            float offset = 0.5f;

            cachedBlurBB.clear();
            cachedBlurBB.putFloat((float) currentDst.width);
            cachedBlurBB.putFloat((float) currentDst.height);
            cachedBlurBB.putFloat(offset);
            cachedBlurBB.putFloat(0f);
            cachedBlurBB.putFloat(0f); cachedBlurBB.putFloat(0f); cachedBlurBB.putFloat(0f); cachedBlurBB.putFloat(0f);
            cachedBlurBB.flip();
            encoder.writeToBuffer(blurUbo.slice(), cachedBlurBB);

            final int stepIdx = i;
            try (var pass = encoder.createRenderPass(() -> "aporia:gui_kawase_up_" + stepIdx,
                    currentDst.getColorTextureView(), OptionalInt.of(0))) {
                pass.setPipeline(kawaseUpPipeline);
                pass.bindTexture("InputTexture", currentSrc.getColorTextureView(),
                        RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
                pass.setUniform("KawaseData", blurUbo.slice());
                pass.setVertexBuffer(0, blurQuadVbo);
                pass.draw(0, 6);
            }
            currentSrc = currentDst;
        }

        guiBlurReady = true;
    }

    public static void prepareBlur(Minecraft mc, float strength, float saturation) {
        ensureBlurTarget(mc);

        if (blurTargetsDirty) {
            blurTargetsDirty = false;
            blurReady = false;
            return;
        }

        var mainTarget = mc.getMainRenderTarget();
        var device = RenderSystem.getDevice();

        if (cachedBlurBB == null) {
            cachedBlurBB = ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder());
        }

        var encoder = device.createCommandEncoder();
        int maxSteps = Math.max(1, Math.min(5, Math.round(strength / 6.0f)));
        RenderTarget currentSrc = mainTarget;

        for (int i = maxSteps - 1; i >= 0; i--) {
            TextureTarget currentDst = (i == 0) ? blurTarget : kawaseDownTargets[i - 1];

            float offset = 0.5f + (float)(maxSteps - 1 - i) * 0.25f;

            cachedBlurBB.clear();
            cachedBlurBB.putFloat((float) currentDst.width);
            cachedBlurBB.putFloat((float) currentDst.height);
            cachedBlurBB.putFloat(offset);
            cachedBlurBB.putFloat(0f);
            cachedBlurBB.putFloat(0f); cachedBlurBB.putFloat(0f); cachedBlurBB.putFloat(0f); cachedBlurBB.putFloat(0f);
            cachedBlurBB.flip();
            encoder.writeToBuffer(blurUbo.slice(), cachedBlurBB);

            final int stepIdx = i;
            try (var pass = encoder.createRenderPass(() -> "aporia:kawase_up_" + stepIdx,
                    currentDst.getColorTextureView(), OptionalInt.of(0))) {
                pass.setPipeline(kawaseUpPipeline);
                pass.bindTexture("InputTexture", currentSrc.getColorTextureView(),
                        RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
                pass.setUniform("KawaseData", blurUbo.slice());
                pass.setVertexBuffer(0, blurQuadVbo);
                pass.draw(0, 6);
            }
            currentSrc = currentDst;
        }

        for (int i = maxSteps - 1; i >= 0; i--) {
            TextureTarget currentDst = (i == 0) ? blurTarget : kawaseDownTargets[i - 1];

            float offset = 0.5f;

            cachedBlurBB.clear();
            cachedBlurBB.putFloat((float) currentDst.width);
            cachedBlurBB.putFloat((float) currentDst.height);
            cachedBlurBB.putFloat(offset);
            cachedBlurBB.putFloat(0f);
            cachedBlurBB.putFloat(0f); cachedBlurBB.putFloat(0f); cachedBlurBB.putFloat(0f); cachedBlurBB.putFloat(0f);
            cachedBlurBB.flip();
            encoder.writeToBuffer(blurUbo.slice(), cachedBlurBB);

            final int stepIdx = i;
            try (var pass = encoder.createRenderPass(() -> "aporia:kawase_up_" + stepIdx,
                    currentDst.getColorTextureView(), OptionalInt.of(0))) {
                pass.setPipeline(kawaseUpPipeline);
                pass.bindTexture("InputTexture", currentSrc.getColorTextureView(),
                        RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
                pass.setUniform("KawaseData", blurUbo.slice());
                pass.setVertexBuffer(0, blurQuadVbo);
                pass.draw(0, 6);
            }
            currentSrc = currentDst;
        }

        blurReady = true;
    }

    public static void prepareBlurForPlayer(Minecraft mc, float strength, float saturation) {
        ensureBlurTarget(mc);
        ensurePrePlayerBlurTarget(mc);

        if (blurTargetsDirty) {
            blurTargetsDirty = false;
            prePlayerBlurReady = false;
            return;
        }

        var mainTarget = mc.getMainRenderTarget();
        var device = RenderSystem.getDevice();
        var encoder = device.createCommandEncoder();

        try (var pass = encoder.createRenderPass(() -> "aporia:player_blit",
                prePlayerBlurTarget.getColorTextureView(), OptionalInt.empty())) {
            pass.setPipeline(blitPipeline);
            pass.bindTexture("InputTexture", mainTarget.getColorTextureView(),
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            pass.setVertexBuffer(0, blurQuadVbo);
            pass.draw(0, 6);
        }

        prePlayerBlurReady = true;
    }

    public static void prepareFrameBlur(Minecraft mc, float strength, float saturation) {
        prepareBlur(mc, strength, saturation);

        cachedBlurStrength = strength;
        cachedBlurSaturation = saturation;
    }

    public static void cleanup() {
        blurTargetW = -1; blurTargetH = -1; blurReady = false; prePlayerBlurReady = false;
    }

    private BlurRenderer() {}
}
