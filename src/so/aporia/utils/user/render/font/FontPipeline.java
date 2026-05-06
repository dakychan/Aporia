/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.utils.user.render.font;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/**
 * Low-level GPU pipeline for batched MSDF glyph rendering.
 * <p>
 * Packs all glyph quads into a single UBO per flush and draws them
 * with one instanced {@code draw()} call using a geometry-shader-free
 * approach: the vertex shader generates quad vertices from instance index.
 */
public class FontPipeline {

    private static final int MAX_CHARS   = 256;

    /**
     * Header size: screenW, screenH, guiScale, outlineWidth, outlineColor(4), atlasW, atlasH, distRange, fontSize, charCount, pad(3).
     * <p>16 floats + 4 ints = 80 bytes.
     */
    private static final int HEADER_SIZE = 16 * 4 + 4 * 4;

    /**
     * Per-glyph data: x,y,w,h, u0,v0,u1,v1, r,g,b,a, rotation, pivotX, pivotY, scale.
     * <p>16 floats = 64 bytes.
     */
    private static final int GLYPH_SIZE  = 16 * 4;
    private static final int BUFFER_SIZE = HEADER_SIZE + MAX_CHARS * GLYPH_SIZE;

    private static final float FIXED_GUI_SCALE = 2.0f;

    /* private static final RenderPipeline PIPELINE = RenderPipeline.builder()
        .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/msdf"))
        .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/msdf"))
        .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/msdf"))
        .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
        .withUniform("FontData", UniformType.UNIFORM_BUFFER)
        .withSampler("Sampler0")
        .withBlend(BlendFunction.TRANSLUCENT)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withDepthWrite(false)
        .withCull(false)
        .build(); */

    private GpuBuffer uniformBuffer;
    private ByteBuffer dataBuffer;
    private boolean initialized = false;

    private final List<CharData> charBatch = new ArrayList<>();
    private FontAtlas currentAtlas;
    private float currentOutlineWidth;
    private int currentOutlineColor;

    /**
     * Per-glyph data staged for the next flush.
     * <p>Данные глифа для следующей отрисовки.
     */
    private static class CharData {
        final float x, y, width, height;
        final float u0, v0, u1, v1;
        final int color;
        final float rotation, pivotX, pivotY, glyphScale;

        CharData(float x, float y, float w, float h,
                 float u0, float v0, float u1, float v1,
                 int color, float rotation, float pivotX, float pivotY, float glyphScale) {
            this.x = x; this.y = y; this.width = w; this.height = h;
            this.u0 = u0; this.v0 = v0; this.u1 = u1; this.v1 = v1;
            this.color = color;
            this.rotation = rotation; this.pivotX = pivotX; this.pivotY = pivotY;
            this.glyphScale = glyphScale;
        }
    }

    private void ensureInitialized() {
        if (initialized) return;
        dataBuffer = MemoryUtil.memAlloc(BUFFER_SIZE);
        initialized = true;
    }

    /**
     * Draws text with no outline and no rotation.
     * <p>
     * Рисует текст без обводки и вращения.
     */
    public void drawText(FontAtlas atlas, String text, float x, float y, float size, int color) {
        drawText(atlas, text, x, y, size, color, 0, 0, 0);
    }

    /**
     * Draws text with optional outline and rotation (degrees).
     * <p>
     * Рисует текст с опциональной обводкой и вращением (в градусах).
     */
    public void drawText(FontAtlas atlas, String text, float x, float y, float size, int color,
                         float outlineWidth, int outlineColor, float rotation) {
        if (text == null || text.isEmpty()) return;
        atlas.ensureLoaded();
        if (atlas.getGlyphCount() == 0) return;
        ensureInitialized();

        if (currentAtlas != null && (currentAtlas != atlas
                || currentOutlineWidth != outlineWidth || currentOutlineColor != outlineColor)) {
            flush();
        }
        currentAtlas = atlas;
        currentOutlineWidth = outlineWidth;
        currentOutlineColor = outlineColor;

        float scale = size / atlas.getFontSize();
        float rotRad = (float) Math.toRadians(rotation);
        float pivotX = x + getTextWidth(atlas, text, size) / 2f;
        float pivotY = y + getTextHeight(atlas, text, size) / 2f;

        appendGlyphs(atlas, text, x, y, scale, color, rotRad, pivotX, pivotY);
        if (!charBatch.isEmpty()) flush();
    }

