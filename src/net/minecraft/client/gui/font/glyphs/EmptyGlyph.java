package net.minecraft.client.gui.font.glyphs;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.UnbakedGlyph;
import com.mojang.blaze3d.font.UnbakedGlyph.Stitcher;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.network.chat.Style;


import org.jspecify.annotations.Nullable;


public class EmptyGlyph implements UnbakedGlyph {
   final GlyphInfo info;

   public EmptyGlyph(float p_431122_) {
      this.info = GlyphInfo.simple(p_431122_);
   }

   public GlyphInfo info() {
      return this.info;
   }

   public BakedGlyph bake(Stitcher p_428658_) {
      return new BakedGlyph() {
         @Override
         public GlyphInfo info() {
            return EmptyGlyph.this.info;
         }

         @Override
         public TextRenderable.@Nullable Styled createGlyph(
            float p_430771_, float p_430332_, int p_429540_, int p_429940_, Style p_431398_, float p_431664_, float p_427174_
         ) {
            return null;
         }
      };
   }
}
