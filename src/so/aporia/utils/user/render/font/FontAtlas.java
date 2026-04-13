/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.utils.user.render.font;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Loads and holds glyph data from an MSDF font atlas JSON + texture pair.
 * <p>
 * Загружает и хранит данные глифов из MSDF шрифта (JSON + текстура).
 */
public class FontAtlas {

    private static final Logger LOGGER = LoggerFactory.getLogger("aporia/Font");

    private final Identifier jsonId;
    private final Identifier textureId;
    private final Map<Integer, Glyph> glyphs = new HashMap<>();
    private final AtomicBoolean loaded = new AtomicBoolean(false);

    private float atlasWidth    = 512;
    private float atlasHeight   = 512;
    private float fontSize      = 32;
    private float lineHeight    = 40;
    private float distanceRange = 4;
    private float ascender      = 0.95f;
    private boolean yOriginBottom = false;

    public FontAtlas(Identifier jsonId, Identifier textureId) {
        this.jsonId    = jsonId;
        this.textureId = textureId;
    }

    /**
     * Loads the atlas if not already loaded (thread-safe).
     * <p>
     * Загружает атлас если ещё не загружен (потокобезопасно).
     */
    public void ensureLoaded() {
        if (loaded.get()) return;
        synchronized (this) {
            if (loaded.get()) return;
            doLoad();
        }
    }

    private void doLoad() {
        try {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(jsonId);
            if (res.isEmpty()) {
                LOGGER.warn("Font JSON not found in ResourceManager: {}, trying AssetManager...", jsonId);
                loadFromAssetManager();
                return;
            }
            try (InputStream is = res.get().open();
                 InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                parseJson(JsonParser.parseReader(reader).getAsJsonObject());
                loaded.set(true);
                LOGGER.info("Loaded font: {} ({} glyphs)", jsonId, glyphs.size());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load font: {}", jsonId, e);
            loaded.set(true);
        }
    }
    
    private void loadFromAssetManager() {
        try {
            String path = jsonId.getPath().replace("aporia:", "").replace(".json", "") + ".json";
            java.nio.file.Path assetFile = so.aporia.utils.files.FilesManager.ROOT.resolve(".assets").resolve(path);
            byte[] data = java.nio.file.Files.readAllBytes(assetFile);
            String json = new String(data, StandardCharsets.UTF_8);
            parseJson(JsonParser.parseString(json).getAsJsonObject());
            loaded.set(true);
            LOGGER.info("Loaded font from AssetManager: {} ({} glyphs)", jsonId, glyphs.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load font from AssetManager: {}", jsonId, e);
            loaded.set(true);
        }
    }

    private void parseJson(JsonObject root) {
        if (root.has("atlas")) {
            JsonObject atlas = root.getAsJsonObject("atlas");
            atlasWidth      = getFloat(atlas, "width",         512);
            atlasHeight     = getFloat(atlas, "height",        512);
            fontSize        = getFloat(atlas, "size",           32);
            distanceRange   = getFloat(atlas, "distanceRange",   4);
            if (atlas.has("yOrigin")) {
                yOriginBottom = atlas.get("yOrigin").getAsString().equalsIgnoreCase("bottom");
            }
        }

        if (root.has("metrics")) {
            JsonObject metrics = root.getAsJsonObject("metrics");
            lineHeight = getFloat(metrics, "lineHeight", 1.2f) * fontSize;
            ascender   = getFloat(metrics, "ascender",   0.95f);
        }

        if (root.has("glyphs")) {
            for (JsonElement elem : root.getAsJsonArray("glyphs")) {
                parseGlyph(elem.getAsJsonObject());
            }
        }
    }

    private void parseGlyph(JsonObject g) {
        int unicode = -1;
        if (g.has("unicode"))     unicode = g.get("unicode").getAsInt();
        else if (g.has("char"))   { String s = g.get("char").getAsString(); if (!s.isEmpty()) unicode = s.codePointAt(0); }
        else if (g.has("id"))     unicode = g.get("id").getAsInt();
        else if (g.has("index"))  unicode = g.get("index").getAsInt() + 0xE000;
        if (unicode < 0) return;

        float advance = getFloat(g, "advance", 0) * fontSize;
        if (advance == 0) advance = getFloat(g, "xadvance", 0);

        float x = 0, y = 0, w = 0, h = 0, xOff = 0, yOff = 0;

        if (g.has("atlasBounds")) {
            JsonObject b = g.getAsJsonObject("atlasBounds");
            float left = getFloat(b, "left", 0), bottom = getFloat(b, "bottom", 0);
            float right = getFloat(b, "right", 0), top = getFloat(b, "top", 0);
            x = left;
            w = right - left;
            h = top - bottom;
            y = yOriginBottom ? atlasHeight - top : bottom;
        } else if (g.has("x")) {
            x = getFloat(g, "x", 0); y = getFloat(g, "y", 0);
            w = getFloat(g, "width", 0); h = getFloat(g, "height", 0);
        }

        if (g.has("planeBounds")) {
            JsonObject p = g.getAsJsonObject("planeBounds");
            xOff = getFloat(p, "left", 0) * fontSize;
            yOff = (ascender - getFloat(p, "top", 0)) * fontSize;
        } else {
            xOff = getFloat(g, "xoffset", 0);
            yOff = getFloat(g, "yoffset", 0);
        }

        glyphs.put(unicode, new Glyph(unicode, x, y, w, h, xOff, yOff, advance, atlasWidth, atlasHeight));
    }

    private float getFloat(JsonObject obj, String key, float def) {
        return obj.has(key) ? obj.get(key).getAsFloat() : def;
    }

    /**
     * Returns glyph for code point.
     * <p>
     * Возвращает глиф для кодовой точки.
     */
    public Glyph getGlyph(int codePoint)    { return glyphs.get(codePoint); }

    /**
     * Checks if glyph exists.
     * <p>
     * Проверяет наличие глифа.
     */
    public boolean hasGlyph(int codePoint)  { return glyphs.containsKey(codePoint); }

    /**
     * Returns texture identifier.
     * <p>
     * Возвращает идентификатор текстуры.
     */
    public Identifier getTextureId()        { return textureId; }

    /**
     * Returns font size.
     * <p>
     * Возвращает размер шрифта.
     */
    public float getFontSize()              { return fontSize; }

    /**
     * Returns line height.
     * <p>
     * Возвращает высоту строки.
     */
    public float getLineHeight()            { return lineHeight; }

    /**
     * Returns atlas width.
     * <p>
     * Возвращает ширину атласа.
     */
    public float getAtlasWidth()            { return atlasWidth; }

    /**
     * Returns atlas height.
     * <p>
     * Возвращает высоту атласа.
     */
    public float getAtlasHeight()           { return atlasHeight; }

    /**
     * Returns distance range.
     * <p>
     * Возвращает диапазон расстояния.
     */
    public float getDistanceRange()         { return distanceRange; }

    /**
     * Checks if loaded.
     * <p>
     * Проверяет загруженность.
     */
    public boolean isLoaded()               { return loaded.get(); }

    /**
     * Returns glyph count.
     * <p>
     * Возвращает количество глифов.
     */
    public int getGlyphCount()              { return glyphs.size(); }

    /**
     * Returns ascender.
     * <p>
     * Возвращает_ascender.
     */
    public float getAscender()              { return ascender; }
}
