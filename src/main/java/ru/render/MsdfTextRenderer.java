package ru.render;

import com.ferra13671.cometrenderer.CometRenderer;
import com.ferra13671.cometrenderer.CometLoaders;
import com.ferra13671.cometrenderer.plugins.minecraft.AbstractMinecraftPlugin;
import com.ferra13671.cometrenderer.plugins.minecraft.CustomVertexFormats;
import com.ferra13671.cometrenderer.plugins.minecraft.CustomVertexElementTypes;
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import com.ferra13671.cometrenderer.glsl.GlProgram;
import com.ferra13671.cometrenderer.glsl.shader.ShaderType;
import com.ferra13671.cometrenderer.glsl.uniform.UniformType;
import com.ferra13671.cometrenderer.vertex.DrawMode;
import com.ferra13671.cometrenderer.vertex.element.VertexElementType;
import com.ferra13671.cometrenderer.vertex.mesh.Mesh;
import org.joml.Vector4f;
import org.joml.Vector2f;

/**
 * MSDF текстовый рендерер для CometRenderer с собственным шейдером
 */
public class MsdfTextRenderer {
    private final MsdfFont font;
    private static GlProgram msdfProgram;
    private static boolean programInitialized = false;

    public MsdfTextRenderer(MsdfFont font) {
        this.font = font;
        initMsdfProgram();
    }

