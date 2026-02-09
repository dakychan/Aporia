package zenith.zov.utility.render.display.base.color;

import lombok.experimental.UtilityClass;
import net.minecraft.util.math.MathHelper;
import zenith.zov.Zenith;

import java.awt.*;
import java.util.regex.Pattern;

@UtilityClass
public class ColorUtil {
    public final int LIGHT_RED = getColor(255, 85, 85,255);

    public int red(int c) {
        return (c >> 16) & 0xFF;
    }

    public int green(int c) {
        return (c >> 8) & 0xFF;
    }

    public int blue(int c) {
        return c & 0xFF;
    }

    public int alpha(int c) {
        return (c >> 24) & 0xFF;
    }

    public float redf(int c) {
        return red(c) / 255.0f;
    }

    public float greenf(int c) {
        return green(c) / 255.0f;
    }

    public float bluef(int c) {
        return blue(c) / 255.0f;
    }

    public float alphaf(int c) {
        return alpha(c) / 255.0f;
    }

    public int[] getRGBA(int c) {
        return new int[]{red(c), green(c), blue(c), alpha(c)};
    }

    public int[] getRGB(int c) {
        return new int[]{red(c), green(c), blue(c)};
    }

    public float[] getRGBAf(int c) {
        return new float[]{redf(c), greenf(c), bluef(c), alphaf(c)};
    }

    public float[] getRGBf(int c) {
        return new float[]{redf(c), greenf(c), bluef(c)};
    }
    public static boolean isValidHexColor(String input) {
        return input != null && input.matches("(?i)^[a-f0-9]{6}$");
    }
    public static ColorRGBA hexToRgb(String colorStr, ColorRGBA fallbackColor) {
        if (!isValidHexColor(colorStr)) return fallbackColor;

        int rgb = Integer.parseInt(colorStr, 16);

        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;

        return new ColorRGBA(new Color(red, green, blue));
    }
    public String colorToHex(ColorRGBA color) {
        int rgb = color.getRGB();
        return String.format("%06X", (rgb & 0xFFFFFF));
    }
    public ColorRGBA lerp(int speed, int index, ColorRGBA start, ColorRGBA end) {
        int angle = (int) (((System.currentTimeMillis()) / speed + index) % 360);
        angle = (angle >= 180 ? 360 - angle : angle) * 2;
        return interpolate(start, end, angle / 360f);
    }
    public ColorRGBA gradient(int speed, int index, ColorRGBA... colors) {
        int angle = (int) ((System.currentTimeMillis() / speed + index) % 360);
        angle = (angle > 180 ? 360 - angle : angle) + 180;
        int colorIndex = (int) (angle / 360f * colors.length);
        if (colorIndex == colors.length) {
            colorIndex--;
        }
        ColorRGBA color1 = colors[colorIndex];
        ColorRGBA color2 = colors[colorIndex == colors.length - 1 ? 0 : colorIndex + 1];
        return interpolate(color1, color2, angle / 360f * colors.length - colorIndex);
    }
    public ColorRGBA interpolate(ColorRGBA color1, ColorRGBA color2, float amount) {
        return color1.mix(color2,amount);
    }
    private static final Pattern FORMATTING_CODE_PATTERN = Pattern.compile("(?i)§[0-9a-f-or]");

    public String removeFormatting(String text) {
        return text == null || text.isEmpty() ? null : FORMATTING_CODE_PATTERN.matcher(text).replaceAll("");
    }
    public int multAlpha(int color, float percent01) {
        return getColor(red(color), green(color), blue(color), Math.round(alpha(color) * percent01));
    }
    private int getColor(int red, int green, int blue, int alpha) {
        return ((MathHelper.clamp(alpha, 0, 255) << 24) |
                (MathHelper.clamp(red, 0, 255) << 16) |
                (MathHelper.clamp(green, 0, 255) << 8) |
                MathHelper.clamp(blue, 0, 255));
    }


    public int fade(int index) {

        return Zenith.getInstance().getThemeManager().getClientColor(index).getRGB();
    }
}