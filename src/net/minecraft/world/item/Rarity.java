package net.minecraft.world.item;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

public enum Rarity implements StringRepresentable {
   COMMON(0, "common", net.minecraft.ChatFormatting.WHITE),
   UNCOMMON(1, "uncommon", net.minecraft.ChatFormatting.YELLOW),
   RARE(2, "rare", net.minecraft.ChatFormatting.AQUA),
   EPIC(3, "epic", net.minecraft.ChatFormatting.LIGHT_PURPLE);

   public static final Codec<Rarity> CODEC = StringRepresentable.fromValues(Rarity::values);
   public static final IntFunction<Rarity> BY_ID = ByIdMap.continuous(p_328775_ -> p_328775_.id, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
   public static final StreamCodec<ByteBuf, Rarity> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, p_330010_ -> p_330010_.id);
   private final int id;
   private final String name;
   private final net.minecraft.ChatFormatting color;

   private Rarity(final int p_330136_, final String p_327766_, final net.minecraft.ChatFormatting p_43028_) {
      this.id = p_330136_;
      this.name = p_327766_;
      this.color = p_43028_;
   }

   public net.minecraft.ChatFormatting color() {
      return this.color;
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }
}
