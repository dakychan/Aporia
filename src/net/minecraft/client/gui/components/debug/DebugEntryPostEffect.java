package net.minecraft.client.gui.components.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;


import org.jspecify.annotations.Nullable;


public class DebugEntryPostEffect implements DebugScreenEntry {
   @Override
   public void display(DebugScreenDisplayer p_424990_, @Nullable Level p_426587_, @Nullable LevelChunk p_431361_, @Nullable LevelChunk p_426455_) {
      Minecraft minecraft = Minecraft.getInstance();
      Identifier identifier = minecraft.gameRenderer.currentPostEffect();
      if (identifier != null) {
         p_424990_.addLine("Post: " + identifier);
      }
   }
}
