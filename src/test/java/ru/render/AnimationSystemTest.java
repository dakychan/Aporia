package ru.render;

import net.jqwik.api.*;
import net.jqwik.api.constraints.FloatRange;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the AnimationSystem class.
 * Includes both unit tests for specific cases and property-based tests for general behavior.
 */
class AnimationSystemTest {
    
    // ========== Unit Tests ==========
    
    @Test
    void testAnimationSystemCreation() {
        AnimationSystem system = new AnimationSystem();
        assertEquals(0, system.getActiveAnimationCount());
    }
    
    @Test
    void testCreateAnimation() {
        AnimationSystem system = new AnimationSystem();
        Animation animation = system.createAnimation("test", 1.0f, EasingFunction.LINEAR);
        
        assertNotNull(animation);
        assertEquals(1, system.getActiveAnimationCount());
        assertTrue(system.hasAnimation("test"));
    }
    
    @Test
    void testCreateAnimationWithNullIdThrowsException() {
        AnimationSystem system = new AnimationSystem();
        assertThrows(IllegalArgumentException.class, 
                () -> system.createAnimation(null, 1.0f, EasingFunction.LINEAR));
    }
    
    @Test
    void testCreateAnimationWithEmptyIdThrowsException() {
        AnimationSystem system = new AnimationSystem();
        assertThrows(IllegalArgumentException.class, 
                () -> system.createAnimation("", 1.0f, EasingFunction.LINEAR));
    }
    
    @Test
    void testCreateAnimationWithInvalidDurationThrowsException() {
        AnimationSystem system = new AnimationSystem();
        assertThrows(IllegalArgumentException.class, 
                () -> system.createAnimation("test", 0.0f, EasingFunction.LINEAR));
        assertThrows(IllegalArgumentException.class, 
                () -> system.createAnimation("test", -1.0f, EasingFunction.LINEAR));
    }
    
    @Test
    void testCreateAnimationReplacesExisting() {
        AnimationSystem system = new AnimationSystem();
        system.createAnimation("test", 1.0f, EasingFunction.LINEAR);
        system.createAnimation("test", 2.0f, EasingFunction.LINEAR);
        
        assertEquals(1, system.getActiveAnimationCount());
        assertTrue(system.hasAnimation("test"));
    }
    
    @Test
    void testGetProgressForNonExistentAnimation() {
        AnimationSystem system = new AnimationSystem();
        assertEquals(0.0f, system.getProgress("nonexistent"), 0.001f);
    }
    
    @Test
    void testGetProgressForExistingAnimation() {
        AnimationSystem system = new AnimationSystem();
        system.createAnimation("test", 2.0f, EasingFunction.LINEAR);
        system.update(1.0f);
        
        assertEquals(0.5f, system.getProgress("test"), 0.001f);
    }
    
    @Test
    void testUpdateAdvancesAnimations() {
        AnimationSystem system = new AnimationSystem();
        system.createAnimation("test", 1.0f, EasingFunction.LINEAR);
        
        system.update(0.5f);
        assertEquals(0.5f, system.getProgress("test"), 0.001f);
        
        system.update(0.3f);
        assertEquals(0.8f, system.getProgress("test"), 0.001f);
    }
    
    @Test
    void testUpdateRemovesCompletedAnimations() {
        AnimationSystem system = new AnimationSystem();
        system.createAnimation("test", 1.0f, EasingFunction.LINEAR);
        
        system.update(1.5f);
        
        assertEquals(0, system.getActiveAnimationCount());
        assertFalse(system.hasAnimation("test"));
    }
    
    @Test
    void testUpdateMultipleAnimations() {
        AnimationSystem system = new AnimationSystem();
        system.createAnimation("anim1", 1.0f, EasingFunction.LINEAR);
        system.createAnimation("anim2", 2.0f, EasingFunction.LINEAR);
        system.createAnimation("anim3", 3.0f, EasingFunction.LINEAR);
        
        system.update(0.5f);
        
        assertEquals(0.5f, system.getProgress("anim1"), 0.001f);
        assertEquals(0.25f, system.getProgress("anim2"), 0.001f);
        assertEquals(0.167f, system.getProgress("anim3"), 0.01f);
    }
    
