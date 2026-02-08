package ru.ui.clickgui;

import org.junit.jupiter.api.Test;
import ru.module.Module;

import static org.junit.jupiter.api.Assertions.*;

class CategoryPanelTest {
    
    @Test
    void testCategoryPanelCreation() {
        ClickGuiScreen.CategoryPanel panel = new ClickGuiScreen.CategoryPanel(
            Module.Category.COMBAT, 100, 50, 140, 400
        );
        
        assertEquals(Module.Category.COMBAT, panel.getCategory());
        assertEquals(100, panel.getX());
        assertEquals(50, panel.getY());
        assertEquals(140, panel.getWidth());
        assertEquals(400, panel.getHeight());
        assertFalse(panel.isDragging());
    }
    
    @Test
    void testSetPosition() {
        ClickGuiScreen.CategoryPanel panel = new ClickGuiScreen.CategoryPanel(
            Module.Category.MOVEMENT, 100, 50, 140, 400
        );
        
        panel.setPosition(200, 100);
        
        assertEquals(200, panel.getX());
        assertEquals(100, panel.getY());
    }
    
    @Test
    void testDraggingState() {
        ClickGuiScreen.CategoryPanel panel = new ClickGuiScreen.CategoryPanel(
            Module.Category.VISUALS, 100, 50, 140, 400
        );
        
        assertFalse(panel.isDragging());
        
        panel.setDragging(true);
        assertTrue(panel.isDragging());
        
        panel.setDragging(false);
        assertFalse(panel.isDragging());
    }
}
