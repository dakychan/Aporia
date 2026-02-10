package ru.util;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MathUtils {
    private static final double GRAVITY = 0.08;
    private static final double DRAG = 0.99;
    private static Map<Item, Integer> consumptionTimes = null;

    private static void initializeConsumptionTimes() {
        if (consumptionTimes != null) {
            return;
        }
        
        consumptionTimes = new HashMap<>();
        
        try {
            consumptionTimes.put(Items.APPLE, 32);
            consumptionTimes.put(Items.BREAD, 32);
            consumptionTimes.put(Items.COOKED_PORKCHOP, 32);
            consumptionTimes.put(Items.COOKED_BEEF, 32);
            consumptionTimes.put(Items.COOKED_CHICKEN, 32);
            consumptionTimes.put(Items.COOKED_COD, 32);
            consumptionTimes.put(Items.COOKED_SALMON, 32);
            consumptionTimes.put(Items.COOKED_MUTTON, 32);
            consumptionTimes.put(Items.COOKED_RABBIT, 32);
            consumptionTimes.put(Items.GOLDEN_APPLE, 32);
            consumptionTimes.put(Items.ENCHANTED_GOLDEN_APPLE, 32);
            consumptionTimes.put(Items.POTION, 32);
            consumptionTimes.put(Items.SPLASH_POTION, 32);
            consumptionTimes.put(Items.LINGERING_POTION, 32);
        } catch (Exception e) {
            consumptionTimes = new HashMap<>();
        }
    }

    public static List<Vec3d> calculateElytraTrajectory(Vec3d position, Vec3d velocity, float pitch, float yaw) {
        if (position == null || velocity == null) {
            return new ArrayList<>();
        }
        
        List<Vec3d> trajectory = new ArrayList<>();
        Vec3d currentPos = position;
        Vec3d currentVel = velocity;

        for (int tick = 0; tick < 200; tick++) {
            trajectory.add(currentPos);

            currentVel = currentVel.multiply(DRAG);

            currentVel = currentVel.subtract(0, GRAVITY, 0);

            currentPos = currentPos.add(currentVel);

            if (currentVel.lengthSquared() < 0.001) {
                break;
            }
        }
        
        return trajectory;
    }

    public static double calculateElytraMaxDistance(Vec3d position, Vec3d velocity) {
        if (position == null || velocity == null) {
            return 0.0;
        }
        
        List<Vec3d> trajectory = calculateElytraTrajectory(position, velocity, 0, 0);
        
        if (trajectory.isEmpty()) {
            return 0.0;
        }
        
        Vec3d start = trajectory.get(0);
        Vec3d end = trajectory.get(trajectory.size() - 1);
        double dx = end.x - start.x;
        double dz = end.z - start.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    public static Vec3d predictElytraPosition(Vec3d position, Vec3d velocity, int ticks) {
        if (position == null || velocity == null || ticks < 0) {
            return position != null ? position : new Vec3d(0, 0, 0);
        }
        
        Vec3d currentPos = position;
        Vec3d currentVel = velocity;
        
        for (int tick = 0; tick < ticks; tick++) {
            currentVel = currentVel.multiply(DRAG);

            currentVel = currentVel.subtract(0, GRAVITY, 0);

            currentPos = currentPos.add(currentVel);
        }
        
        return currentPos;
    }

    public static double calculateDistance(Vec3d pos1, Vec3d pos2) {
        if (pos1 == null || pos2 == null) {
            return 0.0;
        }
        
        double dx = pos2.x - pos1.x;
        double dy = pos2.y - pos1.y;
        double dz = pos2.z - pos1.z;
        
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static Map<AbstractClientPlayerEntity, Double> calculateDistancesToPlayers(ClientPlayerEntity localPlayer) {
        Map<AbstractClientPlayerEntity, Double> distances = new HashMap<>();
        
        if (localPlayer == null || localPlayer.getEntityWorld() == null) {
            return distances;
        }
        
        Vec3d localPos = localPlayer.getEntityPos();

        for (Object playerObj : localPlayer.getEntityWorld().getPlayers()) {
            if (playerObj instanceof AbstractClientPlayerEntity) {
                AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) playerObj;
                if (player.equals(localPlayer)) {
                    continue;
                }
                Vec3d playerPos = player.getEntityPos();
                double distance = calculateDistance(localPos, playerPos);
                distances.put(player, distance);
            }
        }
        
        return distances;
    }

    public static AbstractClientPlayerEntity findNearestPlayer(ClientPlayerEntity localPlayer) {
        if (localPlayer == null || localPlayer.getEntityWorld() == null) {
            return null;
        }
        
        Map<AbstractClientPlayerEntity, Double> distances = calculateDistancesToPlayers(localPlayer);
        
        if (distances.isEmpty()) {
            return null;
        }
        
        AbstractClientPlayerEntity nearestPlayer = null;
        double minDistance = Double.MAX_VALUE;
        
        for (Map.Entry<AbstractClientPlayerEntity, Double> entry : distances.entrySet()) {
            if (entry.getValue() < minDistance) {
                minDistance = entry.getValue();
                nearestPlayer = entry.getKey();
            }
        }
        
        return nearestPlayer;
    }

    public static int getConsumptionTime(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return 0;
        }
        
        initializeConsumptionTimes();
        
        Item itemType = item.getItem();
        return consumptionTimes.getOrDefault(itemType, 32);
    }

    public static int getRemainingTime(ClientPlayerEntity player) {
        if (player == null || !player.isUsingItem()) {
            return 0;
        }
        
        return player.getItemUseTimeLeft();
    }

    public static float getConsumptionProgress(ClientPlayerEntity player) {
        if (player == null || !player.isUsingItem()) {
            return 0.0f;
        }
        
        ItemStack activeItem = player.getActiveItem();
        int maxUseTime = activeItem.getMaxUseTime(player);
        int timeLeft = player.getItemUseTimeLeft();
        
        if (maxUseTime <= 0) {
            return 0.0f;
        }
        
        int timeUsed = maxUseTime - timeLeft;
        return (float) timeUsed / maxUseTime;
    }
}
