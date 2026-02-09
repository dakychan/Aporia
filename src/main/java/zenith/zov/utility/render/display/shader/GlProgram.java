package zenith.zov.utility.render.display.shader;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.DynamicUniforms;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.ScissorState;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.joml.Vector4f;
import zenith.zov.utility.interfaces.IMinecraft;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public class GlProgram implements IMinecraft {

    private static final List<Runnable> REGISTERED_PROGRAMS = new ArrayList<>();
    private static final ThreadLocal<GlProgram> ACTIVE_PROGRAM = new ThreadLocal<>();
    private static final Vector4f COLOR_MODULATOR = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);

    private final Identifier id;
    private final VertexFormat vertexFormat;
    private RenderPipeline pipeline;
    private List<String> samplerNames = List.of();
    private ZenithUniforms uniforms = new ZenithUniforms();
    private boolean dirty = true;
    private GpuBuffer uniformBuffer;
    private GpuBufferSlice uniformSlice;
    private ByteBuffer uniformData;

    public GlProgram(Identifier id, VertexFormat vertexFormat) {
        this.id = id;
        this.vertexFormat = vertexFormat;
        REGISTERED_PROGRAMS.add(this::setup);
    }

    public void use() {
        if (pipeline == null) {
            setup();
        }
        ACTIVE_PROGRAM.set(this);
    }

    public GlUniform findUniform(String name) {
        if (pipeline == null) {
            return new GlUniform(this, name);
        }
        return new GlUniform(this, name);
    }

    public static GlProgram getActive() {
        return ACTIVE_PROGRAM.get();
    }

    public static void clearActive() {
        ACTIVE_PROGRAM.remove();
    }

    protected void setup() {
        try {
            loadPipeline(mc.getResourceManager());
        } catch (IOException ignored) {
            return;
        }
        uniforms = new ZenithUniforms();
        dirty = true;
        if (uniformBuffer != null && !uniformBuffer.isClosed()) {
            uniformBuffer.close();
        }
        uniformBuffer = RenderSystem.getDevice().createBuffer(
                () -> "zenith_uniforms",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                ZenithUniforms.BUFFER_SIZE
        );
        uniformSlice = uniformBuffer.slice();
        uniformData = ByteBuffer.allocateDirect(ZenithUniforms.BUFFER_SIZE).order(ByteOrder.nativeOrder());
    }

    public void draw(RenderLayer layer, BuiltBuffer builtBuffer) {
        if (pipeline == null) {
            layer.draw(builtBuffer);
            return;
        }
        layer.startDrawing();
        try {
            DynamicUniforms dynamicUniforms = RenderSystem.getDynamicUniforms();
            GpuBufferSlice dynamicSlice = dynamicUniforms.write(
                    RenderSystem.getModelViewMatrix(),
                    COLOR_MODULATOR,
                    RenderSystem.getModelOffset(),
                    RenderSystem.getTextureMatrix(),
                    RenderSystem.getShaderLineWidth()
            );

            GpuBuffer vertexBuffer = vertexFormat.uploadImmediateVertexBuffer(builtBuffer.getBuffer());
            GpuBuffer indexBuffer;
            VertexFormat.IndexType indexType;
            if (builtBuffer.getSortedBuffer() == null) {
                RenderSystem.ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(builtBuffer.getDrawParameters().mode());
                indexBuffer = shapeIndexBuffer.getIndexBuffer(builtBuffer.getDrawParameters().indexCount());
                indexType = shapeIndexBuffer.getIndexType();
            } else {
                indexBuffer = vertexFormat.uploadImmediateIndexBuffer(builtBuffer.getSortedBuffer());
                indexType = builtBuffer.getDrawParameters().indexType();
            }

            Framebuffer target = mc.getFramebuffer();
            GpuTextureView color = RenderSystem.outputColorTextureOverride != null
                    ? RenderSystem.outputColorTextureOverride
                    : target.getColorAttachmentView();
            GpuTextureView depth = null;
            if (target.useDepthAttachment) {
                depth = RenderSystem.outputDepthTextureOverride != null
                        ? RenderSystem.outputDepthTextureOverride
                        : target.getDepthAttachmentView();
            }

            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            updateUniformBuffer(encoder);

            try (RenderPass pass = encoder.createRenderPass(
                    () -> "zenith_custom_shader",
                    color,
                    OptionalInt.empty(),
                    depth,
                    OptionalDouble.empty()
            )) {
                pass.setPipeline(pipeline);

                ScissorState scissor = RenderSystem.getScissorStateForRenderTypeDraws();
                if (scissor.method_72091()) {
                    pass.enableScissor(
                            scissor.method_72092(),
                            scissor.method_72093(),
                            scissor.method_72094(),
                            scissor.method_72095()
                    );
                }

                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("DynamicTransforms", dynamicSlice);
                if (uniformSlice != null) {
                    pass.setUniform("ZenithUniforms", uniformSlice);
                }

                pass.setVertexBuffer(0, vertexBuffer);

                for (int i = 0; i < 12; i++) {
                    GpuTextureView textureView = RenderSystem.getShaderTexture(i);
                    if (textureView != null) {
                        pass.bindSampler("Sampler" + i, textureView);
                    }
                }

                pass.setIndexBuffer(indexBuffer, indexType);
                pass.drawIndexed(0, 0, builtBuffer.getDrawParameters().indexCount(), 1);
            }
        } finally {
            builtBuffer.close();
            layer.endDrawing();
        }
    }

    public static void loadAndSetupPrograms() {
        REGISTERED_PROGRAMS.forEach(Runnable::run);
    }

    void setUniform(String name, int v0) {
        setUniform(name, (float) v0);
    }

    void setUniform(String name, float v0) {
        setUniformValues(name, new float[]{v0});
    }

    void setUniform(String name, float v0, float v1) {
        setUniformValues(name, new float[]{v0, v1});
    }

    void setUniform(String name, float v0, float v1, float v2) {
        setUniformValues(name, new float[]{v0, v1, v2});
    }

    void setUniform(String name, float v0, float v1, float v2, float v3) {
        setUniformValues(name, new float[]{v0, v1, v2, v3});
    }

    private void setUniformValues(String name, float[] values) {
        switch (name) {
            case "Size" -> {
                if (values.length >= 2) {
                    uniforms.sizeX = values[0];
                    uniforms.sizeY = values[1];
                    dirty = true;
                }
            }
            case "Radius" -> {
                if (values.length >= 4) {
                    uniforms.radiusX = values[0];
                    uniforms.radiusY = values[1];
                    uniforms.radiusZ = values[2];
                    uniforms.radiusW = values[3];
                    dirty = true;
                }
            }
            case "Smoothness" -> {
                if (values.length >= 2) {
                    uniforms.smoothnessX = values[0];
                    uniforms.smoothnessY = values[1];
                } else if (values.length >= 1) {
                    uniforms.smoothnessX = values[0];
                    uniforms.smoothnessY = 0.0f;
                }
                dirty = true;
            }
            case "CornerSmoothness" -> {
                if (values.length >= 1) {
                    uniforms.cornerSmoothness = values[0];
                    dirty = true;
                }
            }
            case "Thickness" -> {
                if (values.length >= 1) {
                    uniforms.thickness = values[0];
                    dirty = true;
                }
            }
            case "CornerIndex" -> {
                if (values.length >= 1) {
                    uniforms.cornerIndex = values[0];
                    dirty = true;
                }
            }
            case "Progress" -> {
                if (values.length >= 1) {
                    uniforms.progress = values[0];
                    dirty = true;
                }
            }
            case "Fade" -> {
                if (values.length >= 1) {
                    uniforms.fade = values[0];
                    dirty = true;
                }
            }
            case "StripeWidth" -> {
                if (values.length >= 1) {
                    uniforms.stripeWidth = values[0];
                    dirty = true;
                }
            }
            case "TopLeftColor" -> {
                if (values.length >= 4) {
                    uniforms.topLeftR = values[0];
                    uniforms.topLeftG = values[1];
                    uniforms.topLeftB = values[2];
                    uniforms.topLeftA = values[3];
                    dirty = true;
                }
            }
            case "BottomLeftColor" -> {
                if (values.length >= 4) {
                    uniforms.bottomLeftR = values[0];
                    uniforms.bottomLeftG = values[1];
                    uniforms.bottomLeftB = values[2];
                    uniforms.bottomLeftA = values[3];
                    dirty = true;
                }
            }
            case "TopRightColor" -> {
                if (values.length >= 4) {
                    uniforms.topRightR = values[0];
                    uniforms.topRightG = values[1];
                    uniforms.topRightB = values[2];
                    uniforms.topRightA = values[3];
                    dirty = true;
                }
            }
            case "BottomRightColor" -> {
                if (values.length >= 4) {
                    uniforms.bottomRightR = values[0];
                    uniforms.bottomRightG = values[1];
                    uniforms.bottomRightB = values[2];
                    uniforms.bottomRightA = values[3];
                    dirty = true;
                }
            }
            case "BlurRadius" -> {
                if (values.length >= 1) {
                    uniforms.blurRadius = values[0];
                    dirty = true;
                }
            }
            case "Resolution" -> {
                if (values.length >= 2) {
                    uniforms.resolutionX = values[0];
                    uniforms.resolutionY = values[1];
                    dirty = true;
                }
            }
            case "Offset" -> {
                if (values.length >= 1) {
                    uniforms.offset = values[0];
                    dirty = true;
                }
            }
            case "Saturation" -> {
                if (values.length >= 1) {
                    uniforms.saturation = values[0];
                    dirty = true;
                }
            }
            case "TintIntensity" -> {
                if (values.length >= 1) {
                    uniforms.tintIntensity = values[0];
                    dirty = true;
                }
            }
            case "TintColor" -> {
                if (values.length >= 3) {
                    uniforms.tintColorR = values[0];
                    uniforms.tintColorG = values[1];
                    uniforms.tintColorB = values[2];
                    dirty = true;
                }
            }
            case "Range" -> {
                if (values.length >= 1) {
                    uniforms.range = values[0];
                    dirty = true;
                }
            }
            case "Outline" -> {
                if (values.length >= 1) {
                    uniforms.outline = values[0];
                    dirty = true;
                }
            }
            case "OutlineThickness" -> {
                if (values.length >= 1) {
                    uniforms.outlineThickness = values[0];
                    dirty = true;
                }
            }
            case "OutlineColor" -> {
                if (values.length >= 4) {
                    uniforms.outlineColorR = values[0];
                    uniforms.outlineColorG = values[1];
                    uniforms.outlineColorB = values[2];
                    uniforms.outlineColorA = values[3];
                    dirty = true;
                }
            }
            case "EnableFadeout" -> {
                if (values.length >= 1) {
                    uniforms.enableFadeout = values[0];
                    dirty = true;
                }
            }
            case "FadeoutStart" -> {
                if (values.length >= 1) {
                    uniforms.fadeoutStart = values[0];
                    dirty = true;
                }
            }
            case "FadeoutEnd" -> {
                if (values.length >= 1) {
                    uniforms.fadeoutEnd = values[0];
                    dirty = true;
                }
            }
            case "MaxWidth" -> {
                if (values.length >= 1) {
                    uniforms.maxWidth = values[0];
                    dirty = true;
                }
            }
            case "TextPosX" -> {
                if (values.length >= 1) {
                    uniforms.textPosX = values[0];
                    dirty = true;
                }
            }
            default -> {
            }
        }
    }

    private void updateUniformBuffer(CommandEncoder encoder) {
        if (!dirty || uniformSlice == null || uniformData == null) {
            return;
        }
        uniformData.clear();
        Std140Builder builder = Std140Builder.intoBuffer(uniformData);
        uniforms.write(builder);
        uniformData.flip();
        encoder.writeToBuffer(uniformSlice, uniformData);
        dirty = false;
    }

    private void loadPipeline(ResourceManager manager) throws IOException {
        Identifier dataLocation = Identifier.of(id.getNamespace(), "shaders/core/" + id.getPath() + ".json");
        try (BufferedReader reader = manager.openAsReader(dataLocation)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            String vertex = json.get("vertex").getAsString();
            String fragment = json.get("fragment").getAsString();
            Identifier vertexId = Identifier.of(vertex);
            Identifier fragmentId = Identifier.of(fragment);

            List<String> samplers = new ArrayList<>();
            JsonArray samplersJson = json.getAsJsonArray("samplers");
            if (samplersJson != null) {
                for (JsonElement element : samplersJson) {
                    JsonObject sampler = element.getAsJsonObject();
                    if (sampler.has("name")) {
                        samplers.add(sampler.get("name").getAsString());
                    }
                }
            }
            samplerNames = samplers;

            RenderPipeline.Snippet snippet = vertexFormat == VertexFormats.POSITION_TEXTURE_COLOR
                    ? RenderPipelines.POSITION_TEX_COLOR_SNIPPET
                    : RenderPipelines.POSITION_COLOR_SNIPPET;

            Identifier pipelineId = Identifier.of(id.getNamespace(), "pipeline/" + id.getPath().replace('/', '_'));

            RenderPipeline.Builder builder = RenderPipeline.builder(snippet)
                    .withLocation(pipelineId)
                    .withVertexShader(vertexId)
                    .withFragmentShader(fragmentId)
                    .withVertexFormat(vertexFormat, VertexFormat.DrawMode.QUADS)
                    .withCull(false)
                    .withDepthWrite(false)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withUniform("ZenithUniforms", UniformType.UNIFORM_BUFFER);

            for (String samplerName : samplerNames) {
                builder.withSampler(samplerName);
            }

            pipeline = RenderPipelines.register(builder.build());
        }
    }
}

