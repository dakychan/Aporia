package ru.ui.clickgui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.ui.clickgui.comp.Slider;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Slider drag functionality.
 * Validates Requirements 4.1, 4.2, 4.3, 4.4, 4.5
 */
class SliderDragTest {
    
    private Slider slider;
    
    @BeforeEach
    void setUp() {
        slider = new Slider("Test Slider", 50f, 0f, 100f);
    }
    
    @Test
    void testInitialDraggingState() {
        // Requirement 4.1: dragging should be false initially
        // We can't directly access the dragging field, but we can test behavior
        // If not dragging, mouseDragged should not change value
        float initialValue = slider.getValue();
        slider.mouseDragged(200, 100);
        assertEquals(initialValue, slider.getValue(), "Value should not change when not dragging");
    }
    
    @Test
    void testMousePressedStartsDragging() {
        // Requirement 4.1, 4.2: mousePressed should start dragging mode
        // Set slider bounds at position (100, 100) with width 200
        slider.setBounds(100, 100, 200);
        
        // Click within slider bounds
        boolean handled = slider.mouseClicked(150, 110, 0);
        assertTrue(handled, "Slider should handle click within bounds");
        
        // Now mouseDragged should update the value
        slider.mouseDragged(150, 110);
        // Value should be updated based on position
        assertTrue(slider.getValue() >= 0 && slider.getValue() <= 100, 
            "Value should be within bounds after drag");
    }
    
    @Test
    void testMouseDraggedUpdatesValue() {
        // Requirement 4.2, 4.3: mouseDragged should update value when dragging
        slider.setBounds(100, 100, 200);
        slider.mouseClicked(150, 110, 0);
        
        // Drag to 25% position (100 + 200*0.25 = 150)
        slider.mouseDragged(150, 110);
        float value1 = slider.getValue();
        
        // Drag to 75% position (100 + 200*0.75 = 250)
        slider.mouseDragged(250, 110);
        float value2 = slider.getValue();
        
        assertTrue(value2 > value1, "Value should increase when dragging right");
    }
    
    @Test
    void testMouseReleasedStopsDragging() {
        // Requirement 4.4: mouseReleased should stop dragging
        slider.setBounds(100, 100, 200);
        slider.mouseClicked(150, 110, 0);
        
        // Drag to a position
        slider.mouseDragged(200, 110);
        float valueWhileDragging = slider.getValue();
        
        // Release mouse
        boolean handled = slider.mouseReleased(200, 110, 0);
        assertTrue(handled, "Slider should handle mouse release");
        
        // Try to drag again - should not change value
        slider.mouseDragged(250, 110);
        assertEquals(valueWhileDragging, slider.getValue(), 
            "Value should not change after mouse release");
    }
    
    @Test
    void testValueBoundsInvariant() {
        // Requirement 4.5: value should always be within [min, max]
        slider.setBounds(100, 100, 200);
        slider.mouseClicked(150, 110, 0);
        
        // Try to drag beyond left boundary
        slider.mouseDragged(0, 110);
        assertTrue(slider.getValue() >= 0f, "Value should not go below min");
        
        // Try to drag beyond right boundary
        slider.mouseDragged(500, 110);
        assertTrue(slider.getValue() <= 100f, "Value should not exceed max");
    }
    
    @Test
    void testValueBoundsWithSetValue() {
        // Requirement 4.5: setValue should also enforce bounds
        slider.setValue(-10f);
        assertEquals(0f, slider.getValue(), "setValue should clamp to min");
        
        slider.setValue(150f);
        assertEquals(100f, slider.getValue(), "setValue should clamp to max");
        
        slider.setValue(50f);
        assertEquals(50f, slider.getValue(), "setValue should accept valid value");
    }
    
    @Test
    void testClickOutsideBoundsDoesNotStartDrag() {
        // Requirement 4.1: clicking outside should not start dragging
        slider.setBounds(100, 100, 200);
        
        // Click outside slider bounds
        boolean handled = slider.mouseClicked(50, 50, 0);
        assertFalse(handled, "Slider should not handle click outside bounds");
        
        // mouseDragged should not change value
        float initialValue = slider.getValue();
        slider.mouseDragged(150, 110);
        assertEquals(initialValue, slider.getValue(), 
            "Value should not change when click was outside bounds");
    }
    
    @Test
    void testDragLifecycle() {
        // Requirement 4.1, 4.4: Complete drag lifecycle
        slider.setBounds(100, 100, 200);
        
        // Initial state: not dragging
        float initialValue = slider.getValue();
        slider.mouseDragged(200, 110);
        assertEquals(initialValue, slider.getValue(), "Should not drag initially");
        
        // Press: start dragging
        slider.mouseClicked(150, 110, 0);
        // Drag to 75% position (should be different from initial 50%)
        slider.mouseDragged(250, 110);
        float draggedValue = slider.getValue();
        assertNotEquals(initialValue, draggedValue, "Should update value while dragging");
        
        // Release: stop dragging
        slider.mouseReleased(250, 110, 0);
        // Try to drag to a different position
        slider.mouseDragged(150, 110);
        assertEquals(draggedValue, slider.getValue(), "Should not drag after release");
    }
}
