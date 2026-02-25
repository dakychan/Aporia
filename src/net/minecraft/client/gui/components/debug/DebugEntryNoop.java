package net.minecraft.client.gui.components.debug;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;


import org.jspecify.annotations.Nullable;


public class DebugEntryNoop implements DebugScreenEntry {
   private final boolean isAllowedWithReducedDebugInfo;

   public DebugEntryNoop() {
      this(false);
   }

   public DebugEntryNoop(boolean p_429492_) {
      this.isAllowedWithReducedDebugInfo = p_429492_;
   }

   @Override
   public void display(DebugScreenDisplayer p_429238_, @Nullable Level p_423621_, @Nullable LevelChunk p_426327_, @Nullable LevelChunk p_430722_) {
   }

   @Override
   public boolean isAllowed(boolean p_425715_) {
      return this.isAllowedWithReducedDebugInfo || !p_425715_;
   }

   @Override
   public DebugEntryCategory category() {
      return DebugEntryCategory.RENDERER;
   }
}
