package com.mojang.blaze3d.platform;

import java.io.File;
import java.time.Duration;
import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;
import net.minecraft.server.dedicated.ServerWatchdog;




public class ClientShutdownWatchdog {
   private static final Duration CRASH_REPORT_PRELOAD_LOAD = Duration.ofSeconds(15L);

   public static void startShutdownWatchdog(File p_361803_, long p_368685_) {
      Thread thread = new Thread(() -> {
         try {
            Thread.sleep(CRASH_REPORT_PRELOAD_LOAD);
         } catch (InterruptedException var4) {
            return;
         }

         CrashReport crashreport = ServerWatchdog.createWatchdogCrashReport("Client shutdown", p_368685_);
         Minecraft.saveReport(p_361803_, crashreport);
      });
      thread.setDaemon(true);
      thread.setName("Client shutdown watchdog");
      thread.start();
   }
}
