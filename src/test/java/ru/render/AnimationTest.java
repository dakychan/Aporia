package ru.render;

import net.jqwik.api.*;
import net.jqwik.api.constraints.FloatRange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Animation class.
 * Includes both unit tests for specific cases and property-based tests for general behavior.
 */
class AnimationTest {
    
    // ========== Unit Tests ==========
    
    @Test
    void testAnimationCreation() {
        Animation animation = new Animation(1.0f);
        assertEquals(0.0f, animation.getCurrentTime(), 0.001f);
        assertEquals(1.0f, animation.getDuration(), 0.001f);
        assertEquals(0.0f, animation.getProgress(), 0.001f);
        assertFalse(animation.isComplete());
    }
    
    @Test
    void testAnimationWithZeroDurationThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Animation(0.0f));
    }
    
    @Test
    void testAnimationWithNegativeDurationThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Animation(-1.0f));
    }
    
    @Test
    void testUpdateIncreasesCurrentTime() {
        Animation animation = new Animation(2.0f);
        animation.update(0.5f);
        assertEquals(0.5f, animation.getCurrentTime(), 0.001f);
        assertEquals(0.25f, animation.getProgress(), 0.001f);
        assertFalse(animation.isComplete());
    }
    
    @Test
    void testUpdateCompletesAnimation() {
        Animation animation = new Animation(1.0f);
        animation.update(1.5f);
        assertEquals(1.0f, animation.getCurrentTime(), 0.001f);
        assertEquals(1.0f, animation.getProgress(), 0.001f);
        assertTrue(animation.isComplete());
    }
    
    @Test
    void testMultipleUpdates() {
        Animation animation = new Animation(1.0f);
        animation.update(0.3f);
        animation.update(0.3f);
        animation.update(0.3f);
        assertEquals(0.9f, animation.getCurrentTime(), 0.001f);
        assertEquals(0.9f, animation.getProgress(), 0.001f);
        assertFalse(animation.isComplete());
    }
    
    @Test
    void testUpdateAfterCompletionHasNoEffect() {
        Animation animation = new Animation(1.0f);
        animation.update(1.0f);
        assertTrue(animation.isComplete());
        
        animation.update(0.5f);
        assertEquals(1.0f, animation.getCurrentTime(), 0.001f);
        assertEquals(1.0f, animation.getProgress(), 0.001f);
    }
    
    @Test
    void testReset() {
        Animation animation = new Animation(1.0f);
        animation.update(0.5f);
        animation.reset();
        
        assertEquals(0.0f, animation.getCurrentTime(), 0.001f);
        assertEquals(0.0f, animation.getProgress(), 0.001f);
        assertFalse(animation.isComplete());
    }
    
    @Test
    void testNegativeDeltaTimeIsIgnored() {
        Animation animation = new Animation(1.0f);
        animation.update(-0.5f);
        assertEquals(0.0f, animation.getCurrentTime(), 0.001f);
    }
    
    @Test
    void testLargeDeltaTime() {
        Animation animation = new Animation(2.0f);
        animation.update(0.5f);
        // Large delta time should be accepted as-is
        animation.update(1.0f);
        // Should have added 1.0f
        assertEquals(1.5f, animation.getCurrentTime(), 0.001f);
    }
    
    // ========== Property-Based Tests ==========
    
    @Property
    @Tag("Feature: minecraft-client-enhancements, Property: Animation Progress Range")
    void progressIsAlwaysBetweenZeroAndOne(
            @ForAll @FloatRange(min = 0.1f, max = 10.0f) float duration,
            @ForAll @FloatRange(min = 0.0f, max = 20.0f) float deltaTime) {
        Animation animation = new Animation(duration);
        animation.update(deltaTime);
        
        float progress = animation.getProgress();
        assertTrue(progress >= 0.0f && progress <= 1.0f,
                "Progress should be between 0.0 and 1.0, got: " + progress);
    }
    
    @Property
    @Tag("Feature: minecraft-client-enhancements, Property: Animation Time Monotonicity")
    void currentTimeNeverDecreases(
            @ForAll @FloatRange(min = 0.1f, max = 10.0f) float duration,
            @ForAll @FloatRange(min = 0.0f, max = 1.0f) float deltaTime1,
            @ForAll @FloatRange(min = 0.0f, max = 1.0f) float deltaTime2) {
        Animation animation = new Animation(duration);
        
        animation.update(deltaTime1);
        float time1 = animation.getCurrentTime();
        
        animation.update(deltaTime2);
        float time2 = animation.getCurrentTime();
        
        assertTrue(time2 >= time1,
                "Current time should never decrease: " + time1 + " -> " + time2);
    }
    
    @Property
    @Tag("Feature: minecraft-client-enhancements, Property: Animation Completion")
    void animationCompletesWhenTimeExceedsDuration(
            @ForAll @FloatRange(min = 0.1f, max = 10.0f) float duration,
            @ForAll @FloatRange(min = 0.0f, max = 20.0f) float totalDeltaTime) {
        Animation animation = new Animation(duration);
        
        // Apply updates in small increments
        float remaining = totalDeltaTime;
        while (remaining > 0) {
            float delta = Math.min(remaining, 0.05f);
            animation.update(delta);
            remaining -= delta;
        }
        
        if (totalDeltaTime >= duration) {
            assertTrue(animation.isComplete(),
                    "Animation should be complete when total time >= duration");
            assertEquals(1.0f, animation.getProgress(), 0.001f,
                    "Progress should be 1.0 when complete");
        }
    }
    
    @Property
    @Tag("Feature: minecraft-client-enhancements, Property: Animation Progress Calculation")
    void progressMatchesTimeRatio(
            @ForAll @FloatRange(min = 0.1f, max = 10.0f) float duration,
            @ForAll @FloatRange(min = 0.0f, max = 1.0f) float fraction) {
        Animation animation = new Animation(duration);
        float deltaTime = duration * fraction;
        
        animation.update(deltaTime);
        
        float expectedProgress = Math.min(fraction, 1.0f);
        float actualProgress = animation.getProgress();
        
        assertEquals(expectedProgress, actualProgress, 0.01f,
                "Progress should match time/duration ratio");
    }
    
    @Property
    @Tag("Feature: minecraft-client-enhancements, Property: Animation Reset Behavior")
    void resetRestoresInitialState(
            @ForAll @FloatRange(min = 0.1f, max = 10.0f) float duration,
            @ForAll @FloatRange(min = 0.0f, max = 20.0f) float deltaTime) {
        Animation animation = new Animation(duration);
        animation.update(deltaTime);
        
        animation.reset();
        
        assertEquals(0.0f, animation.getCurrentTime(), 0.001f,
                "Current time should be 0 after reset");
        assertEquals(0.0f, animation.getProgress(), 0.001f,
                "Progress should be 0 after reset");
        assertFalse(animation.isComplete(),
                "Animation should not be complete after reset");
    }
    
    @Property
    @Tag("Feature: minecraft-client-enhancements, Property: Animation Idempotent Completion")
    void updatesAfterCompletionAreIdempotent(
            @ForAll @FloatRange(min = 0.1f, max = 10.0f) float duration,
            @ForAll @FloatRange(min = 0.0f, max = 5.0f) float extraDeltaTime) {
        Animation animation = new Animation(duration);
        
        // Complete the animation
        animation.update(duration + 1.0f);
        assertTrue(animation.isComplete());
        
        float timeAfterCompletion = animation.getCurrentTime();
        float progressAfterCompletion = animation.getProgress();
        
        // Try to update further
        animation.update(extraDeltaTime);
        
        assertEquals(timeAfterCompletion, animation.getCurrentTime(), 0.001f,
                "Time should not change after completion");
        assertEquals(progressAfterCompletion, animation.getProgress(), 0.001f,
                "Progress should not change after completion");
        assertTrue(animation.isComplete(),
                "Animation should remain complete");
    }
}
