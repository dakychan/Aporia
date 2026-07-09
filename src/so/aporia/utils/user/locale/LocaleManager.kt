/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.utils.user.locale
import com.chaos.annotation.Obfuscate
import so.aporia.utils.files.FilesManager
import so.aporia.utils.user.logger.Logger
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import com.chaos.annotation.ChaosNative
@Obfuscate
@ChaosNative
class LocaleManager private constructor() {
    private val locales: MutableMap<String, MutableMap<String, String>> = HashMap()
    private var currentLang: String? = null

    private var cachedLang: String? = null
    private var cachedLocale: MutableMap<String, String>? = null

    fun init() {
        this.currentLang = aporia.cc.OsManager.getSystemLocale()
        for (lang in BUILTIN_LANGS) {
            loadOrCreateLocale(lang)
        }
    }

    private fun loadOrCreateLocale(lang: String) {
        if (locales.containsKey(lang)) return

        // 1. Try to load from resources
        val resourcePath = "aporia/locale/$lang.json"
        try {
            val `is` = javaClass.classLoader.getResourceAsStream(resourcePath)
            if (`is` != null) {
                `is`.use { stream ->
                    val content = String(stream.readAllBytes(), StandardCharsets.UTF_8)
                    val localeMap = parseJson(content)
                    if (localeMap.isNotEmpty()) {
                        val defaultTemplate = getDefaultLocaleTemplate(lang)
                        var needsUpdate = false

                        for (key in defaultTemplate.keys) {
                            if (!localeMap.containsKey(key)) {
                                needsUpdate = true
                                localeMap[key] = defaultTemplate[key]!!
                                Logger.info("[LocaleManager] Added missing key '$key' for $lang")
                            }
                        }

                        locales[lang] = localeMap
                        Logger.info("[LocaleManager] Loaded from resources: $lang (${localeMap.size} keys)")

                        if (needsUpdate) {
                            val localeFile = FilesManager.ROOT.resolve(".assets").resolve("aporia").resolve("locale").resolve("$lang.json")
                            saveLocaleFile(localeFile, localeMap)
                            Logger.info("[LocaleManager] Updated locale file with missing keys: $lang")
                        }

                        invalidateCache()
                        return
                    }
                }
            }
        } catch (e: IOException) {
            Logger.warn("[LocaleManager] Failed to read resource $resourcePath: ${e.message}")
        }

        // 2. Try to load from FilesManager
        val localeFile = FilesManager.ROOT.resolve(".assets").resolve("aporia").resolve("locale").resolve("$lang.json")
        if (Files.exists(localeFile)) {
            try {
                val content = Files.readString(localeFile, StandardCharsets.UTF_8)
                val localeMap = parseJson(content)

                if (localeMap.isNotEmpty()) {
                    val defaultTemplate = getDefaultLocaleTemplate(lang)
                    var needsUpdate = false

                    for (key in defaultTemplate.keys) {
                        if (!localeMap.containsKey(key)) {
                            needsUpdate = true
                            localeMap[key] = defaultTemplate[key]!!
                            Logger.info("[LocaleManager] Added missing key '$key' for $lang")
                        }
                    }

                    locales[lang] = localeMap
                    Logger.info("[LocaleManager] Loaded from file: $lang (${localeMap.size} keys)")

                    if (needsUpdate) {
                        saveLocaleFile(localeFile, localeMap)
                        Logger.info("[LocaleManager] Saved updated locale: $lang")
                    }

                    invalidateCache()
                    return
                } else {
                    Logger.warn("[LocaleManager] File is empty or invalid: $localeFile")
                }
            } catch (e: IOException) {
                Logger.error("[LocaleManager] Failed to parse $localeFile: ${e.message}")
            }
        }

        // 3. No file or empty — create with default keys
        Logger.info("[LocaleManager] Creating default locale file: $lang")
        val defaultContent = getDefaultLocaleTemplate(lang)
        saveLocaleFile(localeFile, defaultContent)
        locales[lang] = defaultContent
        Logger.info("[LocaleManager] Created: $lang (${defaultContent.size} keys)")
        invalidateCache()
    }

