package cc.apr.render;

import com.ferra13671.cometrenderer.CometRenderer;
import com.ferra13671.cometrenderer.CometVertexFormats;
import com.ferra13671.cometrenderer.buffer.framebuffer.Framebuffer;
import com.ferra13671.cometrenderer.buffer.framebuffer.FramebufferImpl;
import com.ferra13671.cometrenderer.glsl.GlProgram;
import com.ferra13671.cometrenderer.glsl.uniform.UniformType;
import com.ferra13671.cometrenderer.glsl.uniform.uniforms.FloatUniform;
import com.ferra13671.cometrenderer.glsl.uniform.uniforms.Vec2GlUniform;
import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin;
import com.ferra13671.cometrenderer.plugins.minecraft.RectColors;
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import com.ferra13671.cometrenderer.plugins.minecraft.drawer.impl.BasicTextureDrawer;
import com.ferra13671.cometrenderer.plugins.minecraft.drawer.impl.RoundedRectDrawer;
import com.ferra13671.cometrenderer.vertex.DrawMode;
import com.ferra13671.cometrenderer.vertex.element.VertexElementType;
import com.ferra13671.cometrenderer.vertex.mesh.Mesh;
import com.ferra13671.gltextureutils.atlas.TextureBorder;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.GpuTexture;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import org.joml.Vector2f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;

public class RectangularShader {
   private BlurShader blurShader;
   private Framebuffer captureFramebuffer;
   private Framebuffer tempFramebuffer;
   private boolean screenCaptured = false;
   private boolean useNextFrameCapture = false;
   private int worldOnlyTextureId = -1;
   private int worldOnlyTextureWidth = 0;
   private int worldOnlyTextureHeight = 0;
   private boolean worldOnlyTextureReady = false;
   private long lastDebugTime = 0L;
   private static final long DEBUG_INTERVAL = 5000L;
   private Mesh fullscreenQuadMesh = null;

   public RectangularShader() {
      this.blurShader = new BlurShader();
   }

   public void setBlurShader(BlurShader blurShader) {
      this.blurShader = blurShader;
   }

   private void ensureFramebuffer(int width, int height) {
      if (this.captureFramebuffer == null || this.captureFramebuffer.getWidth() != width || this.captureFramebuffer.getHeight() != height) {
         if (this.captureFramebuffer != null) {
            this.captureFramebuffer.delete();
         }

         this.captureFramebuffer = new FramebufferImpl("capture", false, width, height, new Color(0, 0, 0, 0), 1.0);
      }

      if (this.tempFramebuffer == null || this.tempFramebuffer.getWidth() != width || this.tempFramebuffer.getHeight() != height) {
         if (this.tempFramebuffer != null) {
            this.tempFramebuffer.delete();
         }

         this.tempFramebuffer = new FramebufferImpl("temp", false, width, height, new Color(0, 0, 0, 0), 1.0);
      }
   }

   public void beginBlurBatch(int screenWidth, int screenHeight) {
      System.out.println("[BLUR] === BEGIN BLUR BATCH ===");
      this.ensureFramebuffer(screenWidth, screenHeight);
      this.captureScreenContent(screenWidth, screenHeight);
      this.applyBlurToFramebuffer(screenWidth, screenHeight);
      this.screenCaptured = true;
      System.out.println("[BLUR] Screen captured and blurred for batch rendering");
   }

   public void endBlurBatch() {
      System.out.println("[BLUR] === END BLUR BATCH ===");
      this.screenCaptured = false;
   }

   public void captureForNextFrame(int screenWidth, int screenHeight) {
      System.out.println("[BLUR] === CAPTURE FOR NEXT FRAME ===");
      this.ensureFramebuffer(screenWidth, screenHeight);
      this.captureScreenContent(screenWidth, screenHeight);
      this.applyBlurToFramebuffer(screenWidth, screenHeight);
      this.useNextFrameCapture = true;
      System.out.println("[BLUR] Screen captured and blurred for NEXT frame");
   }

