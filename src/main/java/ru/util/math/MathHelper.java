package ru.util.math;

import java.math.BigDecimal;
import java.math.RoundingMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;

public class MathHelper {
    public static MinecraftClient mc = MinecraftClient.getInstance();

    public static double interpolate(double current, double old, double scale) {
        return old + (current - old) * scale;
    }

    public static float interpolate(float current, float old, double scale) {
        return (float) interpolate((double) current, (double) old, scale);
    }

    public static int interpolate(int current, int old, double scale) {
        return (int) interpolate((double) current, (double) old, scale);
    }

    public static float lerp(float a, float b, float f) {
        return a + f * (b - a);
    }

    public static double lerp(double current, double old, double scale) {
        return current + (old - current) * clamp((float) scale, 0.0F, 1.0F);
    }

    public static float clamp(float value, float min, float max) {
        if (value < min) {
            return min;
        } else {
            return value > max ? max : value;
        }
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static int clampI(int val, float min, int max) {
        if (val <= min) {
            val = (int) min;
        }
        if (val >= max) {
            val = max;
        }
        return val;
    }

    public static float clampF(float val, float min, float max) {
        if (val <= min) {
            val = min;
        }
        if (val >= max) {
            val = max;
        }
        return val;
    }

    public static float clamp01(float x) {
        return (float) clamp(x, 0.0, 1.0);
    }

    public static boolean isHovered(float mouseX, float mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }

    public static float normalize(float value, float min, float max) {
        return (value - min) / (max - min);
    }

    public static float map(float value, float istart, float istop, float ostart, float ostop) {
        return ostart + (ostop - ostart) * (value - istart) / (istop - istart);
    }

    public static double round(double value, int places) {
        if (places < 0) {
            throw new IllegalArgumentException();
        }
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public static double roundToPlace(double value, int places) {
        if (places < 0) {
            throw new IllegalArgumentException();
        }
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public static float roundToDecimal(float value, int decimalPlaces) {
        if (decimalPlaces < 0) {
            throw new IllegalArgumentException("Decimal places must be non-negative");
        }
        double multiplier = Math.pow(10.0, decimalPlaces);
        return (float) (Math.round(value * multiplier) / multiplier);
    }

    public static double random(double min, double max) {
        return Math.random() * (max - min) + min;
    }

    public static float random(float min, float max) {
        return (float) (Math.random() * (max - min) + min);
    }

    public static int randomInt(int min, int max) {
        return (int) (Math.random() * (max - min + 1)) + min;
    }

    public static float wrapDegrees(float value) {
        value = value % 360.0F;
        if (value >= 180.0F) {
            value -= 360.0F;
        }
        if (value < -180.0F) {
            value += 360.0F;
        }
        return value;
    }

    public static int getCenter(int width, int rectWidth) {
        return width / 2 - rectWidth / 2;
    }

    public static int calc(int value) {
        Window mainWindow = MinecraftClient.getInstance().getWindow();
        return (int) ((double) value * mainWindow.getScaleFactor() / 2);
    }

    public static double deltaTime() {
        return MinecraftClient.getInstance().getCurrentFps() > 0 
            ? 1.0 / MinecraftClient.getInstance().getCurrentFps() 
            : 1.0;
    }

    public static float fast(float end, float start, float multiple) {
        return (1.0F - clamp((float) (deltaTime() * multiple), 0.0F, 1.0F)) * end 
            + clamp((float) (deltaTime() * multiple), 0.0F, 1.0F) * start;
    }

    public static float scaleValue(float value, float minInput, float maxInput, float minOutput, float maxOutput) {
        if (maxInput - minInput == 0.0F) {
            throw new IllegalArgumentException("Input range cannot be zero");
        }
        float scaledValue = (value - minInput) / (maxInput - minInput) * (maxOutput - minOutput) + minOutput;
        return Math.max(minOutput, Math.min(maxOutput, scaledValue));
    }

    public static float calcPercentage(float value, float min, float max) {
        if (value < min || value > max) {
            return 0.0F;
        }
        float range = max - min;
        return (value - min) / range * 100.0F;
    }

    public static float calculateValue(float percentage, float min, float max) {
        if (percentage < 0.0F || percentage > 100.0F) {
            return 0.0F;
        }
        float range = max - min;
        return percentage / 100.0F * range + min;
    }
}
