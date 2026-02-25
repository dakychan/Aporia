package net.minecraft.client.gui.components.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;


import org.jspecify.annotations.Nullable;


public class DebugEntryEntityRenderStats implements DebugScreenEntry {
   @Override
   public void display(DebugScreenDisplayer p_424103_, @Nullable Level p_422575_, @Nullable LevelChunk p_424093_, @Nullable LevelChunk p_431435_) {
      String s = Minecraft.getInstance().levelRenderer.getEntityStatistics();
      if (s != null) {
         p_424103_.addLine(s);
      }
   }

   @Override
   public boolean isAllowed(boolean p_429848_) {
      return true;
   }
}
