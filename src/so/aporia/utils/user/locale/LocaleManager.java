/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.utils.user.locale;

import so.aporia.utils.files.FilesManager;
import so.aporia.utils.user.logger.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class LocaleManager {
    public static final LocaleManager INSTANCE = new LocaleManager();

    private static final String[] BUILTIN_LANGS = {"en_EU", "ru_RU", "ch_CH"};

    private final Map<String, Map<String, String>> locales = new HashMap<>();
    private String currentLang = "en_EU";

    // Кеш результата get() — чтобы не лазить в мапу каждый кадр
    private String cachedLang = null;
    private Map<String, String> cachedLocale = null;

    private LocaleManager() {}

    public static LocaleManager getInstance() {
        return INSTANCE;
    }

    /**
     * Инициализация: загружает все встроенные локали,
     * создаёт файлы-заполнители если их нет.
     */
    public void init() {
        for (String lang : BUILTIN_LANGS) {
            loadOrCreateLocale(lang);
        }
    }

    private void loadOrCreateLocale(String lang) {
        if (locales.containsKey(lang)) return;

        // 1. Пробуем загрузить из ресурсов (если есть)
        String resourcePath = "aporia/locale/" + lang + ".json";
        try (var is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is != null) {
                String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> localeMap = parseJson(content);
                if (!localeMap.isEmpty()) {
                    locales.put(lang, localeMap);
                    Logger.info("[LocaleManager] Loaded from resources: " + lang + " (" + localeMap.size() + " keys)");
                    invalidateCache();
                    return;
                }
            }
        } catch (IOException e) {
            Logger.warn("[LocaleManager] Failed to read resource " + resourcePath + ": " + e.getMessage());
        }

        // 2. Пробуем загрузить из FilesManager (папка assets/locale/)
        Path localeFile = FilesManager.ROOT.resolve(".assets").resolve("aporia").resolve("locale").resolve(lang + ".json");
        if (Files.exists(localeFile)) {
            try {
                String content = Files.readString(localeFile, StandardCharsets.UTF_8);
                Map<String, String> localeMap = parseJson(content);
                if (!localeMap.isEmpty()) {
                    locales.put(lang, localeMap);
                    Logger.info("[LocaleManager] Loaded from file: " + lang + " (" + localeMap.size() + " keys)");
                    invalidateCache();
                    return;
                }
            } catch (IOException e) {
                Logger.error("[LocaleManager] Failed to parse " + localeFile + ": " + e.getMessage());
            }
        }

        // 3. Файла нет или пустой — создаём с дефолтными ключами
        Logger.info("[LocaleManager] Creating default locale file: " + lang);
        Map<String, String> defaultContent = getDefaultLocaleTemplate(lang);
        saveLocaleFile(localeFile, defaultContent);
        locales.put(lang, defaultContent);
        Logger.info("[LocaleManager] Created: " + lang + " (" + defaultContent.size() + " keys)");
        invalidateCache();
    }

    private Map<String, String> getDefaultLocaleTemplate(String lang) {
        Map<String, String> map = new LinkedHashMap<>();
        switch (lang) {
            case "en_EU":
                map.put("gui.config", "Config");
                map.put("gui.stats", "Stats");
                map.put("gui.tasks", "Tasks");
                map.put("gui.search", "Search...");
                map.put("gui.settings", "Settings");
                map.put("gui.close", "Close");
                map.put("gui.save", "Save");
                map.put("gui.cancel", "Cancel");
                map.put("gui.enabled", "Enabled");
                map.put("gui.disabled", "Disabled");
                break;
            case "ru_RU":
                map.put("gui.config", "Конфиг");
                map.put("gui.stats", "Статы");
                map.put("gui.tasks", "Задачи");
                map.put("gui.search", "Поиск...");
                map.put("gui.settings", "Настройки");
                map.put("gui.close", "Закрыть");
                map.put("gui.save", "Сохранить");
                map.put("gui.cancel", "Отмена");
                map.put("gui.enabled", "Включено");
                map.put("gui.disabled", "Выключено");
                break;
            case "ch_CH":
                map.put("gui.config", "配置");
                map.put("gui.stats", "统计");
                map.put("gui.tasks", "任务");
                map.put("gui.search", "搜索...");
                map.put("gui.settings", "设置");
                map.put("gui.close", "关闭");
                map.put("gui.save", "保存");
                map.put("gui.cancel", "取消");
                map.put("gui.enabled", "已启用");
                map.put("gui.disabled", "已禁用");
                break;
            default:
                // Для новых языков — пустые ключи
                map.put("gui.config", lang + ":config");
                map.put("gui.stats", lang + ":stats");
                map.put("gui.tasks", lang + ":tasks");
                map.put("gui.search", lang + ":search...");
                map.put("gui.settings", lang + ":settings");
                map.put("gui.close", lang + ":close");
                map.put("gui.save", lang + ":save");
                map.put("gui.cancel", lang + ":cancel");
                map.put("gui.enabled", lang + ":enabled");
                map.put("gui.disabled", lang + ":disabled");
                break;
        }
        return map;
    }

    private void saveLocaleFile(Path path, Map<String, String> content) {
        try {
            Files.createDirectories(path.getParent());
            StringBuilder sb = new StringBuilder("{\n");
            int i = 0;
            for (var entry : content.entrySet()) {
                sb.append("  \"").append(escapeJson(entry.getKey())).append("\": \"")
                  .append(escapeJson(entry.getValue())).append("\"");
                if (i < content.size() - 1) sb.append(",");
                sb.append("\n");
                i++;
            }
            sb.append("}");
            Files.writeString(path, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Logger.error("[LocaleManager] Failed to save " + path + ": " + e.getMessage());
        }
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public String get(String key) {
        Map<String, String> locale = getLocaleCache();
        return locale != null ? locale.getOrDefault(key, key) : key;
    }

    public String get(String key, String lang) {
        Map<String, String> locale = locales.get(lang);
        if (locale == null) {
            locale = getLocaleCache();
        }
        return locale != null ? locale.getOrDefault(key, key) : key;
    }

    public String getCurrentLang() {
        return currentLang;
    }

    public void setCurrentLang(String lang) {
        this.currentLang = lang;
        invalidateCache();
        // Загружаем если ещё не загружен
        if (!locales.containsKey(lang)) {
            loadOrCreateLocale(lang);
        }
    }

    public void reloadAll() {
        locales.clear();
        invalidateCache();
        for (String lang : BUILTIN_LANGS) {
            loadOrCreateLocale(lang);
        }
    }

    private Map<String, String> getLocaleCache() {
        if (cachedLang == null || !cachedLang.equals(currentLang)) {
            cachedLang = currentLang;
            cachedLocale = locales.get(currentLang);
            if (cachedLocale == null) {
                cachedLocale = locales.get("en_EU");
            }
        }
        return cachedLocale;
    }

    private void invalidateCache() {
        cachedLang = null;
        cachedLocale = null;
    }

    private Map<String, String> parseJson(String json) {
        Map<String, String> result = new HashMap<>();
        json = json.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) {
            return result;
        }

        json = json.substring(1, json.length() - 1);
        StringBuilder key = new StringBuilder();
        StringBuilder value = new StringBuilder();
        boolean inKey = true;
        boolean inString = false;
        int braceDepth = 0;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (c == '{') braceDepth++;
            if (c == '}') braceDepth--;

            if (!inString && braceDepth > 0) continue;

            if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                inString = !inString;
                if (inString) {
                    if (inKey) {
                        key.setLength(0);
                    } else {
                        value.setLength(0);
                    }
                } else {
                    if (inKey) {
                        inKey = false;
                    } else {
                        result.put(key.toString(), value.toString());
                        inKey = true;
                    }
                }
            } else if (inString) {
                if (inKey) {
                    key.append(c);
                } else {
                    value.append(c);
                }
            }
        }

        return result;
    }
}
