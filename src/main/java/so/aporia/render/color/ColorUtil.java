package so.aporia.render.color;

/**
 * Central color utility for Aporia.
 * <p>
 * All colors are ARGB ints ({@code 0xAARRGGBB}).
 * Supports:
 * <ul>
 *   <li>Hex strings — {@code "#RRGGBB"}, {@code "#AARRGGBB"}, {@code "RRGGBB"}</li>
 *   <li>Minecraft {@code §}-codes — {@code "§c"}, {@code "§a"}, etc.</li>
 *   <li>RGB / RGBA construction helpers</li>
 *   <li>Linear and HSV gradients between two colors</li>
 *   <li>Rainbow (hue-cycle) gradient across N steps</li>
 *   <li>Alpha manipulation</li>
 * </ul>
 */
public final class ColorUtil {

    /* ------------------------------------------------------------------ */
    /* Minecraft §-code palette                                             */
    /* ------------------------------------------------------------------ */

    /** ARGB values for Minecraft legacy color codes §0–§f. */
    private static final int[] LEGACY = {
        0xFF000000, 0xFF0000AA, 0xFF00AA00, 0xFF00AAAA,
        0xFFAA0000, 0xFFAA00AA, 0xFFFFAA00, 0xFFAAAAAA,
        0xFF555555, 0xFF5555FF, 0xFF55FF55, 0xFF55FFFF,
        0xFFFF5555, 0xFFFF55FF, 0xFFFFFF55, 0xFFFFFFFF,
    };

    private ColorUtil() {}

    /* ------------------------------------------------------------------ */
    /* Construction                                                         */
    /* ------------------------------------------------------------------ */

