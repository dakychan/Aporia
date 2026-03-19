package so.aporia.render.font;

/**
 * Registry of all Aporia fonts.
 */
public final class Fonts {

    public static final String REGULAR = "regular";
    public static final String BOLD    = "bold";
    public static final String ICONS   = "icons";
    /** Massive icon/glyph font — index-based, mapped to PUA codepoints (0xE000+). */
    public static final String FONT    = "font";

    private Fonts() {}

    /** Registers all fonts into the given {@link FontRenderer}. */
    public static void register(FontRenderer renderer) {
        renderer.loadFont(REGULAR, "regularnew");
        renderer.loadFont(BOLD,    "bold");
        renderer.loadFont(ICONS,   "icons");
        renderer.loadFont(FONT,    "font");
    }
}
