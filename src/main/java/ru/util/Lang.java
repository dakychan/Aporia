package ru.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Система локализации для модулей
 */
public class Lang {
    private static final Map<String, String> translations = new HashMap<>();
    private static boolean loaded = false;
    
    public static void load() {
        if (loaded) return;
        
        try {
            InputStream stream = Lang.class.getClassLoader()
                .getResourceAsStream("assets/aporia/lang/ru_ru.json");
            
            if (stream != null) {
                InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
                Gson gson = new Gson();
                JsonObject json = gson.fromJson(reader, JsonObject.class);
                
                json.entrySet().forEach(entry -> {
                    translations.put(entry.getKey(), entry.getValue().getAsString());
                });
                
                reader.close();
                loaded = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Получить перевод по ключу
     */
    public static String get(String key) {
        if (!loaded) load();
        return translations.getOrDefault(key, key);
    }
    
    /**
     * Получить описание модуля
     */
    public static String getModuleDescription(String moduleName) {
        String key = "module." + moduleName.toLowerCase() + ".description";
        return get(key);
    }
}
