package ru.utils.render;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;

/**
 * Manages blur rendering using KawaseBlurPipeline.
 * Blur is applied in a separate render pass before GUI rendering.
 */
public class BlurManager {
    private static BlurManager instance;
    private KawaseBlurPipeline kawaseBlur;
    private GpuTextureView cachedBlurredView;
    private int cachedBlurTexId = -1;
    private int blurIterations = 4;
    private float blurOffset = 1.0f;
    private boolean needsUpdate = true;
    
    private BlurManager() {
        kawaseBlur = new KawaseBlurPipeline();
    }
    
    public static BlurManager getInstance() {
        if (instance == null) {
            instance = new BlurManager();
        }
        return instance;
    }
    
    /**
     * Called from FrameGraph to apply blur to mainTarget.
     * This should be called AFTER world rendering but BEFORE GUI rendering.
     */
    public void applyBlur(RenderTarget mainTarget) {
        System.out.println("[BLUR MANAGER] applyBlur called");
        
        if (mainTarget == null) {
            System.err.println("[BLUR MANAGER] mainTarget is null");
            return;
        }
        
        System.out.println("[BLUR MANAGER] mainTarget size: " + mainTarget.width + "x" + mainTarget.height);
        
        GpuTexture sourceTexture = mainTarget.getColorTexture();
        GpuTextureView sourceView = mainTarget.getColorTextureView();
        
        if (sourceTexture == null || sourceView == null) {
            System.err.println("[BLUR MANAGER] Source texture/view is null");
            return;
        }
        
        System.out.println("[BLUR MANAGER] Starting blur with iterations=" + blurIterations + ", offset=" + blurOffset);
        
        try {
            // Apply Kawase blur
            cachedBlurredView = kawaseBlur.blur(
                sourceTexture, 
                sourceView, 
                mainTarget.width, 
                mainTarget.height, 
                blurIterations, 
                blurOffset
            );
            
            if (cachedBlurredView != null) {
                GpuTexture blurredTexture = cachedBlurredView.texture();
                if (blurredTexture instanceof GlTexture) {
                    cachedBlurTexId = ((GlTexture)blurredTexture).glId();
                    System.out.println("[BLUR MANAGER] Blur applied successfully, texId=" + cachedBlurTexId);
                    needsUpdate = false;
                } else {
                    System.err.println("[BLUR MANAGER] Blurred texture is not GlTexture");
                    cachedBlurTexId = -1;
                }
            } else {
                System.err.println("[BLUR MANAGER] Blur failed - returned null");
                cachedBlurTexId = -1;
            }
        } catch (Exception e) {
            System.err.println("[BLUR MANAGER] Exception during blur: " + e.getMessage());
            e.printStackTrace();
            cachedBlurTexId = -1;
        }
    }
    
    /**
     * Get the cached blurred texture ID.
     * Returns -1 if blur is not available.
     */
    public int getBlurredTextureId() {
        return cachedBlurTexId;
    }
    
    /**
     * Check if blurred texture is available.
     */
    public boolean isBlurAvailable() {
        return cachedBlurTexId > 0;
    }
    
    /**
     * Mark that blur needs to be updated on next frame.
     */
    public void markDirty() {
        needsUpdate = true;
    }
    
    public void setBlurIterations(int iterations) {
        this.blurIterations = Math.max(1, Math.min(8, iterations));
        markDirty();
    }
    
    public void setBlurOffset(float offset) {
        this.blurOffset = offset;
        markDirty();
    }
    
    public void setBlurRadius(float radius) {
        // Convert radius to iterations (approximately)
        this.blurIterations = Math.max(1, Math.min(8, (int)(radius / 3.0f)));
        markDirty();
    }
    
    public void cleanup() {
        if (kawaseBlur != null) {
            kawaseBlur.close();
            kawaseBlur = null;
        }
        cachedBlurredView = null;
        cachedBlurTexId = -1;
    }
}
