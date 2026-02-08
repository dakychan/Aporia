package ru.util;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MathUtils elytra calculations.
 */
class MathUtilsTest {
    
    private static final double EPSILON = 0.001;
    
    @Test
    void testCalculateElytraTrajectory_withValidInputs_returnsNonEmptyList() {
        Vec3d position = new Vec3d(0, 100, 0);
        Vec3d velocity = new Vec3d(1, 0, 0);
        
        List<Vec3d> trajectory = MathUtils.calculateElytraTrajectory(position, velocity, 0, 0);
        
        assertNotNull(trajectory);
        assertFalse(trajectory.isEmpty());
        assertEquals(position, trajectory.get(0));
    }
    
    @Test
    void testCalculateElytraTrajectory_withNullPosition_returnsEmptyList() {
        Vec3d velocity = new Vec3d(1, 0, 0);
        
        List<Vec3d> trajectory = MathUtils.calculateElytraTrajectory(null, velocity, 0, 0);
        
        assertNotNull(trajectory);
        assertTrue(trajectory.isEmpty());
    }
    
    @Test
    void testCalculateElytraTrajectory_withNullVelocity_returnsEmptyList() {
        Vec3d position = new Vec3d(0, 100, 0);
        
        List<Vec3d> trajectory = MathUtils.calculateElytraTrajectory(position, null, 0, 0);
        
        assertNotNull(trajectory);
        assertTrue(trajectory.isEmpty());
    }
    
    @Test
    void testCalculateElytraTrajectory_appliesGravity() {
        Vec3d position = new Vec3d(0, 100, 0);
        Vec3d velocity = new Vec3d(1, 0, 0);
        
        List<Vec3d> trajectory = MathUtils.calculateElytraTrajectory(position, velocity, 0, 0);
        
        // Check that Y coordinate decreases over time (gravity effect)
        assertTrue(trajectory.size() > 2);
        double firstY = trajectory.get(0).y;
        double lastY = trajectory.get(trajectory.size() - 1).y;
        assertTrue(lastY < firstY, "Y coordinate should decrease due to gravity");
    }
    
    @Test
    void testCalculateElytraTrajectory_appliesDrag() {
        Vec3d position = new Vec3d(0, 100, 0);
        Vec3d velocity = new Vec3d(10, 0, 0);
        
        List<Vec3d> trajectory = MathUtils.calculateElytraTrajectory(position, velocity, 0, 0);
        
        // Check that horizontal velocity decreases over time (drag effect)
        assertTrue(trajectory.size() > 2);
        Vec3d first = trajectory.get(0);
        Vec3d second = trajectory.get(1);
        Vec3d last = trajectory.get(trajectory.size() - 1);
        
        double firstDelta = second.x - first.x;
        double lastDelta = last.x - trajectory.get(trajectory.size() - 2).x;
        
        assertTrue(Math.abs(lastDelta) < Math.abs(firstDelta), "Horizontal velocity should decrease due to drag");
    }
    
    @Test
    void testCalculateElytraMaxDistance_withValidInputs_returnsPositiveDistance() {
        Vec3d position = new Vec3d(0, 100, 0);
        Vec3d velocity = new Vec3d(1, 0, 1);
        
        double distance = MathUtils.calculateElytraMaxDistance(position, velocity);
        
        assertTrue(distance > 0, "Distance should be positive");
    }
    
    @Test
    void testCalculateElytraMaxDistance_withNullPosition_returnsZero() {
        Vec3d velocity = new Vec3d(1, 0, 0);
        
        double distance = MathUtils.calculateElytraMaxDistance(null, velocity);
        
        assertEquals(0.0, distance, EPSILON);
    }
    
    @Test
    void testCalculateElytraMaxDistance_withNullVelocity_returnsZero() {
        Vec3d position = new Vec3d(0, 100, 0);
        
        double distance = MathUtils.calculateElytraMaxDistance(position, null);
        
        assertEquals(0.0, distance, EPSILON);
    }
    
    @Test
    void testCalculateElytraMaxDistance_withZeroVelocity_returnsZero() {
        Vec3d position = new Vec3d(0, 100, 0);
        Vec3d velocity = new Vec3d(0, 0, 0);
        
        double distance = MathUtils.calculateElytraMaxDistance(position, velocity);
        
        assertEquals(0.0, distance, EPSILON);
    }
    
    @Test
    void testPredictElytraPosition_withValidInputs_returnsNewPosition() {
        Vec3d position = new Vec3d(0, 100, 0);
        Vec3d velocity = new Vec3d(1, 0, 0);
        int ticks = 10;
        
        Vec3d predicted = MathUtils.predictElytraPosition(position, velocity, ticks);
        
        assertNotNull(predicted);
        assertNotEquals(position, predicted);
    }
    