    @Test
    void testUpdateRemovesOnlyCompletedAnimations() {
        AnimationSystem system = new AnimationSystem();
        system.createAnimation("short", 0.5f, EasingFunction.LINEAR);
        system.createAnimation("long", 2.0f, EasingFunction.LINEAR);
        
        system.update(1.0f);
        
        assertEquals(1, system.getActiveAnimationCount());
        assertFalse(system.hasAnimation("short"));
        assertTrue(system.hasAnimation("long"));
    }
    
    @Test
    void testHasAnimation() {
        AnimationSystem system = new AnimationSystem();
        assertFalse(system.hasAnimation("test"));
        
        system.createAnimation("test", 1.0f, EasingFunction.LINEAR);
        assertTrue(system.hasAnimation("test"));
    }
    
    @Test
    void testRemoveAnimation() {
        AnimationSystem system = new AnimationSystem();
        system.createAnimation("test", 1.0f, EasingFunction.LINEAR);
        
        assertTrue(system.removeAnimation("test"));
        assertFalse(system.hasAnimation("test"));
        assertEquals(0, system.getActiveAnimationCount());
    }
    
    @Test
    void testRemoveNonExistentAnimation() {
        AnimationSystem system = new AnimationSystem();
        assertFalse(system.removeAnimation("nonexistent"));
    }
    
    @Test
    void testClear() {
        AnimationSystem system = new AnimationSystem();
        system.createAnimation("anim1", 1.0f, EasingFunction.LINEAR);
        system.createAnimation("anim2", 2.0f, EasingFunction.LINEAR);
        system.createAnimation("anim3", 3.0f, EasingFunction.LINEAR);
        
        system.clear();
        
        assertEquals(0, system.getActiveAnimationCount());
        assertFalse(system.hasAnimation("anim1"));
        assertFalse(system.hasAnimation("anim2"));
        assertFalse(system.hasAnimation("anim3"));
    }
    
    @Test
    void testGetActiveAnimationCount() {
        AnimationSystem system = new AnimationSystem();
        assertEquals(0, system.getActiveAnimationCount());
        
        system.createAnimation("anim1", 1.0f, EasingFunction.LINEAR);
        assertEquals(1, system.getActiveAnimationCount());
        
        system.createAnimation("anim2", 1.0f, EasingFunction.LINEAR);
        assertEquals(2, system.getActiveAnimationCount());
        
        system.removeAnimation("anim1");
        assertEquals(1, system.getActiveAnimationCount());
    }
    
    @Test
    void testUpdateWithZeroDeltaTime() {
        AnimationSystem system = new AnimationSystem();
        system.createAnimation("test", 1.0f, EasingFunction.LINEAR);
        
        system.update(0.0f);
        
        assertEquals(0.0f, system.getProgress("test"), 0.001f);
        assertTrue(system.hasAnimation("test"));
    }
    
    @Test
    void testUpdateWithNegativeDeltaTime() {
        AnimationSystem system = new AnimationSystem();
        system.createAnimation("test", 1.0f, EasingFunction.LINEAR);
        
        system.update(-0.5f);
        
        assertEquals(0.0f, system.getProgress("test"), 0.001f);
        assertTrue(system.hasAnimation("test"));
    }
    
    // ========== Property-Based Tests ==========
    
    @Property
    @Tag("Feature: minecraft-client-enhancements, Property: Animation System Registry")
    void createdAnimationsAreRegistered(
            @ForAll @StringLength(min = 1, max = 50) String id,
            @ForAll @FloatRange(min = 0.1f, max = 10.0f) float duration) {
        AnimationSystem system = new AnimationSystem();
        
        system.createAnimation(id, duration, EasingFunction.LINEAR);
        
        assertTrue(system.hasAnimation(id),
                "Created animation should be registered in the system");
        assertEquals(1, system.getActiveAnimationCount(),
                "System should have exactly one animation");
    }
    
