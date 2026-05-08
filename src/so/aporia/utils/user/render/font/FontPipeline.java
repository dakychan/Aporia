/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.utils.user.render.font;

import so.aporia.utils.user.render.core.AporiaRenderer;
import net.minecraft.resources.Identifier;

/**
 * MSDF font pipeline — рисует глифы через MSDF шейдер.
 */
public class FontPipeline {

    public void drawText(FontAtlas atlas, String text, float x, float y, float size, int color) {
        drawText(atlas, text, x, y, size, color, 0, 0, 0);
    }

    public void drawText(FontAtlas atlas, String text, float x, float y, float size,
                         int color, float outlineWidth, int outlineColor, float rotation) {
        if (atlas == null || text == null || text.isEmpty()) return;

        float scale = size / atlas.getFontSize();
        Identifier texId = atlas.getTextureId();
        float pxRange = atlas.getDistanceRange();
        float atlasW = atlas.getAtlasWidth();
        float atlasH = atlas.getAtlasHeight();
        float cx = x;

        for (int i = 0; i < text.length(); i++) {
            int cp = text.codePointAt(i);
            if (Character.isSupplementaryCodePoint(cp)) i++;

            Glyph g = atlas.getGlyph(cp);
            if (g == null) continue;

            float gx = cx + g.xOffset * scale;
            float gy = y + g.yOffset * scale;
            float gw = g.width * scale;
            float gh = g.height * scale;

            AporiaRenderer.INSTANCE.drawMsdfGlyph(
                    texId, gx, gy, gw, gh,
                    g.u0, g.v0, g.u1, g.v1,
                    color, outlineWidth, outlineColor,
                    pxRange, atlasW, atlasH
            );

            cx += g.xAdvance * scale;
        }
    }

    public float getTextWidth(FontAtlas atlas, String text, float size) {
        if (atlas == null || text == null) return 0;
        float scale = size / atlas.getFontSize();
        float width = 0;
        for (int i = 0; i < text.length(); i++) {
            int cp = text.codePointAt(i);
            if (Character.isSupplementaryCodePoint(cp)) i++;
            Glyph g = atlas.getGlyph(cp);
            width += (g != null ? g.xAdvance : 0) * scale;
        }
        return width;
    }

    public void close() {}
}