package ru.render;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

public class BlurShaderTest {
    
    private BlurShader blurShader;
    
    @BeforeEach
    public void setUp() {
        blurShader = new BlurShader();
    }
    
    @Test
    public void testSetBlurRadiusWithinRange() {
        blurShader.setBlurRadius(4.0f);
        assertEquals(4.0f, blurShader.getBlurRadius(), 0.001f);
    }
    
    @Test
    public void testSetBlurRadiusClampMin() {
        blurShader.setBlurRadius(1.0f);
        assertEquals(2.0f, blurShader.getBlurRadius(), 0.001f);
    }
    
    @Test
    public void testSetBlurRadiusClampMax() {
        blurShader.setBlurRadius(10.0f);
        assertEquals(6.0f, blurShader.getBlurRadius(), 0.001f);
    }
    
    @Test
    public void testSetBlurRadiusAtMinBoundary() {
        blurShader.setBlurRadius(2.0f);
        assertEquals(2.0f, blurShader.getBlurRadius(), 0.001f);
    }
    
    @Test
    public void testSetBlurRadiusAtMaxBoundary() {
        blurShader.setBlurRadius(6.0f);
        assertEquals(6.0f, blurShader.getBlurRadius(), 0.001f);
    }
    
    @Test
    public void testSetPassesWithinRange() {
        blurShader.setPasses(3);
        assertEquals(3, blurShader.getPasses());
    }
    
    @Test
    public void testSetPassesClampMin() {
        blurShader.setPasses(0);
        assertEquals(1, blurShader.getPasses());
    }
    
    @Test
    public void testSetPassesClampMax() {
        blurShader.setPasses(10);
        assertEquals(6, blurShader.getPasses());
    }
    
    @Test
    public void testSetPassesAtMinBoundary() {
        blurShader.setPasses(1);
        assertEquals(1, blurShader.getPasses());
    }
    
    @Test
    public void testSetPassesAtMaxBoundary() {
        blurShader.setPasses(6);
        assertEquals(6, blurShader.getPasses());
    }
    
    @Test
    public void testDefaultBlurRadius() {
        assertEquals(4.0f, blurShader.getBlurRadius(), 0.001f);
    }
    
    @Test
    public void testDefaultPasses() {
        assertEquals(2, blurShader.getPasses());
    }
    
    @Test
    public void testCleanupDoesNotThrow() {
        assertDoesNotThrow(() -> blurShader.cleanup());
    }
}