    @Property
    @Tag("Feature: minecraft-client-enhancements, Property: Animation System Update")
    void updateAdvancesAllAnimations(
            @ForAll @IntRange(min = 1, max = 5) int animationCount,
            @ForAll @FloatRange(min = 0.1f, max = 2.0f) float duration,
            @ForAll @FloatRange(min = 0.01f, max = 0.5f) float deltaTime) {
        AnimationSystem system = new AnimationSystem();
        
        // Create multiple animations
        for (int i = 0; i < animationCount; i++) {
            system.createAnimation("anim" + i, duration, EasingFunction.LINEAR);
        }
        
        system.update(deltaTime);
        
        // All animations should have progressed or been removed if completed
        for (int i = 0; i < animationCount; i++) {
            if (system.hasAnimation("anim" + i)) {
                float progress = system.getProgress("anim" + i);
                assertTrue(progress > 0.0f && progress <= 1.0f,
                        "Animation progress should be in valid range after update");
            } else {
                // Animation was completed and removed, which is valid
                assertEquals(0.0f, system.getProgress("anim" + i), 0.001f,
                        "Removed animation should return 0.0 progress");
            }
        }
    }
    
    @Property
    @Tag("Feature: minecraft-client-enhancements, Property: Animation System Completion Removal")
    void completedAnimationsAreRemoved(
            @ForAll @FloatRange(min = 0.1f, max = 2.0f) float duration) {
        AnimationSystem system = new AnimationSystem();
        system.createAnimation("test", duration, EasingFunction.LINEAR);
        
        // Update with more than enough time to complete the animation
        system.update(duration + 0.5f);
        
        assertFalse(system.hasAnimation("test"),
                "Completed animation should be removed from the system");
        assertEquals(0, system.getActiveAnimationCount(),
                "System should have no active animations after completion");
    }
    
    @Property
    @Tag("Feature: minecraft-client-enhancements, Property: Animation System Progress Retrieval")
    void getProgressReturnsValidValues(
            @ForAll @StringLength(min = 1, max = 50) String id,
            @ForAll @FloatRange(min = 0.1f, max = 10.0f) float duration,
            @ForAll @FloatRange(min = 0.0f, max = 20.0f) float deltaTime) {
        AnimationSystem system = new AnimationSystem();
        system.createAnimation(id, duration, EasingFunction.LINEAR);
        
        system.update(deltaTime);
        
        float progress = system.getProgress(id);
        
        // Progress should be in valid range or animation should be removed
        if (system.hasAnimation(id)) {
            assertTrue(progress >= 0.0f && progress <= 1.0f,
                    "Progress should be between 0.0 and 1.0, got: " + progress);
        } else {
            // Animation was completed and removed
            assertEquals(0.0f, progress, 0.001f,
                    "Progress for removed animation should return 0.0");
        }
    }
    
    @Property
    @Tag("Feature: minecraft-client-enhancements, Property: Animation System Multiple Updates")
    void multipleUpdatesAccumulateTime(
            @ForAll @FloatRange(min = 0.5f, max = 2.0f) float duration,
            @ForAll @FloatRange(min = 0.05f, max = 0.2f) float deltaTime1,
            @ForAll @FloatRange(min = 0.05f, max = 0.2f) float deltaTime2) {
        AnimationSystem system = new AnimationSystem();
        system.createAnimation("test", duration, EasingFunction.LINEAR);
        
        system.update(deltaTime1);
        float progress1 = system.getProgress("test");
        
        system.update(deltaTime2);
        float progress2 = system.getProgress("test");
        
        if (system.hasAnimation("test")) {
            assertTrue(progress2 >= progress1,
                    "Progress should increase or stay the same after update");
        }
    }
    