    /**
     * Draws text rotated around an explicit pivot point.
     * <p>
     * Рисует текст с вращением вокруг явной точки опоры.
     */
    public void drawTextRotatedAroundPoint(FontAtlas atlas, String text, float x, float y, float size,
                                           int color, float outlineWidth, int outlineColor,
                                           float rotation, float pivotX, float pivotY) {
        if (text == null || text.isEmpty()) return;
        atlas.ensureLoaded();
        if (atlas.getGlyphCount() == 0) return;
        ensureInitialized();

        if (currentAtlas != null && (currentAtlas != atlas
                || currentOutlineWidth != outlineWidth || currentOutlineColor != outlineColor)) {
            flush();
        }
        currentAtlas = atlas;
        currentOutlineWidth = outlineWidth;
        currentOutlineColor = outlineColor;

        float scale = size / atlas.getFontSize();
        appendGlyphs(atlas, text, x, y, scale, color, (float) Math.toRadians(rotation), pivotX, pivotY);
        if (!charBatch.isEmpty()) flush();
    }

    /**
     * Iterates the string, resolves color codes, and stages glyphs into charBatch.
     * <p>
     * Итерирует строку, обрабатывает коды цветов и добавляет глифы в charBatch.
     */
    private void appendGlyphs(FontAtlas atlas, String text, float startX, float startY,
                               float scale, int baseColor, float rotRad, float pivotX, float pivotY) {
        float cursorX = startX, cursorY = startY;
        int currentColor = baseColor;
        int i = 0;

        while (i < text.length()) {
            int cp = text.codePointAt(i);
            int cc = Character.charCount(cp);

            if ((cp == '§' || cp == '&') && i + cc < text.length()) {
                int next = text.codePointAt(i + cc);
                if (next == '#' && i + cc + 6 < text.length()) {
                    try {
                        currentColor = 0xFF000000 | Integer.parseInt(text.substring(i + cc + 1, i + cc + 7), 16);
                        i += cc + 7; continue;
                    } catch (Exception ignored) {}
                }
                int code = "0123456789abcdefklmnor".indexOf(Character.toLowerCase((char) next));
                if (code >= 0) {
                    if (code < 16) currentColor = legacyColor(code);
                    else if (code == 21) currentColor = baseColor;
                    i += cc + Character.charCount(next); continue;
                }
            }

            if (cp == '\n') {
                cursorX = startX;
                cursorY += atlas.getLineHeight() * scale;
                i += cc; continue;
            }

            Glyph glyph = atlas.getGlyph(cp);
            if (glyph == null) {
                Glyph fallback = atlas.getGlyph('?');
                cursorX += fallback != null ? fallback.xAdvance * scale : atlas.getFontSize() * scale * 0.5f;
                i += cc; continue;
            }

            if (glyph.width > 0 && glyph.height > 0) {
                charBatch.add(new CharData(
                    cursorX + glyph.xOffset * scale, cursorY + glyph.yOffset * scale,
                    glyph.width * scale, glyph.height * scale,
                    glyph.u0, glyph.v0, glyph.u1, glyph.v1,
                    currentColor, rotRad, pivotX, pivotY, scale));
            }

            cursorX += glyph.xAdvance * scale;
            if (charBatch.size() >= MAX_CHARS) flush();
            i += cc;
        }
    }

    /**
     * Submits the current batch to the GPU and clears it.
     * <p>
     * Отправляет текущий пакет в GPU и очищает его.
     */
    public void flush() {
        charBatch.clear();
        currentAtlas = null;
    }

