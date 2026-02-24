package net.minecraft.world.level.storage;

public record DataVersion(int version, String series) {
   public static final String MAIN_SERIES = "main";

   public boolean isSideSeries() {
      return !this.series.equals("main");
   }

   public boolean isCompatible(DataVersion p_193004_) {
      return net.minecraft.SharedConstants.DEBUG_OPEN_INCOMPATIBLE_WORLDS ? true : this.series().equals(p_193004_.series());
   }
}
