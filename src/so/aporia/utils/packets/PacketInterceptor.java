/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.utils.packets;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import so.aporia.utils.events.impl.PacketEvent;
import so.aporia.utils.user.rotation.RotationUtil;

/**
 * Intercepts outbound player movement packets and replaces rotation values
 * with server-side rotations from {@link RotationUtil}.
 * <p>
 * Handles position correction when yaw differs significantly to prevent
 * server-side movement desync.
 */
public class PacketInterceptor {
    private static final Minecraft mc = Minecraft.getInstance();

    private static double lastSentX, lastSentY, lastSentZ;
    private static float lastSentYaw;
    private static boolean hasLastSent = false;
    private static boolean sending = false;

    public static void onPacketSend(PacketEvent event) {
        if (sending) return;
        if (event.direction() != PacketEvent.Direction.OUTBOUND) return;
        if (!RotationUtil.isActive()) return;
        if (!(event.packet() instanceof ServerboundMovePlayerPacket packet)) return;
        if (mc.player == null) return;

        float serverYaw = RotationUtil.getServerYaw();
        float serverPitch = RotationUtil.getServerPitch();

        if (!hasLastSent) {
            lastSentX = mc.player.getX();
            lastSentY = mc.player.getY();
            lastSentZ = mc.player.getZ();
            lastSentYaw = mc.player.getYRot();
            hasLastSent = true;
        }

        double currentX = mc.player.getX();
        double currentY = mc.player.getY();
        double currentZ = mc.player.getZ();

        double deltaX = currentX - lastSentX;
        double deltaZ = currentZ - lastSentZ;

        float realYaw = mc.player.getYRot();
        float yawDiff = Mth.wrapDegrees(serverYaw - realYaw);

        if (packet.hasPosition() && packet.hasRotation()) {
            if (Math.abs(yawDiff) > 0.5f) {
                double rad = Math.toRadians(yawDiff);
                double cos = Math.cos(rad);
                double sin = Math.sin(rad);

                double rotatedDx = deltaX * cos - deltaZ * sin;
                double rotatedDz = deltaX * sin + deltaZ * cos;

                double newX = lastSentX + rotatedDx;
                double newZ = lastSentZ + rotatedDz;

                lastSentX = newX;
                lastSentY = currentY;
                lastSentZ = newZ;
                lastSentYaw = serverYaw;

                event.cancel();
                sending = true;
                mc.player.connection.send(new ServerboundMovePlayerPacket.PosRot(
                    newX, currentY, newZ,
                    serverYaw, serverPitch,
                    packet.isOnGround(), packet.horizontalCollision()
                ));
                sending = false;
            } else {
                event.cancel();
                sending = true;
                mc.player.connection.send(new ServerboundMovePlayerPacket.PosRot(
                    currentX, currentY, currentZ,
                    serverYaw, serverPitch,
                    packet.isOnGround(), packet.horizontalCollision()
                ));
                sending = false;

                lastSentX = currentX;
                lastSentY = currentY;
                lastSentZ = currentZ;
                lastSentYaw = serverYaw;
            }
        } else if (packet.hasRotation()) {
            event.cancel();
            sending = true;
            mc.player.connection.send(new ServerboundMovePlayerPacket.Rot(
                serverYaw, serverPitch,
                packet.isOnGround(), packet.horizontalCollision()
            ));
            sending = false;

            lastSentYaw = serverYaw;
        } else if (packet.hasPosition()) {
            lastSentX = currentX;
            lastSentY = currentY;
            lastSentZ = currentZ;
        }
    }

    public static void reset() {
        hasLastSent = false;
        lastSentX = 0;
        lastSentY = 0;
        lastSentZ = 0;
        lastSentYaw = 0;
    }
}
