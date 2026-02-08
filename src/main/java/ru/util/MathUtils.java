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

/**
 * Mathematical utility class for Minecraft client calculations.
 * Provides elytra flight physics, distance calculations, and consumable item timing.
 */
public class MathUtils {
    
    // Elytra physics constants
    private static final double GRAVITY = 0.08;
    private static final double DRAG = 0.99;
    
    // Consumable item timing constants (in ticks)
    // Using lazy initialization to avoid static initialization issues in tests
    private static Map<Item, Integer> consumptionTimes = null;
    
    /**
     * Initializes the consumption times map if not already initialized.
     * This is done lazily to avoid issues with Minecraft class loading in tests.
     */
    private static void initializeConsumptionTimes() {
        if (consumptionTimes != null) {
            return;
        }
        
        consumptionTimes = new HashMap<>();
        
        try {
            // Food items - 32 ticks (1.6 seconds)
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
            
            // Potions - 32 ticks (1.6 seconds)
            consumptionTimes.put(Items.POTION, 32);
            consumptionTimes.put(Items.SPLASH_POTION, 32);
            consumptionTimes.put(Items.LINGERING_POTION, 32);
        } catch (Exception e) {
            // If Items class is not available (e.g., in tests), just use empty map
            consumptionTimes = new HashMap<>();
        }
    }
    
    /**
     * Calculates the elytra flight trajectory from a given position and velocity.
     * Simulates physics including gravity and air resistance.
     * 
     * @param position Starting position
     * @param velocity Starting velocity
     * @param pitch Player pitch angle (not currently used in basic simulation)
     * @param yaw Player yaw angle (not currently used in basic simulation)
     * @return List of Vec3d points representing the flight path
     */
    public static List<Vec3d> calculateElytraTrajectory(Vec3d position, Vec3d velocity, float pitch, float yaw) {
        if (position == null || velocity == null) {
            return new ArrayList<>();
        }
        
        List<Vec3d> trajectory = new ArrayList<>();
        Vec3d currentPos = position;
        Vec3d currentVel = velocity;
        
        // Simulate up to 200 ticks (10 seconds) or until velocity is very low
        for (int tick = 0; tick < 200; tick++) {
            trajectory.add(currentPos);
            
            // Apply drag to velocity
            currentVel = currentVel.multiply(DRAG);
            
            // Apply gravity (downward acceleration)
            currentVel = currentVel.subtract(0, GRAVITY, 0);
            
            // Update position
            currentPos = currentPos.add(currentVel);
            
            // Stop if velocity becomes negligible (player would hit ground)
            if (currentVel.lengthSquared() < 0.001) {
                break;
            }
        }
        
        return trajectory;
    }
    
    /**
     * Calculates the maximum horizontal distance an elytra flight can cover.
     * 
     * @param position Starting position
     * @param velocity Starting velocity
     * @return Maximum distance in blocks
     */
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
        
        // Calculate horizontal distance (ignore Y component)
        double dx = end.x - start.x;
        double dz = end.z - start.z;
        
        return Math.sqrt(dx * dx + dz * dz);
    }
    
    /**
     * Predicts the elytra position after a given number of ticks.
     * 
     * @param position Starting position
     * @param velocity Starting velocity
     * @param ticks Number of ticks to simulate
     * @return Predicted position after the specified ticks
     */
    public static Vec3d predictElytraPosition(Vec3d position, Vec3d velocity, int ticks) {
        if (position == null || velocity == null || ticks < 0) {
            return position != null ? position : new Vec3d(0, 0, 0);
        }
        
        Vec3d currentPos = position;
        Vec3d currentVel = velocity;
        
        for (int tick = 0; tick < ticks; tick++) {
            // Apply drag to velocity
            currentVel = currentVel.multiply(DRAG);
            
            // Apply gravity (downward acceleration)
            currentVel = currentVel.subtract(0, GRAVITY, 0);
            
            // Update position
            currentPos = currentPos.add(currentVel);
        }
        
        return currentPos;
    }
    
    /**
     * Calculates the Euclidean distance between two positions in 3D space.
     * 
     * @param pos1 First position
     * @param pos2 Second position
     * @return Distance in blocks, or 0.0 if either position is null
     */
    public static double calculateDistance(Vec3d pos1, Vec3d pos2) {
        if (pos1 == null || pos2 == null) {
            return 0.0;
        }
        
        double dx = pos2.x - pos1.x;
        double dy = pos2.y - pos1.y;
        double dz = pos2.z - pos1.z;
        
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    /**
     * Calculates distances from the local player to all other players in the world.
     * 
     * @param localPlayer The local client player
     * @return Map of players to their distances from the local player
     */
    public static Map<AbstractClientPlayerEntity, Double> calculateDistancesToPlayers(ClientPlayerEntity localPlayer) {
        Map<AbstractClientPlayerEntity, Double> distances = new HashMap<>();
        
        if (localPlayer == null || localPlayer.getEntityWorld() == null) {
            return distances;
        }
        
        Vec3d localPos = localPlayer.getEntityPos();
        
        // Get all players from the world
        for (Object playerObj : localPlayer.getEntityWorld().getPlayers()) {
            if (playerObj instanceof AbstractClientPlayerEntity) {
                AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) playerObj;
                
                // Skip the local player
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
    
    /**
     * Finds the nearest player to the local player.
     * 
     * @param localPlayer The local client player
     * @return The nearest player, or null if no other players exist
     */
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
    
    /**
     * Gets the consumption time for a given item.
     * 
     * @param item The item stack to check
     * @return Consumption time in ticks, or 32 (default) if not in the map
     */
    public static int getConsumptionTime(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return 0;
        }
        
        initializeConsumptionTimes();
        
        Item itemType = item.getItem();
        return consumptionTimes.getOrDefault(itemType, 32);
    }
    
    /**
     * Gets the remaining consumption time for a player currently using an item.
     * 
     * @param player The player to check
     * @return Remaining time in ticks, or 0 if not consuming
     */
    public static int getRemainingTime(ClientPlayerEntity player) {
        if (player == null || !player.isUsingItem()) {
            return 0;
        }
        
        return player.getItemUseTimeLeft();
    }
    
    /**
     * Gets the consumption progress for a player currently using an item.
     * 
     * @param player The player to check
     * @return Progress from 0.0 (just started) to 1.0 (complete), or 0.0 if not consuming
     */
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