    private void prepareUniformData(FontAtlas atlas, float outlineWidth, int outlineColor) {
        Minecraft mc = Minecraft.getInstance();
        float sw = mc.getWindow().getGuiScaledWidth();
        float sh = mc.getWindow().getGuiScaledHeight();

        dataBuffer.clear();
        dataBuffer.putFloat(sw).putFloat(sh).putFloat(FIXED_GUI_SCALE).putFloat(outlineWidth);

        dataBuffer.putFloat(((outlineColor >> 16) & 0xFF) / 255f);
        dataBuffer.putFloat(((outlineColor >>  8) & 0xFF) / 255f);
        dataBuffer.putFloat(( outlineColor        & 0xFF) / 255f);
        dataBuffer.putFloat(((outlineColor >> 24) & 0xFF) / 255f);

        dataBuffer.putFloat(atlas.getAtlasWidth()).putFloat(atlas.getAtlasHeight());
        dataBuffer.putFloat(atlas.getDistanceRange()).putFloat(atlas.getFontSize());

        dataBuffer.putInt(charBatch.size()).putInt(0).putInt(0).putInt(0);

        for (CharData cd : charBatch) {
            dataBuffer.putFloat(cd.x).putFloat(cd.y).putFloat(cd.width).putFloat(cd.height);
            dataBuffer.putFloat(cd.u0).putFloat(cd.v0).putFloat(cd.u1).putFloat(cd.v1);
            dataBuffer.putFloat(((cd.color >> 16) & 0xFF) / 255f);
            dataBuffer.putFloat(((cd.color >>  8) & 0xFF) / 255f);
            dataBuffer.putFloat(( cd.color        & 0xFF) / 255f);
            dataBuffer.putFloat(((cd.color >> 24) & 0xFF) / 255f);
            dataBuffer.putFloat(cd.rotation).putFloat(cd.pivotX).putFloat(cd.pivotY).putFloat(cd.glyphScale);
        }

        dataBuffer.flip();
    }

    /**
     * Returns the rendered pixel width of the given string.
     * <p>
     * Возвращает ширину строки в пикселях.
     */
    public float getTextWidth(FontAtlas atlas, String text, float size) {
        atlas.ensureLoaded();
        float scale = size / atlas.getFontSize();
        float width = 0, maxWidth = 0;
        int i = 0;
        while (i < text.length()) {
            int cp = text.codePointAt(i);
            int cc = Character.charCount(cp);
            if ((cp == '§' || cp == '&') && i + cc < text.length()) {
                int next = text.codePointAt(i + cc);
                if (next == '#' && i + cc + 6 < text.length()) { i += cc + 7; continue; }
                if ("0123456789abcdefklmnor".indexOf(Character.toLowerCase((char) next)) >= 0) {
                    i += cc + Character.charCount(next); continue;
                }
            }
            if (cp == '\n') { maxWidth = Math.max(maxWidth, width); width = 0; i += cc; continue; }
            Glyph g = atlas.getGlyph(cp);
            if (g != null) width += g.xAdvance * scale;
            else { Glyph fb = atlas.getGlyph('?'); width += fb != null ? fb.xAdvance * scale : size * 0.5f; }
            i += cc;
        }
        return Math.max(maxWidth, width);
    }

    /**
     * Returns the line height in pixels for the given font at the given size.
     * <p>
     * Возвращает высоту строки в пикселях для данного шрифта и размера.
     */
    public float getTextHeight(FontAtlas atlas, String text, float size) {
        atlas.ensureLoaded();
        int lines = 1;
        for (int i = 0; i < text.length(); i++) if (text.charAt(i) == '\n') lines++;
        return lines * atlas.getLineHeight() * (size / atlas.getFontSize());
    }

    /**
     * Releases all GPU resources.
     * <p>
     * Освобождает все GPU ресурсы.
     */
    public void close() {
        if (uniformBuffer != null) { uniformBuffer.close(); uniformBuffer = null; }
        if (dataBuffer    != null) { MemoryUtil.memFree(dataBuffer); dataBuffer = null; }
        initialized = false;
    }

    /**
     * Converts a Minecraft legacy color index (0-15) to ARGB.
     * <p>
     * Конвертирует индекс цвета Minecraft (0-15) в ARGB.
     */
    private static int legacyColor(int index) {
        int j = (index >> 3 & 1) * 85;
        int r = (index >> 2 & 1) * 170 + j;
        int g = (index >> 1 & 1) * 170 + j;
        int b = (index       & 1) * 170 + j;
        if (index == 6) r += 85;
        return (255 << 24) | (r << 16) | (g << 8) | b;
    }
}
