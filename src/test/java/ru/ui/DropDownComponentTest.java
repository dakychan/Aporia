package ru.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import ru.render.AnimationSystem;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DropDownComponent class.
 * Tests specific examples and edge cases for dropdown component behavior.
 */
class DropDownComponentTest {

    private AnimationSystem animationSystem;
    private DropDownComponent dropdown;

    @BeforeEach
    void setUp() {
        animationSystem = new AnimationSystem();
        dropdown = new DropDownComponent("Test Dropdown", 100, 100, 200, animationSystem);
    }

    @Test
    void testConstructor() {
        assertEquals("Test Dropdown", dropdown.getTitle());
        assertEquals(100, dropdown.getX());
        assertEquals(100, dropdown.getY());
        assertEquals(200, dropdown.getWidth());
        assertFalse(dropdown.isExpanded(), "Dropdown should start collapsed");
        assertEquals(0.0f, dropdown.getAnimationProgress(), 0.001f);
        assertTrue(dropdown.getItems().isEmpty(), "Dropdown should start with no items");
    }

    @Test
    void testAddItem() {
        DropDownItem item1 = new DropDownItem("Item 1", () -> {});
        DropDownItem item2 = new DropDownItem("Item 2", () -> {});
        
        dropdown.addItem(item1);
        dropdown.addItem(item2);
        
        assertEquals(2, dropdown.getItems().size());
        assertEquals("Item 1", dropdown.getItems().get(0).getLabel());
        assertEquals("Item 2", dropdown.getItems().get(1).getLabel());
    }

    @Test
    void testAddNullItem() {
        dropdown.addItem(null);
        
        assertTrue(dropdown.getItems().isEmpty(), "Null items should not be added");
    }

    @Test
    void testToggleExpandsDropdown() {
        assertFalse(dropdown.isExpanded());
        
        dropdown.toggle();
        
        assertTrue(dropdown.isExpanded(), "Toggle should expand the dropdown");
    }

    @Test
    void testToggleCollapsesDropdown() {
        dropdown.toggle(); // Expand
        assertTrue(dropdown.isExpanded());
        
        dropdown.toggle(); // Collapse
        
        assertFalse(dropdown.isExpanded(), "Toggle should collapse the dropdown");
    }

    @Test
    void testToggleCreatesAnimation() {
        dropdown.toggle();
        
        // Animation should be created in the animation system
        // We can't directly check the animation ID, but we can verify the system has animations
        assertTrue(animationSystem.getActiveAnimationCount() > 0, 
            "Toggle should create an animation");
    }

    @Test
    void testMultipleToggles() {
        dropdown.toggle(); // Expand
        dropdown.toggle(); // Collapse
        dropdown.toggle(); // Expand again
        
        assertTrue(dropdown.isExpanded(), "Multiple toggles should work correctly");
    }

    @Test
    void testIsMouseOverTitleBar() {
        // Mouse over title bar
        assertTrue(dropdown.isMouseOver(150, 110), 
            "Mouse should be over title bar");
        
        // Mouse outside
        assertFalse(dropdown.isMouseOver(50, 50), 
            "Mouse should not be over dropdown");
    }

    @Test
    void testIsMouseOverExpandedItems() {
        dropdown.addItem(new DropDownItem("Item 1", () -> {}));
        dropdown.addItem(new DropDownItem("Item 2", () -> {}));
        dropdown.setExpanded(true);
        
        // Mouse over items area (title height is 30, item height is 25)
        assertTrue(dropdown.isMouseOver(150, 140), 
            "Mouse should be over expanded items area");
    }

    @Test
    void testIsMouseOverCollapsedItems() {
        dropdown.addItem(new DropDownItem("Item 1", () -> {}));
        dropdown.setExpanded(false);
        
        // Mouse over where items would be if expanded
        assertFalse(dropdown.isMouseOver(150, 140), 
            "Mouse should not be over collapsed items area");
    }

    @Test
    void testHandleClickOnTitleBar() {
        assertFalse(dropdown.isExpanded());
        
        boolean handled = dropdown.handleClick(150, 110);
        
        assertTrue(handled, "Click on title bar should be handled");
        assertTrue(dropdown.isExpanded(), "Click on title bar should toggle expansion");
    }

    @Test
    void testHandleClickOnItem() {
        boolean[] itemClicked = {false};
        dropdown.addItem(new DropDownItem("Item 1", () -> itemClicked[0] = true));
        dropdown.setExpanded(true);
        
        // Click on first item (y = 100 + 30 + 12 = 142, middle of first item)
        boolean handled = dropdown.handleClick(150, 142);
        
        assertTrue(handled, "Click on item should be handled");
        assertTrue(itemClicked[0], "Item callback should be executed");
    }

    @Test
    void testHandleClickOutside() {
        boolean handled = dropdown.handleClick(50, 50);
        
        assertFalse(handled, "Click outside dropdown should not be handled");
        assertFalse(dropdown.isExpanded(), "Click outside should not change state");
    }

    @Test
    void testHandleClickOnCollapsedItems() {
        boolean[] itemClicked = {false};
        dropdown.addItem(new DropDownItem("Item 1", () -> itemClicked[0] = true));
        dropdown.setExpanded(false);
        
        // Click where item would be if expanded
        boolean handled = dropdown.handleClick(150, 142);
        
        assertFalse(handled, "Click on collapsed items should not be handled");
        assertFalse(itemClicked[0], "Item callback should not be executed when collapsed");
    }

