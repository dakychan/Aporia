package so.aporia.utils.user.render.core;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.resources.Identifier;

/**
 * Pre-built pipeline snippets for common rendering operations.
 */
public final class DefaultSnippets {

    public static final PipelineSnippet SHAPE = PipelineSnippet.create("shape")
            .location(Identifier.fromNamespaceAndPath("aporia", "pipeline/aporia"))
            .shaders(
                    Identifier.fromNamespaceAndPath("aporia", "core/aporia"),
                    Identifier.fromNamespaceAndPath("aporia", "core/aporia")
            )
            .vertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR)
            .drawMode(VertexFormat.Mode.TRIANGLES)
            .blend(BlendFunction.TRANSLUCENT)
            .noDepth()
            .noCull();

    public static final PipelineSnippet IMAGE = PipelineSnippet.create("image")
            .location(Identifier.fromNamespaceAndPath("aporia", "pipeline/image"))
            .shaders(
                    Identifier.fromNamespaceAndPath("aporia", "core/image"),
                    Identifier.fromNamespaceAndPath("aporia", "core/image")
            )
            .vertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR)
            .drawMode(VertexFormat.Mode.TRIANGLES)
            .blend(BlendFunction.TRANSLUCENT)
            .noDepth()
            .noCull();

    public static final PipelineSnippet ROUNDED_RECT = PipelineSnippet.create("rounded_rect")
            .location(Identifier.fromNamespaceAndPath("aporia", "pipeline/rounded_rect"))
            .shaders(
                    Identifier.fromNamespaceAndPath("aporia", "core/rounded_rect"),
                    Identifier.fromNamespaceAndPath("aporia", "core/rounded_rect")
            )
            .vertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR)
            .drawMode(VertexFormat.Mode.TRIANGLES)
            .blend(BlendFunction.TRANSLUCENT)
            .noDepth()
            .noCull();

    public static final PipelineSnippet BLIT = PipelineSnippet.create("blit")
            .location(Identifier.fromNamespaceAndPath("aporia", "pipeline/blur_blit"))
            .shaders(
                    Identifier.fromNamespaceAndPath("aporia", "core/blit"),
                    Identifier.fromNamespaceAndPath("aporia", "core/blit")
            )
            .vertexFormat(DefaultVertexFormat.POSITION_TEX)
            .drawMode(VertexFormat.Mode.TRIANGLES)
            .blend(BlendFunction.TRANSLUCENT)
            .noDepth()
            .noCull();

    public static final PipelineSnippet KAWASE_DOWN = PipelineSnippet.create("kawase_down")
            .location(Identifier.fromNamespaceAndPath("aporia", "pipeline/kawase_down"))
            .shaders(
                    Identifier.fromNamespaceAndPath("aporia", "core/kawase_down"),
                    Identifier.fromNamespaceAndPath("aporia", "core/kawase_down")
            )
            .vertexFormat(DefaultVertexFormat.POSITION_TEX)
            .drawMode(VertexFormat.Mode.TRIANGLES)
            .blend(BlendFunction.TRANSLUCENT)
            .noDepth()
            .noCull();

    public static final PipelineSnippet KAWASE_UP = PipelineSnippet.create("kawase_up")
            .location(Identifier.fromNamespaceAndPath("aporia", "pipeline/kawase_up"))
            .shaders(
                    Identifier.fromNamespaceAndPath("aporia", "core/kawase_up"),
                    Identifier.fromNamespaceAndPath("aporia", "core/kawase_up")
            )
            .vertexFormat(DefaultVertexFormat.POSITION_TEX)
            .drawMode(VertexFormat.Mode.TRIANGLES)
            .blend(BlendFunction.TRANSLUCENT)
            .noDepth()
            .noCull();

    public static final PipelineSnippet POSTPROCESS = PipelineSnippet.create("postprocess")
            .location(Identifier.fromNamespaceAndPath("aporia", "pipeline/postprocess"))
            .shaders(
                    Identifier.fromNamespaceAndPath("aporia", "core/postprocess"),
                    Identifier.fromNamespaceAndPath("aporia", "core/postprocess")
            )
            .vertexFormat(DefaultVertexFormat.POSITION_TEX)
            .drawMode(VertexFormat.Mode.TRIANGLES)
            .blend(BlendFunction.TRANSLUCENT)
            .noDepth()
            .noCull();

    public static final PipelineSnippet ENTITY_GLOW = PipelineSnippet.create("entity_glow")
            .location(Identifier.fromNamespaceAndPath("aporia", "pipeline/entity_glow"))
            .shaders(
                    Identifier.fromNamespaceAndPath("aporia", "core/entity_glow"),
                    Identifier.fromNamespaceAndPath("aporia", "core/entity_glow")
            )
            .vertexFormat(DefaultVertexFormat.NEW_ENTITY)
            .drawMode(VertexFormat.Mode.QUADS)
            .blend(BlendFunction.TRANSLUCENT)
            .noDepth()
            .noCull();

    public static void registerAll() {
        PipelineSnippetRegistry r = PipelineSnippetRegistry.INSTANCE;
        r.register(SHAPE);
        r.register(IMAGE);
        r.register(ROUNDED_RECT);
        r.register(BLIT);
        r.register(KAWASE_DOWN);
        r.register(KAWASE_UP);
        r.register(POSTPROCESS);
        r.register(ENTITY_GLOW);
    }

    private DefaultSnippets() {}
}