    @Property
    @Tag("Feature: minecraft-client-enhancements, Property: Animation System ID Uniqueness")
    void sameIdReplacesAnimation(
            @ForAll @StringLength(min = 1, max = 50) String id,
            @ForAll @FloatRange(min = 0.1f, max = 5.0f) float duration1,
            @ForAll @FloatRange(min = 0.1f, max = 5.0f) float duration2) {
        AnimationSystem system = new AnimationSystem();
        
        system.createAnimation(id, duration1, EasingFunction.LINEAR);
        system.update(duration1 * 0.5f); // Advance first animation
        
        system.createAnimation(id, duration2, EasingFunction.LINEAR);
        
        assertEquals(1, system.getActiveAnimationCount(),
                "Creating animation with same ID should replace existing one");
        assertEquals(0.0f, system.getProgress(id), 0.001f,
                "Replaced animation should start from 0.0 progress");
    }
    
    @Property
    @Tag("Feature: minecraft-client-enhancements, Property: Animation System Clear")
    void clearRemovesAllAnimations(
            @ForAll @IntRange(min = 1, max = 10) int animationCount) {
        AnimationSystem system = new AnimationSystem();
        
        // Create multiple animations
        for (int i = 0; i < animationCount; i++) {
            system.createAnimation("anim" + i, 1.0f, EasingFunction.LINEAR);
        }
        
        system.clear();
        
        assertEquals(0, system.getActiveAnimationCount(),
                "Clear should remove all animations");
        
        for (int i = 0; i < animationCount; i++) {
            assertFalse(system.hasAnimation("anim" + i),
                    "Animation should not exist after clear");
        }
    }
    
    @Property
    @Tag("Feature: minecraft-client-enhancements, Property: Animation System Remove")
    void removeAnimationWorks(
            @ForAll @StringLength(min = 1, max = 50) String id,
            @ForAll @FloatRange(min = 0.1f, max = 10.0f) float duration) {
        AnimationSystem system = new AnimationSystem();
        system.createAnimation(id, duration, EasingFunction.LINEAR);
        
        boolean removed = system.removeAnimation(id);
        
        assertTrue(removed, "Remove should return true for existing animation");
        assertFalse(system.hasAnimation(id), "Animation should not exist after removal");
        assertEquals(0, system.getActiveAnimationCount(),
                "System should have no animations after removal");
    }
    
    @Property
    @Tag("Feature: minecraft-client-enhancements, Property: Animation System Non-Existent Progress")
    void getProgressForNonExistentReturnsZero(
            @ForAll @StringLength(min = 1, max = 50) String id) {
        AnimationSystem system = new AnimationSystem();
        
        float progress = system.getProgress(id);
        
        assertEquals(0.0f, progress, 0.001f,
                "Progress for non-existent animation should be 0.0");
    }
    
    @Property
    @Tag("Feature: minecraft-client-enhancements, Property: Animation System Partial Completion")
    void someAnimationsCompleteWhileOthersContinue(
            @ForAll @FloatRange(min = 0.1f, max = 1.0f) float shortDuration,
            @ForAll @FloatRange(min = 2.0f, max = 5.0f) float longDuration,
            @ForAll @FloatRange(min = 1.0f, max = 1.5f) float updateTime) {
        AnimationSystem system = new AnimationSystem();
        system.createAnimation("short", shortDuration, EasingFunction.LINEAR);
        system.createAnimation("long", longDuration, EasingFunction.LINEAR);
        
        system.update(updateTime);
        
        // Short animation should be completed and removed
        assertFalse(system.hasAnimation("short"),
                "Short animation should be completed and removed");
        
        // Long animation should still be active
        assertTrue(system.hasAnimation("long"),
                "Long animation should still be active");
        
        float longProgress = system.getProgress("long");
        assertTrue(longProgress > 0.0f && longProgress < 1.0f,
                "Long animation should be in progress");
    }
}