    @Test
    void testPredictElytraPosition_withZeroTicks_returnsSamePosition() {
        Vec3d position = new Vec3d(0, 100, 0);
        Vec3d velocity = new Vec3d(1, 0, 0);
        
        Vec3d predicted = MathUtils.predictElytraPosition(position, velocity, 0);
        
        assertEquals(position, predicted);
    }
    
    @Test
    void testPredictElytraPosition_withNegativeTicks_returnsSamePosition() {
        Vec3d position = new Vec3d(0, 100, 0);
        Vec3d velocity = new Vec3d(1, 0, 0);
        
        Vec3d predicted = MathUtils.predictElytraPosition(position, velocity, -5);
        
        assertEquals(position, predicted);
    }
    
    @Test
    void testPredictElytraPosition_withNullPosition_returnsOrigin() {
        Vec3d velocity = new Vec3d(1, 0, 0);
        
        Vec3d predicted = MathUtils.predictElytraPosition(null, velocity, 10);
        
        assertNotNull(predicted);
        assertEquals(new Vec3d(0, 0, 0), predicted);
    }
    
    @Test
    void testPredictElytraPosition_withNullVelocity_returnsSamePosition() {
        Vec3d position = new Vec3d(0, 100, 0);
        
        Vec3d predicted = MathUtils.predictElytraPosition(position, null, 10);
        
        assertEquals(position, predicted);
    }
    
    @Test
    void testPredictElytraPosition_appliesPhysics() {
        Vec3d position = new Vec3d(0, 100, 0);
        Vec3d velocity = new Vec3d(1, 0, 0);
        int ticks = 20;
        
        Vec3d predicted = MathUtils.predictElytraPosition(position, velocity, ticks);
        
        // X should increase (horizontal movement)
        assertTrue(predicted.x > position.x, "X should increase due to velocity");
        
        // Y should decrease (gravity)
        assertTrue(predicted.y < position.y, "Y should decrease due to gravity");
    }
    
    @Test
    void testCalculateDistance_withValidPositions_returnsCorrectDistance() {
        Vec3d pos1 = new Vec3d(0, 0, 0);
        Vec3d pos2 = new Vec3d(3, 4, 0);
        
        double distance = MathUtils.calculateDistance(pos1, pos2);
        
        // 3-4-5 triangle
        assertEquals(5.0, distance, EPSILON);
    }
    
    @Test
    void testCalculateDistance_withSamePosition_returnsZero() {
        Vec3d pos = new Vec3d(10, 20, 30);
        
        double distance = MathUtils.calculateDistance(pos, pos);
        
        assertEquals(0.0, distance, EPSILON);
    }
    
    @Test
    void testCalculateDistance_withNullFirstPosition_returnsZero() {
        Vec3d pos2 = new Vec3d(1, 2, 3);
        
        double distance = MathUtils.calculateDistance(null, pos2);
        
        assertEquals(0.0, distance, EPSILON);
    }
    
    @Test
    void testCalculateDistance_withNullSecondPosition_returnsZero() {
        Vec3d pos1 = new Vec3d(1, 2, 3);
        
        double distance = MathUtils.calculateDistance(pos1, null);
        
        assertEquals(0.0, distance, EPSILON);
    }
    
    @Test
    void testCalculateDistance_with3DDistance_calculatesCorrectly() {
        Vec3d pos1 = new Vec3d(0, 0, 0);
        Vec3d pos2 = new Vec3d(1, 1, 1);
        
        double distance = MathUtils.calculateDistance(pos1, pos2);
        
        // sqrt(1^2 + 1^2 + 1^2) = sqrt(3)
        assertEquals(Math.sqrt(3), distance, EPSILON);
    }
    
    @Test
    void testCalculateDistance_isSymmetric() {
        Vec3d pos1 = new Vec3d(5, 10, 15);
        Vec3d pos2 = new Vec3d(20, 30, 40);
        
        double distance1 = MathUtils.calculateDistance(pos1, pos2);
        double distance2 = MathUtils.calculateDistance(pos2, pos1);
        
        assertEquals(distance1, distance2, EPSILON);
    }
    
    @Test
    void testGetConsumptionTime_withNullItem_returnsZero() {
        int time = MathUtils.getConsumptionTime(null);
        
        assertEquals(0, time);
    }
    
    @Test
    void testGetRemainingTime_withNullPlayer_returnsZero() {
        int remaining = MathUtils.getRemainingTime(null);
        
        assertEquals(0, remaining);
    }
    
    @Test
    void testGetConsumptionProgress_withNullPlayer_returnsZero() {
        float progress = MathUtils.getConsumptionProgress(null);
        
        assertEquals(0.0f, progress, EPSILON);
    }
}
