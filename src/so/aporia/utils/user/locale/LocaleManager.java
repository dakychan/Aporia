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
                map.put("module.aura", "Aura");
                map.put("module.aura.desc", "Automatically attacks nearby entities");
                map.put("module.aura.combat_mode", "Combat Mode");
                map.put("module.aura.combat_mode.desc", "1.8 - spam clicking, 1.9+ - attack cooldown");
                map.put("module.aura.range", "Range");
                map.put("module.aura.range.desc", "Attack distance from target");
                map.put("module.aura.rotation_speed", "Rotation Speed");
                map.put("module.aura.rotation_speed.desc", "Rotation speed in degrees per tick");
                map.put("module.aura.fov", "FOV");
                map.put("module.aura.fov.desc", "Field of view for target search");
                map.put("module.aura.min_cps", "Min CPS");
                map.put("module.aura.min_cps.desc", "Minimum attack speed (1.8 mode)");
                map.put("module.aura.max_cps", "Max CPS");
                map.put("module.aura.max_cps.desc", "Maximum attack speed (1.8 mode)");
                map.put("module.aura.targets", "Targets");
                map.put("module.aura.targets.desc", "Entity types to attack");
                map.put("module.aura.target_mode", "Target Priority");
                map.put("module.aura.target_mode.desc", "How targets are selected");
                map.put("module.aura.target_closest", "Closest");
                map.put("module.aura.target_health", "Health");
                map.put("module.aura.target_players", "Players");
                map.put("module.aura.target_mobs", "Mobs");
                map.put("module.aura.target_animals", "Animals");
                map.put("module.autosprint", "AutoSprint");
                map.put("module.autosprint.desc", "Automatically sprints when moving forward");
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
                map.put("module.aura", "Аура");
                map.put("module.aura.desc", "Автоматически атакует ближайших существ");
                map.put("module.aura.combat_mode", "Режим боя");
                map.put("module.aura.combat_mode.desc", "1.8 - спам кликов, 1.9+ - кулдаун атаки");
                map.put("module.aura.range", "Дистанция");
                map.put("module.aura.range.desc", "Дистанция атаки до цели");
                map.put("module.aura.rotation_speed", "Скорость поворота");
                map.put("module.aura.rotation_speed.desc", "Скорость поворота в градусах за тик");
                map.put("module.aura.fov", "Поле зрения");
                map.put("module.aura.fov.desc", "Поле зрения для поиска целей");
                map.put("module.aura.min_cps", "Мин. CPS");
                map.put("module.aura.min_cps.desc", "Минимальная скорость атаки (1.8 режим)");
                map.put("module.aura.max_cps", "Макс. CPS");
                map.put("module.aura.max_cps.desc", "Максимальная скорость атаки (1.8 режим)");
                map.put("module.aura.targets", "Цели");
                map.put("module.aura.targets.desc", "Типы существ для атаки");
                map.put("module.aura.target_mode", "Приоритет цели");
                map.put("module.aura.target_mode.desc", "Как выбираются цели");
                map.put("module.aura.target_closest", "Ближайший");
                map.put("module.aura.target_health", "Здоровье");
                map.put("module.aura.target_players", "Игроки");
                map.put("module.aura.target_mobs", "Мобы");
                map.put("module.aura.target_animals", "Животные");
                map.put("module.autosprint", "АвтоСпринт");
                map.put("module.autosprint.desc", "Автоматически бежит при движении вперёд");
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
                map.put("module.aura", "杀戮光环");
                map.put("module.aura.desc", "自动攻击附近的实体");
                map.put("module.aura.combat_mode", "战斗模式");
                map.put("module.aura.combat_mode.desc", "1.8 - 连点，1.9+ - 攻击冷却");
                map.put("module.aura.range", "距离");
                map.put("module.aura.range.desc", "攻击目标的距离");
                map.put("module.aura.rotation_speed", "旋转速度");
                map.put("module.aura.rotation_speed.desc", "每 tick 的旋转速度（度）");
                map.put("module.aura.fov", "视场角");
                map.put("module.aura.fov.desc", "搜索目标的视场角");
                map.put("module.aura.min_cps", "最小 CPS");
                map.put("module.aura.min_cps.desc", "最小攻击速度（1.8 模式）");
                map.put("module.aura.max_cps", "最大 CPS");
                map.put("module.aura.max_cps.desc", "最大攻击速度（1.8 模式）");
                map.put("module.aura.targets", "目标");
                map.put("module.aura.targets.desc", "要攻击的实体类型");
                map.put("module.aura.target_mode", "目标优先级");
                map.put("module.aura.target_mode.desc", "如何选择目标");
                map.put("module.aura.target_closest", "最近");
                map.put("module.aura.target_health", "生命值");
                map.put("module.aura.target_players", "玩家");
                map.put("module.aura.target_mobs", "怪物");
                map.put("module.aura.target_animals", "动物");
                map.put("module.autosprint", "自动疾跑");
                map.put("module.autosprint.desc", "向前移动时自动疾跑");
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