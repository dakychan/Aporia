package net.minecraft.client.renderer.entity.state;

import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;




public class HoldingEntityRenderState extends LivingEntityRenderState {
   public final ItemStackRenderState heldItem = new ItemStackRenderState();

   public static void extractHoldingEntityRenderState(LivingEntity p_376251_, HoldingEntityRenderState p_378625_, ItemModelResolver p_378710_) {
      p_378710_.updateForLiving(p_378625_.heldItem, p_376251_.getMainHandItem(), ItemDisplayContext.GROUND, p_376251_);
   }
}
