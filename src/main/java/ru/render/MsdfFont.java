package ru.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ferra13671.gltextureutils.GLTexture;
import com.ferra13671.gltextureutils.ColorMode;
import com.ferra13671.gltextureutils.TextureFiltering;
import com.ferra13671.gltextureutils.TextureWrapping;
import com.ferra13671.gltextureutils.loader.FileEntry;
import com.ferra13671.gltextureutils.loader.TextureLoaders;
import com.ferra13671.gltextureutils.PathMode;
import com.ferra13671.gltextureutils.atlas.TextureAtlas;
import com.ferra13671.gltextureutils.atlas.TextureBorder;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;

/**
 * MSDF шрифт с поддержкой JSON метрик и TextureAtlas
 */
public class MsdfFont {
    private final Map<Integer, Glyph> glyphs;
    private final Map<Long, Float> kerning;
    private final GLTexture texture;
    private final TextureAtlas atlas;
    private final float distanceRange;
    private final float emSize;
    private final float lineHeight;
    private final float ascender;
    private final float descender;

    public MsdfFont(String jsonPath, String texturePath) {
        try {
            // Загружаем JSON метрики
            String json = readJsonFile(jsonPath);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            
            // Парсим atlas
            JsonObject atlasObj = root.getAsJsonObject("atlas");
            int atlasWidth = atlasObj.get("width").getAsInt();
            int atlasHeight = atlasObj.get("height").getAsInt();
            this.distanceRange = atlasObj.has("distanceRange") ? atlasObj.get("distanceRange").getAsFloat() : 6.0f;
            
            // Парсим metrics
            JsonObject metrics = root.getAsJsonObject("metrics");
            this.emSize = metrics.has("emSize") ? metrics.get("emSize").getAsFloat() : 1.0f;
            this.lineHeight = metrics.has("lineHeight") ? metrics.get("lineHeight").getAsFloat() : emSize;
            this.ascender = metrics.has("ascender") ? metrics.get("ascender").getAsFloat() : lineHeight;
            float descenderValue = metrics.has("descender") ? metrics.get("descender").getAsFloat() : 0.0f;
            this.descender = Math.abs(descenderValue);
            
            // Загружаем текстуру
            FileEntry fontTextureEntry = new FileEntry(texturePath, PathMode.INSIDE_JAR);
            GLTexture fontTexture = TextureLoaders.FILE_ENTRY.createTextureBuilder()
                    .name("msdf-font-" + System.nanoTime())
                    .info(fontTextureEntry)
                    .filtering(com.ferra13671.gltextureutils.TextureFiltering.SMOOTH)
                    .wrapping(com.ferra13671.gltextureutils.TextureWrapping.DEFAULT)
                    .build();
            
            // Создаем атлас с одной текстурой
            this.atlas = new TextureAtlas("msdf-font-atlas", Arrays.asList(fontTexture));
            this.texture = fontTexture;
            
            // Парсим глифы
            this.glyphs = new HashMap<>();
            JsonArray glyphArray = root.getAsJsonArray("glyphs");
            if (glyphArray != null) {
                TextureBorder border = atlas.getBorder(fontTexture);
                float borderU1 = border.getU1();
                float borderV1 = border.getV1();
                float borderU2 = border.getU2();
                float borderV2 = border.getV2();
                
                for (JsonElement element : glyphArray) {
                    JsonObject glyphObj = element.getAsJsonObject();
                    int codepoint = glyphObj.get("unicode").getAsInt();
                    float advance = glyphObj.has("advance") ? glyphObj.get("advance").getAsFloat() : 0.0f;
                    
                    JsonObject planeBounds = glyphObj.has("planeBounds") ? glyphObj.getAsJsonObject("planeBounds") : null;
                    JsonObject atlasBounds = glyphObj.has("atlasBounds") ? glyphObj.getAsJsonObject("atlasBounds") : null;
                    
                    Glyph glyph;
                    if (planeBounds != null && atlasBounds != null) {
                        float planeLeft = planeBounds.get("left").getAsFloat();
                        float planeBottom = planeBounds.get("bottom").getAsFloat();
                        float planeRight = planeBounds.get("right").getAsFloat();
                        float planeTop = planeBounds.get("top").getAsFloat();
                        
                        float atlasLeft = atlasBounds.get("left").getAsFloat();
                        float atlasBottom = atlasBounds.get("bottom").getAsFloat();
                        float atlasRight = atlasBounds.get("right").getAsFloat();
                        float atlasTop = atlasBounds.get("top").getAsFloat();
                        
                        // Нормализуем координаты в пределах исходной текстуры
                        float u0 = atlasLeft / atlasWidth;
                        float v0 = atlasBottom / atlasHeight;
                        float u1 = atlasRight / atlasWidth;
                        float v1 = atlasTop / atlasHeight;
                        
                        // Добавляем padding (1 пиксель) чтобы избежать артефактов от соседних глифов
                        float padding = 0.0f / atlasWidth;
                        u0 += padding;
                        u1 -= padding;
                        v0 += padding;
                        v1 -= padding;
                        
                        // Масштабируем координаты в пределы атласа
                        float atlasU0 = borderU1 + (u0 * (borderU2 - borderU1));
                        float atlasV0 = borderV1 + (v0 * (borderV2 - borderV1));
                        float atlasU1 = borderU1 + (u1 * (borderU2 - borderU1));
                        float atlasV1 = borderV1 + (v1 * (borderV2 - borderV1));
                        
                        glyph = new Glyph(
                            advance, planeLeft, planeBottom, planeRight, planeTop,
                            atlasU0, 1.0f - atlasV1, atlasU1, 1.0f - atlasV0
                        );
                    } else {
                        glyph = new Glyph(advance);
                    }
                    
                    glyphs.put(codepoint, glyph);
                }
            }
            
            // Парсим kerning
            this.kerning = new HashMap<>();
            JsonArray kerningArray = root.getAsJsonArray("kerning");
            if (kerningArray != null) {
                for (JsonElement element : kerningArray) {
                    JsonObject kObj = element.getAsJsonObject();
                    int left = kObj.get("unicode1").getAsInt();
                    int right = kObj.get("unicode2").getAsInt();
                    float advance = kObj.has("advance") ? kObj.get("advance").getAsFloat() : 0.0f;
                    kerning.put(pairKey(left, right), advance);
                }
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to load MSDF font: " + jsonPath, e);
        }
    }

    private String readJsonFile(String path) throws Exception {
        try (java.io.InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new RuntimeException("Resource not found: " + path);
            }
            return new String(is.readAllBytes());
        }
    }