    /** Builds an opaque ARGB color from 0–255 components. */
    public static int rgb(int r, int g, int b) {
        return 0xFF000000 | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    /** Builds an ARGB color from 0–255 components. */
    public static int rgba(int r, int g, int b, int a) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    /** Builds an ARGB color from 0.0–1.0 float components. */
    public static int rgbf(float r, float g, float b) {
        return rgb((int)(r * 255), (int)(g * 255), (int)(b * 255));
    }

    /** Builds an ARGB color from 0.0–1.0 float components including alpha. */
    public static int rgbaf(float r, float g, float b, float a) {
        return rgba((int)(r * 255), (int)(g * 255), (int)(b * 255), (int)(a * 255));
    }

    /* ------------------------------------------------------------------ */
    /* Parsing                                                              */
    /* ------------------------------------------------------------------ */

    /**
     * Parses a hex color string.
     * Accepts: {@code "#RGB"}, {@code "#RRGGBB"}, {@code "#AARRGGBB"},
     * {@code "RGB"}, {@code "RRGGBB"}, {@code "AARRGGBB"} (no {@code #}).
     * Returns {@code fallback} on parse failure.
     */
    public static int fromHex(String hex, int fallback) {
        if (hex == null) return fallback;
        String s = hex.startsWith("#") ? hex.substring(1) : hex;
        try {
            return switch (s.length()) {
                case 3  -> {
                    int r = Integer.parseInt(s.substring(0, 1), 16);
                    int g = Integer.parseInt(s.substring(1, 2), 16);
                    int b = Integer.parseInt(s.substring(2, 3), 16);
                    yield rgb(r * 17, g * 17, b * 17);
                }
                case 6  -> 0xFF000000 | Integer.parseInt(s, 16);
                case 8  -> (int) Long.parseLong(s, 16);
                default -> fallback;
            };
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** Parses a Minecraft {@code §X} color code character. Returns -1 if not a color code. */
    public static int fromLegacyCode(char code) {
        int idx = "0123456789abcdef".indexOf(Character.toLowerCase(code));
        return idx >= 0 ? LEGACY[idx] : -1;
    }

    /* ------------------------------------------------------------------ */
    /* Component extraction                                                 */
    /* ------------------------------------------------------------------ */

    public static int alpha(int argb) { return (argb >> 24) & 0xFF; }
    public static int red  (int argb) { return (argb >> 16) & 0xFF; }
    public static int green(int argb) { return (argb >>  8) & 0xFF; }
    public static int blue (int argb) { return  argb        & 0xFF; }

    /* ------------------------------------------------------------------ */
    /* Alpha manipulation                                                   */
    /* ------------------------------------------------------------------ */

    /** Returns the color with alpha replaced. {@code a} is 0–255. */
    public static int withAlpha(int argb, int a) {
        return (argb & 0x00FFFFFF) | ((a & 0xFF) << 24);
    }

    /** Returns the color with alpha multiplied by {@code factor} (0.0–1.0). */
    public static int multiplyAlpha(int argb, float factor) {
        int a = (int)(alpha(argb) * factor);
        return withAlpha(argb, a);
    }

    /* ------------------------------------------------------------------ */
    /* Gradients                                                            */
    /* ------------------------------------------------------------------ */

    /**
     * Linear interpolation between two ARGB colors.
     * {@code t} is 0.0 (= {@code a}) to 1.0 (= {@code b}).
     */
    public static int lerp(int a, int b, float t) {
        float it = 1f - t;
        int ar = (int)(red(a)   * it + red(b)   * t);
        int ag = (int)(green(a) * it + green(b) * t);
        int ab = (int)(blue(a)  * it + blue(b)  * t);
        int aa = (int)(alpha(a) * it + alpha(b) * t);
        return rgba(ar, ag, ab, aa);
    }

    /**
     * Returns an array of {@code steps} colors linearly interpolated from {@code start} to {@code end}.
     * Index 0 = start, index steps-1 = end.
     */
    public static int[] gradient(int start, int end, int steps) {
        if (steps <= 1) return new int[]{ start };
        int[] out = new int[steps];
        for (int i = 0; i < steps; i++) {
            out[i] = lerp(start, end, (float) i / (steps - 1));
        }
        return out;
    }

    /**
     * Returns a rainbow gradient of {@code steps} colors cycling through full hue range.
     * {@code alpha} is 0–255.
     */
    public static int[] rainbow(int steps, int alpha) {
        int[] out = new int[steps];
        for (int i = 0; i < steps; i++) {
            float hue = (float) i / steps;
            out[i] = withAlpha(fromHSV(hue, 1f, 1f), alpha);
        }
        return out;
    }

    /**
     * Returns a single rainbow color at a given time offset (0.0–1.0).
     * Useful for animating a single element through the hue wheel.
     */
    public static int rainbowAt(float t, float saturation, float value, int alpha) {
        return withAlpha(fromHSV(t % 1f, saturation, value), alpha);
    }

    /* ------------------------------------------------------------------ */
    /* HSV conversion                                                       */
    /* ------------------------------------------------------------------ */

    /**
     * Converts HSV (all 0.0–1.0) to opaque ARGB.
     */
    public static int fromHSV(float h, float s, float v) {
        if (s == 0f) {
            int c = (int)(v * 255);
            return rgb(c, c, c);
        }
        float hh = (h % 1f) * 6f;
        int   i  = (int) hh;
        float f  = hh - i;
        float p  = v * (1f - s);
        float q  = v * (1f - s * f);
        float t  = v * (1f - s * (1f - f));
        return switch (i) {
            case 0  -> rgbf(v, t, p);
            case 1  -> rgbf(q, v, p);
            case 2  -> rgbf(p, v, t);
            case 3  -> rgbf(p, q, v);
            case 4  -> rgbf(t, p, v);
            default -> rgbf(v, p, q);
        };
    }

    /**
     * Converts ARGB to HSV. Returns float[3]: {h, s, v} all 0.0–1.0.
     */
    public static float[] toHSV(int argb) {
        float r = red(argb)   / 255f;
        float g = green(argb) / 255f;
        float b = blue(argb)  / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float d   = max - min;
        float h   = 0f;
        float s   = max == 0f ? 0f : d / max;
        float v   = max;
        if (d != 0f) {
            if      (max == r) h = (g - b) / d + (g < b ? 6f : 0f);
            else if (max == g) h = (b - r) / d + 2f;
            else               h = (r - g) / d + 4f;
            h /= 6f;
        }
        return new float[]{ h, s, v };
    }
}
