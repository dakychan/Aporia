package so.aporia.utils.user.player.rotation;

import com.chaos.annotation.Obfuscate;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import java.util.Random;

@Obfuscate
public class RotationUtil {

    public enum RotationMode {
        SMOOTH, SNAP, HVH, MATRIX, VULCAN, GRIM, NCP, INTAVE
    }

    private static final Minecraft mc = Minecraft.getInstance();

    private static float serverYaw, serverPitch;
    private static float clientYaw, clientPitch;
    private static float prevServerYaw, prevServerPitch;
    private static float targetYaw, targetPitch;
    private static float lastDeltaYaw, lastDeltaPitch;
    private static boolean active = false;
    private static boolean gcdFix = true;
    private static final Random RANDOM = new Random();
    private static RotationMode currentMode = RotationMode.SMOOTH;
    private static int tickCounter = 0;
    private static float f5OrbitYaw, f5OrbitPitch;
    private static boolean lastDetached = false;
    private static long lastUpdateTime = 0;

    public static void sync() {
        if (mc.player == null) return;
        serverYaw = mc.player.getYRot();
        serverPitch = mc.player.getXRot();
        clientYaw = serverYaw;
        clientPitch = serverPitch;
        prevServerYaw = serverYaw;
        prevServerPitch = serverPitch;
        lastDeltaYaw = 0;
        lastDeltaPitch = 0;
        f5OrbitYaw = 0;
        f5OrbitPitch = 0;
        lastDetached = false;
        tickCounter = 0;
        active = true;
    }

    public static void setMode(RotationMode mode) {
        currentMode = mode;
    }

    public static RotationMode getMode() {
        return currentMode;
    }

    public static void update(Entity target, float speed) {
        if (mc.player == null || target == null) {
            active = false;
            return;
        }
        active = true;
        lastUpdateTime = System.currentTimeMillis();
        Vec3 eyes = mc.player.getEyePosition(1.0F);
        Vec3 targetPos = predicted(target);
        Vec3 diff = targetPos.subtract(eyes);
        float calcYaw = (float) (Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0);
        float calcPitch = (float) -Math.toDegrees(Math.atan2(diff.y, Math.hypot(diff.x, diff.z)));
        calcPitch = Mth.clamp(calcPitch, -90.0f, 90.0f);
        targetYaw = calcYaw;
        targetPitch = calcPitch;
        prevServerYaw = serverYaw;
        prevServerPitch = serverPitch;
        tickCounter++;
        switch (currentMode) {
            case HVH    -> applyHvh(calcYaw, calcPitch);
            case SNAP   -> applySnap(calcYaw, calcPitch, speed);
            case MATRIX -> applyMatrix(calcYaw, calcPitch, speed);
            case VULCAN -> applyVulcan(calcYaw, calcPitch, speed);
            case GRIM   -> applyGrim(calcYaw, calcPitch, speed);
            case NCP    -> applyNcp(calcYaw, calcPitch, speed);
            case INTAVE -> applyIntave(calcYaw, calcPitch, speed);
            default     -> applySmooth(calcYaw, calcPitch, speed);
        }
    }

    public static void updateRotationOnly(float yaw, float pitch, float speed) {
        active = true;
        lastUpdateTime = System.currentTimeMillis();
        prevServerYaw = serverYaw;
        prevServerPitch = serverPitch;
        targetYaw = yaw;
        targetPitch = pitch;
        tickCounter++;
        switch (currentMode) {
            case HVH    -> applyHvh(yaw, pitch);
            case SNAP   -> applySnap(yaw, pitch, speed);
            case MATRIX -> applyMatrix(yaw, pitch, speed);
            case VULCAN -> applyVulcan(yaw, pitch, speed);
            case GRIM   -> applyGrim(yaw, pitch, speed);
            case NCP    -> applyNcp(yaw, pitch, speed);
            case INTAVE -> applyIntave(yaw, pitch, speed);
            default     -> applySmooth(yaw, pitch, speed);
        }
    }

    // ─── helpers ──────────────────────────────────────────────

    private static float degsPerTick(float speed) {
        return Math.max(speed / 20.0f, 1.0f);
    }

    private static float gauss(float intensity) {
        return (float) (new java.util.Random().nextGaussian() * intensity);
    }

    private static float sanitize(float v) {
        return Math.round(v * 1000000f) / 1000000f;
    }

    private static float gcdFix(float angle, float prev) {
        float sens = mc.options.sensitivity().get().floatValue() * 0.6F + 0.2F;
        float gcd = sens * sens * sens * 8.0F * 0.15F;
        float delta = angle - prev;
        long steps = Math.round(delta / (double) gcd);
        return prev + (float) (steps * (double) gcd);
    }