   public void setWorldOnlyTexture(int textureId, int width, int height) {
      this.worldOnlyTextureId = textureId;
      this.worldOnlyTextureWidth = width;
      this.worldOnlyTextureHeight = height;
      this.ensureFramebuffer(width, height);
      this.copyWorldTextureToTemp(textureId, width, height);
      this.applyBlurFromTempToCapture(width, height);
      this.worldOnlyTextureReady = true;
   }

   private void copyWorldTextureToTemp(int textureId, int width, int height) {
      int tempWorldFbo = GL30.glGenFramebuffers();
      GL30.glBindFramebuffer(36160, tempWorldFbo);
      GL30.glFramebufferTexture2D(36160, 36064, 3553, textureId, 0);
      this.tempFramebuffer.bind(false);
      int tempFbo = GL30.glGetInteger(36006);
      GL30.glBindFramebuffer(36008, tempWorldFbo);
      GL30.glBindFramebuffer(36009, tempFbo);
      GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, 16384, 9728);
      GL30.glDeleteFramebuffers(tempWorldFbo);
      MinecraftPlugin.getInstance().bindMainFramebuffer(false);
   }

   private void copyWorldOnlyTextureToFramebuffer(int textureId, int width, int height) {
      int captureTexId = this.captureFramebuffer.getColorTextureId();
      int tempWorldFbo = GL30.glGenFramebuffers();
      GL30.glBindFramebuffer(36160, tempWorldFbo);
      GL30.glFramebufferTexture2D(36160, 36064, 3553, textureId, 0);
      this.captureFramebuffer.bind(false);
      int captureFbo = GL30.glGetInteger(36006);
      GL30.glBindFramebuffer(36008, tempWorldFbo);
      GL30.glBindFramebuffer(36009, captureFbo);
      GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, 16384, 9728);
      GL30.glDeleteFramebuffers(tempWorldFbo);
      MinecraftPlugin.getInstance().bindMainFramebuffer(false);
   }

   private void captureScreenContent(int screenWidth, int screenHeight) {
      System.out.println("[BLUR] === captureScreenContent START ===");
      Minecraft mc = Minecraft.getInstance();
      RenderTarget mainTarget = mc.getMainRenderTarget();
      GpuTexture mcGpuTexture = mainTarget.getColorTexture();
      if (mcGpuTexture == null) {
         System.err.println("[BLUR] Main render target color texture is null!");
      } else {
         int mcTexId = ((GlTexture)mcGpuTexture).glId();
         int captureTexId = this.captureFramebuffer.getColorTextureId();
         System.out.println("[BLUR] MC texture ID: " + mcTexId + ", Capture texture ID: " + captureTexId);
         System.out.println("[BLUR] Size: " + screenWidth + "x" + screenHeight);
         int tempMcFbo = GL30.glGenFramebuffers();
         GL30.glBindFramebuffer(36160, tempMcFbo);
         GL30.glFramebufferTexture2D(36160, 36064, 3553, mcTexId, 0);
         int status = GL30.glCheckFramebufferStatus(36160);
         System.out.println("[BLUR] Temp MC FBO status: " + (status == 36053 ? "COMPLETE" : "INCOMPLETE " + status));
         this.captureFramebuffer.bind(false);
         int captureFbo = GL30.glGetInteger(36006);
         System.out.println("[BLUR] Temp MC FBO: " + tempMcFbo + ", Capture FBO: " + captureFbo);
         GL30.glBindFramebuffer(36008, tempMcFbo);
         GL30.glBindFramebuffer(36009, captureFbo);
         GL30.glBlitFramebuffer(0, 0, screenWidth, screenHeight, 0, 0, screenWidth, screenHeight, 16384, 9728);
         int err = GL11.glGetError();
         System.out.println("[BLUR] glBlitFramebuffer: " + (err == 0 ? "OK" : "ERROR " + err));
         GL30.glDeleteFramebuffers(tempMcFbo);
         MinecraftPlugin.getInstance().bindMainFramebuffer(false);
         System.out.println("[BLUR] === captureScreenContent END ===");
      }
   }

   private void applyBlurFromTempToCapture(int width, int height) {
      GlProgram combinedProgram = this.blurShader.getCombinedProgram();
      if (combinedProgram != null) {
         float radius = this.blurShader.getBlurRadius();
         int previousFbo = GL30.glGetInteger(36006);
         long currentTime = System.currentTimeMillis();
         this.captureFramebuffer.bind(false);
         GL11.glViewport(0, 0, width, height);
         GL11.glClear(16384);
         CometRenderer.setGlobalProgram(combinedProgram);
         ((Vec2GlUniform)combinedProgram.getUniform("uTexelSize", UniformType.VEC2)).set(new Vector2f(1.0F / width, 1.0F / height));
         ((FloatUniform)combinedProgram.getUniform("uRadius", UniformType.FLOAT)).set(radius);
         int tempTexId = this.tempFramebuffer.getColorTextureId();
         combinedProgram.getSampler(0).set(tempTexId);
         GL13.glActiveTexture(33984);
         GL11.glBindTexture(3553, tempTexId);
         Mesh blurMesh = CometRenderer.createMesh(DrawMode.QUADS, CometVertexFormats.POSITION_TEXTURE, builder -> {
            builder.vertex(-1.0F, -1.0F, 0.0F).element("Texture", VertexElementType.FLOAT, new Float[]{0.0F, 0.0F});
            builder.vertex(1.0F, -1.0F, 0.0F).element("Texture", VertexElementType.FLOAT, new Float[]{1.0F, 0.0F});
            builder.vertex(1.0F, 1.0F, 0.0F).element("Texture", VertexElementType.FLOAT, new Float[]{1.0F, 1.0F});
            builder.vertex(-1.0F, 1.0F, 0.0F).element("Texture", VertexElementType.FLOAT, new Float[]{0.0F, 1.0F});
         });
         if (blurMesh != null) {
            CometRenderer.draw(blurMesh);
         }

         GL30.glBindFramebuffer(36160, previousFbo);
         MinecraftPlugin.getInstance().bindMainFramebuffer(false);
      }
   }

   private void applyBlurToFramebuffer(int width, int height) {
      System.err.println("[BLUR] WARNING: applyBlurToFramebuffer() is deprecated!");
   }

   private void renderFullscreenQuad() {
      if (this.fullscreenQuadMesh == null) {
         this.fullscreenQuadMesh = CometRenderer.createMesh(DrawMode.QUADS, CometVertexFormats.POSITION_TEXTURE, builder -> {
            builder.vertex(-1.0F, -1.0F, 0.0F).element("Texture", VertexElementType.FLOAT, new Float[]{0.0F, 0.0F});
            builder.vertex(1.0F, -1.0F, 0.0F).element("Texture", VertexElementType.FLOAT, new Float[]{1.0F, 0.0F});
            builder.vertex(1.0F, 1.0F, 0.0F).element("Texture", VertexElementType.FLOAT, new Float[]{1.0F, 1.0F});
            builder.vertex(-1.0F, 1.0F, 0.0F).element("Texture", VertexElementType.FLOAT, new Float[]{0.0F, 1.0F});
         });
      }

      if (this.fullscreenQuadMesh != null) {
         CometRenderer.draw(this.fullscreenQuadMesh, false);
      }
   }

   private void renderBlurredBackground(float x, float y, float width, float height) {
      int texId = this.captureFramebuffer.getColorTextureId();
      long currentTime = System.currentTimeMillis();
      if (currentTime - this.lastDebugTime < 1000L) {
         System.out.println("[BLUR] renderBlurredBackground: Using BasicTextureDrawer with texture ID " + texId);
      }

      int screenWidth = Minecraft.getInstance().getWindow().getWidth();
      int screenHeight = Minecraft.getInstance().getWindow().getHeight();
      float u0 = x / screenWidth;
      float v0 = y / screenHeight;
      float u1 = (x + width) / screenWidth;
      float v1 = (y + height) / screenHeight;
      TextureBorder textureBorder = new TextureBorder(u0, v0, u1, v1);
      new BasicTextureDrawer().setTexture(texId).rectSized(x, y, width, height, textureBorder).build().tryDraw().close();
   }

   private void saveTextureToFile(int textureId, String filename) {
      try {
         int width = this.captureFramebuffer.getWidth();
         int height = this.captureFramebuffer.getHeight();
         ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
         GL11.glBindTexture(3553, textureId);
         GL11.glGetTexImage(3553, 0, 6408, 5121, buffer);
         BufferedImage image = new BufferedImage(width, height, 2);

         for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
               int i = (y * width + x) * 4;
               int r = buffer.get(i) & 255;
               int g = buffer.get(i + 1) & 255;
               int b = buffer.get(i + 2) & 255;
               int a = buffer.get(i + 3) & 255;
               image.setRGB(x, height - 1 - y, a << 24 | r << 16 | g << 8 | b);
            }
         }

         ImageIO.write(image, "PNG", new File(filename));
         System.out.println("[BLUR] Saved texture to " + filename);
      } catch (Exception var14) {
         System.err.println("[BLUR] Failed to save texture: " + var14.getMessage());
      }
   }

   public void drawRectangle(float x, float y, float width, float height, RenderColor color, float cornerRadius) {
      new RoundedRectDrawer().rectSized(x, y, width, height, cornerRadius, RectColors.oneColor(color)).build().tryDraw().close();
   }

   public void drawRectangle(float x, float y, float width, float height, int color, float cornerRadius) {
      int a = color >> 24 & 0xFF;
      int r = color >> 16 & 0xFF;
      int g = color >> 8 & 0xFF;
      int b = color & 0xFF;
      this.drawRectangle(x, y, width, height, RenderColor.of(r, g, b, a), cornerRadius);
   }

   public void drawRectangleWithBlur(float x, float y, float width, float height, RenderColor color, float cornerRadius, float blurAmount) {
      if (this.worldOnlyTextureReady && this.captureFramebuffer != null) {
         int texId = this.captureFramebuffer.getColorTextureId();
         long currentTime = System.currentTimeMillis();
         boolean shouldDebug = currentTime - this.lastDebugTime < 1000L;
         if (shouldDebug) {
            System.out.println("[BLUR] drawRectangleWithBlur: captureFramebuffer texture ID = " + texId);
         }

         this.renderBlurredBackground(x, y, width, height);

         float[] rgba = color.getColor();
         float originalAlpha = rgba[3];
         float tintAlpha = Math.max(0.08F, originalAlpha / 4.0F);
         RenderColor tintColor = RenderColor.of(rgba[0], rgba[1], rgba[2], tintAlpha);
         new RoundedRectDrawer().rectSized(x, y, width, height, cornerRadius, RectColors.oneColor(tintColor)).build().tryDraw().close();
      } else {
         this.drawRectangle(x, y, width, height, color, cornerRadius);
      }
   }

   public void drawRectangleWithBlur(float x, float y, float width, float height, int color, float cornerRadius, float blurAmount) {
      int a = color >> 24 & 0xFF;
      int r = color >> 16 & 0xFF;
      int g = color >> 8 & 0xFF;
      int b = color & 0xFF;
      this.drawRectangleWithBlur(x, y, width, height, RenderColor.of(r, g, b, a), cornerRadius, blurAmount);
   }

   public void cleanup() {
      this.blurShader.cleanup();
      if (this.captureFramebuffer != null) {
         this.captureFramebuffer.delete();
         this.captureFramebuffer = null;
      }

      if (this.tempFramebuffer != null) {
         this.tempFramebuffer.delete();
         this.tempFramebuffer = null;
      }

      if (this.fullscreenQuadMesh != null) {
         this.fullscreenQuadMesh.close();
         this.fullscreenQuadMesh = null;
      }
   }
}
