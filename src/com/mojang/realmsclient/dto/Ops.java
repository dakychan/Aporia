package com.mojang.realmsclient.dto;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.util.LenientJsonParser;


import org.slf4j.Logger;


public record Ops(Set<String> ops) {
   private static final Logger LOGGER = LogUtils.getLogger();

   public static Ops parse(String p_87421_) {
      Set<String> set = new HashSet<>();

      try {
         JsonObject jsonobject = LenientJsonParser.parse(p_87421_).getAsJsonObject();
         JsonElement jsonelement = jsonobject.get("ops");
         if (jsonelement.isJsonArray()) {
            for (JsonElement jsonelement1 : jsonelement.getAsJsonArray()) {
               set.add(jsonelement1.getAsString());
            }
         }
      } catch (Exception var6) {
         LOGGER.error("Could not parse Ops", var6);
      }

      return new Ops(set);
   }
}
