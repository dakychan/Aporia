package net.minecraft.client.gui.components.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;


import org.jspecify.annotations.Nullable;


public class DebugEntryParticleRenderStats implements DebugScreenEntry {
   @Override
   public void display(DebugScreenDisplayer p_423293_, @Nullable Level p_425796_, @Nullable LevelChunk p_430608_, @Nullable LevelChunk p_427478_) {
      p_423293_.addLine("P: " + Minecraft.getInstance().particleEngine.countParticles());
   }
}
