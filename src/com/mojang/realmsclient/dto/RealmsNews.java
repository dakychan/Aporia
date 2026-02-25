package com.mojang.realmsclient.dto;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import net.minecraft.util.LenientJsonParser;


import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;


public record RealmsNews(@Nullable String newsLink) {
   private static final Logger LOGGER = LogUtils.getLogger();

   public static RealmsNews parse(String p_87472_) {
      String s = null;

      try {
         JsonObject jsonobject = LenientJsonParser.parse(p_87472_).getAsJsonObject();
         s = JsonUtils.getStringOr("newsLink", jsonobject, null);
      } catch (Exception var3) {
         LOGGER.error("Could not parse RealmsNews", var3);
      }

      return new RealmsNews(s);
   }
}
