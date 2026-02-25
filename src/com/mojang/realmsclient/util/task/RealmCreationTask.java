package com.mojang.realmsclient.util.task;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.exception.RealmsServiceException;
import net.minecraft.network.chat.Component;


import org.slf4j.Logger;


public class RealmCreationTask extends LongRunningTask {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component TITLE = Component.translatable("mco.create.world.wait");
   private final String name;
   private final String motd;
   private final long realmId;

   public RealmCreationTask(long p_329245_, String p_335972_, String p_329587_) {
      this.realmId = p_329245_;
      this.name = p_335972_;
      this.motd = p_329587_;
   }

   @Override
   public void run() {
      RealmsClient realmsclient = RealmsClient.getOrCreate();

      try {
         realmsclient.initializeRealm(this.realmId, this.name, this.motd);
      } catch (RealmsServiceException var3) {
         LOGGER.error("Couldn't create world", var3);
         this.error(var3);
      } catch (Exception var4) {
         LOGGER.error("Could not create world", var4);
         this.error(var4);
      }
   }

   @Override
   public Component getTitle() {
      return TITLE;
   }
}
