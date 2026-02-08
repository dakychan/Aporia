package ru.ui.clickgui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PanelPositionTest {
    
    @Test
    void testPanelPositionCreation() {
        PanelPosition position = new PanelPosition("COMBAT", 100, 50);
        
        assertEquals("COMBAT", position.getCategory());
        assertEquals(100, position.getX());
        assertEquals(50, position.getY());
    }
    
    @Test
    void testPanelPositionSetters() {
        PanelPosition position = new PanelPosition();
        
        position.setCategory("MOVEMENT");
        position.setX(200);
        position.setY(100);
        
        assertEquals("MOVEMENT", position.getCategory());
        assertEquals(200, position.getX());
        assertEquals(100, position.getY());
    }
}
