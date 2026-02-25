package net.minecraft.client.gui.components.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;


import org.jspecify.annotations.Nullable;


public class DebugEntryChunkRenderStats implements DebugScreenEntry {
   @Override
   public void display(DebugScreenDisplayer p_426358_, @Nullable Level p_427239_, @Nullable LevelChunk p_422413_, @Nullable LevelChunk p_422317_) {
      String s = Minecraft.getInstance().levelRenderer.getSectionStatistics();
      if (s != null) {
         p_426358_.addLine(s);
      }
   }

   @Override
   public boolean isAllowed(boolean p_426647_) {
      return true;
   }
}
