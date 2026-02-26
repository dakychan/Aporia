package ru.utils.render;

import com.ferra13671.cometrenderer.CometLoaders;
import com.ferra13671.cometrenderer.CometRenderer;
import com.ferra13671.cometrenderer.glsl.GlProgram;
import com.ferra13671.cometrenderer.glsl.shader.ShaderType;
import com.ferra13671.cometrenderer.glsl.uniform.UniformType;
import com.mojang.blaze3d.pipeline.TextureTarget;
import net.minecraft.client.Minecraft;
import org.joml.Vector2f;

public class BlurShader {
    private static GlProgram blurHorizontalProgram;
    private static GlProgram blurVerticalProgram;
    private static GlProgram blurCombinedProgram; // NEW: Single-pass 2D blur
    private static GlProgram blurDisplayProgram; // NEW: For final rendering with matrices
    private static GlProgram simpleDisplayProgram; // Simple texture display without blur
    private static boolean initialized = false;

    private float blurRadius = 15.0f; // Увеличен с 4.0 до 15.0 для видимого blur эффекта
    private int passes = 2;
    private TextureTarget blurFramebuffer;

    public BlurShader() {
        initProgram();
    }

    private static void initProgram() {
        if (initialized) return;

        try {
            // Create combined 2D blur program (single pass)
            blurCombinedProgram = CometLoaders.IN_JAR.createProgramBuilder()
                    .name("blur_combined")
                    .shader(CometLoaders.IN_JAR.createGlslFileEntry("blur_ndc", "assets/aporia/shaders/blur_ndc.vert"), ShaderType.Vertex)
                    .shader(CometLoaders.IN_JAR.createGlslFileEntry("blur_combined", "assets/aporia/shaders/blur_combined.frag"), ShaderType.Fragment)
                    .sampler("uSource")
                    .uniform("uTexelSize", UniformType.VEC2)
                    .uniform("uRadius", UniformType.FLOAT)
                    .build();

            // Create horizontal blur program with NDC vertex shader
            blurHorizontalProgram = CometLoaders.IN_JAR.createProgramBuilder()
                    .name("blur_horizontal")
                    .shader(CometLoaders.IN_JAR.createGlslFileEntry("blur_ndc", "assets/aporia/shaders/blur_ndc.vert"), ShaderType.Vertex)
                    .shader(CometLoaders.IN_JAR.createGlslFileEntry("blur_horizontal", "assets/aporia/shaders/blur_small_horizontal.frag"), ShaderType.Fragment)
                    .sampler("uSource")
                    .uniform("uTexelSize", UniformType.VEC2)
                    .uniform("uRadius", UniformType.FLOAT)
                    .build();

            // Create vertical blur program with NDC vertex shader
            blurVerticalProgram = CometLoaders.IN_JAR.createProgramBuilder()
                    .name("blur_vertical")
                    .shader(CometLoaders.IN_JAR.createGlslFileEntry("blur_ndc", "assets/aporia/shaders/blur_ndc.vert"), ShaderType.Vertex)
                    .shader(CometLoaders.IN_JAR.createGlslFileEntry("blur_vertical", "assets/aporia/shaders/blur_small_vertical.frag"), ShaderType.Fragment)
                    .sampler("uSource")
                    .uniform("uTexelSize", UniformType.VEC2)
                    .uniform("uRadius", UniformType.FLOAT)
                    .build();

            // Создаем display программу с blur для финального рендера
            blurDisplayProgram = CometLoaders.IN_JAR.createProgramBuilder()
                    .name("blur_display")
                    .shader(CometLoaders.IN_JAR.createGlslFileEntry("blur_vert", "assets/aporia/shaders/blur.vert"), ShaderType.Vertex)
                    .shader(CometLoaders.IN_JAR.createGlslFileEntry("blur_frag", "assets/aporia/shaders/blur.frag"), ShaderType.Fragment)
                    .sampler("Sampler0")
                    .uniform("direction", com.ferra13671.cometrenderer.glsl.uniform.UniformType.VEC2)
                    .uniform("radius", com.ferra13671.cometrenderer.glsl.uniform.UniformType.FLOAT)
                    .build();

            // Создаем простую display программу БЕЗ blur для отображения уже размытой текстуры
            simpleDisplayProgram = CometLoaders.IN_JAR.createProgramBuilder()
                    .name("simple_display")
                    .shader(CometLoaders.IN_JAR.createGlslFileEntry("display_vert", "assets/aporia/shaders/display.vert"), ShaderType.Vertex)
                    .shader(CometLoaders.IN_JAR.createGlslFileEntry("display_frag", "assets/aporia/shaders/display.frag"), ShaderType.Fragment)
                    .sampler("Sampler0")
                    .build();

            initialized = true;
            System.out.println("[BLUR] All programs initialized successfully");
        } catch (Exception e) {
            System.err.println("[BLUR] Failed to initialize programs:");
            e.printStackTrace();
        }
    }

    public void setBlurRadius(float radius) {
        this.blurRadius = Math.max(2.0f, Math.min(30.0f, radius));
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

    public GlProgram getCombinedProgram() {
        return blurCombinedProgram;
    }

    public GlProgram getHorizontalProgram() {
        return blurHorizontalProgram;
    }

    public GlProgram getVerticalProgram() {
        return blurVerticalProgram;
    }

    // NEW: Getter for display program
    public GlProgram getDisplayProgram() {
        return blurDisplayProgram;
    }

    // NEW: Getter for simple display program (no blur)
    public GlProgram getSimpleDisplayProgram() {
        return simpleDisplayProgram;
    }

    public boolean areProgramsInitialized() {
        return blurCombinedProgram != null && blurHorizontalProgram != null && blurVerticalProgram != null && blurDisplayProgram != null && simpleDisplayProgram != null;
    }

    public void renderToFramebuffer(int width, int height, Runnable guiRenderer) {
        if (!initialized || blurHorizontalProgram == null || blurVerticalProgram == null) {
            if (guiRenderer != null) {
                guiRenderer.run();
            }
            return;
        }

        if (blurFramebuffer == null || blurFramebuffer.width != width || blurFramebuffer.height != height) {
            if (blurFramebuffer != null) {
                blurFramebuffer.destroyBuffers();
            }
            blurFramebuffer = new TextureTarget("blur_framebuffer", width, height, true);
        }

        if (guiRenderer != null) {
            guiRenderer.run();
        }
    }

    public void cleanup() {
        if (blurFramebuffer != null) {
            blurFramebuffer.destroyBuffers();
            blurFramebuffer = null;
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