    private static void initMsdfProgram() {
        if (programInitialized) return;
        
        try {
            msdfProgram = CometLoaders.IN_JAR.createProgramBuilder()
                    .name("msdf-text")
                    .shader(CometLoaders.IN_JAR.createGlslFileEntry("shader.msdf-text.vertex", "assets/aporia/shaders/msdf_text.vert"), ShaderType.Vertex)
                    .shader(CometLoaders.IN_JAR.createGlslFileEntry("shader.msdf-text.fragment", "assets/aporia/shaders/msdf_text.frag"), ShaderType.Fragment)
                    .sampler("Sampler0")
                    .uniform("ColorModulator", UniformType.VEC4)
                    .uniform("pxRange", UniformType.FLOAT)
                    .build();
            programInitialized = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Рендерит текст на экран
     */
    public void drawText(float x, float y, float size, String text, RenderColor color) {
        if (text == null || text.isEmpty() || size <= 0 || msdfProgram == null) {
            return;
        }

        // Сохраняем текущую программу
        GlProgram previousProgram = CometRenderer.getGlobalProgram();

        // Устанавливаем MSDF программу
        CometRenderer.setGlobalProgram(msdfProgram);
        CometRenderer.initShaderColor();
        
        // Сбрасываем матрицу перед рендерингом текста
        AbstractMinecraftPlugin.getInstance().initMatrix();
        
        // Устанавливаем атлас через sampler uniform
        msdfProgram.getSampler(0).set(font.getAtlas());
        
        // Устанавливаем цвет
        msdfProgram.getUniform("ColorModulator", UniformType.VEC4)
                .set(color.toVector4f());
        
        // Правильный расчет pxRange для MSDF
        // scale = размер на экране / размер в метриках
        float scale = size / Math.max(1e-6f, font.getEmSize());
        // pxRange должен быть просто distanceRange (уже в пикселях атласа)
        float pxRange = font.getDistanceRange();
        msdfProgram.getUniform("pxRange", UniformType.FLOAT).set(pxRange);

        float lineHeight = font.getLineHeight() * scale;
        float baselineY = y;

        String[] lines = text.split("\n", -1);
        
        for (String line : lines) {
            drawTextLine(x, baselineY, scale, line);
            baselineY += lineHeight;
        }
        
        // Восстанавливаем предыдущую программу
        if (previousProgram != null) {
            CometRenderer.setGlobalProgram(previousProgram);
            CometRenderer.initShaderColor();
            AbstractMinecraftPlugin.getInstance().initMatrix();
        }
    }

    private void drawTextLine(float x, float baseline, float scale, String line) {
        if (line.isEmpty()) {
            return;
        }

        Mesh mesh = CometRenderer.createMesh(
                DrawMode.QUADS,
                CustomVertexFormats.POSITION_TEXTURE_COLOR,
                builder -> {
                    float penX = x;
                    float baselineY = baseline;
                    int prevCodepoint = -1;
                    int i = 0;
                    int glyphCount = 0;

                    while (i < line.length()) {
                        int cp = line.codePointAt(i);
                        int cpLen = Character.charCount(cp);
                        i += cpLen;

                        MsdfFont.Glyph glyph = font.getGlyph(cp);
                        int glyphCode = cp;

                        if (glyph == null) {
                            glyph = font.getGlyph('?');
                            glyphCode = '?';
                            if (glyph == null) {
                                continue;
                            }
                        }

                        // Применяем kerning
                        if (prevCodepoint != -1) {
                            penX += font.getKerning(prevCodepoint, glyphCode) * scale;
                        }

                        // Рендерим глиф если он видимый
                        if (glyph.renderable) {
                            float x0 = penX + glyph.planeLeft * scale;
                            float y0 = baselineY - glyph.planeTop * scale;
                            float width = (glyph.planeRight - glyph.planeLeft) * scale;
                            float height = (glyph.planeTop - glyph.planeBottom) * scale;

                            if (width > 0 && height > 0) {
                                
                                // Добавляем квад для глифа
                                float x1 = x0;
                                float y1 = y0;
                                float x2 = x0 + width;
                                float y2 = y0 + height;
                                
                                // Белый цвет для текстуры (цвет будет применен в шейдере)
                                RenderColor white = RenderColor.WHITE;
                                
                                // Вершина 1 (левый верх)
                                builder.vertex(x1, y1, 0)
                                        .element("Texture", VertexElementType.FLOAT, glyph.u0, glyph.v0)
                                        .element("Color", CustomVertexElementTypes.RENDER_COLOR, white);
                                
                                // Вершина 2 (левый низ)
                                builder.vertex(x1, y2, 0)
                                        .element("Texture", VertexElementType.FLOAT, glyph.u0, glyph.v1)
                                        .element("Color", CustomVertexElementTypes.RENDER_COLOR, white);
                                
                                // Вершина 3 (правый низ)
                                builder.vertex(x2, y2, 0)
                                        .element("Texture", VertexElementType.FLOAT, glyph.u1, glyph.v1)
                                        .element("Color", CustomVertexElementTypes.RENDER_COLOR, white);
                                
                                // Вершина 4 (правый верх)
                                builder.vertex(x2, y1, 0)
                                        .element("Texture", VertexElementType.FLOAT, glyph.u1, glyph.v0)
                                        .element("Color", CustomVertexElementTypes.RENDER_COLOR, white);
                                
                                glyphCount++;
                            }
                        }

                        penX += glyph.advance * scale;
                        prevCodepoint = glyphCode;
                    }
                    
                    if (glyphCount > 0) {
                    }
                }
        );

        if (mesh != null) {
            CometRenderer.draw(mesh);
        }
    }

    /**
     * Измеряет ширину текста
     */
    public float measureWidth(String text, float size) {
        if (text == null || text.isEmpty() || size <= 0) {
            return 0;
        }

        float scale = size / Math.max(1e-6f, font.getEmSize());
        float maxWidth = 0;

        String[] lines = text.split("\n", -1);
        for (String line : lines) {
            maxWidth = Math.max(maxWidth, measureLineWidth(line, scale));
        }

        return maxWidth;
    }

    private float measureLineWidth(String line, float scale) {
        if (line.isEmpty()) {
            return 0;
        }

        float penX = 0;
        int prevCodepoint = -1;
        int i = 0;

        while (i < line.length()) {
            int cp = line.codePointAt(i);
            int cpLen = Character.charCount(cp);
            i += cpLen;

            MsdfFont.Glyph glyph = font.getGlyph(cp);
            int glyphCode = cp;

            if (glyph == null) {
                glyph = font.getGlyph('?');
                glyphCode = '?';
                if (glyph == null) {
                    continue;
                }
            }

            if (prevCodepoint != -1) {
                penX += font.getKerning(prevCodepoint, glyphCode) * scale;
            }

            penX += glyph.advance * scale;
            prevCodepoint = glyphCode;
        }

        return penX;
    }

    /**
     * Измеряет высоту текста
     */
    public float measureHeight(String text, float size) {
        if (text == null || text.isEmpty() || size <= 0) {
            return 0;
        }

        float scale = size / Math.max(1e-6f, font.getEmSize());
        float lineHeight = font.getLineHeight() * scale;
        int lineCount = text.split("\n", -1).length;

        return lineHeight * lineCount;
    }
}
