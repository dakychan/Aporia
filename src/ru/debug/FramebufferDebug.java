package ru.debug;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public class FramebufferDebug {
   private static int screenshotCounter = 0;

   public static void saveCurrentFramebuffer(String location) {
      try {
         int currentFbo = GL30.glGetInteger(36006);
         int[] viewport = new int[4];
         GL11.glGetIntegerv(2978, viewport);
         int width = viewport[2];
         int height = viewport[3];
         if (width <= 0 || height <= 0) {
            System.out.println("[DEBUG] " + location + " - Invalid viewport: " + width + "x" + height);
            return;
         }

         ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
         GL11.glReadPixels(0, 0, width, height, 6408, 5121, buffer);
         boolean isEmpty = true;

         for (int i = 0; i < Math.min(1000, buffer.capacity()); i++) {
            if (buffer.get(i) != 0) {
               isEmpty = false;
               break;
            }
         }

         buffer.rewind();
         BufferedImage image = new BufferedImage(width, height, 2);

         for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
               int ix = (y * width + x) * 4;
               int r = buffer.get(ix) & 255;
               int g = buffer.get(ix + 1) & 255;
               int b = buffer.get(ix + 2) & 255;
               int a = buffer.get(ix + 3) & 255;
               image.setRGB(x, height - 1 - y, a << 24 | r << 16 | g << 8 | b);
            }
         }

         String filename = String.format("debug_%03d_%s.png", screenshotCounter++, location.replaceAll("[^a-zA-Z0-9]", "_"));
         ImageIO.write(image, "PNG", new File(filename));
         System.out
            .println("[DEBUG] " + location + " - FBO=" + currentFbo + ", Size=" + width + "x" + height + ", Empty=" + isEmpty + ", Saved to " + filename);
      } catch (Exception var15) {
         System.err.println("[DEBUG] Failed to save framebuffer at " + location + ": " + var15.getMessage());
      }
   }

   public static void saveRenderTarget(RenderTarget target, String location) {
      if (target == null) {
         System.out.println("[DEBUG] " + location + " - RenderTarget is NULL");
      } else {
         try {
            int texId = ((GlTexture)target.getColorTexture()).glId();
            int width = target.width;
            int height = target.height;
            int tempFbo = GL30.glGenFramebuffers();
            GL30.glBindFramebuffer(36160, tempFbo);
            GL30.glFramebufferTexture2D(36160, 36064, 3553, texId, 0);
            ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
            GL11.glReadPixels(0, 0, width, height, 6408, 5121, buffer);
            boolean isEmpty = true;

            for (int i = 0; i < Math.min(1000, buffer.capacity()); i++) {
               if (buffer.get(i) != 0) {
                  isEmpty = false;
                  break;
               }
            }

            buffer.rewind();
            BufferedImage image = new BufferedImage(width, height, 2);

            for (int y = 0; y < height; y++) {
               for (int x = 0; x < width; x++) {
                  int ix = (y * width + x) * 4;
                  int r = buffer.get(ix) & 255;
                  int g = buffer.get(ix + 1) & 255;
                  int b = buffer.get(ix + 2) & 255;
                  int a = buffer.get(ix + 3) & 255;
                  image.setRGB(x, height - 1 - y, a << 24 | r << 16 | g << 8 | b);
               }
            }

            String filename = String.format("debug_%03d_%s.png", screenshotCounter++, location.replaceAll("[^a-zA-Z0-9]", "_"));
            ImageIO.write(image, "PNG", new File(filename));
            GL30.glDeleteFramebuffers(tempFbo);
            System.out
               .println("[DEBUG] " + location + " - TexID=" + texId + ", Size=" + width + "x" + height + ", Empty=" + isEmpty + ", Saved to " + filename);
         } catch (Exception var16) {
            System.err.println("[DEBUG] Failed to save RenderTarget at " + location + ": " + var16.getMessage());
         }
      }
   }
}