    private static Vec3 predicted(Entity target) {
        Vec3 center = target.getBoundingBox().getCenter();
        Vec3 motion = target.getDeltaMovement();
        double vy = motion.y;
        if (!target.onGround()) vy -= 0.08 * 0.5;
        double ox = (RANDOM.nextDouble() - 0.5) * 0.2;
        double oy = (RANDOM.nextDouble() - 0.5) * 0.3;
        double oz = (RANDOM.nextDouble() - 0.5) * 0.2;
        return new Vec3(center.x + motion.x * 0.5 + ox, center.y + vy + oy, center.z + motion.z * 0.5 + oz);
    }

    // ─── SMOOTH ───────────────────────────────────────────────

    private static void applySmooth(float yaw, float pitch, float speed) {
        float maxDelta = degsPerTick(speed);
        float dy = Mth.wrapDegrees(yaw - serverYaw);
        float dp = Mth.wrapDegrees(pitch - serverPitch);
        float left = (float) Math.hypot(
            Math.abs(Mth.wrapDegrees(yaw - serverYaw)),
            Math.abs(Mth.wrapDegrees(pitch - serverPitch))
        );
        if (left < maxDelta) {
            float t = left / maxDelta;
            dy *= t; dp *= t;
        } else {
            dy = Mth.clamp(dy, -maxDelta, maxDelta);
            dp = Mth.clamp(dp, -maxDelta, maxDelta);
        }
        serverYaw += dy;
        serverPitch += dp;
        serverPitch = Mth.clamp(serverPitch, -90.0f, 90.0f);
        if (gcdFix) { serverYaw = gcdFix(serverYaw, prevServerYaw); serverPitch = gcdFix(serverPitch, prevServerPitch); }
        lastDeltaYaw = serverYaw - prevServerYaw;
        lastDeltaPitch = serverPitch - prevServerPitch;
        mc.player.setYRot(serverYaw);
        mc.player.setXRot(serverPitch);
    }

    // ─── SNAP ─────────────────────────────────────────────────

    private static void applySnap(float yaw, float pitch, float speed) {
        float maxDelta = degsPerTick(speed);
        float dy = Mth.wrapDegrees(yaw - serverYaw);
        float dp = Mth.wrapDegrees(pitch - serverPitch);
        if (Math.abs(dy) > maxDelta || Math.abs(dp) > maxDelta) {
            applySmooth(yaw, pitch, speed);
            return;
        }
        serverYaw = gcdFix(yaw, prevServerYaw);
        serverPitch = gcdFix(pitch, prevServerPitch);
        lastDeltaYaw = serverYaw - prevServerYaw;
        lastDeltaPitch = serverPitch - prevServerPitch;
        mc.player.setYRot(serverYaw);
        mc.player.setXRot(serverPitch);
    }

    // ─── HVH ──────────────────────────────────────────────────

    private static void applyHvh(float yaw, float pitch) {
        float dy = Mth.wrapDegrees(yaw - serverYaw);
        float dp = Mth.wrapDegrees(pitch - serverPitch);
        float jitterYaw = (tickCounter % 2 == 0) ? 120f : -120f;
        float noisePitch = (float) (Math.sin(tickCounter * 0.4) * 3f + gauss(1.5f));
        serverYaw += dy + jitterYaw;
        serverPitch = Mth.clamp(serverPitch + dp + noisePitch, -90.0f, 90.0f);
        lastDeltaYaw = serverYaw - prevServerYaw;
        lastDeltaPitch = serverPitch - prevServerPitch;
        mc.player.setYRot(serverYaw);
        mc.player.setXRot(serverPitch);
    }

    // ─── MATRIX ───────────────────────────────────────────────

    private static void applyMatrix(float yaw, float pitch, float speed) {
        float maxDelta = degsPerTick(speed) * 0.85f;
        float dy = Mth.clamp(Mth.wrapDegrees(yaw - serverYaw), -maxDelta, maxDelta);
        float dp = Mth.clamp(Mth.wrapDegrees(pitch - serverPitch), -maxDelta * 0.8f, maxDelta * 0.8f);
        if (Math.abs(dy) > 0.1f || Math.abs(dp) > 0.1f) { dy += gauss(0.04f); dp += gauss(0.03f); }
        serverYaw += dy;
        serverPitch += dp;
        serverPitch = Mth.clamp(serverPitch, -90.0f, 90.0f);
        if (gcdFix) { serverYaw = gcdFix(serverYaw, prevServerYaw); serverPitch = gcdFix(serverPitch, prevServerPitch); }
        lastDeltaYaw = serverYaw - prevServerYaw;
        lastDeltaPitch = serverPitch - prevServerPitch;
        mc.player.setYRot(serverYaw);
        mc.player.setXRot(serverPitch);
    }

    // ─── VULCAN ───────────────────────────────────────────────

