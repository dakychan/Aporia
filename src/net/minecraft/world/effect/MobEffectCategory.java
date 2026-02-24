package net.minecraft.world.effect;

public enum MobEffectCategory {
   BENEFICIAL(net.minecraft.ChatFormatting.BLUE),
   HARMFUL(net.minecraft.ChatFormatting.RED),
   NEUTRAL(net.minecraft.ChatFormatting.BLUE);

   private final net.minecraft.ChatFormatting tooltipFormatting;

   private MobEffectCategory(final net.minecraft.ChatFormatting p_19496_) {
      this.tooltipFormatting = p_19496_;
   }

   public net.minecraft.ChatFormatting getTooltipFormatting() {
      return this.tooltipFormatting;
   }
}