    fun updateAllLocales() {
        Logger.info("[LocaleManager] Updating all locales with missing keys...")
        for (lang in locales.keys) {
            updateLocale(lang)
        }
    }

    fun updateLocale(lang: String) {
        if (!locales.containsKey(lang)) {
            loadOrCreateLocale(lang)
            return
        }

        val currentLocale = locales[lang]!!
        val defaultTemplate = getDefaultLocaleTemplate(lang)
        var needsUpdate = false

        for (key in defaultTemplate.keys) {
            if (!currentLocale.containsKey(key)) {
                needsUpdate = true
                currentLocale[key] = defaultTemplate[key]!!
                Logger.info("[LocaleManager] Added missing key '$key' for $lang")
            }
        }

        if (needsUpdate) {
            val localeFile = FilesManager.ROOT.resolve(".assets").resolve("aporia").resolve("locale").resolve("$lang.json")
            saveLocaleFile(localeFile, currentLocale)
            Logger.info("[LocaleManager] Updated locale file: $lang")
            invalidateCache()
        }
    }

    fun validateLocaleFile(lang: String): Boolean {
        val localeFile = FilesManager.ROOT.resolve(".assets").resolve("aporia").resolve("locale").resolve("$lang.json")
        if (!Files.exists(localeFile)) {
            Logger.warn("[LocaleManager] Locale file does not exist: $lang")
            return false
        }

        try {
            val content = Files.readString(localeFile, StandardCharsets.UTF_8)
            if (content.trim { it <= ' ' }.isEmpty()) {
                Logger.warn("[LocaleManager] Locale file is empty: $lang")
                return false
            }

            val parsed = parseJson(content)
            if (parsed.isEmpty()) {
                Logger.warn("[LocaleManager] Locale file has invalid JSON structure: $lang")
                return false
            }

            Logger.info("[LocaleManager] Locale file is valid: $lang (${parsed.size} keys)")
            return true
        } catch (e: IOException) {
            Logger.error("[LocaleManager] Failed to validate locale file $lang: ${e.message}")
            return false
        }
    }