    private static void applyVulcan(float yaw, float pitch, float speed) {
        float maxDelta = Math.max(degsPerTick(speed) * 0.65f, 1.0f);
        float dy = Mth.clamp(Mth.wrapDegrees(yaw - serverYaw), -maxDelta, maxDelta);
        float dp = Mth.clamp(Mth.wrapDegrees(pitch - serverPitch), -maxDelta * 0.7f, maxDelta * 0.7f);

        // AimPatternC: deltaYaw % 0.5 == 0
        if (Math.abs(dy % 0.5f) < 0.001f && dy != 0) {
            dy += (float) (Math.random() > 0.5 ? 0.01 : -0.01);
        }

        // AimPatternB: yaw > 1.1 при pitch == 0
        if (Math.abs(dy) > 1.1f && Math.abs(dp) < 0.01f) {
            dp += (float) (Math.random() - 0.5) * 0.1f;
        }

        // AimPatternA: точное повторение deltaYaw
        if (Math.abs(dy - lastDeltaYaw) < 0.001f) {
            dy += (float) (Math.random() - 0.5) * 0.01f;
        }
        if (Math.abs(dp - lastDeltaPitch) < 0.001f) {
            dp += (float) (Math.random() - 0.5) * 0.01f;
        }

        // KillAuraM: научная нотация pitch при yaw > 1
        if (Math.abs(dy) > 1.0f && Math.abs(dp) < 0.001f) {
            dp = (float) (Math.random() - 0.5) * 0.02f;
        }

        // sanitize чтобы не было scientific notation
        dy = sanitize(dy);
        dp = sanitize(dp);

        serverYaw += dy;
        serverPitch += dp;
        serverPitch = Mth.clamp(serverPitch, -90.0f, 90.0f);
        if (gcdFix) { serverYaw = gcdFix(serverYaw, prevServerYaw); serverPitch = gcdFix(serverPitch, prevServerPitch); }
        lastDeltaYaw = serverYaw - prevServerYaw;
        lastDeltaPitch = serverPitch - prevServerPitch;
        mc.player.setYRot(serverYaw);
        mc.player.setXRot(serverPitch);
    }

    // ─── GRIM ─────────────────────────────────────────────────

    private static void applyGrim(float yaw, float pitch, float speed) {
        float dy = Mth.wrapDegrees(yaw - serverYaw);
        float dp = Mth.wrapDegrees(pitch - serverPitch);

        // AimModulo360: |dy| > 320 если предыдущий |dy| < 30
        if (Math.abs(lastDeltaYaw) < 30f && Math.abs(dy) > 320f) {
            dy = Math.signum(dy) * 320f;
        }

        serverYaw += dy;
        serverPitch += dp;
        serverPitch = Mth.clamp(serverPitch, -90.0f, 90.0f);
        if (gcdFix) { serverYaw = gcdFix(serverYaw, prevServerYaw); serverPitch = gcdFix(serverPitch, prevServerPitch); }
        lastDeltaYaw = serverYaw - prevServerYaw;
        lastDeltaPitch = serverPitch - prevServerPitch;
        mc.player.setYRot(serverYaw);
        mc.player.setXRot(serverPitch);
    }

    // ─── NCP ──────────────────────────────────────────────────

    private static void applyNcp(float yaw, float pitch, float speed) {
        float maxDelta = degsPerTick(speed);
        float dy = Mth.clamp(Mth.wrapDegrees(yaw - serverYaw), -maxDelta, maxDelta);
        float dp = Mth.clamp(Mth.wrapDegrees(pitch - serverPitch), -maxDelta, maxDelta);
        serverYaw += dy;
        serverPitch += dp;
        serverPitch = Mth.clamp(serverPitch, -90.0f, 90.0f);
        if (gcdFix) { serverYaw = gcdFix(serverYaw, prevServerYaw); serverPitch = gcdFix(serverPitch, prevServerPitch); }
        lastDeltaYaw = serverYaw - prevServerYaw;
        lastDeltaPitch = serverPitch - prevServerPitch;
        mc.player.setYRot(serverYaw);
        mc.player.setXRot(serverPitch);
    }

    // ─── INTAVE ───────────────────────────────────────────────

