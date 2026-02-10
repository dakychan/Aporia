package ru.render;

import com.ferra13671.cometrenderer.CometLoaders;
import com.ferra13671.cometrenderer.CometRenderer;
import com.ferra13671.cometrenderer.glsl.GlProgram;
import com.ferra13671.cometrenderer.glsl.shader.ShaderType;
import com.ferra13671.cometrenderer.glsl.uniform.UniformType;
import org.joml.Vector2f;
import org.joml.Vector4f;

public class BlurRenderer {
    private static GlProgram BLUR_PROGRAM;
    private static boolean initialized = false;
    
    private BlurColor currentColor = BlurColor.BLACK;
    private float blurRadius = 5.0f;
    
    public enum BlurColor {
        BLACK(0.0f, 0.0f, 0.0f),
        WHITE(1.0f, 1.0f, 1.0f);
        
        public final float r, g, b;
        
        BlurColor(float r, float g, float b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }
        
        public Vector4f toVector4f(float alpha) {
            return new Vector4f(r, g, b, alpha);
        }
    }
    
    static {
        initProgram();
    }
    
    private static void initProgram() {
        try {
            BLUR_PROGRAM = CometLoaders.IN_JAR.createProgramBuilder()
                    .name("gaussian-blur")
                    .shader(CometLoaders.IN_JAR.createGlslFileEntry(
                            "shader.blur.vertex", 
                            "assets/aporia/shaders/blur.vert"), 
                            ShaderType.Vertex)
                    .shader(CometLoaders.IN_JAR.createGlslFileEntry(
                            "shader.blur.fragment", 
                            "assets/aporia/shaders/blur.frag"), 
                            ShaderType.Fragment)
                    .uniform("Sampler0", UniformType.SAMPLER)
                    .uniform("BlurDir", UniformType.VEC2)
                    .uniform("Radius", UniformType.FLOAT)
                    .uniform("ColorTint", UniformType.VEC4)
                    .uniform("ModelViewMat", UniformType.MATRIX4)
                    .uniform("ProjMat", UniformType.MATRIX4)
                    .build();
            initialized = true;
            CometRenderer.getLogger().log("BlurRenderer: Blur shader loaded successfully");
        } catch (Exception e) {
            CometRenderer.getLogger().error("BlurRenderer: Failed to load blur shader: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void toggleColor() {
        currentColor = (currentColor == BlurColor.BLACK) ? BlurColor.WHITE : BlurColor.BLACK;
    }
    public void setBlurColor(BlurColor color) {
        this.currentColor = color;
    }
    public BlurColor getCurrentColor() {
        return currentColor;
    }
    public void setBlurRadius(float radius) {
        this.blurRadius = Math.max(0.0f, radius);
    }
    public float getBlurRadius() {
        return blurRadius;
    }
    public void applyBlur(int width, int height) {
        if (!initialized) {
            CometRenderer.getLogger().warn("BlurRenderer: Shader not initialized!");
            return;
        }
        
        GlProgram previousProgram = CometRenderer.getGlobalProgram();
        CometRenderer.setGlobalProgram(BLUR_PROGRAM);

        BLUR_PROGRAM.getUniform("Radius", UniformType.FLOAT).set(blurRadius);
        BLUR_PROGRAM.getUniform("ColorTint", UniformType.VEC4)
                .set(currentColor.toVector4f(0.3f));

        BLUR_PROGRAM.getUniform("BlurDir", UniformType.VEC2).set(new Vector2f(1.0f / width, 0.0f));

        BLUR_PROGRAM.getUniform("BlurDir", UniformType.VEC2).set(new Vector2f(0.0f, 1.0f / height));
        
        CometRenderer.setGlobalProgram(previousProgram);
    }
    
    public static GlProgram getBlurProgram() {
        return BLUR_PROGRAM;
    }
    
    public static boolean isInitialized() {
        return initialized;
    }
}
