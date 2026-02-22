package ru.debug;

import com.mojang.blaze3d.pipeline.RenderTarget;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;

public class FramebufferDebug {
    private static int screenshotCounter = 0;
    
    /**
     * Сохраняет текущий framebuffer в PNG файл для debug
     */
    public static void saveCurrentFramebuffer(String location) {
        try {
            // Получаем текущий FBO
            int currentFbo = GL30.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
            
            // Получаем размеры viewport
            int[] viewport = new int[4];
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
            int width = viewport[2];
            int height = viewport[3];
            
            if (width <= 0 || height <= 0) {
                System.out.println("[DEBUG] " + location + " - Invalid viewport: " + width + "x" + height);
                return;
            }
            
            // Читаем pixels
            ByteBuffer buffer = org.lwjgl.BufferUtils.createByteBuffer(width * height * 4);
            GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
            
            // Проверяем не пустой ли
            boolean isEmpty = true;
            for (int i = 0; i < Math.min(1000, buffer.capacity()); i++) {
                if (buffer.get(i) != 0) {
                    isEmpty = false;
                    break;
                }
            }
            buffer.rewind();
            
            // Создаем image
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int i = (y * width + x) * 4;
                    int r = buffer.get(i) & 0xFF;
                    int g = buffer.get(i + 1) & 0xFF;
                    int b = buffer.get(i + 2) & 0xFF;
                    int a = buffer.get(i + 3) & 0xFF;
                    image.setRGB(x, height - 1 - y, (a << 24) | (r << 16) | (g << 8) | b);
                }
            }
            
            // Сохраняем
            String filename = String.format("debug_%03d_%s.png", screenshotCounter++, location.replaceAll("[^a-zA-Z0-9]", "_"));
            ImageIO.write(image, "PNG", new File(filename));
            
            System.out.println("[DEBUG] " + location + " - FBO=" + currentFbo + ", Size=" + width + "x" + height + 
                             ", Empty=" + isEmpty + ", Saved to " + filename);
        } catch (Exception e) {
            System.err.println("[DEBUG] Failed to save framebuffer at " + location + ": " + e.getMessage());
        }
    }
    
    /**
     * Сохраняет конкретный RenderTarget
     */
    public static void saveRenderTarget(RenderTarget target, String location) {
        if (target == null) {
            System.out.println("[DEBUG] " + location + " - RenderTarget is NULL");
            return;
        }
        
        try {
            int texId = ((com.mojang.blaze3d.opengl.GlTexture)target.getColorTexture()).glId();
            int width = target.width;
            int height = target.height;
            
            // Создаем временный FBO
            int tempFbo = GL30.glGenFramebuffers();
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, tempFbo);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, texId, 0);
            
            // Читаем pixels
            ByteBuffer buffer = org.lwjgl.BufferUtils.createByteBuffer(width * height * 4);
            GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
            
            // Проверяем не пустой ли
            boolean isEmpty = true;
            for (int i = 0; i < Math.min(1000, buffer.capacity()); i++) {
                if (buffer.get(i) != 0) {
                    isEmpty = false;
                    break;
                }
            }
            buffer.rewind();
            
            // Создаем image
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int i = (y * width + x) * 4;
                    int r = buffer.get(i) & 0xFF;
                    int g = buffer.get(i + 1) & 0xFF;
                    int b = buffer.get(i + 2) & 0xFF;
                    int a = buffer.get(i + 3) & 0xFF;
                    image.setRGB(x, height - 1 - y, (a << 24) | (r << 16) | (g << 8) | b);
                }
            }
            
            // Сохраняем
            String filename = String.format("debug_%03d_%s.png", screenshotCounter++, location.replaceAll("[^a-zA-Z0-9]", "_"));
            ImageIO.write(image, "PNG", new File(filename));
            
            // Cleanup
            GL30.glDeleteFramebuffers(tempFbo);
            
            System.out.println("[DEBUG] " + location + " - TexID=" + texId + ", Size=" + width + "x" + height + 
                             ", Empty=" + isEmpty + ", Saved to " + filename);
        } catch (Exception e) {
            System.err.println("[DEBUG] Failed to save RenderTarget at " + location + ": " + e.getMessage());
        }
    }
}
