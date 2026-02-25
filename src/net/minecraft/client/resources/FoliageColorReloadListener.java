package net.minecraft.client.resources;

import java.io.IOException;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.FoliageColor;




public class FoliageColorReloadListener extends SimplePreparableReloadListener<int[]> {
   private static final Identifier LOCATION = Identifier.withDefaultNamespace("textures/colormap/foliage.png");

   protected int[] prepare(ResourceManager p_118660_, ProfilerFiller p_118661_) {
      try {
         return LegacyStuffWrapper.getPixels(p_118660_, LOCATION);
      } catch (IOException var4) {
         throw new IllegalStateException("Failed to load foliage color texture", var4);
      }
   }

   protected void apply(int[] p_118667_, ResourceManager p_118668_, ProfilerFiller p_118669_) {
      FoliageColor.init(p_118667_);
   }
}
