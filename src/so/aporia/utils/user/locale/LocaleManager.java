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
    private String currentLang;

    private String cachedLang = null;
    private Map<String, String> cachedLocale = null;

    private LocaleManager() {}

    public static LocaleManager getInstance() {
        return INSTANCE;
    }

    public void init() {
        this.currentLang = aporia.cc.OsManager.getSystemLocale();
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
                    // Проверяем на полноту и обновляем при необходимости
                    Map<String, String> defaultTemplate = getDefaultLocaleTemplate(lang);
                    boolean needsUpdate = false;

                    for (String key : defaultTemplate.keySet()) {
                        if (!localeMap.containsKey(key)) {
                            needsUpdate = true;
                            localeMap.put(key, defaultTemplate.get(key));
                            Logger.info("[LocaleManager] Added missing key '" + key + "' for " + lang);
                        }
                    }

                    locales.put(lang, localeMap);
                    Logger.info("[LocaleManager] Loaded from resources: " + lang + " (" + localeMap.size() + " keys)");

                    if (needsUpdate) {
                        // Сохраняем обновлённую версию в файл
                        Path localeFile = FilesManager.ROOT.resolve(".assets").resolve("aporia").resolve("locale").resolve(lang + ".json");
                        saveLocaleFile(localeFile, localeMap);
                        Logger.info("[LocaleManager] Updated locale file with missing keys: " + lang);
                    }

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
                    // Проверяем на полноту
                    Map<String, String> defaultTemplate = getDefaultLocaleTemplate(lang);
                    boolean needsUpdate = false;

                    for (String key : defaultTemplate.keySet()) {
                        if (!localeMap.containsKey(key)) {
                            needsUpdate = true;
                            localeMap.put(key, defaultTemplate.get(key));
                            Logger.info("[LocaleManager] Added missing key '" + key + "' for " + lang);
                        }
                    }

                    locales.put(lang, localeMap);
                    Logger.info("[LocaleManager] Loaded from file: " + lang + " (" + localeMap.size() + " keys)");

                    if (needsUpdate) {
                        saveLocaleFile(localeFile, localeMap);
                        Logger.info("[LocaleManager] Saved updated locale: " + lang);
                    }

                    invalidateCache();
                    return;
                } else {
                    Logger.warn("[LocaleManager] File is empty or invalid: " + localeFile);
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

    /**
     * Обновляет все локали, добавляя недостающие ключи из шаблона
     */
    public void updateAllLocales() {
        Logger.info("[LocaleManager] Updating all locales with missing keys...");
        for (String lang : locales.keySet()) {
            updateLocale(lang);
        }
    }

    /**
     * Обновляет конкретную локаль, добавляя недостающие ключи
     */
    public void updateLocale(String lang) {
        if (!locales.containsKey(lang)) {
            loadOrCreateLocale(lang);
            return;
        }

        Map<String, String> currentLocale = locales.get(lang);
        Map<String, String> defaultTemplate = getDefaultLocaleTemplate(lang);
        boolean needsUpdate = false;

        for (String key : defaultTemplate.keySet()) {
            if (!currentLocale.containsKey(key)) {
                needsUpdate = true;
                currentLocale.put(key, defaultTemplate.get(key));
                Logger.info("[LocaleManager] Added missing key '" + key + "' for " + lang);
            }
        }

        if (needsUpdate) {
            Path localeFile = FilesManager.ROOT.resolve(".assets").resolve("aporia").resolve("locale").resolve(lang + ".json");
            saveLocaleFile(localeFile, currentLocale);
            Logger.info("[LocaleManager] Updated locale file: " + lang);
            invalidateCache();
        }
    }

    /**
     * Проверяет валидность JSON файла локализации
     */
    public boolean validateLocaleFile(String lang) {
        Path localeFile = FilesManager.ROOT.resolve(".assets").resolve("aporia").resolve("locale").resolve(lang + ".json");
        if (!Files.exists(localeFile)) {
            Logger.warn("[LocaleManager] Locale file does not exist: " + lang);
            return false;
        }

        try {
            String content = Files.readString(localeFile, StandardCharsets.UTF_8);
            if (content.trim().isEmpty()) {
                Logger.warn("[LocaleManager] Locale file is empty: " + lang);
                return false;
            }

            Map<String, String> parsed = parseJson(content);
            if (parsed.isEmpty()) {
                Logger.warn("[LocaleManager] Locale file has invalid JSON structure: " + lang);
                return false;
            }

            Logger.info("[LocaleManager] Locale file is valid: " + lang + " (" + parsed.size() + " keys)");
            return true;
        } catch (IOException e) {
            Logger.error("[LocaleManager] Failed to validate locale file " + lang + ": " + e.getMessage());
            return false;
        }
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
                map.put("menu.title", "Aporia Client");
                map.put("menu.subtitle", "Your choice to victory.");
                map.put("menu.singleplayer", "Singleplayer");
                map.put("menu.multiplayer", "Multiplayer");
                map.put("menu.settings", "Settings");
                map.put("menu.exit", "Exit");
                map.put("lock.time_format", "%02d:%02d");
                map.put("lock.day_0", "Sunday");
                map.put("lock.day_1", "Monday");
                map.put("lock.day_2", "Tuesday");
                map.put("lock.day_3", "Wednesday");
                map.put("lock.day_4", "Thursday");
                map.put("lock.day_5", "Friday");
                map.put("lock.day_6", "Saturday");
                map.put("lock.click_hint", "Click anywhere to unlock");
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
                map.put("menu.title", "Апория Клиент");
                map.put("menu.subtitle", "Твой выбор к победе.");
                map.put("menu.singleplayer", "Одиночная игра");
                map.put("menu.multiplayer", "Мультиплеер");
                map.put("menu.settings", "Настройки");
                map.put("menu.exit", "Выход");
                map.put("lock.time_format", "%02d:%02d");
                map.put("lock.day_0", "Воскресенье");
                map.put("lock.day_1", "Понедельник");
                map.put("lock.day_2", "Вторник");
                map.put("lock.day_3", "Среда");
                map.put("lock.day_4", "Четверг");
                map.put("lock.day_5", "Пятница");
                map.put("lock.day_6", "Суббота");
                map.put("lock.click_hint", "Нажмите в любом месте для разблокировки");
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
                map.put("menu.title", "Aporia Client");
                map.put("menu.subtitle", "你的胜利之选.");
                map.put("menu.singleplayer", "单人游戏");
                map.put("menu.multiplayer", "多人游戏");
                map.put("menu.settings", "设置");
                map.put("menu.exit", "退出");
                map.put("lock.time_format", "%02d:%02d");
                map.put("lock.day_0", "星期日");
                map.put("lock.day_1", "星期一");
                map.put("lock.day_2", "星期二");
                map.put("lock.day_3", "星期三");
                map.put("lock.day_4", "星期四");
                map.put("lock.day_5", "星期五");
                map.put("lock.day_6", "星期六");
                map.put("lock.click_hint", "点击任意位置解锁");
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
                map.put("menu.title", "Aporia Client");
                map.put("menu.subtitle", "Your choice to victory.");
                map.put("menu.singleplayer", "Singleplayer");
                map.put("menu.multiplayer", "Multiplayer");
                map.put("menu.settings", "Settings");
                map.put("menu.exit", "Exit");
                map.put("lock.time_format", "%02d:%02d");
                map.put("lock.day_0", "Sunday");
                map.put("lock.day_1", "Monday");
                map.put("lock.day_2", "Tuesday");
                map.put("lock.day_3", "Wednesday");
                map.put("lock.day_4", "Thursday");
                map.put("lock.day_5", "Friday");
                map.put("lock.day_6", "Saturday");
                map.put("lock.click_hint", "Click anywhere to unlock");
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
            Logger.info("[LocaleManager] Saved locale file: " + path);
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

        if (json == null || json.trim().isEmpty()) {
            Logger.warn("[LocaleManager] Empty JSON string");
            return result;
        }

        json = json.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) {
            Logger.warn("[LocaleManager] Invalid JSON format: does not start/end with {}");
            return result;
        }

        json = json.substring(1, json.length() - 1);
        StringBuilder key = new StringBuilder();
        StringBuilder value = new StringBuilder();
        boolean inKey = true;
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (escaped) {
                if (inKey) {
                    key.append(c);
                } else {
                    value.append(c);
                }
                escaped = false;
                continue;
            }

            if (c == '\\') {
                escaped = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                if (!inString) {
                    if (inKey) {
                        inKey = false;
                    } else {
                        if (key.length() > 0 && value.length() > 0) {
                            result.put(key.toString(), value.toString());
                        }
                        key.setLength(0);
                        value.setLength(0);
                        inKey = true;
                    }
                }
                continue;
            }

            if (inString) {
                if (inKey) {
                    key.append(c);
                } else {
                    value.append(c);
                }
            }
        }

        if (result.isEmpty() && json.trim().length() > 0) {
            Logger.warn("[LocaleManager] Failed to parse JSON, result is empty");
        }

        return result;
    }
}