package ru.render;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Bug Condition Exploration Test for Blur Shader Vertex Data Issue
 *
 * **Validates: Requirements 1.1, 1.2, 1.3**
 *
 * Property 1: Fault Condition - Blur Shader Receives Vertex Data
 *
 * CRITICAL: This test MUST FAIL on unfixed code - failure confirms the bug exists.
 *
 * The bug manifests when drawRectangleWithBlur() is called with any positive blur amount.
 * The blur_fullscreen.vert shader used incorrect attribute names ('Position' and 'Texture')
 * but the mesh provides 'pos' (vec4), 'UV' (vec2), and 'color' (vec4) via
 * CustomVertexFormats.POSITION_TEXTURE_COLOR format. This name mismatch causes the shader to
 * receive no vertex data, resulting in no blur effect being rendered.
 *
 * Expected behavior on UNFIXED code:
 * - Test FAILS because blur effect is invisible (shader receives no vertex data)
 * - Counterexamples: blur amounts 5, 10, 15, 20, 30 all produce no visible blur
 *
 * Expected behavior on FIXED code:
 * - Test PASSES because blur effect is visible (shader receives vertex data correctly)
 * - The shader now uses 'pos', 'UV', and 'color' matching the vertex format
 */
public class BlurShaderBugConditionTest {
    
    /**
     * Test that blur shader attribute names match the vertex format.
     *
     * This test verifies the root cause: the shader should use 'pos' and 'UV'
     * to match CustomVertexFormats.POSITION_TEXTURE_COLOR format used by MsdfTextRenderer.
     *
     * CustomVertexFormats.POSITION_TEXTURE_COLOR provides:
     * - "pos" (vec4) - vertex position
     * - "UV" (vec2) - texture coordinates
     * - "color" (vec4) - vertex color
     *
     * On UNFIXED code: shader uses 'Position' and 'Texture' (wrong names) -> test FAILS
     * On FIXED code: shader uses 'pos' and 'UV' (correct names) -> test PASSES
     */
    @Test
    public void testBlurShaderAttributeNamesMismatch() {
        // Read the blur_fullscreen.vert shader content
        String shaderPath = "assets/aporia/shaders/blur_fullscreen.vert";
        String shaderContent = readShaderFromResources(shaderPath);

        assertNotNull(shaderContent, "Blur shader file should exist");

        // Check if shader uses the CORRECT attribute names that match CustomVertexFormats.POSITION_TEXTURE_COLOR
        // This format provides: pos (vec4), UV (vec2), color (vec4)
        boolean usesCorrectPositionAttribute = shaderContent.contains("in vec4 pos");
        boolean usesCorrectTextureAttribute = shaderContent.contains("in vec2 UV");

        // On UNFIXED code: shader uses 'Position' and 'Texture' (wrong names) -> test FAILS
        // On FIXED code: shader uses 'pos' and 'UV' (correct names) -> test PASSES
        assertTrue(usesCorrectPositionAttribute,
            "Blur shader should use 'pos' attribute to match CustomVertexFormats.POSITION_TEXTURE_COLOR. " +
            "Current shader uses wrong attribute name which causes vertex data mismatch. " +
            "Counterexample: Any call to drawRectangleWithBlur() with positive blur amount " +
            "produces no visible blur effect because shader receives no vertex data.");

        assertTrue(usesCorrectTextureAttribute,
            "Blur shader should use 'UV' attribute to match CustomVertexFormats.POSITION_TEXTURE_COLOR. " +
            "Current shader uses wrong attribute name which causes vertex data mismatch. " +
            "Counterexample: Any call to drawRectangleWithBlur() with positive blur amount " +
            "produces no visible blur effect because shader receives no UV coordinates.");
    }
    
    /**
     * Test that blur shader uses correct position attribute type.
     *
     * CustomVertexFormats.POSITION_TEXTURE_COLOR provides pos as vec4,
     * so the shader should declare it as vec4.
     */
    @Test
    public void testBlurShaderPositionAttributeType() {
        String shaderPath = "assets/aporia/shaders/blur_fullscreen.vert";
        String shaderContent = readShaderFromResources(shaderPath);

        assertNotNull(shaderContent, "Blur shader file should exist");

        // Check if shader declares position as vec4 to match CustomVertexFormats.POSITION_TEXTURE_COLOR
        // On UNFIXED code: shader uses 'vec3 Position' or 'vec2 Position' -> incompatible
        // On FIXED code: shader uses 'vec4 pos' -> compatible
        boolean usesVec4Position = shaderContent.contains("in vec4 pos");

        assertTrue(usesVec4Position,
            "Blur shader should declare pos as vec4 to match CustomVertexFormats.POSITION_TEXTURE_COLOR. " +
            "Current shader uses incompatible type. " +
            "Counterexample: drawRectangleWithBlur(100, 100, 200, 200, color, 5, 10) " +
            "produces no visible blur effect due to type mismatch.");
    }

    /**
     * Test that blur shader correctly uses pos for 2D coordinates.
     *
     * Since pos is vec4 but we need 2D coordinates for fullscreen quad,
     * the shader should use pos.xy or just pos directly.
     */
    @Test
    public void testBlurShaderUsesPositionCorrectly() {
        String shaderPath = "assets/aporia/shaders/blur_fullscreen.vert";
        String shaderContent = readShaderFromResources(shaderPath);

        assertNotNull(shaderContent, "Blur shader file should exist");

        // Check if shader uses pos for gl_Position
        // On UNFIXED code: shader uses 'Position.xy' (wrong variable name)
        // On FIXED code: shader uses 'pos' directly
        boolean usesPos = shaderContent.contains("gl_Position = pos");

        assertTrue(usesPos,
            "Blur shader should use pos for gl_Position. " +
            "Current shader uses wrong variable name. " +
            "Counterexample: drawRectangleWithBlur(0, 0, 500, 300, color, 0, 5) " +
            "produces no visible blur effect because gl_Position receives no data.");
    }

    /**
     * Test that blur shader correctly passes texture coordinates to fragment shader.
     *
     * The shader should read from 'UV' attribute and pass it to vUv output.
     */
    @Test
    public void testBlurShaderPassesTextureCoordinates() {
        String shaderPath = "assets/aporia/shaders/blur_fullscreen.vert";
        String shaderContent = readShaderFromResources(shaderPath);

        assertNotNull(shaderContent, "Blur shader file should exist");

        // Check if shader assigns UV to vUv
        // On UNFIXED code: shader uses 'vUv = Texture' (wrong variable name)
        // On FIXED code: shader uses 'vUv = UV' (correct variable name)
        boolean assignsUvToVUv = shaderContent.contains("vUv = UV");

        assertTrue(assignsUvToVUv,
            "Blur shader should assign UV attribute to vUv output variable. " +
            "Current shader uses wrong variable name. " +
            "Counterexample: drawRectangleWithBlur(50, 50, 100, 100, color, 10, 20) " +
            "produces no visible blur effect because fragment shader receives no UV coordinates.");
    }
    
    /**
     * Helper method to read shader content from resources.
     * This simulates reading the shader file that would be loaded by the game.
     */
    private String readShaderFromResources(String path) {
        try {
            java.io.InputStream stream = getClass().getClassLoader().getResourceAsStream(path);
            if (stream == null) {
                return null;
            }
            java.util.Scanner scanner = new java.util.Scanner(stream).useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
