package net.minecraft.client.resources.server;

import java.util.UUID;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface PackLoadFeedback {
   void reportUpdate(UUID var1, PackLoadFeedback.Update var2);

   void reportFinalResult(UUID var1, PackLoadFeedback.FinalResult var2);

   @OnlyIn(Dist.CLIENT)
   public static enum FinalResult {
      DECLINED,
      APPLIED,
      DISCARDED,
      DOWNLOAD_FAILED,
      ACTIVATION_FAILED;
   }

   @OnlyIn(Dist.CLIENT)
   public static enum Update {
      ACCEPTED,
      DOWNLOADED;
   }
}
