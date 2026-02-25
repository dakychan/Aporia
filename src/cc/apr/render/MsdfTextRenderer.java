package cc.apr.render;

import com.ferra13671.cometrenderer.CometLoaders;
import com.ferra13671.cometrenderer.CometRenderer;
import com.ferra13671.cometrenderer.glsl.GlProgram;
import com.ferra13671.cometrenderer.glsl.GlProgramSnippet;
import com.ferra13671.cometrenderer.glsl.shader.ShaderType;
import com.ferra13671.cometrenderer.glsl.uniform.UniformType;
import com.ferra13671.cometrenderer.glsl.uniform.uniforms.FloatUniform;
import com.ferra13671.cometrenderer.glsl.uniform.uniforms.Vec4GlUniform;
import com.ferra13671.cometrenderer.plugins.minecraft.AbstractMinecraftPlugin;
import com.ferra13671.cometrenderer.plugins.minecraft.CustomVertexElementTypes;
import com.ferra13671.cometrenderer.plugins.minecraft.CustomVertexFormats;
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import com.ferra13671.cometrenderer.vertex.DrawMode;
import com.ferra13671.cometrenderer.vertex.element.VertexElementType;
import com.ferra13671.cometrenderer.vertex.mesh.Mesh;

public class MsdfTextRenderer {
   private final MsdfFont font;
   private static GlProgram msdfProgram;
   private static boolean programInitialized = false;

   public MsdfTextRenderer(MsdfFont font) {
      this.font = font;
      initMsdfProgram();
   }

   private static void initMsdfProgram() {
      if (!programInitialized) {
         try {
            msdfProgram = CometLoaders.IN_JAR
               .createProgramBuilder(new GlProgramSnippet[0])
               .name("msdf-text")
               .shader(CometLoaders.IN_JAR.createGlslFileEntry("shader.msdf-text.vertex", "assets/aporia/shaders/msdf_text.vert"), ShaderType.Vertex)
               .shader(CometLoaders.IN_JAR.createGlslFileEntry("shader.msdf-text.fragment", "assets/aporia/shaders/msdf_text.frag"), ShaderType.Fragment)
               .sampler("Sampler0")
               .uniform("ColorModulator", UniformType.VEC4)
               .uniform("pxRange", UniformType.FLOAT)
               .build();
            programInitialized = true;
         } catch (Exception var1) {
            var1.printStackTrace();
         }
      }
   }

   public void drawText(float x, float y, float size, String text, RenderColor color) {
      if (text != null && !text.isEmpty() && !(size <= 0.0F) && msdfProgram != null) {
         GlProgram previousProgram = CometRenderer.getGlobalProgram();
         CometRenderer.setGlobalProgram(msdfProgram);
         CometRenderer.initShaderColor();
         AbstractMinecraftPlugin.getInstance().initMatrix();
         msdfProgram.getSampler(0).set(this.font.getAtlas());
         ((Vec4GlUniform)msdfProgram.getUniform("ColorModulator", UniformType.VEC4)).set(color.toVector4f());
         float scale = size / Math.max(1.0E-6F, this.font.getEmSize());
         float pxRange = this.font.getDistanceRange();
         ((FloatUniform)msdfProgram.getUniform("pxRange", UniformType.FLOAT)).set(pxRange);
         float lineHeight = this.font.getLineHeight() * scale;
         float baselineY = y;
         String[] lines = text.split("\n", -1);

         for (String line : lines) {
            this.drawTextLine(x, baselineY, scale, line);
            baselineY += lineHeight;
         }

         if (previousProgram != null) {
            CometRenderer.setGlobalProgram(previousProgram);
            CometRenderer.initShaderColor();
            AbstractMinecraftPlugin.getInstance().initMatrix();
         }
      }
   }

