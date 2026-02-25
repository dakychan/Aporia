package net.minecraft.client.resources.model;

import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.resources.Identifier;


import org.jspecify.annotations.Nullable;


public interface UnbakedModel {
   String PARTICLE_TEXTURE_REFERENCE = "particle";

   default @Nullable Boolean ambientOcclusion() {
      return null;
   }

   default UnbakedModel.@Nullable GuiLight guiLight() {
      return null;
   }

   default @Nullable ItemTransforms transforms() {
      return null;
   }

   default TextureSlots.Data textureSlots() {
      return TextureSlots.Data.EMPTY;
   }

   default @Nullable UnbakedGeometry geometry() {
      return null;
   }

   default @Nullable Identifier parent() {
      return null;
   }

   
   public static enum GuiLight {
      FRONT("front"),
      SIDE("side");

      private final String name;

      private GuiLight(final String p_377886_) {
         this.name = p_377886_;
      }

      public static UnbakedModel.GuiLight getByName(String p_378162_) {
         for (UnbakedModel.GuiLight unbakedmodel$guilight : values()) {
            if (unbakedmodel$guilight.name.equals(p_378162_)) {
               return unbakedmodel$guilight;
            }
         }

         throw new IllegalArgumentException("Invalid gui light: " + p_378162_);
      }

      public boolean lightLikeBlock() {
         return this == SIDE;
      }
   }
}
