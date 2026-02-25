package net.minecraft.client.renderer.fog.environment;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;


import org.jspecify.annotations.Nullable;


public abstract class MobEffectFogEnvironment extends FogEnvironment {
   public abstract Holder<MobEffect> getMobEffect();

   @Override
   public boolean providesColor() {
      return false;
   }

   @Override
   public boolean modifiesDarkness() {
      return true;
   }

   @Override
   public boolean isApplicable(@Nullable FogType p_409479_, Entity p_410566_) {
      return p_410566_ instanceof LivingEntity livingentity && livingentity.hasEffect(this.getMobEffect());
   }
}