    private static void applyIntave(float yaw, float pitch, float speed) {
        float maxDelta = Math.max(degsPerTick(speed) * 0.7f, 1.0f);
        float dy = Mth.wrapDegrees(yaw - serverYaw);
        float dp = Mth.wrapDegrees(pitch - serverPitch);

        // RotationSnap: |prev| < 9 -> |current| > 40
        if (Math.abs(lastDeltaYaw) < 9f && Math.abs(dy) > 40f) {
            dy = Math.signum(dy) * Math.min(Math.abs(dy), maxDelta * 2f);
        }

        dy = Mth.clamp(dy, -maxDelta, maxDelta);
        dp = Mth.clamp(dp, -maxDelta * 0.7f, maxDelta * 0.7f);

        // RotationModuloReset: >100° за 1 тик к цели
        if (Math.abs(dy) > 100f) {
            dy = Math.signum(dy) * 100f;
        }

        // GCD флуктуация pitch — Intave проверяет GCD pitch дельт
        // Случайный шум 0-0.15° ломает стабильный GCD
        dp += (float) (Math.random() - 0.5) * 0.15f;

        // RotationSD: yaw SD < 1.0 pitch SD < 3.0 от perfect
        // Добавляем шум 0-3° по yaw, 0-5° по pitch
        if (Math.abs(dy) > 0.5f) {
            dy += (float) (Math.random() - 0.5) * 3.0f;
        }
        if (Math.abs(dp) > 0.5f) {
            dp += (float) (Math.random() - 0.5) * 5.0f;
        }

        // RotationExact: distanceToPerfectYaw/Pitch == 0
        if (Math.abs(dy) < 0.01f) {
            dy = (float) (Math.random() - 0.5) * 0.15f;
        }
        if (Math.abs(dp) < 0.01f) {
            dp = (float) (Math.random() - 0.5) * 0.15f;
        }

        // Random pause 1 tick раз в 50-80 тиков — ломает механический паттерн
        if (tickCounter % (50 + (int)(Math.random() * 30)) == 0 && Math.random() < 0.2) {
            mc.player.setYRot(serverYaw);
            mc.player.setXRot(serverPitch);
            lastDeltaYaw = 0;
            lastDeltaPitch = 0;
            return;
        }

        dy = sanitize(dy);
        dp = sanitize(dp);

        serverYaw += dy;
        serverPitch += dp;
        serverPitch = Mth.clamp(serverPitch, -90.0f, 90.0f);
        if (gcdFix) { serverYaw = gcdFix(serverYaw, prevServerYaw); serverPitch = gcdFix(serverPitch, prevServerPitch); }
        lastDeltaYaw = serverYaw - prevServerYaw;
        lastDeltaPitch = serverPitch - prevServerPitch;
        mc.player.setYRot(serverYaw);
        mc.player.setXRot(serverPitch);
    }

    // ─── public API ───────────────────────────────────────────

    public static float getServerYaw()     { return serverYaw; }
    public static float getServerPitch()   { return serverPitch; }
    public static float getClientYaw()     { return clientYaw; }
    public static float getClientPitch()   { return clientPitch; }
    public static float getF5OrbitYaw()    { return f5OrbitYaw; }
    public static float getF5OrbitPitch()  { return f5OrbitPitch; }
    public static float getTargetYaw()     { return targetYaw; }
    public static float getTargetPitch()   { return targetPitch; }

    public static void onCameraSetup(boolean detached) {
        if (detached && !lastDetached) {
            f5OrbitYaw = 0;
            f5OrbitPitch = 0;
        }
        lastDetached = detached;
    }

    public static boolean isActive() {
        if (!active) return false;
        if (System.currentTimeMillis() - lastUpdateTime > 150) {
            active = false;
            return false;
        }
        return true;
    }

    public static void reset() {
        active = false;
        tickCounter = 0;
        lastDeltaYaw = 0;
        lastDeltaPitch = 0;
        f5OrbitYaw = 0;
        f5OrbitPitch = 0;
        lastDetached = false;
    }

    public static void deactivate() {
        active = false;
        lastDetached = false;
    }

    public static void setGcdFix(boolean enabled) {
        gcdFix = enabled;
    }

    public static void turnClientCamera(float dx, float dy) {
        float sensitivity = mc.options.sensitivity().get().floatValue() * 0.6F + 0.2F;
        float factor = sensitivity * sensitivity * sensitivity * 8.0F * 0.15F;
        clientYaw += dx * factor;
        clientPitch += dy * factor;
        clientPitch = Mth.clamp(clientPitch, -90.0f, 90.0f);
        f5OrbitYaw += dx * factor;
        f5OrbitPitch += dy * factor;
        f5OrbitPitch = Mth.clamp(f5OrbitPitch, -90.0f, 90.0f);
    }

    public static boolean isOnTarget(float tolerance) {
        if (!active) return false;
        return Math.abs(Mth.wrapDegrees(serverYaw - targetYaw)) <= tolerance
            && Math.abs(serverPitch - targetPitch) <= tolerance;
    }

    public static float getAngleToTarget() {
        if (!active) return Float.MAX_VALUE;
        return (float) Math.hypot(
            Math.abs(Mth.wrapDegrees(serverYaw - targetYaw)),
            Math.abs(serverPitch - targetPitch)
        );
    }
}
