package cc.apr.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Lang {
   private static final Map<String, String> translations = new HashMap<>();
   private static boolean loaded = false;

   public static void load() {
      if (!loaded) {
         try {
            InputStream stream = Lang.class.getClassLoader().getResourceAsStream("assets/aporia/lang/ru_cc.aprjson");
            if (stream != null) {
               InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
               Gson gson = new Gson();
               JsonObject json = (JsonObject)gson.fromJson(reader, JsonObject.class);
               json.entrySet().forEach(entry -> translations.put((String)entry.getKey(), ((JsonElement)entry.getValue()).getAsString()));
               reader.close();
               loaded = true;
            }
         } catch (Exception var4) {
            var4.printStackTrace();
         }
      }
   }

   public static String get(String key) {
      if (!loaded) {
         load();
      }

      return translations.getOrDefault(key, key);
   }

   public static String getModuleDescription(String moduleName) {
      String key = "module." + moduleName.toLowerCase() + ".description";
      return get(key);
   }
}
