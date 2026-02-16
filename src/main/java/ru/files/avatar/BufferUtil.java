package ru.files.avatar;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.Map;

/**
 * Utility class for loading and registering textures from URLs.
 * Minimal version for avatar loading.
 */
public class BufferUtil {
    private static final Map<String, Integer> dynamicIdCounters = Maps.newHashMap();
    
    /**
     * Load head texture from URL.
     * 
     * @param url The URL to load from
     * @return DynamicTexture or null if failed
     * @throws IOException if URL cannot be read
     */
    public static DynamicTexture getHeadFromURL(String url) throws IOException {
        if (url == null || url.isEmpty()) return null;
        
        NativeImage image = NativeImage.read(new URL(url).openStream());
        NativeImage parsed = parseHead(image);
        return new DynamicTexture(() -> "Avatar from " + url, parsed);
    }
    
    /**
     * Parse and resize head image.
     * 
     * @param image The source image
     * @return Resized NativeImage
     */
    public static NativeImage parseHead(NativeImage image) {
        if (image == null) return null;
        
        int imageWidth = 22;
        int imageHeight = 22;
        int imageSrcWidth = image.getWidth();
        int srcHeight = image.getHeight();
        
        for (int imageSrcHeight = image.getHeight(); imageWidth < imageSrcWidth || imageHeight < imageSrcHeight; imageHeight *= 2) {
            imageWidth *= 2;
        }
        
        NativeImage imgNew = new NativeImage(imageWidth, imageHeight, true);
        for (int x = 0; x < imageSrcWidth; x++) {
            for (int y = 0; y < srcHeight; y++) {
                imgNew.setPixel(x, y, image.getPixel(x, y));
            }
        }
        image.close();
        return imgNew;
    }
    
    /**
     * Register dynamic texture with auto-incrementing ID.
     * 
     * @param prefix The prefix for the texture ID
     * @param texture The texture to register
     * @return Identifier for the registered texture
     */
    public static Identifier registerDynamicTexture(String prefix, DynamicTexture texture) {
        Integer integer = dynamicIdCounters.get(prefix);
        if (integer == null) {
            integer = 1;
        } else {
            integer = integer + 1;
        }
        
        dynamicIdCounters.put(prefix, integer);
        Identifier identifier = Identifier.withDefaultNamespace(String.format(Locale.ROOT, "dynamic/%s_%d", prefix, integer));
        Minecraft.getInstance().getTextureManager().register(identifier, texture);
        return identifier;
    }
}
