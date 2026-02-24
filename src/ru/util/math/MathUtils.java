package ru.util.math;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public class MathUtils {
   private static final double GRAVITY = 0.08;
   private static final double DRAG = 0.99;
   private static Map<Item, Integer> consumptionTimes = null;

   private static void initializeConsumptionTimes() {
      if (consumptionTimes == null) {
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
         } catch (Exception var1) {
            consumptionTimes = new HashMap<>();
         }
      }
   }

   public static List<Vec3> calculateElytraTrajectory(Vec3 position, Vec3 velocity, float pitch, float yaw) {
      if (position != null && velocity != null) {
         List<Vec3> trajectory = new ArrayList<>();
         Vec3 currentPos = position;
         Vec3 currentVel = velocity;

         for (int tick = 0; tick < 200; tick++) {
            trajectory.add(currentPos);
            Vec3 var8 = currentVel.scale(0.99);
            currentVel = var8.subtract(0.0, 0.08, 0.0);
            currentPos = currentPos.add(currentVel);
            if (currentVel.lengthSqr() < 0.001) {
               break;
            }
         }

         return trajectory;
      } else {
         return new ArrayList<>();
      }
   }

   public static double calculateElytraMaxDistance(Vec3 position, Vec3 velocity) {
      if (position != null && velocity != null) {
         List<Vec3> trajectory = calculateElytraTrajectory(position, velocity, 0.0F, 0.0F);
         if (trajectory.isEmpty()) {
            return 0.0;
         } else {
            Vec3 start = trajectory.get(0);
            Vec3 end = trajectory.get(trajectory.size() - 1);
            double dx = end.x - start.x;
            double dz = end.z - start.z;
            return Math.sqrt(dx * dx + dz * dz);
         }
      } else {
         return 0.0;
      }
   }

   public static Vec3 predictElytraPosition(Vec3 position, Vec3 velocity, int ticks) {
      if (position != null && velocity != null && ticks >= 0) {
         Vec3 currentPos = position;
         Vec3 currentVel = velocity;

         for (int tick = 0; tick < ticks; tick++) {
            Vec3 var6 = currentVel.scale(0.99);
            currentVel = var6.subtract(0.0, 0.08, 0.0);
            currentPos = currentPos.add(currentVel);
         }

         return currentPos;
      } else {
         return position != null ? position : new Vec3(0.0, 0.0, 0.0);
      }
   }

   public static double calculateDistance(Vec3 pos1, Vec3 pos2) {
      if (pos1 != null && pos2 != null) {
         double dx = pos2.x - pos1.x;
         double dy = pos2.y - pos1.y;
         double dz = pos2.z - pos1.z;
         return Math.sqrt(dx * dx + dy * dy + dz * dz);
      } else {
         return 0.0;
      }
   }

   public static Map<AbstractClientPlayer, Double> calculateDistancesToPlayers(LocalPlayer localPlayer) {
      Map<AbstractClientPlayer, Double> distances = new HashMap<>();
      if (localPlayer != null && localPlayer.level() != null) {
         Vec3 localPos = localPlayer.position();

         for (Object playerObj : localPlayer.level().players()) {
            if (playerObj instanceof AbstractClientPlayer player && !player.equals(localPlayer)) {
               Vec3 playerPos = player.position();
               double distance = calculateDistance(localPos, playerPos);
               distances.put(player, distance);
            }
         }

         return distances;
      } else {
         return distances;
      }
   }

   public static AbstractClientPlayer findNearestPlayer(LocalPlayer localPlayer) {
      if (localPlayer != null && localPlayer.level() != null) {
         Map<AbstractClientPlayer, Double> distances = calculateDistancesToPlayers(localPlayer);
         if (distances.isEmpty()) {
            return null;
         } else {
            AbstractClientPlayer nearestPlayer = null;
            double minDistance = Double.MAX_VALUE;

            for (Entry<AbstractClientPlayer, Double> entry : distances.entrySet()) {
               if (entry.getValue() < minDistance) {
                  minDistance = entry.getValue();
                  nearestPlayer = entry.getKey();
               }
            }

            return nearestPlayer;
         }
      } else {
         return null;
      }
   }

   public static int getConsumptionTime(ItemStack item) {
      if (item != null && !item.isEmpty()) {
         initializeConsumptionTimes();
         Item itemType = item.getItem();
         return consumptionTimes.getOrDefault(itemType, 32);
      } else {
         return 0;
      }
   }

   public static int getRemainingTime(LocalPlayer player) {
      return player != null && player.isUsingItem() ? player.getTicksUsingItem() : 0;
   }

   public static float getConsumptionProgress(LocalPlayer player) {
      if (player != null && player.isUsingItem()) {
         ItemStack activeItem = player.getActiveItem();
         int maxUseTime = activeItem.getUseDuration(player);
         int timeLeft = player.getTicksUsingItem();
         if (maxUseTime <= 0) {
            return 0.0F;
         } else {
            int timeUsed = maxUseTime - timeLeft;
            return (float)timeUsed / maxUseTime;
         }
      } else {
         return 0.0F;
      }
   }
}
