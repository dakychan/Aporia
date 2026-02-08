package ru.render;

import net.jqwik.api.*;
import net.jqwik.api.constraints.FloatRange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the EasingFunction enum.
 * Includes both unit tests for specific cases and property-based tests for general behavior.
 */
class EasingFunctionTest {
    
    // ========== Unit Tests ==========
    
    @Test
    void testLinearAtBoundaries() {
        assertEquals(0.0f, EasingFunction.LINEAR.apply(0.0f), 0.001f);
        assertEquals(1.0f, EasingFunction.LINEAR.apply(1.0f), 0.001f);
    }
    
    @Test
    void testLinearAtMidpoint() {
        assertEquals(0.5f, EasingFunction.LINEAR.apply(0.5f), 0.001f);
    }
    
    @Test
    void testLinearQuarterPoints() {
        assertEquals(0.25f, EasingFunction.LINEAR.apply(0.25f), 0.001f);
        assertEquals(0.75f, EasingFunction.LINEAR.apply(0.75f), 0.001f);
    }
    
    @Test
    void testEaseInOutCubicAtBoundaries() {
        assertEquals(0.0f, EasingFunction.EASE_IN_OUT_CUBIC.apply(0.0f), 0.001f);
        assertEquals(1.0f, EasingFunction.EASE_IN_OUT_CUBIC.apply(1.0f), 0.001f);
    }
    
    @Test
    void testEaseInOutCubicAtMidpoint() {
        // At t=0.5, the function should return 0.5 (smooth transition point)
        assertEquals(0.5f, EasingFunction.EASE_IN_OUT_CUBIC.apply(0.5f), 0.001f);
    }
    
    @Test
    void testEaseInOutCubicFirstHalf() {
        // At t=0.25, f(t) = 4 * 0.25³ = 4 * 0.015625 = 0.0625
        assertEquals(0.0625f, EasingFunction.EASE_IN_OUT_CUBIC.apply(0.25f), 0.001f);
    }
    
    @Test
    void testEaseInOutCubicSecondHalf() {
        // At t=0.75, adjusted = -2*0.75 + 2 = 0.5
        // f(t) = 1 - 0.5³/2 = 1 - 0.125/2 = 1 - 0.0625 = 0.9375
        assertEquals(0.9375f, EasingFunction.EASE_IN_OUT_CUBIC.apply(0.75f), 0.001f);
    }
    
    @Test
    void testEaseInOutQuadAtBoundaries() {
        assertEquals(0.0f, EasingFunction.EASE_IN_OUT_QUAD.apply(0.0f), 0.001f);
        assertEquals(1.0f, EasingFunction.EASE_IN_OUT_QUAD.apply(1.0f), 0.001f);
    }
    
    @Test
    void testEaseInOutQuadAtMidpoint() {
        // At t=0.5, the function should return 0.5 (smooth transition point)
        assertEquals(0.5f, EasingFunction.EASE_IN_OUT_QUAD.apply(0.5f), 0.001f);
    }
    
    @Test
    void testEaseInOutQuadFirstHalf() {
        // At t=0.25, f(t) = 2 * 0.25² = 2 * 0.0625 = 0.125
        assertEquals(0.125f, EasingFunction.EASE_IN_OUT_QUAD.apply(0.25f), 0.001f);
    }
    
    @Test
    void testEaseInOutQuadSecondHalf() {
        // At t=0.75, adjusted = -2*0.75 + 2 = 0.5
        // f(t) = 1 - 0.5²/2 = 1 - 0.25/2 = 1 - 0.125 = 0.875
        assertEquals(0.875f, EasingFunction.EASE_IN_OUT_QUAD.apply(0.75f), 0.001f);
    }
    
    @Test
    void testAllEasingFunctionsExist() {
        // Verify all three required easing functions are present
        assertNotNull(EasingFunction.LINEAR);
        assertNotNull(EasingFunction.EASE_IN_OUT_CUBIC);
        assertNotNull(EasingFunction.EASE_IN_OUT_QUAD);
    }
    
    // ========== Property-Based Tests ==========
    
    @Property
    @Tag("Feature: minecraft-client-enhancements, Property: Easing Function Output Range")
    void easingFunctionOutputIsInValidRange(
            @ForAll @FloatRange(min = 0.0f, max = 1.0f) float t) {
        for (EasingFunction easing : EasingFunction.values()) {
            float result = easing.apply(t);
            assertTrue(result >= -0.01f && result <= 1.01f,
                    easing.name() + " output should be roughly in [0, 1] range for t=" + t + ", got: " + result);
        }
    }
    
    @Property
    @Tag("Feature: minecraft-client-enhancements, Property: Easing Function Boundary Conditions")
    void easingFunctionsBoundaryConditions(
            @ForAll("easingFunctions") EasingFunction easing) {
        // At t=0, all easing functions should return 0
        assertEquals(0.0f, easing.apply(0.0f), 0.001f,
                easing.name() + " should return 0 at t=0");
        
        // At t=1, all easing functions should return 1
        assertEquals(1.0f, easing.apply(1.0f), 0.001f,
                easing.name() + " should return 1 at t=1");
    }
    