    private fun getDefaultLocaleTemplate(lang: String): MutableMap<String, String> {
        val map: MutableMap<String, String> = LinkedHashMap()
        when (lang) {
            "en_EU" -> {
                map["watermark.name"] = "Aporia.cc"
                map["gui.config"] = "Config"
                map["gui.stats"] = "Stats"
                map["gui.tasks"] = "Tasks"
                map["gui.search"] = "Search..."
                map["gui.settings"] = "Settings"
                map["gui.close"] = "Close"
                map["gui.save"] = "Save"
                map["gui.cancel"] = "Cancel"
                map["gui.enabled"] = "Enabled"
                map["gui.disabled"] = "Disabled"
                map["gui.tab.settings_title"] = "ClickGui Settings"
                map["menu.title"] = "Aporia Client"
                map["menu.subtitle"] = "Your choice to victory."
                map["menu.singleplayer"] = "Singleplayer"
                map["menu.multiplayer"] = "Multiplayer"
                map["menu.settings"] = "Settings"
                map["menu.exit"] = "Exit"
                map["lock.time_format"] = "%02d:%02d"
                map["lock.day_0"] = "Sunday"
                map["lock.day_1"] = "Monday"
                map["lock.day_2"] = "Tuesday"
                map["lock.day_3"] = "Wednesday"
                map["lock.day_4"] = "Thursday"
                map["lock.day_5"] = "Friday"
                map["lock.day_6"] = "Saturday"
                map["lock.click_hint"] = "Click anywhere to unlock"
                map["module.aura"] = "Aura"
                map["module.aura.desc"] = "Automatically attacks nearby entities"
                map["module.aura.combat_mode"] = "Combat Mode"
                map["module.aura.combat_mode.desc"] = "1.8 - spam clicking, 1.9+ - attack cooldown"
                map["module.aura.range"] = "Range"
                map["module.aura.range.desc"] = "Attack distance from target"
                map["module.aura.rotation_speed"] = "Rotation Speed"
                map["module.aura.rotation_speed.desc"] = "Rotation speed in degrees per tick"
                map["module.aura.fov"] = "FOV"
                map["module.aura.fov.desc"] = "Field of view for target search"
                map["module.aura.min_cps"] = "Min CPS"
                map["module.aura.min_cps.desc"] = "Minimum attack speed (1.8 mode)"
                map["module.aura.max_cps"] = "Max CPS"
                map["module.aura.max_cps.desc"] = "Maximum attack speed (1.8 mode)"
                map["module.aura.targets"] = "Targets"
                map["module.aura.targets.desc"] = "Entity types to attack"
                map["module.aura.target_mode"] = "Target Priority"
                map["module.aura.target_mode.desc"] = "How targets are selected"
                map["module.aura.target_closest"] = "Closest"
                map["module.aura.target_health"] = "Health"
                map["module.aura.target_players"] = "Players"
                map["module.aura.target_mobs"] = "Mobs"
                map["module.aura.target_animals"] = "Animals"
                map["module.autosprint"] = "AutoSprint"
                map["module.autosprint.desc"] = "Automatically sprints when moving forward"
                map["gui.tab.avatar"] = "Avatar"
                map["gui.tab.quests"] = "Quests"
                map["gui.tab.browser"] = "Browser"
                map["gui.tab.settings"] = "Settings"
                map["gui.on"] = "ON"
                map["gui.off"] = "OFF"
                map["gui.bindprompt"] = "Press any key..."
                map["gui.other"] = "Other"
                map["gui.other.title"] = "Other Modules"
                map["gui.quests.title"] = "Daily Quests"
                map["gui.info.title"] = "Info"
                map["category.combat"] = "Combat"
                map["category.move"] = "Movement"
                map["category.visual"] = "Visual"
                map["category.player"] = "Player"
                map["category.render"] = "Render"
                map["category.world"] = "World"
                map["category.misc"] = "Misc"
            }
            "ru_RU" -> {
                map["watermark.name"] = "Aporia.cc"
                map["gui.config"] = "Конфиг"
                map["gui.stats"] = "Статы"
                map["gui.tasks"] = "Задачи"
                map["gui.search"] = "Поиск..."
                map["gui.settings"] = "Настройки"
                map["gui.close"] = "Закрыть"
                map["gui.save"] = "Сохранить"
                map["gui.cancel"] = "Отмена"
                map["gui.enabled"] = "Включено"
                map["gui.disabled"] = "Выключено"
                map["gui.tab.settings_title"] = "Настройки ClickGui"
                map["menu.title"] = "Апория Клиент"
                map["menu.subtitle"] = "Твой выбор к победе."
                map["menu.singleplayer"] = "Одиночная игра"
                map["menu.multiplayer"] = "Мультиплеер"
                map["menu.settings"] = "Настройки"
                map["menu.exit"] = "Выход"
                map["lock.time_format"] = "%02d:%02d"
                map["lock.day_0"] = "Воскресенье"
                map["lock.day_1"] = "Понедельник"
                map["lock.day_2"] = "Вторник"
                map["lock.day_3"] = "Среда"
                map["lock.day_4"] = "Четверг"
                map["lock.day_5"] = "Пятница"
                map["lock.day_6"] = "Суббота"
                map["lock.click_hint"] = "Нажмите в любом месте для разблокировки"
                map["module.aura"] = "Аура"
                map["module.aura.desc"] = "Автоматически атакует ближайших существ"
                map["module.aura.combat_mode"] = "Режим боя"
                map["module.aura.combat_mode.desc"] = "1.8 - спам кликов, 1.9+ - кулдаун атаки"
                map["module.aura.range"] = "Дистанция"
                map["module.aura.range.desc"] = "Дистанция атаки до цели"
                map["module.aura.rotation_speed"] = "Скорость поворота"
                map["module.aura.rotation_speed.desc"] = "Скорость поворота в градусах за тик"
                map["module.aura.fov"] = "Поле зрения"
                map["module.aura.fov.desc"] = "Поле зрения для поиска целей"
                map["module.aura.min_cps"] = "Мин. CPS"
                map["module.aura.min_cps.desc"] = "Минимальная скорость атаки (1.8 режим)"
                map["module.aura.max_cps"] = "Макс. CPS"
                map["module.aura.max_cps.desc"] = "Максимальная скорость атаки (1.8 режим)"
                map["module.aura.targets"] = "Цели"
                map["module.aura.targets.desc"] = "Типы существ для атаки"
                map["module.aura.target_mode"] = "Приоритет цели"
                map["module.aura.target_mode.desc"] = "Как выбираются цели"
                map["module.aura.target_closest"] = "Ближайший"
                map["module.aura.target_health"] = "Здоровье"
                map["module.aura.target_players"] = "Игроки"
                map["module.aura.target_mobs"] = "Мобы"
                map["module.aura.target_animals"] = "Животные"
                map["module.autosprint"] = "АвтоСпринт"
                map["module.autosprint.desc"] = "Автоматически бежит при движении вперёд"
                map["gui.tab.avatar"] = "АВАТАР"
                map["gui.tab.quests"] = "КВЕСТЫ"
                map["gui.tab.browser"] = "БРАУЗЕР"
                map["gui.tab.settings"] = "НАСТРОЙКИ"
                map["gui.on"] = "ВКЛ"
                map["gui.off"] = "ВЫКЛ"
                map["gui.bindprompt"] = "Нажми любую клавишу..."
                map["gui.other"] = "Остальное"
                map["gui.other.title"] = "Остальные модули"
                map["gui.quests.title"] = "Ежедневные квесты"
                map["gui.info.title"] = "Инфо"
                map["category.combat"] = "Бой"
                map["category.move"] = "Движение"
                map["category.visual"] = "Визуал"
                map["category.player"] = "Игрок"
                map["category.render"] = "Рендер"
                map["category.world"] = "Мир"
                map["category.misc"] = "Разное"
            }
            "ch_CH" -> {
                map["watermark.name"] = "Aporia.cc"
                map["gui.config"] = "配置"
                map["gui.stats"] = "统计"
                map["gui.tasks"] = "任务"
                map["gui.search"] = "搜索..."
                map["gui.settings"] = "设置"
                map["gui.close"] = "关闭"
                map["gui.save"] = "保存"
                map["gui.cancel"] = "取消"
                map["gui.enabled"] = "已启用"
                map["gui.disabled"] = "已禁用"
                map["gui.tab.settings_title"] = "点击GUI设置"
                map["menu.title"] = "Aporia Client"
                map["menu.subtitle"] = "你的胜利之选."
                map["menu.singleplayer"] = "单人游戏"
                map["menu.multiplayer"] = "多人游戏"
                map["menu.settings"] = "设置"
                map["menu.exit"] = "退出"
                map["lock.time_format"] = "%02d:%02d"
                map["lock.day_0"] = "星期日"
                map["lock.day_1"] = "星期一"
                map["lock.day_2"] = "星期二"
                map["lock.day_3"] = "星期三"
                map["lock.day_4"] = "星期四"
                map["lock.day_5"] = "星期五"
                map["lock.day_6"] = "星期六"
                map["lock.click_hint"] = "点击任意位置解锁"
                map["module.aura"] = "杀戮光环"
                map["module.aura.desc"] = "自动攻击附近的实体"
                map["module.aura.combat_mode"] = "战斗模式"
                map["module.aura.combat_mode.desc"] = "1.8 - 连点，1.9+ - 攻击冷却"
                map["module.aura.range"] = "距离"
                map["module.aura.range.desc"] = "攻击目标的距离"
                map["module.aura.rotation_speed"] = "旋转速度"
                map["module.aura.rotation_speed.desc"] = "每 tick 的旋转速度（度）"
                map["module.aura.fov"] = "视场角"
                map["module.aura.fov.desc"] = "搜索目标的视场角"
                map["module.aura.min_cps"] = "最小 CPS"
                map["module.aura.min_cps.desc"] = "最小攻击速度（1.8 模式）"
                map["module.aura.max_cps"] = "最大 CPS"
                map["module.aura.max_cps.desc"] = "最大攻击速度（1.8 模式）"
                map["module.aura.targets"] = "目标"
                map["module.aura.targets.desc"] = "要攻击的实体类型"
                map["module.aura.target_mode"] = "目标优先级"
                map["module.aura.target_mode.desc"] = "如何选择目标"
                map["module.aura.target_closest"] = "最近"
                map["module.aura.target_health"] = "生命值"
                map["module.aura.target_players"] = "玩家"
                map["module.aura.target_mobs"] = "怪物"
                map["module.aura.target_animals"] = "动物"
                map["module.autosprint"] = "自动疾跑"
                map["module.autosprint.desc"] = "向前移动时自动疾跑"
                map["gui.tab.avatar"] = "头像"
                map["gui.tab.quests"] = "任务"
                map["gui.tab.browser"] = "浏览器"
                map["gui.tab.settings"] = "设置"
                map["gui.on"] = "开"
                map["gui.off"] = "关"
                map["gui.bindprompt"] = "按任意键..."
                map["gui.other"] = "其他"
                map["gui.other.title"] = "其他模块"
                map["gui.quests.title"] = "每日任务"
                map["gui.info.title"] = "信息"
                map["category.combat"] = "战斗"
                map["category.move"] = "移动"
                map["category.visual"] = "视觉"
                map["category.player"] = "玩家"
                map["category.render"] = "渲染"
                map["category.world"] = "世界"
                map["category.misc"] = "其他"
            }
            else -> {
                map["watermark.name"] = "Aporia.cc"
                map["gui.config"] = "$lang:config"
                map["gui.stats"] = "$lang:stats"
                map["gui.tasks"] = "$lang:tasks"
                map["gui.search"] = "$lang:search..."
                map["gui.settings"] = "$lang:settings"
                map["gui.close"] = "$lang:close"
                map["gui.save"] = "$lang:save"
                map["gui.cancel"] = "$lang:cancel"
                map["gui.enabled"] = "$lang:enabled"
                map["gui.disabled"] = "$lang:disabled"
                map["gui.tab.settings_title"] = "ClickGui Settings"
                map["menu.title"] = "Aporia Client"
                map["menu.subtitle"] = "Your choice to victory."
                map["menu.singleplayer"] = "Singleplayer"
                map["menu.multiplayer"] = "Multiplayer"
                map["menu.settings"] = "Settings"
                map["menu.exit"] = "Exit"
                map["lock.time_format"] = "%02d:%02d"
                map["lock.day_0"] = "Sunday"
                map["lock.day_1"] = "Monday"
                map["lock.day_2"] = "Tuesday"
                map["lock.day_3"] = "Wednesday"
                map["lock.day_4"] = "Thursday"
                map["lock.day_5"] = "Friday"
                map["lock.day_6"] = "Saturday"
                map["lock.click_hint"] = "Click anywhere to unlock"
                map["gui.tab.avatar"] = "Avatar"
                map["gui.tab.quests"] = "Quests"
                map["gui.tab.browser"] = "Browser"
                map["gui.tab.settings"] = "Settings"
                map["gui.on"] = "ON"
                map["gui.off"] = "OFF"
                map["gui.bindprompt"] = "Press any key..."
                map["gui.other"] = "Other"
                map["gui.other.title"] = "Other Modules"
                map["gui.quests.title"] = "Daily Quests"
                map["gui.info.title"] = "Info"
                map["category.combat"] = "Combat"
                map["category.move"] = "Movement"
                map["category.visual"] = "Visual"
                map["category.player"] = "Player"
                map["category.render"] = "Render"
                map["category.world"] = "World"
                map["category.misc"] = "Misc"
            }
        }
        return map
    }