   private void drawTextLine(float x, float baseline, float scale, String line) {
      if (!line.isEmpty()) {
         Mesh mesh = CometRenderer.createMesh(
            DrawMode.QUADS,
            CustomVertexFormats.POSITION_TEXTURE_COLOR,
            builder -> {
               float penX = x;
               float baselineY = baseline;
               int prevCodepoint = -1;
               int i = 0;
               int glyphCount = 0;

               while (i < line.length()) {
                  int cp = line.codePointAt(i);
                  int cpLen = Character.charCount(cp);
                  i += cpLen;
                  MsdfFont.Glyph glyph = this.font.getGlyph(cp);
                  int glyphCode = cp;
                  if (glyph == null) {
                     glyph = this.font.getGlyph(63);
                     glyphCode = 63;
                     if (glyph == null) {
                        continue;
                     }
                  }

                  if (prevCodepoint != -1) {
                     penX += this.font.getKerning(prevCodepoint, glyphCode) * scale;
                  }

                  if (glyph.renderable) {
                     float x0 = penX + glyph.planeLeft * scale;
                     float y0 = baselineY - glyph.planeTop * scale;
                     float width = (glyph.planeRight - glyph.planeLeft) * scale;
                     float height = (glyph.planeTop - glyph.planeBottom) * scale;
                     if (width > 0.0F && height > 0.0F) {
                        float x2 = x0 + width;
                        float y2 = y0 + height;
                        RenderColor white = RenderColor.WHITE;
                        builder.vertex(x0, y0, 0.0F)
                           .element("Texture", VertexElementType.FLOAT, new Float[]{glyph.u0, glyph.v0})
                           .element("Color", CustomVertexElementTypes.RENDER_COLOR, new RenderColor[]{white});
                        builder.vertex(x0, y2, 0.0F)
                           .element("Texture", VertexElementType.FLOAT, new Float[]{glyph.u0, glyph.v1})
                           .element("Color", CustomVertexElementTypes.RENDER_COLOR, new RenderColor[]{white});
                        builder.vertex(x2, y2, 0.0F)
                           .element("Texture", VertexElementType.FLOAT, new Float[]{glyph.u1, glyph.v1})
                           .element("Color", CustomVertexElementTypes.RENDER_COLOR, new RenderColor[]{white});
                        builder.vertex(x2, y0, 0.0F)
                           .element("Texture", VertexElementType.FLOAT, new Float[]{glyph.u1, glyph.v0})
                           .element("Color", CustomVertexElementTypes.RENDER_COLOR, new RenderColor[]{white});
                        glyphCount++;
                     }
                  }

                  penX += glyph.advance * scale;
                  prevCodepoint = glyphCode;
               }

               if (glyphCount > 0) {
               }
            }
         );
         if (mesh != null) {
            CometRenderer.draw(mesh);
         }
      }
   }

   public float measureWidth(String text, float size) {
      if (text != null && !text.isEmpty() && !(size <= 0.0F)) {
         float scale = size / Math.max(1.0E-6F, this.font.getEmSize());
         float maxWidth = 0.0F;
         String[] lines = text.split("\n", -1);

         for (String line : lines) {
            maxWidth = Math.max(maxWidth, this.measureLineWidth(line, scale));
         }

         return maxWidth;
      } else {
         return 0.0F;
      }
   }

   private float measureLineWidth(String line, float scale) {
      if (line.isEmpty()) {
         return 0.0F;
      } else {
         float penX = 0.0F;
         int prevCodepoint = -1;
         int i = 0;

         while (i < line.length()) {
            int cp = line.codePointAt(i);
            int cpLen = Character.charCount(cp);
            i += cpLen;
            MsdfFont.Glyph glyph = this.font.getGlyph(cp);
            int glyphCode = cp;
            if (glyph == null) {
               glyph = this.font.getGlyph(63);
               glyphCode = 63;
               if (glyph == null) {
                  continue;
               }
            }

            if (prevCodepoint != -1) {
               penX += this.font.getKerning(prevCodepoint, glyphCode) * scale;
            }

            penX += glyph.advance * scale;
            prevCodepoint = glyphCode;
         }

         return penX;
      }
   }

   public float measureHeight(String text, float size) {
      if (text != null && !text.isEmpty() && !(size <= 0.0F)) {
         float scale = size / Math.max(1.0E-6F, this.font.getEmSize());
         float lineHeight = this.font.getLineHeight() * scale;
         int lineCount = text.split("\n", -1).length;
         return lineHeight * lineCount;
      } else {
         return 0.0F;
      }
   }
}
