package ru.render;

import com.ferra13671.cometrenderer.CometRenderer;
import com.ferra13671.cometrenderer.glsl.GlProgram;
import com.ferra13671.cometrenderer.glsl.shader.ShaderType;
import com.ferra13671.cometrenderer.CometLoaders;
import com.ferra13671.cometrenderer.glsl.uniform.UniformType;
import com.ferra13671.cometrenderer.plugins.minecraft.AbstractMinecraftPlugin;
import com.ferra13671.cometrenderer.plugins.minecraft.CustomVertexElementTypes;
import com.ferra13671.cometrenderer.plugins.minecraft.CustomVertexFormats;
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import com.ferra13671.cometrenderer.plugins.minecraft.drawer.AbstractDrawer;
import com.ferra13671.cometrenderer.vertex.DrawMode;
import com.ferra13671.cometrenderer.vertex.element.VertexElementType;
import com.ferra13671.cometrenderer.vertex.mesh.Mesh;

public class RoundedRectRenderer extends AbstractDrawer {
    private static GlProgram CUSTOM_ROUNDED_RECT_PROGRAM;
    private static boolean initialized = false;

    static {
        initProgram();
    }

    public RoundedRectRenderer() {
        super(Mesh.builder(DrawMode.QUADS, CustomVertexFormats.ROUNDED_RECT));
    }

    public RoundedRectRenderer(int allocatorSize) {
        super(Mesh.builder(allocatorSize, DrawMode.QUADS, CustomVertexFormats.ROUNDED_RECT));
    }

    private static void initProgram() {
        try {
            CUSTOM_ROUNDED_RECT_PROGRAM = CometLoaders.IN_JAR.createProgramBuilder()
                    .name("custom-rounded-rect")
                    .shader(CometLoaders.IN_JAR.createGlslFileEntry("shader.custom-rounded-rect.vertex", "assets/aporia/shaders/rounded_rect.vert"), ShaderType.Vertex)
                    .shader(CometLoaders.IN_JAR.createGlslFileEntry("shader.custom-rounded-rect.fragment", "assets/aporia/shaders/rounded_rect.frag"), ShaderType.Fragment)
                    .uniform("height", UniformType.FLOAT)
                    .build();
            initialized = true;
            CometRenderer.getLogger().log("RoundedRectRenderer: Шейдер успешно загружен");
        } catch (Exception e) {
            CometRenderer.getLogger().error("RoundedRectRenderer: Ошибка загрузки шейдера: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Добавляет закругленный прямоугольник в меш
     */
    public RoundedRectRenderer rectSized(float x, float y, float width, float height, float radius, RenderColor color) {
        return rectPositioned(x, y, x + width, y + height, radius, color);
    }

    /**
     * Добавляет закругленный прямоугольник в меш (позиционированный)
     */
    public RoundedRectRenderer rectPositioned(float x1, float y1, float x2, float y2, float radius, RenderColor color) {
        float[] halfSize = {(x2 - x1) / 2, (y2 - y1) / 2};
        float[] pos = {x1 + halfSize[0], y1 + halfSize[1]};

        x1 -= 2;
        x2 += 2;
        y1 -= 2;
        y2 += 2;
        // Вершина 1
        this.meshBuilder.vertex(x1, y1, 0)
                .element("Color", CustomVertexElementTypes.RENDER_COLOR, color)
                .element("Rect Position", VertexElementType.FLOAT, pos[0], pos[1])
                .element("Half Size", VertexElementType.FLOAT, halfSize[0], halfSize[1])
                .element("Radius", VertexElementType.FLOAT, radius);

        // Вершина 2
        this.meshBuilder.vertex(x1, y2, 0)
                .element("Color", CustomVertexElementTypes.RENDER_COLOR, color)
                .element("Rect Position", VertexElementType.FLOAT, pos[0], pos[1])
                .element("Half Size", VertexElementType.FLOAT, halfSize[0], halfSize[1])
                .element("Radius", VertexElementType.FLOAT, radius);

        // Вершина 3
        this.meshBuilder.vertex(x2, y2, 0)
                .element("Color", CustomVertexElementTypes.RENDER_COLOR, color)
                .element("Rect Position", VertexElementType.FLOAT, pos[0], pos[1])
                .element("Half Size", VertexElementType.FLOAT, halfSize[0], halfSize[1])
                .element("Radius", VertexElementType.FLOAT, radius);

        // Вершина 4
        this.meshBuilder.vertex(x2, y1, 0)
                .element("Color", CustomVertexElementTypes.RENDER_COLOR, color)
                .element("Rect Position", VertexElementType.FLOAT, pos[0], pos[1])
                .element("Half Size", VertexElementType.FLOAT, halfSize[0], halfSize[1])
                .element("Radius", VertexElementType.FLOAT, radius);

        return this;
    }

    @Override
    protected void draw() {
        if (!initialized) {
            CometRenderer.getLogger().warn("RoundedRectRenderer: Шейдер не инициализирован!");
            return;
        }

        CometRenderer.setGlobalProgram(CUSTOM_ROUNDED_RECT_PROGRAM);

        CometRenderer.initShaderColor();
        AbstractMinecraftPlugin.getInstance().initMatrix();
        CometRenderer.getGlobalProgram().getUniform("height", UniformType.FLOAT)
                .set((float) AbstractMinecraftPlugin.getInstance().getMainFramebufferHeight());

        CometRenderer.draw(this.mesh, false);
    }
}

