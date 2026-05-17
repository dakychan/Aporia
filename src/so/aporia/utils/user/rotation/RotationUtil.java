/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.utils.user.rotation;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Server-side rotation utility with free camera decoupling.
 * <p>
 * Manages two independent rotation sets:
 * <ul>
 *   <li><b>Server rotation</b> — applied to the player entity, sent in packets</li>
 *   <li><b>Client rotation</b> — used only for camera rendering, controlled by mouse</li>
 * </ul>
 */
public class RotationUtil {
    private static final Minecraft mc = Minecraft.getInstance();

    private static float serverYaw, serverPitch;
    private static float clientYaw, clientPitch;
    private static float prevServerYaw, prevServerPitch;
    private static float targetYaw, targetPitch;

    private static boolean active = false;
    private static boolean gcdFix = true;

    /**
     * Initializes rotation state from current player rotation.
     * Call when the module is enabled.
     */
    public static void sync() {
        if (mc.player == null) return;
        serverYaw = mc.player.getYRot();
        serverPitch = mc.player.getXRot();
        clientYaw = serverYaw;
        clientPitch = serverPitch;
        prevServerYaw = serverYaw;
        prevServerPitch = serverPitch;
        active = true;
    }

    /**
     * Updates server rotation towards the target entity with smooth interpolation.
     * Also applies server rotation to the player entity so packets are sent correctly.
     *
     * @param target entity to rotate towards
     * @param speed  maximum degrees per tick
     */
    public static void update(Entity target, float speed) {
        if (mc.player == null || target == null) {
            active = false;
            return;
        }

        active = true;

        Vec3 eyes = mc.player.getEyePosition(1.0F);
        Vec3 targetPos = getPredictedTargetPos(target);
        Vec3 diff = targetPos.subtract(eyes);

        float calcYaw = (float) (Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0);
        float calcPitch = (float) -Math.toDegrees(Math.atan2(diff.y, Math.hypot(diff.x, diff.z)));
        calcPitch = Mth.clamp(calcPitch, -90.0f, 90.0f);

        targetYaw = calcYaw;
        targetPitch = calcPitch;

        float deltaYaw = Mth.wrapDegrees(calcYaw - serverYaw);
        float deltaPitch = Mth.wrapDegrees(calcPitch - serverPitch);

        deltaYaw = Mth.clamp(deltaYaw, -speed, speed);
        deltaPitch = Mth.clamp(deltaPitch, -speed, speed);

        prevServerYaw = serverYaw;
        prevServerPitch = serverPitch;

        serverYaw += deltaYaw;
        serverPitch += deltaPitch;
        serverPitch = Mth.clamp(serverPitch, -90.0f, 90.0f);

        if (gcdFix) {
            serverYaw = applyGcdFix(serverYaw);
            serverPitch = applyGcdFix(serverPitch);
        }

        mc.player.setYRot(serverYaw);
        mc.player.setXRot(serverPitch);
    }

    private static Vec3 getPredictedTargetPos(Entity target) {
        Vec3 pos = target.getEyePosition(1.0F);
        return new Vec3(
            pos.x + target.getDeltaMovement().x * 0.5,
            pos.y + target.getDeltaMovement().y * 0.5,
            pos.z + target.getDeltaMovement().z * 0.5
        );
    }

    private static float applyGcdFix(float angle) {
        float gcd = 0.0001f;
        return Math.round(angle / gcd) * gcd;
    }

    public static float getServerYaw()     { return serverYaw; }
    public static float getServerPitch()   { return serverPitch; }
    public static float getClientYaw()     { return clientYaw; }
    public static float getClientPitch()   { return clientPitch; }
    public static float getTargetYaw()     { return targetYaw; }
    public static float getTargetPitch()   { return targetPitch; }
    public static boolean isActive()       { return active; }

    public static void reset() {
        active = false;
    }

    public static void setGcdFix(boolean enabled) {
        gcdFix = enabled;
    }

    /**
     * Updates client (camera) rotation from mouse input.
     * Called from {@code Entity.turn()} injection.
     */
    public static void turnClientCamera(float dx, float dy) {
        float sensitivity = mc.options.sensitivity().get().floatValue() * 0.6F + 0.2F;
        float factor = sensitivity * sensitivity * sensitivity * 8.0F * 0.15F;

        clientYaw += dx * factor;
        clientPitch += dy * factor;
        clientPitch = Mth.clamp(clientPitch, -90.0f, 90.0f);
    }

    /**
     * Checks if server rotation is within tolerance of the target angle.
     */
    public static boolean isOnTarget(float tolerance) {
        if (!active) return false;
        float yawDiff = Math.abs(Mth.wrapDegrees(serverYaw - targetYaw));
        float pitchDiff = Math.abs(serverPitch - targetPitch);
        return yawDiff <= tolerance && pitchDiff <= tolerance;
    }

    public static float getAngleToTarget() {
        if (!active) return Float.MAX_VALUE;
        float yawDiff = Math.abs(Mth.wrapDegrees(serverYaw - targetYaw));
        float pitchDiff = Math.abs(serverPitch - targetPitch);
        return (float) Math.hypot(yawDiff, pitchDiff);
    }
}
