package com.mojang.realmsclient.client;

import java.net.Proxy;


import org.jspecify.annotations.Nullable;


public class RealmsClientConfig {
   private static @Nullable Proxy proxy;

   public static @Nullable Proxy getProxy() {
      return proxy;
   }

   public static void setProxy(Proxy p_87294_) {
      if (proxy == null) {
         proxy = p_87294_;
      }
   }
}
