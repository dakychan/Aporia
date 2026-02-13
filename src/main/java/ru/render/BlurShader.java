package ru.render;

import com.ferra13671.cometrenderer.CometLoaders;
import com.ferra13671.cometrenderer.CometRenderer;
import com.ferra13671.cometrenderer.glsl.GlProgram;
import com.ferra13671.cometrenderer.glsl.shader.ShaderType;
import com.ferra13671.cometrenderer.glsl.uniform.UniformType;
import org.joml.Vector2f;
import org.joml.Vector4f;

public class BlurShader {
    private static GlProgram blurProgram;
    private static boolean initialized = false;
    
    private float blurRadius = 4.0f;
    private int passes = 2;
    
    public BlurShader() {
        initProgram();
    }
    
    private static void initProgram() {
        if (initialized) return;
        
        try {
            blurProgram = CometLoaders.IN_JAR.createProgramBuilder()
                    .name("blur")
                    .shader(CometLoaders.IN_JAR.createGlslFileEntry("shader.blur.vertex", "assets/aporia/shaders/blur.vert"), ShaderType.Vertex)
                    .shader(CometLoaders.IN_JAR.createGlslFileEntry("shader.blur.fragment", "assets/aporia/shaders/blur.frag"), ShaderType.Fragment)
                    .sampler("Sampler0")
                    .uniform("BlurDir", UniformType.VEC2)
                    .uniform("Radius", UniformType.FLOAT)
                    .build();
            
            initialized = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void setBlurRadius(float radius) {
        this.blurRadius = Math.max(2.0f, Math.min(6.0f, radius));
    }
    
    public float getBlurRadius() {
        return blurRadius;
    }
    
    public void setPasses(int passes) {
        this.passes = Math.max(1, Math.min(6, passes));
    }
    
    public int getPasses() {
        return passes;
    }
    
    public void apply(int width, int height) {
        if (!initialized || blurProgram == null) {
            return;
        }
        
        GlProgram previousProgram = CometRenderer.getGlobalProgram();
        CometRenderer.setGlobalProgram(blurProgram);
        
        blurProgram.getUniform("Radius", UniformType.FLOAT).set(blurRadius);
        
        for (int i = 0; i < passes; i++) {
            blurProgram.getUniform("BlurDir", UniformType.VEC2).set(new Vector2f(1.0f, 0.0f));
            
            blurProgram.getUniform("BlurDir", UniformType.VEC2).set(new Vector2f(0.0f, 1.0f));
        }
        
        if (previousProgram != null) {
            CometRenderer.setGlobalProgram(previousProgram);
        }
    }
    
    public void cleanup() {
    }
    
    public static boolean isInitialized() {
        return initialized;
    }
}