    private fun saveLocaleFile(path: Path, content: Map<String, String>) {
        try {
            Files.createDirectories(path.parent)
            val sb = StringBuilder("{\n")
            val entries = content.entries.toTypedArray()
            for (i in entries.indices) {
                sb.append("  \"").append(escapeJson(entries[i].key)).append("\": \"")
                    .append(escapeJson(entries[i].value)).append("\"")
                if (i < entries.size - 1) sb.append(",")
                sb.append("\n")
            }
            sb.append("}")
            Files.writeString(path, sb.toString(), StandardCharsets.UTF_8)
            Logger.info("[LocaleManager] Saved locale file: $path")
        } catch (e: IOException) {
            Logger.error("[LocaleManager] Failed to save $path: ${e.message}")
        }
    }

    private fun escapeJson(s: String): String {
        return s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    @JvmOverloads
    fun get(key: String, lang: String? = null): String {
        if (lang != null) {
            val locale = locales[lang]
            if (locale != null) {
                return locale.getOrDefault(key, key)
            }
        }
        val locale = getLocaleCache()
        return locale?.getOrDefault(key, key) ?: key
    }

    fun getCurrentLang(): String? {
        return currentLang
    }

    fun setCurrentLang(lang: String) {
        this.currentLang = lang
        invalidateCache()
        if (!locales.containsKey(lang)) {
            loadOrCreateLocale(lang)
        }
    }

    fun reloadAll() {
        locales.clear()
        invalidateCache()
        for (lang in BUILTIN_LANGS) {
            loadOrCreateLocale(lang)
        }
    }

    private fun getLocaleCache(): MutableMap<String, String>? {
        if (cachedLang == null || cachedLang != currentLang) {
            cachedLang = currentLang
            cachedLocale = locales[currentLang]
            if (cachedLocale == null) {
                cachedLocale = locales["en_EU"]
            }
        }
        return cachedLocale
    }

    private fun invalidateCache() {
        cachedLang = null
        cachedLocale = null
    }

    private fun parseJson(json: String?): MutableMap<String, String> {
        val result: MutableMap<String, String> = HashMap()

        if (json == null || json.trim { it <= ' ' }.isEmpty()) {
            Logger.warn("[LocaleManager] Empty JSON string")
            return result
        }

        var jsonVar = json.trim { it <= ' ' }
        if (!jsonVar.startsWith("{") || !jsonVar.endsWith("}")) {
            Logger.warn("[LocaleManager] Invalid JSON format: does not start/end with {}")
            return result
        }

        jsonVar = jsonVar.substring(1, jsonVar.length - 1).trim { it <= ' ' }
        var i = 0
        val len = jsonVar.length

        while (i < len) {
            while (i < len && Character.isWhitespace(jsonVar[i])) i++
            if (i >= len) break

            val keyResult = parseJsonString(jsonVar, i) ?: break
            i += keyResult[1].toInt()

            while (i < len && Character.isWhitespace(jsonVar[i])) i++
            if (i >= len || jsonVar[i] != ':') break
            i++

            while (i < len && Character.isWhitespace(jsonVar[i])) i++

            val valResult = parseJsonString(jsonVar, i) ?: break
            i += valResult[1].toInt()

            result[keyResult[0]] = valResult[0]

            while (i < len && Character.isWhitespace(jsonVar[i])) i++
            if (i < len && jsonVar[i] == ',') i++
        }

        if (result.isEmpty() && jsonVar.trim { it <= ' ' }.isNotEmpty()) {
            Logger.warn("[LocaleManager] Failed to parse JSON, result is empty")
        }

        return result
    }

    private fun parseJsonString(json: String, start: Int): Array<String>? {
        if (start >= json.length || json[start] != '"') return null
        val sb = StringBuilder()
        var escaped = false
        for (i in start + 1 until json.length) {
            val c = json[i]
            if (escaped) {
                sb.append(c)
                escaped = false
                continue
            }
            if (c == '\\') {
                escaped = true
                continue
            }
            if (c == '"') {
                return arrayOf(sb.toString(), (i - start + 1).toString())
            }
            sb.append(c)
        }
        return null
    }

    companion object {
        @JvmField
        val INSTANCE = LocaleManager()

        private val BUILTIN_LANGS = arrayOf("en_EU", "ru_RU", "ch_CH")

        @JvmStatic
        fun getInstance(): LocaleManager {
            return INSTANCE
        }
    }
}