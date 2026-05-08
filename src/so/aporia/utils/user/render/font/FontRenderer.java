/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.utils.user.render.font;

import net.minecraft.resources.Identifier;
import so.aporia.utils.user.render.core.AporiaRenderer;
import so.aporia.utils.assets.AssetManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class FontRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger("aporia/FontRenderer");

    private final FontPipeline pipeline = new FontPipeline();
    private final Map<String, FontAtlas> fonts = new HashMap<>();
    private boolean initialized = false;

    public FontRenderer() {}

    public void loadFont(String name, String path) {
        Identifier json    = Identifier.fromNamespaceAndPath("aporia", "fonts/" + path + ".json");
        Identifier texture = Identifier.fromNamespaceAndPath("aporia", "fonts/" + path + ".png");
        fonts.put(name, new FontAtlas(json, texture));
        LOGGER.info("Registered font: {} -> {}", name, path);
    }

    public void initialize() {
        if (initialized) return;
        LOGGER.info("Initializing {} fonts...", fonts.size());
        long t = System.currentTimeMillis();

        for (FontAtlas atlas : fonts.values()) {
            atlas.ensureLoaded();
            loadAtlasTexture(atlas);
        }

        initialized = true;
        LOGGER.info("Fonts ready in {}ms", System.currentTimeMillis() - t);
    }

    private void loadAtlasTexture(FontAtlas atlas) {
        Identifier texId = atlas.getTextureId();

        if (AporiaRenderer.INSTANCE.isTextureLoaded(texId)) {
            LOGGER.info("Texture already loaded: {}", texId);
            return;
        }

        try {
            Path path = AssetManager.getResourcePath(texId);
            LOGGER.info("Loading texture: {} from {}", texId, path);

            if (path != null && Files.exists(path)) {
                try (InputStream stream = Files.newInputStream(path)) {
                    AporiaRenderer.INSTANCE.loadImage(stream);
                    LOGGER.info("Loaded atlas texture: {}", texId);
                }
            } else {
                LOGGER.warn("Atlas texture not found: {} (path: {})", texId, path);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load atlas texture: {}", texId, e);
        }
    }

    public void reload() {
        initialized = false;
        fonts.clear();
        Fonts.register(this);
        initialize();
    }

    public boolean isInitialized() { return initialized; }
    public FontAtlas getAtlas(String name) { return fonts.get(name); }

    public void drawText(String fontName, String text, float x, float y, float size, int color) {
        FontAtlas atlas = fonts.get(fontName);
        if (atlas == null) return;
        pipeline.drawText(atlas, text, x, y, size, color);
    }

    public void drawText(String fontName, String text, float x, float y, float size, int color, float rotation) {
        FontAtlas atlas = fonts.get(fontName);
        if (atlas == null) return;
        pipeline.drawText(atlas, text, x, y, size, color, 0, 0, rotation);
    }

    public void drawTextWithOutline(String fontName, String text, float x, float y, float size,
                                    int color, float outlineWidth, int outlineColor) {
        FontAtlas atlas = fonts.get(fontName);
        if (atlas == null) return;
        pipeline.drawText(atlas, text, x, y, size, color, outlineWidth, outlineColor, 0);
    }

    public void drawCenteredText(String fontName, String text, float x, float y, float size, int color) {
        FontAtlas atlas = fonts.get(fontName);
        if (atlas == null) return;
        float w = pipeline.getTextWidth(atlas, text, size);
        pipeline.drawText(atlas, text, x - w / 2f, y, size, color);
    }

    public float getTextWidth(String fontName, String text, float size) {
        FontAtlas atlas = fonts.get(fontName);
        return atlas != null ? pipeline.getTextWidth(atlas, text, size) : 0;
    }

    public float getLineHeight(String fontName, float size) {
        FontAtlas atlas = fonts.get(fontName);
        if (atlas == null) return size;
        return (atlas.getLineHeight() / atlas.getFontSize()) * size;
    }

    public void drawGlyph(String fontName, int index, float x, float y, float size, int color) {
        drawText(fontName, new String(Character.toChars(0xE000 + index)), x, y, size, color);
    }

    public void close() {
        pipeline.close();
        fonts.clear();
        initialized = false;
    }
}