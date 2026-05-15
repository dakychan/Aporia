package so.aporia.utils.user.render.font;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * High-level font rendering API.
 * <p>
 * Manages a registry of named {@link FontAtlas} instances and delegates
 * all GPU work to {@link FontPipeline}.
 */
public class FontRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger("aporia/FontRenderer");

    private final FontPipeline pipeline = new FontPipeline();
    private final Map<String, FontAtlas> fonts = new HashMap<>();
    private boolean initialized = false;

    /**
     * Registers a font by name, pointing to {@code assets/aporia/fonts/<path>.json}
     * and {@code assets/aporia/fonts/<path>.png}.
     */
    public void loadFont(String name, String path) {
        Identifier json    = Identifier.fromNamespaceAndPath("aporia", "fonts/" + path + ".json");
        Identifier texture = Identifier.fromNamespaceAndPath("aporia", "fonts/" + path + ".png");
        fonts.put(name, new FontAtlas(json, texture));
        LOGGER.info("Registered font: {} -> {}", name, path);
    }

    /**
     * Force-loads all registered atlases. Called once after resources are ready.
     */
    public void initialize() {
        if (initialized) return;
        LOGGER.info("Initializing {} fonts...", fonts.size());
        long t = System.currentTimeMillis();
        fonts.values().forEach(FontAtlas::ensureLoaded);
        initialized = true;
        LOGGER.info("Fonts ready in {}ms", System.currentTimeMillis() - t);
    }

    /** Re-initializes fonts after a resource reload. */
    public void reload() {
        initialized = false;
        fonts.clear();
        Fonts.register(this);
        initialize();
    }

    public boolean isInitialized() { return initialized; }

    public FontAtlas getAtlas(String name) { return fonts.get(name); }

    /** Draws text at (x, y) with the given pixel size and ARGB color. */
    public void drawText(String fontName, String text, float x, float y, float size, int color) {
        FontAtlas atlas = fonts.get(fontName);
        if (atlas == null) return;
        pipeline.drawText(atlas, text, x, y, size, color);
    }

    /** Draws text with rotation (degrees). */
    public void drawText(String fontName, String text, float x, float y, float size, int color, float rotation) {
        FontAtlas atlas = fonts.get(fontName);
        if (atlas == null) return;
        pipeline.drawText(atlas, text, x, y, size, color, 0, 0, rotation);
    }

    /** Draws text with an MSDF outline. */
    public void drawTextWithOutline(String fontName, String text, float x, float y, float size,
                                    int color, float outlineWidth, int outlineColor) {
        FontAtlas atlas = fonts.get(fontName);
        if (atlas == null) return;
        pipeline.drawText(atlas, text, x, y, size, color, outlineWidth, outlineColor, 0);
    }

    /** Draws text horizontally centered at (x, y). */
    public void drawCenteredText(String fontName, String text, float x, float y, float size, int color) {
        FontAtlas atlas = fonts.get(fontName);
        if (atlas == null) return;
        float w = pipeline.getTextWidth(atlas, text, size);
        pipeline.drawText(atlas, text, x - w / 2f, y, size, color);
    }

    /** Returns the rendered pixel width of the given string. */
    public float getTextWidth(String fontName, String text, float size) {
        FontAtlas atlas = fonts.get(fontName);
        return atlas != null ? pipeline.getTextWidth(atlas, text, size) : 0;
    }

    /** Returns the line height in pixels for the given font at the given size. */
    public float getLineHeight(String fontName, float size) {
        FontAtlas atlas = fonts.get(fontName);
        if (atlas == null) return size;
        return (atlas.getLineHeight() / atlas.getFontSize()) * size;
    }

    /**
     * Draws a single glyph from an index-based font (like {@link Fonts#FONT}).
     * Index is mapped to PUA codepoint {@code 0xE000 + index}.
     */
    public void drawGlyph(String fontName, int index, float x, float y, float size, int color) {
        drawText(fontName, new String(Character.toChars(0xE000 + index)), x, y, size, color);
    }

    public void close() {
        pipeline.close();
        fonts.clear();
        initialized = false;
    }
}