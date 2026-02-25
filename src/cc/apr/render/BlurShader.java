package cc.apr.render;

import com.ferra13671.cometrenderer.CometLoaders;
import com.ferra13671.cometrenderer.glsl.GlProgram;
import com.ferra13671.cometrenderer.glsl.GlProgramSnippet;
import com.ferra13671.cometrenderer.glsl.shader.ShaderType;
import com.ferra13671.cometrenderer.glsl.uniform.UniformType;
import com.mojang.blaze3d.pipeline.TextureTarget;

public class BlurShader {
   private static GlProgram blurHorizontalProgram;
   private static GlProgram blurVerticalProgram;
   private static GlProgram blurCombinedProgram;
   private static GlProgram blurDisplayProgram;
   private static GlProgram simpleDisplayProgram;
   private static boolean initialized = false;
   private float blurRadius = 15.0F;
   private int passes = 2;
   private TextureTarget blurFramebuffer;

   public BlurShader() {
      initProgram();
   }

   private static void initProgram() {
      if (!initialized) {
         try {
            blurCombinedProgram = CometLoaders.IN_JAR
               .createProgramBuilder(new GlProgramSnippet[0])
               .name("blur_combined")
               .shader(CometLoaders.IN_JAR.createGlslFileEntry("blur_ndc", "assets/aporia/shaders/blur_ndc.vert"), ShaderType.Vertex)
               .shader(CometLoaders.IN_JAR.createGlslFileEntry("blur_combined", "assets/aporia/shaders/blur_combined.frag"), ShaderType.Fragment)
               .sampler("uSource")
               .uniform("uTexelSize", UniformType.VEC2)
               .uniform("uRadius", UniformType.FLOAT)
               .build();
            blurHorizontalProgram = CometLoaders.IN_JAR
               .createProgramBuilder(new GlProgramSnippet[0])
               .name("blur_horizontal")
               .shader(CometLoaders.IN_JAR.createGlslFileEntry("blur_ndc", "assets/aporia/shaders/blur_ndc.vert"), ShaderType.Vertex)
               .shader(CometLoaders.IN_JAR.createGlslFileEntry("blur_horizontal", "assets/aporia/shaders/blur_small_horizontal.frag"), ShaderType.Fragment)
               .sampler("uSource")
               .uniform("uTexelSize", UniformType.VEC2)
               .uniform("uRadius", UniformType.FLOAT)
               .build();
            blurVerticalProgram = CometLoaders.IN_JAR
               .createProgramBuilder(new GlProgramSnippet[0])
               .name("blur_vertical")
               .shader(CometLoaders.IN_JAR.createGlslFileEntry("blur_ndc", "assets/aporia/shaders/blur_ndc.vert"), ShaderType.Vertex)
               .shader(CometLoaders.IN_JAR.createGlslFileEntry("blur_vertical", "assets/aporia/shaders/blur_small_vertical.frag"), ShaderType.Fragment)
               .sampler("uSource")
               .uniform("uTexelSize", UniformType.VEC2)
               .uniform("uRadius", UniformType.FLOAT)
               .build();
            blurDisplayProgram = CometLoaders.IN_JAR
               .createProgramBuilder(new GlProgramSnippet[0])
               .name("blur_display")
               .shader(CometLoaders.IN_JAR.createGlslFileEntry("blur_vert", "assets/aporia/shaders/blur.vert"), ShaderType.Vertex)
               .shader(CometLoaders.IN_JAR.createGlslFileEntry("blur_frag", "assets/aporia/shaders/blur.frag"), ShaderType.Fragment)
               .sampler("Sampler0")
               .uniform("direction", UniformType.VEC2)
               .uniform("radius", UniformType.FLOAT)
               .build();
            simpleDisplayProgram = CometLoaders.IN_JAR
               .createProgramBuilder(new GlProgramSnippet[0])
               .name("simple_display")
               .shader(CometLoaders.IN_JAR.createGlslFileEntry("display_vert", "assets/aporia/shaders/display.vert"), ShaderType.Vertex)
               .shader(CometLoaders.IN_JAR.createGlslFileEntry("display_frag", "assets/aporia/shaders/display.frag"), ShaderType.Fragment)
               .sampler("Sampler0")
               .build();
            initialized = true;
            System.out.println("[BLUR] All programs initialized successfully");
         } catch (Exception var1) {
            System.err.println("[BLUR] Failed to initialize programs:");
            var1.printStackTrace();
         }
      }
   }

   public void setBlurRadius(float radius) {
      this.blurRadius = Math.max(2.0F, Math.min(30.0F, radius));
   }

   public float getBlurRadius() {
      return this.blurRadius;
   }

   public void setPasses(int passes) {
      this.passes = Math.max(1, Math.min(6, passes));
   }

   public int getPasses() {
      return this.passes;
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

   public GlProgram getDisplayProgram() {
      return blurDisplayProgram;
   }

   public GlProgram getSimpleDisplayProgram() {
      return simpleDisplayProgram;
   }

   public boolean areProgramsInitialized() {
      return blurCombinedProgram != null
         && blurHorizontalProgram != null
         && blurVerticalProgram != null
         && blurDisplayProgram != null
         && simpleDisplayProgram != null;
   }

   public void renderToFramebuffer(int width, int height, Runnable guiRenderer) {
      if (initialized && blurHorizontalProgram != null && blurVerticalProgram != null) {
         if (this.blurFramebuffer == null || this.blurFramebuffer.width != width || this.blurFramebuffer.height != height) {
            if (this.blurFramebuffer != null) {
               this.blurFramebuffer.destroyBuffers();
            }

            this.blurFramebuffer = new TextureTarget("blur_framebuffer", width, height, true);
         }

         if (guiRenderer != null) {
            guiRenderer.run();
         }
      } else {
         if (guiRenderer != null) {
            guiRenderer.run();
         }
      }
   }

   public void cleanup() {
      if (this.blurFramebuffer != null) {
         this.blurFramebuffer.destroyBuffers();
         this.blurFramebuffer = null;
      }
   }

   public static boolean isInitialized() {
      return initialized;
   }
}
