package ru.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DropDownItem class.
 * Tests specific examples and edge cases for dropdown item behavior.
 */
class DropDownItemTest {

    private boolean callbackExecuted;

    @BeforeEach
    void setUp() {
        callbackExecuted = false;
    }

    @Test
    void testConstructorWithTwoParameters() {
        DropDownItem item = new DropDownItem("Test", () -> {});
        
        assertEquals("Test", item.getLabel());
        assertTrue(item.isEnabled(), "Item should be enabled by default");
        assertNotNull(item.getOnClick());
    }

    @Test
    void testConstructorWithThreeParameters() {
        DropDownItem item = new DropDownItem("Test", false, () -> {});
        
        assertEquals("Test", item.getLabel());
        assertFalse(item.isEnabled());
        assertNotNull(item.getOnClick());
    }

    @Test
    void testClickExecutesCallbackWhenEnabled() {
        DropDownItem item = new DropDownItem("Test", true, () -> callbackExecuted = true);
        
        item.click();
        
        assertTrue(callbackExecuted, "Callback should be executed when item is enabled");
    }

    @Test
    void testClickDoesNotExecuteCallbackWhenDisabled() {
        DropDownItem item = new DropDownItem("Test", false, () -> callbackExecuted = true);
        
        item.click();
        
        assertFalse(callbackExecuted, "Callback should not be executed when item is disabled");
    }

    @Test
    void testClickDoesNothingWhenCallbackIsNull() {
        DropDownItem item = new DropDownItem("Test", true, null);
        
        // Should not throw exception
        assertDoesNotThrow(() -> item.click());
    }

    @Test
    void testSettersAndGetters() {
        DropDownItem item = new DropDownItem("Initial", () -> {});
        
        item.setLabel("Updated");
        assertEquals("Updated", item.getLabel());
        
        item.setEnabled(false);
        assertFalse(item.isEnabled());
        
        Runnable newCallback = () -> callbackExecuted = true;
        item.setOnClick(newCallback);
        assertEquals(newCallback, item.getOnClick());
    }

    @Test
    void testEnableDisableToggle() {
        DropDownItem item = new DropDownItem("Test", true, () -> callbackExecuted = true);
        
        // Initially enabled, click should work
        item.click();
        assertTrue(callbackExecuted);
        
        // Disable and reset
        callbackExecuted = false;
        item.setEnabled(false);
        item.click();
        assertFalse(callbackExecuted, "Callback should not execute when disabled");
        
        // Re-enable
        item.setEnabled(true);
        item.click();
        assertTrue(callbackExecuted, "Callback should execute when re-enabled");
    }

    @Test
    void testEmptyLabel() {
        DropDownItem item = new DropDownItem("", () -> {});
        
        assertEquals("", item.getLabel());
        assertTrue(item.isEnabled());
    }

    @Test
    void testNullLabel() {
        DropDownItem item = new DropDownItem(null, () -> {});
        
        assertNull(item.getLabel());
        assertTrue(item.isEnabled());
    }

    @Test
    void testMultipleClicks() {
        int[] clickCount = {0};
        DropDownItem item = new DropDownItem("Test", () -> clickCount[0]++);
        
        item.click();
        item.click();
        item.click();
        
        assertEquals(3, clickCount[0], "Callback should be executed for each click");
    }

    @Test
    void testCallbackCanModifyItemState() {
        DropDownItem item = new DropDownItem("Test", () -> {});
        
        // Set callback that modifies the item itself
        item.setOnClick(() -> item.setLabel("Clicked"));
        
        item.click();
        
        assertEquals("Clicked", item.getLabel(), "Callback should be able to modify item state");
    }
}