    @Property
    @Tag("Feature: minecraft-client-enhancements, Property: Linear Easing Identity")
    void linearEasingIsIdentityFunction(
            @ForAll @FloatRange(min = 0.0f, max = 1.0f) float t) {
        float result = EasingFunction.LINEAR.apply(t);
        assertEquals(t, result, 0.001f,
                "LINEAR easing should return the input value unchanged");
    }
    
    @Property
    @Tag("Feature: minecraft-client-enhancements, Property: Easing Function Monotonicity")
    void easingFunctionsAreMonotonic(
            @ForAll("easingFunctions") EasingFunction easing,
            @ForAll @FloatRange(min = 0.0f, max = 1.0f) float t1,
            @ForAll @FloatRange(min = 0.0f, max = 1.0f) float t2) {
        // For t1 < t2, f(t1) should be <= f(t2) (non-decreasing)
        if (t1 < t2) {
            float result1 = easing.apply(t1);
            float result2 = easing.apply(t2);
            assertTrue(result1 <= result2 + 0.001f,
                    easing.name() + " should be monotonic: f(" + t1 + ")=" + result1 + 
                    " should be <= f(" + t2 + ")=" + result2);
        }
    }
    
    @Property
    @Tag("Feature: minecraft-client-enhancements, Property: Symmetric Easing Functions")
    void easeInOutFunctionsAreSymmetric(
            @ForAll @FloatRange(min = 0.0f, max = 0.5f) float t) {
        // For ease-in-out functions, f(t) + f(1-t) should equal 1
        // This tests the symmetry property
        
        float cubicResult1 = EasingFunction.EASE_IN_OUT_CUBIC.apply(t);
        float cubicResult2 = EasingFunction.EASE_IN_OUT_CUBIC.apply(1.0f - t);
        assertEquals(1.0f, cubicResult1 + cubicResult2, 0.01f,
                "EASE_IN_OUT_CUBIC should be symmetric around 0.5");
        
        float quadResult1 = EasingFunction.EASE_IN_OUT_QUAD.apply(t);
        float quadResult2 = EasingFunction.EASE_IN_OUT_QUAD.apply(1.0f - t);
        assertEquals(1.0f, quadResult1 + quadResult2, 0.01f,
                "EASE_IN_OUT_QUAD should be symmetric around 0.5");
    }
    
    @Property
    @Tag("Feature: minecraft-client-enhancements, Property: Easing Function Midpoint")
    void easeInOutFunctionsPassThroughMidpoint(
            @ForAll("easingFunctions") EasingFunction easing) {
        // All ease-in-out functions should pass through (0.5, 0.5)
        if (easing == EasingFunction.EASE_IN_OUT_CUBIC || 
            easing == EasingFunction.EASE_IN_OUT_QUAD) {
            float result = easing.apply(0.5f);
            assertEquals(0.5f, result, 0.001f,
                    easing.name() + " should pass through (0.5, 0.5)");
        }
    }
    
    @Property
    @Tag("Feature: minecraft-client-enhancements, Property: Cubic vs Quad Comparison")
    void cubicEasingIsSteeperThanQuadInFirstHalf(
            @ForAll @FloatRange(min = 0.01f, max = 0.49f) float t) {
        // In the first half, cubic should be less than quad (steeper acceleration)
        float cubic = EasingFunction.EASE_IN_OUT_CUBIC.apply(t);
        float quad = EasingFunction.EASE_IN_OUT_QUAD.apply(t);
        
        assertTrue(cubic <= quad + 0.001f,
                "EASE_IN_OUT_CUBIC should be <= EASE_IN_OUT_QUAD in first half at t=" + t +
                " (cubic=" + cubic + ", quad=" + quad + ")");
    }
    
    @Property
    @Tag("Feature: minecraft-client-enhancements, Property: Cubic vs Quad Comparison Second Half")
    void cubicEasingIsSteeperThanQuadInSecondHalf(
            @ForAll @FloatRange(min = 0.51f, max = 0.99f) float t) {
        // In the second half, cubic should be greater than quad (steeper deceleration)
        float cubic = EasingFunction.EASE_IN_OUT_CUBIC.apply(t);
        float quad = EasingFunction.EASE_IN_OUT_QUAD.apply(t);
        
        assertTrue(cubic >= quad - 0.001f,
                "EASE_IN_OUT_CUBIC should be >= EASE_IN_OUT_QUAD in second half at t=" + t +
                " (cubic=" + cubic + ", quad=" + quad + ")");
    }
    
    // ========== Arbitraries ==========
    
    @Provide
    Arbitrary<EasingFunction> easingFunctions() {
        return Arbitraries.of(EasingFunction.values());
    }
}
