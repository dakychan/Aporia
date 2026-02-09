package zenith.zov.utility.render.display.base.color;

import lombok.Getter;
import net.minecraft.util.math.MathHelper;
import zenith.zov.utility.math.MathUtil;

import java.awt.*;
import java.nio.ByteBuffer;
import java.util.Objects;

@Getter
public class ColorRGBA {
    public static final ColorRGBA WHITE = new ColorRGBA(255, 255, 255);
    public static final ColorRGBA BLACK = new ColorRGBA(0, 0, 0);
    public static final ColorRGBA GREEN = new ColorRGBA(0, 255, 0);
    public static final ColorRGBA RED = new ColorRGBA(255, 0, 0);
    public static final ColorRGBA BLUE = new ColorRGBA(0, 0, 255);
    public static final ColorRGBA YELLOW = new ColorRGBA(255, 255, 0);
    public static final ColorRGBA GRAY = new ColorRGBA(88, 87, 93);
    public static final ColorRGBA TRANSPARENT = new ColorRGBA(0, 0, 0,0);

    private transient float[] hsbValues;

    private final int red;
    private final int green;
    private final int blue;
    private final int alpha;
    private static final ByteBuffer PIXEL_BUFFER = ByteBuffer.allocateDirect(4);

    public ColorRGBA(int color) {

        this(ColorUtil.red(color), ColorUtil.green(color), ColorUtil.blue(color), ColorUtil.alpha(color));
    }
    public ColorRGBA(Color color) {
        this(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    public ColorRGBA(int red, int green, int blue) {
        this(red, green, blue, 255);
    }

    public ColorRGBA(int red, int green, int blue, int alpha) {
        red = MathHelper.clamp(red, 0, 255);
        green = MathHelper.clamp(green, 0, 255);
        blue = MathHelper.clamp(blue, 0, 255);
        alpha = MathHelper.clamp(alpha, 0, 255);

        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
    }

    public int getRGB() {
        int a = Math.round(clamp(alpha));
        int r = Math.round(clamp(red));
        int g = Math.round(clamp(green));
        int b = Math.round(clamp(blue));
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    private int clamp(float value) {
        return (int) Math.max(0, Math.min(255, value));
    }

    public static ColorRGBA fromHex(String hex) {
        String sanitized = hex.startsWith("#") ? hex.substring(1) : hex;
        if (sanitized.length() != 6 && sanitized.length() != 8) {
            throw new IllegalArgumentException("Hex color must be in the format #RRGGBB or #RRGGBBAA");
        }

        int red = Integer.parseInt(sanitized.substring(0, 2), 16);
        int green = Integer.parseInt(sanitized.substring(2, 4), 16);
        int blue = Integer.parseInt(sanitized.substring(4, 6), 16);
        int alpha = sanitized.length() == 8
                ? Integer.parseInt(sanitized.substring(6, 8), 16)
                : 255;

        return new ColorRGBA(red, green, blue, alpha);
    }

    public static ColorRGBA lerp(ColorRGBA startColor, ColorRGBA endColor, float delta) {
        float clampedDelta = Math.max(0.0f, Math.min(1.0f, delta));

        int r = (int) (startColor.getRed() + (endColor.getRed() - startColor.getRed()) * clampedDelta);
        int g = (int) (startColor.getGreen() + (endColor.getGreen() - startColor.getGreen()) * clampedDelta);
        int b = (int) (startColor.getBlue() + (endColor.getBlue() - startColor.getBlue()) * clampedDelta);
        int a = (int) (startColor.getAlpha() + (endColor.getAlpha() - startColor.getAlpha()) * clampedDelta);

        return new ColorRGBA(r, g, b, a);
    }

    public static ColorRGBA fromInt(int colorInt) {
        int alpha = (colorInt >> 24) & 0xFF;
        int red = (colorInt >> 16) & 0xFF;
        int green = (colorInt >> 8) & 0xFF;
        int blue = colorInt & 0xFF;
        return new ColorRGBA(red, green, blue, alpha);
    }
    public ColorRGBA withAlpha(float newAlpha) {
        return new ColorRGBA(this.red, this.green, this.blue, clamp((int) (255*newAlpha)));
    }
    public ColorRGBA withAlpha(int newAlpha) {
        return new ColorRGBA(this.red, this.green, this.blue, newAlpha);
    }

    public ColorRGBA mulAlpha(float percent) {
        return withAlpha((int) (alpha * percent));
    }

    public ColorRGBA mix(ColorRGBA color2, float amount) {
        amount = Math.min(1, Math.max(0, amount));
        return new ColorRGBA((int) MathUtil.interpolate(this.getRed(), color2.getRed(), amount),
                (int) MathUtil.interpolate(this.getGreen(), color2.getGreen(), amount),
                (int) MathUtil.interpolate(this.getBlue(), color2.getBlue(), amount),
                (int) MathUtil.interpolate(this.getAlpha(), color2.getAlpha(), amount));
    }

    public ColorRGBA darker(float amount) {
        amount = MathHelper.clamp(amount, 0f, 1f);
        return new ColorRGBA(
                (int) (red * (1 - amount)),
                (int) (green * (1 - amount)),
                (int) (blue * (1 - amount)),
                alpha
        );
    }

    public static ColorRGBA fromHSB(float hue, float saturation, float brightness) {
        if (saturation == 0) {
            int grayValue = (int) (brightness * 255.0f + 0.5f);
            return new ColorRGBA(grayValue, grayValue, grayValue);
        }

        float h = (hue - (float) Math.floor(hue)) * 6.0f;
        float f = h - (float) Math.floor(h);
        float p = brightness * (1.0f - saturation);
        float q = brightness * (1.0f - saturation * f);
        float t = brightness * (1.0f - (saturation * (1.0f - f)));

        float r = 0, g = 0, b = 0;

        switch ((int) h) {
            case 0: r = brightness; g = t; b = p; break;
            case 1: r = q; g = brightness; b = p; break;
            case 2: r = p; g = brightness; b = t; break;
            case 3: r = p; g = q; b = brightness; break;
            case 4: r = t; g = p; b = brightness; break;
            case 5: r = brightness; g = p; b = q; break;
        }

        return new ColorRGBA((int) (r * 255), (int) (g * 255), (int) (b * 255));
    }

    public float getHue() {
        return getHSBValues()[0];
    }

    public float getSaturation() {
        return getHSBValues()[2];
    }

    public float getBrightness() {
        return getHSBValues()[1];
    }

    private float[] getHSBValues() {
        if (this.hsbValues == null) {
            this.hsbValues = calculateHSB();
        }
        return this.hsbValues;
    }

    private float[] calculateHSB() {
        float r = this.red / 255.0f;
        float g = this.green / 255.0f;
        float b = this.blue / 255.0f;

        float maxC = Math.max(r, Math.max(g, b));
        float minC = Math.min(r, Math.min(g, b));
        float delta = maxC - minC;

        float hue = 0f;
        if (delta != 0) {
            if (maxC == r) {
                hue = ((g - b) / delta);
            } else if (maxC == g) {
                hue = ((b - r) / delta) + 2f;
            } else { // maxC == b
                hue = ((r - g) / delta) + 4f;
            }
            hue /= 6f;
            if (hue < 0) {
                hue += 1f;
            }
        }

        float saturation = (maxC == 0) ? 0f : (delta / maxC);
        float brightness = maxC;

        return new float[]{hue, saturation, brightness};
    }

    public ColorRGBA brighter(float amount) {
        amount = MathHelper.clamp(amount, 0f, 1f);

        return new ColorRGBA(
                (int) (red + (255f - red) * amount),
                (int) (green + (255f - green) * amount),
                (int) (blue + (255f - blue) * amount),
                alpha
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ColorRGBA colorRGBA = (ColorRGBA) o;
        return Float.compare(red, colorRGBA.red) == 0 && Float.compare(green, colorRGBA.green) == 0 && Float.compare(blue, colorRGBA.blue) == 0 && Float.compare(alpha, colorRGBA.alpha) == 0;
    }

    public float difference(ColorRGBA colorRGBA) {
        return Math.abs(getHue() - colorRGBA.getHue()) + Math.abs(getBrightness() - colorRGBA.getBrightness()) + Math.abs(getSaturation() - colorRGBA.getSaturation());
    }

    @Override
    public int hashCode() {
        return Objects.hash(red, green, blue, alpha);
    }



}