package com.mojang.blaze3d.font;

import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.client.gui.font.FontOption.Filter;


import org.jspecify.annotations.Nullable;


public interface GlyphProvider extends AutoCloseable {
   float BASELINE = 7.0F;

   @Override
   default void close() {
   }

   default @Nullable UnbakedGlyph getGlyph(int p_231091_) {
      return null;
   }

   IntSet getSupportedGlyphs();

   
   public record Conditional(GlyphProvider provider, Filter filter) implements AutoCloseable {
      @Override
      public void close() {
         this.provider.close();
      }
   }
}
