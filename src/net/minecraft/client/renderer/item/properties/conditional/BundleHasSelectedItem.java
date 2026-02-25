package net.minecraft.client.renderer.item.properties.conditional;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;


import org.jspecify.annotations.Nullable;


public record BundleHasSelectedItem() implements ConditionalItemModelProperty {
   public static final MapCodec<BundleHasSelectedItem> MAP_CODEC = MapCodec.unit(new BundleHasSelectedItem());

   @Override
   public boolean get(ItemStack p_376405_, @Nullable ClientLevel p_376648_, @Nullable LivingEntity p_377927_, int p_375391_, ItemDisplayContext p_377648_) {
      return BundleItem.hasSelectedItem(p_376405_);
   }

   @Override
   public MapCodec<BundleHasSelectedItem> type() {
      return MAP_CODEC;
   }
}
