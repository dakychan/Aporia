package ru.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.render.AnimationSystem;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the ToggleButton class.
 * Tests basic functionality, state management, and mouse interaction.
 */
class ToggleButtonTest {
    
    private AnimationSystem animationSystem;
    private ToggleButton toggleButton;
    
    @BeforeEach
    void setUp() {
        animationSystem = new AnimationSystem();
        toggleButton = new ToggleButton(100, 100, animationSystem);
    }
    
    @Test
    void testConstructorWithDefaultLabels() {
        assertNotNull(toggleButton);
        assertEquals(100, toggleButton.getX());
        assertEquals(100, toggleButton.getY());
        assertFalse(toggleButton.getState());
        assertEquals("ON", toggleButton.getLabelOn());
        assertEquals("OFF", toggleButton.getLabelOff());
    }
    
    @Test
    void testConstructorWithCustomLabels() {
        ToggleButton customButton = new ToggleButton(50, 50, 150, 40, "Enabled", "Disabled", animationSystem);
        
        assertNotNull(customButton);
        assertEquals(50, customButton.getX());
        assertEquals(50, customButton.getY());
        assertEquals(150, customButton.getWidth());
        assertEquals(40, customButton.getHeight());
        assertFalse(customButton.getState());
        assertEquals("Enabled", customButton.getLabelOn());
        assertEquals("Disabled", customButton.getLabelOff());
    }
    
    @Test
    void testToggleChangesState() {
        assertFalse(toggleButton.getState());
        
        toggleButton.toggle();
        assertTrue(toggleButton.getState());
        
        toggleButton.toggle();
        assertFalse(toggleButton.getState());
    }
    
    @Test
    void testToggleCreatesAnimation() {
        toggleButton.toggle();
        
        // Animation should be created with a unique ID
        // We can't directly check the animation ID, but we can verify the animation system has an animation
        assertTrue(animationSystem.getActiveAnimationCount() > 0);
    }
    
    @Test
    void testIsMouseOverInsideBounds() {
        // Mouse inside button bounds
        assertTrue(toggleButton.isMouseOver(110, 110));
        assertTrue(toggleButton.isMouseOver(100, 100)); // Top-left corner
        assertTrue(toggleButton.isMouseOver(220, 130)); // Bottom-right corner (default 120x30)
    }
    
    @Test
    void testIsMouseOverOutsideBounds() {
        // Mouse outside button bounds
        assertFalse(toggleButton.isMouseOver(50, 50));
        assertFalse(toggleButton.isMouseOver(250, 150));
        assertFalse(toggleButton.isMouseOver(100, 50));
        assertFalse(toggleButton.isMouseOver(50, 100));
    }
    
    @Test
    void testIsMouseOverEdgeCases() {
        // Just outside the bounds
        assertFalse(toggleButton.isMouseOver(99, 110));
        assertFalse(toggleButton.isMouseOver(110, 99));
        assertFalse(toggleButton.isMouseOver(221, 110));
        assertFalse(toggleButton.isMouseOver(110, 131));
    }
    
    @Test
    void testSettersAndGetters() {
        toggleButton.setX(200);
        assertEquals(200, toggleButton.getX());
        
        toggleButton.setY(300);
        assertEquals(300, toggleButton.getY());
        
        toggleButton.setWidth(150);
        assertEquals(150, toggleButton.getWidth());
        
        toggleButton.setHeight(40);
        assertEquals(40, toggleButton.getHeight());
        
        toggleButton.setState(true);
        assertTrue(toggleButton.getState());
        
        toggleButton.setLabelOn("Active");
        assertEquals("Active", toggleButton.getLabelOn());
        
        toggleButton.setLabelOff("Inactive");
        assertEquals("Inactive", toggleButton.getLabelOff());
    }
    
    @Test
    void testRenderWithNullContext() {
        // Should not throw exception when rendering with null context (headless testing)
        assertDoesNotThrow(() -> toggleButton.render(null, 0.016f, null));
    }
    
    @Test
    void testAnimationProgressInitiallyZero() {
        assertEquals(0.0f, toggleButton.getAnimationProgress());
    }
    
    @Test
    void testAnimationProgressAfterToggle() {
        toggleButton.toggle();
        
        // Update animation system
        animationSystem.update(0.016f);
        
        // Render to update animation progress
        toggleButton.render(null, 0.016f, null);
        
        // Animation progress should be greater than 0 after toggle and update
        assertTrue(toggleButton.getAnimationProgress() > 0.0f);
    }
    
    @Test
    void testMultipleToggles() {
        // Toggle multiple times
        for (int i = 0; i < 5; i++) {
            toggleButton.toggle();
            boolean expectedState = ((i + 1) % 2) == 1;
            assertEquals(expectedState, toggleButton.getState());
        }
    }
    
    @Test
    void testToggleButtonWithNullAnimationSystem() {
        // Should work without animation system (no animations, but functional)
        ToggleButton buttonWithoutAnimation = new ToggleButton(100, 100, 120, 30, "ON", "OFF", null);
        
        assertDoesNotThrow(() -> buttonWithoutAnimation.toggle());
        assertTrue(buttonWithoutAnimation.getState());
        
        assertDoesNotThrow(() -> buttonWithoutAnimation.render(null, 0.016f, null));
    }
}
