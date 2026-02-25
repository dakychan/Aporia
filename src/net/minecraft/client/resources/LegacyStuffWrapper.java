package net.minecraft.client.resources;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;




public class LegacyStuffWrapper {
   @Deprecated
   public static int[] getPixels(ResourceManager p_118727_, Identifier p_458530_) throws IOException {
      int[] aint;
      try (InputStream inputstream = p_118727_.open(p_458530_)) {
         NativeImage nativeimage = NativeImage.read(inputstream);

         try {
            aint = nativeimage.makePixelArray();
         } catch (Throwable var9) {
            if (nativeimage != null) {
               try {
                  nativeimage.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (nativeimage != null) {
            nativeimage.close();
         }
      }

      return aint;
   }
}