    @Test
    void testRenderWithoutTextRenderer() {
        // Should not throw exception when rendering without text renderer
        assertDoesNotThrow(() -> dropdown.render(null, 0.016f, null));
    }

    @Test
    void testRenderUpdatesAnimationProgress() {
        dropdown.toggle(); // Start expanding
        
        // Simulate several frames
        for (int i = 0; i < 10; i++) {
            animationSystem.update(0.016f);
            dropdown.render(null, 0.016f, null);
        }
        
        // Animation progress should have increased
        assertTrue(dropdown.getAnimationProgress() > 0.0f, 
            "Animation progress should increase during expansion");
    }

    @Test
    void testRenderWithEmptyTitle() {
        DropDownComponent emptyTitleDropdown = new DropDownComponent("", 100, 100, 200, animationSystem);
        
        assertDoesNotThrow(() -> emptyTitleDropdown.render(null, 0.016f, null));
    }

    @Test
    void testRenderWithNullTitle() {
        DropDownComponent nullTitleDropdown = new DropDownComponent(null, 100, 100, 200, animationSystem);
        
        assertDoesNotThrow(() -> nullTitleDropdown.render(null, 0.016f, null));
    }

    @Test
    void testRenderWithNoItems() {
        dropdown.setExpanded(true);
        
        assertDoesNotThrow(() -> dropdown.render(null, 0.016f, null));
    }

    @Test
    void testSetters() {
        dropdown.setTitle("New Title");
        assertEquals("New Title", dropdown.getTitle());
        
        dropdown.setX(200);
        assertEquals(200, dropdown.getX());
        
        dropdown.setY(300);
        assertEquals(300, dropdown.getY());
        
        dropdown.setWidth(400);
        assertEquals(400, dropdown.getWidth());
        
        dropdown.setExpanded(true);
        assertTrue(dropdown.isExpanded());
    }

    @Test
    void testGetItemsReturnsDefensiveCopy() {
        DropDownItem item = new DropDownItem("Item 1", () -> {});
        dropdown.addItem(item);
        
        List<DropDownItem> items = dropdown.getItems();
        items.clear();
        
        assertEquals(1, dropdown.getItems().size(), 
            "Modifying returned list should not affect internal list");
    }

    @Test
    void testAnimationProgressBounds() {
        dropdown.toggle(); // Start expanding
        
        // Simulate many frames to complete animation
        for (int i = 0; i < 100; i++) {
            animationSystem.update(0.1f);
            dropdown.render(null, 0.1f, null);
        }
        
        // Animation progress should be clamped to 1.0
        assertTrue(dropdown.getAnimationProgress() <= 1.0f, 
            "Animation progress should not exceed 1.0");
        assertTrue(dropdown.getAnimationProgress() >= 0.0f, 
            "Animation progress should not be negative");
    }

    @Test
    void testCollapseAnimation() {
        // First expand
        dropdown.toggle();
        for (int i = 0; i < 100; i++) {
            animationSystem.update(0.1f);
            dropdown.render(null, 0.1f, null);
        }
        
        // Then collapse
        dropdown.toggle();
        for (int i = 0; i < 100; i++) {
            animationSystem.update(0.1f);
            dropdown.render(null, 0.1f, null);
        }
        
        // Animation progress should return to 0.0
        assertEquals(0.0f, dropdown.getAnimationProgress(), 0.1f, 
            "Animation progress should return to 0.0 when collapsed");
    }

    @Test
    void testWithoutAnimationSystem() {
        DropDownComponent noAnimDropdown = new DropDownComponent("Test", 100, 100, 200, null);
        
        noAnimDropdown.toggle();
        
        // Should still work without animation system
        assertTrue(noAnimDropdown.isExpanded());
        assertDoesNotThrow(() -> noAnimDropdown.render(null, 0.016f, null));
    }

    @Test
    void testMouseOverBoundaries() {
        // Test exact boundaries
        assertTrue(dropdown.isMouseOver(100, 100), "Left-top corner should be inside");
        assertTrue(dropdown.isMouseOver(300, 100), "Right-top corner should be inside");
        assertTrue(dropdown.isMouseOver(100, 130), "Left-bottom corner should be inside");
        assertTrue(dropdown.isMouseOver(300, 130), "Right-bottom corner should be inside");
        
        // Test just outside boundaries
        assertFalse(dropdown.isMouseOver(99, 100), "Just left of boundary should be outside");
        assertFalse(dropdown.isMouseOver(301, 100), "Just right of boundary should be outside");
        assertFalse(dropdown.isMouseOver(100, 99), "Just above boundary should be outside");
        assertFalse(dropdown.isMouseOver(100, 131), "Just below boundary should be outside");
    }

    @Test
    void testMultipleItemsClick() {
        boolean[] item1Clicked = {false};
        boolean[] item2Clicked = {false};
        boolean[] item3Clicked = {false};
        
        dropdown.addItem(new DropDownItem("Item 1", () -> item1Clicked[0] = true));
        dropdown.addItem(new DropDownItem("Item 2", () -> item2Clicked[0] = true));
        dropdown.addItem(new DropDownItem("Item 3", () -> item3Clicked[0] = true));
        dropdown.setExpanded(true);
        
        // Click on second item (y = 100 + 30 + 25 + 12 = 167)
        dropdown.handleClick(150, 167);
        
        assertFalse(item1Clicked[0], "First item should not be clicked");
        assertTrue(item2Clicked[0], "Second item should be clicked");
        assertFalse(item3Clicked[0], "Third item should not be clicked");
    }
}
