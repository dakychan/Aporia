package so.aporia.utils.user.render.core;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.resources.Identifier;

import java.util.*;

/**
 * Reusable shader fragment / pipeline configuration snippet.
 * Attach to pipeline builders to avoid repeating common settings.
 *
 * Usage:
 *   PipelineSnippet blur = PipelineSnippet.create("blur")
 *       .vertexShader(Identifier.fromNamespaceAndPath("aporia", "core/blur"))
 *       .fragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/blur"))
 *       .sampler("BlurTexture")
 *       .uniform("BlurData", UniformType.UNIFORM_BUFFER)
 *       .noDepth()
 *       .translucent();
 *
 *   // Apply to pipeline
 *   pipelineBuilder = snippet.applyTo(RenderPipeline.builder());
 */
public class PipelineSnippet {

    private String name;
    private Identifier location;
    private Identifier vertexShader;
    private Identifier fragmentShader;
    private VertexFormat vertexFormat;
    private VertexFormat.Mode drawMode;
    private BlendFunction blendFunction;
    private DepthTestFunction depthTest;
    private boolean depthWrite = true;
    private boolean cull = true;
    private boolean noDepth = false;

    private final List<String> samplers = new ArrayList<>();
    private final Map<String, UniformType> uniforms = new LinkedHashMap<>();

    private PipelineSnippet() {}

    public static PipelineSnippet create(String name) {
        PipelineSnippet s = new PipelineSnippet();
        s.name = name;
        return s;
    }

    public PipelineSnippet location(Identifier location) {
        this.location = location;
        return this;
    }

    public PipelineSnippet vertexShader(Identifier shader) {
        this.vertexShader = shader;
        return this;
    }

    public PipelineSnippet fragmentShader(Identifier shader) {
        this.fragmentShader = shader;
        return this;
    }

    public PipelineSnippet shaders(Identifier vertex, Identifier fragment) {
        this.vertexShader = vertex;
        this.fragmentShader = fragment;
        return this;
    }

    public PipelineSnippet vertexFormat(VertexFormat format) {
        this.vertexFormat = format;
        return this;
    }

    public PipelineSnippet drawMode(VertexFormat.Mode mode) {
        this.drawMode = mode;
        return this;
    }

    public PipelineSnippet blend(BlendFunction blend) {
        this.blendFunction = blend;
        return this;
    }

    public PipelineSnippet translucent() {
        this.blendFunction = BlendFunction.TRANSLUCENT;
        return this;
    }

    public PipelineSnippet noDepth() {
        this.noDepth = true;
        this.depthTest = DepthTestFunction.NO_DEPTH_TEST;
        this.depthWrite = false;
        return this;
    }

    public PipelineSnippet depthWrite(boolean write) {
        this.depthWrite = write;
        return this;
    }

    public PipelineSnippet cull(boolean cull) {
        this.cull = cull;
        return this;
    }

    public PipelineSnippet noCull() {
        this.cull = false;
        return this;
    }

    public PipelineSnippet sampler(String name) {
        this.samplers.add(name);
        return this;
    }

    public PipelineSnippet uniform(String name, UniformType type) {
        this.uniforms.put(name, type);
        return this;
    }

    public RenderPipeline.Builder applyTo(RenderPipeline.Builder builder) {
        if (location != null) builder.withLocation(location);
        if (vertexShader != null) builder.withVertexShader(vertexShader);
        if (fragmentShader != null) builder.withFragmentShader(fragmentShader);
        if (vertexFormat != null && drawMode != null) {
            builder.withVertexFormat(vertexFormat, drawMode);
        }
        if (blendFunction != null) builder.withBlend(blendFunction);
        if (noDepth) {
            builder.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST);
            builder.withDepthWrite(false);
        }
        builder.withCull(cull);

        for (String sampler : samplers) {
            builder.withSampler(sampler);
        }
        for (var entry : uniforms.entrySet()) {
            builder.withUniform(entry.getKey(), entry.getValue());
        }
        return builder;
    }

    public RenderPipeline build() {
        RenderPipeline.Builder builder = RenderPipeline.builder();
        applyTo(builder);
        return builder.build();
    }

    public String getName() { return name; }
    public Identifier getVertexShader() { return vertexShader; }
    public Identifier getFragmentShader() { return fragmentShader; }
    public List<String> getSamplers() { return Collections.unmodifiableList(samplers); }
    public Map<String, UniformType> getUniforms() { return Collections.unmodifiableMap(uniforms); }
}
