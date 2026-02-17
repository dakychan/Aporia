package ru.ui.clickgui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.ui.clickgui.comp.MultiSetting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic unit tests for MultiSetting component.
 * Tests core functionality without rendering.
 * 
 * Validates Requirements 1.2, 1.4, 1.5, 1.6
 */
class MultiSettingBasicTest {
    
    private MultiSetting multiSetting;
    private List<String> options;
    private List<String> selectedOptions;
    
    @BeforeEach
    void setUp() {
        options = Arrays.asList("Option1", "Option2", "Option3");
        selectedOptions = new ArrayList<>(Arrays.asList("Option1"));
        multiSetting = new MultiSetting("Test Setting", options, selectedOptions);
    }
    
    @Test
    void testInitialState() {
        assertFalse(multiSetting.isExpanded(), "MultiSetting should start collapsed");
        assertEquals(25, multiSetting.getHeight(), "Collapsed height should be 25");
    }
    
    @Test
    void testHeaderClickTogglesExpansion() {
        // Set bounds without rendering
        multiSetting.setBounds(0, 0, 200);
        
        // Click header
        boolean handled = multiSetting.mouseClicked(10, 10, 0);
        
        assertTrue(handled, "Header click should be handled");
        assertTrue(multiSetting.isExpanded(), "MultiSetting should expand after header click");
        
        // Update bounds for expanded state
        multiSetting.setBounds(0, 0, 200);
        
        // Click header again
        handled = multiSetting.mouseClicked(10, 10, 0);
        
        assertTrue(handled, "Second header click should be handled");
        assertFalse(multiSetting.isExpanded(), "MultiSetting should collapse after second header click");
    }
    
    @Test
    void testCheckboxToggle() {
        // Set bounds and expand the component
        multiSetting.setBounds(0, 0, 200);
        multiSetting.mouseClicked(10, 10, 0); // Expand
        multiSetting.setBounds(0, 0, 200); // Update bounds for expanded state
        
        int initialSize = selectedOptions.size();
        
        // Click on second option (Option2) which is not selected
        // Options start at y = 25 (header height)
        // Option1 is at y=25-49, Option2 is at y=50-74, Option3 is at y=75-99
        boolean handled = multiSetting.mouseClicked(10, 55, 0);
        
        assertTrue(handled, "Checkbox click should be handled");
        assertTrue(selectedOptions.contains("Option2"), "Option2 should be selected after click");
        assertEquals(initialSize + 1, selectedOptions.size(), "Selected options count should increase");
        
        // Click again to deselect
        handled = multiSetting.mouseClicked(10, 55, 0);
        
        assertTrue(handled, "Second checkbox click should be handled");
        assertFalse(selectedOptions.contains("Option2"), "Option2 should be deselected after second click");
        assertEquals(initialSize, selectedOptions.size(), "Selected options count should return to initial");
    }
    
    @Test
    void testSelectionPersistence() {
        // Select an option
        multiSetting.setBounds(0, 0, 200);
        multiSetting.mouseClicked(10, 10, 0); // Expand
        multiSetting.setBounds(0, 0, 200);
        multiSetting.mouseClicked(10, 55, 0); // Select Option2
        
        assertTrue(selectedOptions.contains("Option2"), "Option2 should be selected");
        
        // Collapse and expand again
        multiSetting.mouseClicked(10, 10, 0); // Collapse
        multiSetting.mouseClicked(10, 10, 0); // Expand
        multiSetting.setBounds(0, 0, 200);
        
        assertTrue(selectedOptions.contains("Option2"), "Option2 should still be selected after collapse/expand");
    }
    
    @Test
    void testOutsideClickDetection() {
        multiSetting.setBounds(0, 0, 200);
        multiSetting.mouseClicked(10, 10, 0); // Expand
        multiSetting.setBounds(0, 0, 200); // Update bounds for expanded state
        
        assertTrue(multiSetting.isExpanded(), "MultiSetting should be expanded");
        
        // Click outside the component bounds
        boolean isOutside = multiSetting.isClickOutside(300, 300);
        
        assertTrue(isOutside, "Click at (300, 300) should be outside component");
        
        // Collapse on outside click
        multiSetting.collapse();
        
        assertFalse(multiSetting.isExpanded(), "MultiSetting should collapse after outside click");
    }
    
    @Test
    void testClickInsideNotDetectedAsOutside() {
        multiSetting.setBounds(0, 0, 200);
        multiSetting.mouseClicked(10, 10, 0); // Expand
        multiSetting.setBounds(0, 0, 200); // Update bounds for expanded state
        
        // Click inside the component bounds
        boolean isOutside = multiSetting.isClickOutside(10, 10);
        
        assertFalse(isOutside, "Click inside component should not be detected as outside");
    }
}
