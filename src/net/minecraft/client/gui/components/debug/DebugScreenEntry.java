package net.minecraft.client.gui.components.debug;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;


import org.jspecify.annotations.Nullable;


public interface DebugScreenEntry {
   void display(DebugScreenDisplayer var1, @Nullable Level var2, @Nullable LevelChunk var3, @Nullable LevelChunk var4);

   default boolean isAllowed(boolean p_424604_) {
      return !p_424604_;
   }

   default DebugEntryCategory category() {
      return DebugEntryCategory.SCREEN_TEXT;
   }
}
