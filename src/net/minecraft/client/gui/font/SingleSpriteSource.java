package net.minecraft.client.gui.font;

import net.minecraft.client.gui.GlyphSource;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.util.RandomSource;




public record SingleSpriteSource(BakedGlyph glyph) implements GlyphSource {
   @Override
   public BakedGlyph getGlyph(int p_425593_) {
      return this.glyph;
   }

   @Override
   public BakedGlyph getRandomGlyph(RandomSource p_422752_, int p_424845_) {
      return this.glyph;
   }
}
