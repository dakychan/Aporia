package aporia.cc.utility.render.display;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.Tessellator;
import net.minecraft.util.Identifier;
import aporia.cc.utility.render.display.shader.GlProgram;

import java.util.HashMap;
import java.util.Map;

public final class RenderLayerUtil {
    private static final RenderLayer POSITION_COLOR = RenderLayer.of(
            "Aporia_position_color_no_depth",
            256,
            RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
                    .withLocation(Identifier.of("Aporia", "pipeline/position_color_no_depth"))
                    .withCull(false)
                    .withDepthWrite(false)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .build(),
            RenderLayer.MultiPhaseParameters.builder().build(false));
    private static final RenderLayer POSITION_COLOR_DEPTH = RenderLayer.of(
            "Aporia_position_color_depth",
            256,
            RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
                    .withLocation(Identifier.of("Aporia", "pipeline/position_color_depth"))
                    .withCull(false)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .build(),
            RenderLayer.MultiPhaseParameters.builder().build(false));
    private static final RenderLayer LINES_NO_DEPTH = RenderLayer.of(
            "Aporia_lines_no_depth",
            256,
            RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
                    .withLocation(Identifier.of("Aporia", "pipeline/lines_no_depth"))
                    .withDepthWrite(false)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .build(),
            RenderLayer.MultiPhaseParameters.builder().build(false));
    private static final RenderLayer LINES_DEPTH_NO_WRITE = RenderLayer.of(
            "Aporia_lines_depth_no_write",
            256,
            RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
                    .withLocation(Identifier.of("Aporia", "pipeline/lines_depth_no_write"))
                    .withDepthWrite(false)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .build(),
            RenderLayer.MultiPhaseParameters.builder().build(false));
    private static final RenderLayer GUI_TEXTURED_NO_TEXTURE = RenderLayer.of(
            "Aporia_gui_textured",
            256,
            RenderPipelines.GUI_TEXTURED,
            RenderLayer.MultiPhaseParameters.builder()
                    .texture(RenderPhase.NO_TEXTURE)
                    .build(false));
    private static final Map<Identifier, RenderLayer> GUI_TEXTURED = new HashMap<>();
    private static final Map<RenderLayer, Identifier> GUI_TEXTURED_IDS = new HashMap<>();
    private static final ThreadLocal<RenderLayer> CURRENT_LAYER = new ThreadLocal<>();

    private RenderLayerUtil() {
    }

    public static RenderLayer positionColor() {
        return POSITION_COLOR;
    }

    public static RenderLayer positionColorDepth() {
        return POSITION_COLOR_DEPTH;
    }

    public static RenderLayer lines() {
        return LINES_NO_DEPTH;
    }

    public static RenderLayer linesDepthNoWrite() {
        return LINES_DEPTH_NO_WRITE;
    }

    public static RenderLayer guiTextured(Identifier texture) {
        return GUI_TEXTURED.computeIfAbsent(texture, id -> {
            RenderLayer layer = RenderLayer.of(
                    "Aporia_gui_texture_" + id,
                    256,
                    RenderPipelines.GUI_TEXTURED,
                    RenderLayer.MultiPhaseParameters.builder()
                            .texture(new RenderPhase.Texture(id, false))
                            .build(false));
            GUI_TEXTURED_IDS.put(layer, id);
            return layer;
        });
    }

    public static RenderLayer guiTexturedNoTexture() {
        return GUI_TEXTURED_NO_TEXTURE;
    }

    public static BufferBuilder begin(RenderLayer layer) {
        return Tessellator.getInstance().begin(layer.getDrawMode(), layer.getVertexFormat());
    }

    public static void setCurrentLayer(RenderLayer layer) {
        CURRENT_LAYER.set(layer);
    }

    public static void drawCurrent(BufferBuilder buffer) {
        RenderLayer layer = CURRENT_LAYER.get();
        if (layer == null) {
            layer = POSITION_COLOR;
        }
        draw(layer, buffer);
    }

    public static void draw(RenderLayer layer, BufferBuilder buffer) {
        BuiltBuffer builtBuffer = buffer.endNullable();
        if (builtBuffer == null) {
            return;
        }
        GlProgram activeProgram = GlProgram.getActive();
        if (activeProgram != null) {
            Identifier texture = GUI_TEXTURED_IDS.get(layer);
            if (texture != null) {
                RenderSystem.setShaderTexture(0, MinecraftClient.getInstance()
                        .getTextureManager()
                        .getTexture(texture)
                        .getGlTextureView());
            }
            activeProgram.draw(layer, builtBuffer);
            GlProgram.clearActive();
            return;
        }
        layer.draw(builtBuffer);
    }
}