    private static long pairKey(int left, int right) {
        return ((long) left << 32) | (right & 0xFFFFFFFFL);
    }

    public Glyph getGlyph(int codepoint) {
        return glyphs.get(codepoint);
    }

    public float getKerning(int left, int right) {
        return kerning.getOrDefault(pairKey(left, right), 0.0f);
    }

    public GLTexture getTexture() {
        return texture;
    }

    public TextureAtlas getAtlas() {
        return atlas;
    }

    public float getDistanceRange() {
        return distanceRange;
    }

    public float getEmSize() {
        return emSize;
    }

    public float getLineHeight() {
        return lineHeight;
    }

    public float getAscender() {
        return ascender;
    }

    public float getDescender() {
        return descender;
    }

    public static class Glyph {
        public final float advance;
        public final boolean renderable;
        public final float planeLeft;
        public final float planeBottom;
        public final float planeRight;
        public final float planeTop;
        public final float u0;
        public final float v0;
        public final float u1;
        public final float v1;

        public Glyph(float advance) {
            this.advance = advance;
            this.renderable = false;
            this.planeLeft = 0;
            this.planeBottom = 0;
            this.planeRight = 0;
            this.planeTop = 0;
            this.u0 = 0;
            this.v0 = 0;
            this.u1 = 0;
            this.v1 = 0;
        }

        public Glyph(float advance, float planeLeft, float planeBottom, float planeRight, float planeTop,
                     float u0, float v0, float u1, float v1) {
            this.advance = advance;
            this.renderable = true;
            this.planeLeft = planeLeft;
            this.planeBottom = planeBottom;
            this.planeRight = planeRight;
            this.planeTop = planeTop;
            this.u0 = u0;
            this.v0 = v0;
            this.u1 = u1;
            this.v1 = v1;
        }
    }
}
