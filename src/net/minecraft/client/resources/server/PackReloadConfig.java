package net.minecraft.client.resources.server;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;




public interface PackReloadConfig {
   void scheduleReload(PackReloadConfig.Callbacks var1);

   
   public interface Callbacks {
      void onSuccess();

      void onFailure(boolean var1);

      List<PackReloadConfig.IdAndPath> packsToLoad();
   }

   
   public record IdAndPath(UUID id, Path path) {
   }
}
