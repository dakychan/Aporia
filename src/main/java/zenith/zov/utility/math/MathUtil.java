package zenith.zov.utility.math;

import lombok.experimental.UtilityClass;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3d;
import zenith.zov.utility.interfaces.IMinecraft;

import java.util.concurrent.ThreadLocalRandom;

import static net.minecraft.util.math.MathHelper.lerp;

@UtilityClass
public class MathUtil implements IMinecraft {
    public double PI2 = Math.PI * 2;
    private static final int TABLE_SIZE = 1 << 16; // 65536
    private static final double TWO_PI = Math.PI * 2.0;
    private static final double[] TRIG_TABLE = new double[TABLE_SIZE];

    static {
        for (int i = 0; i < TABLE_SIZE; i++) {
            TRIG_TABLE[i] = Math.sin((double) i * TWO_PI / TABLE_SIZE);
        }
    }

    public static double sin(double radians) {
        int index = (int) (radians * (TABLE_SIZE / TWO_PI)) & (TABLE_SIZE - 1);
        return TRIG_TABLE[index];
    }

    public static double cos(double radians) {
        int index = (int) (radians * (TABLE_SIZE / TWO_PI) + TABLE_SIZE / 4.0) & (TABLE_SIZE - 1);
        return TRIG_TABLE[index];
    }


    public float random(double min, double max) {
        return (float) (min + (max - min) * Math.random());
    }


    public double cubicBezier(double t, double p0, double p1, double p2, double p3) {
        return Math.pow(1 - t, 3) * p0 +
                3 * t * Math.pow(1 - t, 2) * p1 +
                3 * Math.pow(t, 2) * (1 - t) * p2 +
                Math.pow(t, 3) * p3;
    }

    public int levenshtein(String a, String b) {
        int n = a.length(), m = b.length();
        int[] dp = new int[m + 1];

        for (int j = 0; j <= m; j++) dp[j] = j;
        for (int i = 1; i <= n; i++) {
            int prev = dp[0];
            dp[0] = i;
            for (int j = 1; j <= m; j++) {
                int tmp = dp[j];
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[j] = Math.min(Math.min(dp[j] + 1, dp[j - 1] + 1), prev + cost);
                prev = tmp;
            }
        }
        return dp[m];
    }

    public float angleDifference(float angle1, float angle2) {
        float diff = (angle1 - angle2) % 360;
        if (diff < -180) {
            diff += 360;
        } else if (diff > 180) {
            diff -= 360;
        }
        return diff;
    }

    public static boolean isHovered(double mouseX, double mouseY, double x, double y, double width, double height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public boolean isHoveredByCords(double mouseX, double mouseY, int x, int y, int xEnd, int yEnd) {
        return mouseX >= x && mouseX <= xEnd && mouseY >= y && mouseY <= yEnd;
    }

    public float interpolate(double oldValue, double newValue, double interpolationValue){
        return (float) (oldValue + (newValue - oldValue) * interpolationValue);
        //  MathHelper.lerp(interpolationValue, oldValue, newValue);
    }

    public float goodSubtract(float value1, float value2) {
        return Math.abs(value1 - value2);
    }

    public double getRandom(double min, double max) {
        if (min == max) {
            return min;
        } else if (min > max) {
            final double d = min;
            min = max;
            max = d;
        }
        return ThreadLocalRandom.current().nextDouble() * (max - min) + min;
    }

    public float round(float value) {
        return Math.round(value * 10f) / 10f;
    }
    public double round(double num, double increment) {
        double rounded = Math.round(num / increment) * increment;
        return Math.round(rounded * 100.0) / 100.0;
    }

    public Vec3d cosSin(int i, int size, double width) {
        int index = Math.min(i, size);
        float cos = (float) (Math.cos(index * MathUtil.PI2 / size) * width);
        float sin = (float) (-Math.sin(index * MathUtil.PI2 / size) * width);
        return new Vec3d(cos, 0, sin);
    }
    public Vector3d interpolate(Vector3d prevPos, Vector3d pos) {
        return new Vector3d(interpolate(prevPos.x, pos.x), interpolate(prevPos.y, pos.y), interpolate(prevPos.z, pos.z));
    }

    public Vec3d interpolate(Vec3d prevPos, Vec3d pos) {
        return new Vec3d(interpolate(prevPos.x, pos.x), interpolate(prevPos.y, pos.y), interpolate(prevPos.z, pos.z));
    }

    public Vec3d interpolate(Entity entity) {
        if (entity == null) return Vec3d.ZERO;
        return new Vec3d(interpolate(entity.lastX, entity.getX()), interpolate(entity.lastY, entity.getY()), interpolate(entity.lastZ, entity.getZ()));
    }

    public float interpolate(float prev, float orig) {
        return lerp(mc.getRenderTickCounter().getTickProgress(false), prev, orig);
    }

    public double interpolate(double prev, double orig) {
        return lerp(mc.getRenderTickCounter().getTickProgress(false), prev, orig);
    }

    public int interpolateSmooth(double smooth, int prev, int orig) {
        return (int) lerp(mc.getRenderTickCounter().getDynamicDeltaTicks() / smooth, prev, orig);
    }

    public float interpolateSmooth(double smooth, float prev, float orig) {
        return (float) lerp(mc.getRenderTickCounter().getDynamicDeltaTicks() / smooth, prev, orig);
    }

    public double interpolateSmooth(double smooth, double prev, double orig) {
        return lerp(mc.getRenderTickCounter().getDynamicDeltaTicks() / smooth, prev, orig);
    }

    public double getDistance(Vec3d pos1, Vec3d pos2) {
        double deltaX = pos1.getX() - pos2.getX();
        double deltaY = pos1.getY() - pos2.getY();
        double deltaZ = pos1.getZ() - pos2.getZ();
        return MathHelper.sqrt((float) (deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ));
    }
}
