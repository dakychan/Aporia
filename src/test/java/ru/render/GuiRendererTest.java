package ru.render;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GuiRenderer class.
 */
class GuiRendererTest {
    
    private GuiRenderer guiRenderer;
    
    @BeforeEach
    void setUp() {
        guiRenderer = new GuiRenderer();
    }
    
    @Test
    void testConstructor() {
        assertNotNull(guiRenderer.getBlurRenderer(), "BlurRenderer should be initialized");
        assertNotNull(guiRenderer.getAnimationSystem(), "AnimationSystem should be initialized");
        assertFalse(guiRenderer.isInitialized(), "GuiRenderer should not be initialized yet");
    }
    
    @Test
    void testInitialize() {
        guiRenderer.initialize();
        assertTrue(guiRenderer.isInitialized(), "GuiRenderer should be initialized");
    }
    
    @Test
    void testInitializeIdempotent() {
        guiRenderer.initialize();
        guiRenderer.initialize();
        assertTrue(guiRenderer.isInitialized(), "GuiRenderer should remain initialized");
    }
    
    @Test
    void testCleanup() {
        guiRenderer.initialize();
        guiRenderer.cleanup();
        assertFalse(guiRenderer.isInitialized(), "GuiRenderer should not be initialized after cleanup");
    }
    
    @Test
    void testGetBlurRenderer() {
        BlurRenderer blurRenderer = guiRenderer.getBlurRenderer();
        assertNotNull(blurRenderer, "BlurRenderer should not be null");
        assertSame(blurRenderer, guiRenderer.getBlurRenderer(), "Should return same instance");
    }
    
    @Test
    void testGetAnimationSystem() {
        AnimationSystem animationSystem = guiRenderer.getAnimationSystem();
        assertNotNull(animationSystem, "AnimationSystem should not be null");
        assertSame(animationSystem, guiRenderer.getAnimationSystem(), "Should return same instance");
    }
}
