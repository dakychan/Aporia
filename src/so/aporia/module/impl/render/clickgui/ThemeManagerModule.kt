package so.aporia.module.impl.render.clickgui

import com.chaos.annotation.Obfuscate
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.settings.SelectSetting
import so.aporia.utils.files.AprParser
import so.aporia.utils.files.FilesManager
import so.aporia.utils.files.impl.ConfigFile
import so.aporia.utils.user.logger.Logger
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

@Obfuscate
class ThemeManagerModule : Module("Theme Manager", Category.VISUAL) {

    data class Theme(val name: String) {
        var guiBackground: Int = 0xCC0A0A14.toInt(); var guiTitleBg: Int = 0xC81E1E28.toInt(); var guiTitleText: Int = 0xFFDCDCFF.toInt()
        var guiModuleText: Int = 0xFFFFFFFF.toInt(); var guiEnabledDot: Int = 0xFF64FF64.toInt(); var guiDisabledDot: Int = 0xFF505050.toInt()
        var guiHoverBg: Int = 0x18FFFFFF.toInt(); var guiSeparator: Int = 0x18FFFFFF.toInt(); var guiSettingText: Int = 0xFFFFFFFF.toInt()
        var guiSettingValue: Int = 0xFFDCDCFF.toInt(); var espPlayer: Int = 0xFFFF5555.toInt(); var espFriend: Int = 0xFF55FF55.toInt()
        var espItem: Int = 0xFFFFFF55.toInt(); var espMob: Int = 0xFFFFAA00.toInt(); var fontNormal: Int = 0xFFFFFFFF.toInt()
        var fontHighlight: Int = 0xFFFFFF55.toInt(); var fontShadow: Int = 0x80000000.toInt(); var mmTitle: Int = 0xFFFFFFFF.toInt()
        var mmSubtitle: Int = 0xFFB48CFF.toInt(); var mmButtonBg: Int = 0x4C140A28.toInt(); var mmButtonFg: Int = 0xFFFFFFFF.toInt()
    }

    private val themes = linkedMapOf<String, Theme>()
    private var currentName = "DefaultAporia"

    val themeSelect = SelectSetting("Theme", "Active theme").selected("DefaultAporia")
    override val settings = listOf(themeSelect)

    private val dir: Path get() = FilesManager.ROOT.resolve("themes")
    private val selectedFile: Path get() = dir.resolve("_selected.apr")

    init {
        _instance = this
        ConfigFile.markModuleActivated(this)
        registerBuiltins()
        loadAll()
        updateThemeSelect()
    }

    override fun onEnable() {}
    override fun onDisable() {}

    fun active(): Theme = themes[currentName] ?: createDefault()
    fun activeName(): String = currentName
    fun all(): Collection<Theme> = themes.values
    fun names(): Set<String> = themes.keys

    fun select(name: String) {
        if (themes.containsKey(name)) {
            currentName = name
            themeSelect.selected(name)
            saveSelected()
        }
    }

    fun create(name: String) {
        if (themes.containsKey(name)) return
        val t = createDefault().copy(name = name)
        themes[name] = t
        saveOne(name)
    }

    fun delete(name: String) {
        if (name == "DefaultAporia") return
        themes.remove(name)
        val f = dir.resolve("$name.apr")
        try { Files.deleteIfExists(f) } catch (_: IOException) {}
        if (currentName == name) {
            currentName = themes.keys.first()
            saveSelected()
        }
    }

    private fun updateThemeSelect() {
        themeSelect.value(*themes.keys.toTypedArray()).selected(currentName)
    }

    private fun registerBuiltins() {
        themes["DefaultAporia"] = createDefault()
        themes["Amethyst"] = createAmethyst()
        themes["Synthwave"] = createSynthwave()
        themes["Matrix"] = createMatrix()
        themes["Ocean"] = createOcean()
        themes["Blood"] = createBlood()
        themes["Midnight"] = createMidnight()
        themes["Forest"] = createForest()
        themes["Sunrise"] = createSunrise()
        themes["Cyberpunk"] = createCyberpunk()
        currentName = "DefaultAporia"
    }

    private fun createDefault() = Theme("DefaultAporia")

    private fun createAmethyst() = Theme("Amethyst").apply {
        guiBackground  = c(0xCC180828); guiTitleBg     = c(0xE0301058); guiTitleText   = c(0xFFD4A0FF)
        guiModuleText  = c(0xFFE8D0FF); guiEnabledDot  = c(0xFFB040FF); guiDisabledDot = c(0xFF503070)
        guiHoverBg     = c(0x30A040FF); guiSeparator   = c(0x20FFFFFF); guiSettingText = c(0xFFC8B0E0)
        guiSettingValue= c(0xFFE8D0FF); espPlayer      = c(0xFFFF40FF); espFriend      = c(0xFF55FFAA)
        espItem        = c(0xFFFFD700); espMob         = c(0xFFFF6600); fontNormal     = c(0xFFFFFFFF)
        fontHighlight  = c(0xFFFFAAFF); fontShadow     = c(0x80000000); mmTitle        = c(0xFFFFD0FF)
        mmSubtitle     = c(0xFFD080FF); mmButtonBg     = c(0x50301060); mmButtonFg     = c(0xFFFFD0FF)
    }

    private fun createSynthwave() = Theme("Synthwave").apply {
        guiBackground  = c(0xCC080018); guiTitleBg     = c(0xE0180048); guiTitleText   = c(0xFFFF6EB4)
        guiModuleText  = c(0xFFE0E0FF); guiEnabledDot  = c(0xFFFF00AA); guiDisabledDot = c(0xFF381050)
        guiHoverBg     = c(0x30FF00AA); guiSeparator   = c(0x20FFFFFF); guiSettingText = c(0xFFC080D0)
        guiSettingValue= c(0xFF80D0FF); espPlayer      = c(0xFFFF0080); espFriend      = c(0xFF00FFAA)
        espItem        = c(0xFFFFCC00); espMob         = c(0xFFFF4400); fontNormal     = c(0xFFFFFFFF)
        fontHighlight  = c(0xFFFF00AA); fontShadow     = c(0x80000000); mmTitle        = c(0xFFFF6EB4)
        mmSubtitle     = c(0xFF4ADEFF); mmButtonBg     = c(0x501A0050); mmButtonFg     = c(0xFFFF6EB4)
    }

    private fun createMatrix() = Theme("Matrix").apply {
        guiBackground  = c(0xCC001800); guiTitleBg     = c(0xE0004000); guiTitleText   = c(0xFF00FF41)
        guiModuleText  = c(0xFFAAFFAA); guiEnabledDot  = c(0xFF00FF41); guiDisabledDot = c(0xFF006000)
        guiHoverBg     = c(0x3000FF41); guiSeparator   = c(0x20FFFFFF); guiSettingText = c(0xFF80C080)
        guiSettingValue= c(0xFF00FF41); espPlayer      = c(0xFFFF3333); espFriend      = c(0xFF00FF41)
        espItem        = c(0xFFFFFF00); espMob         = c(0xFFFF8800); fontNormal     = c(0xFF00FF41)
        fontHighlight  = c(0xFFFFFFFF); fontShadow     = c(0x80000000); mmTitle        = c(0xFF00FF41)
        mmSubtitle     = c(0xFF80FF80); mmButtonBg     = c(0x50004000); mmButtonFg     = c(0xFF00FF41)
    }

    private fun createOcean() = Theme("Ocean").apply {
        guiBackground  = c(0xCC002040); guiTitleBg     = c(0xE0004070); guiTitleText   = c(0xFF80D0FF)
        guiModuleText  = c(0xFFD0F0FF); guiEnabledDot  = c(0xFF00BBFF); guiDisabledDot = c(0xFF004868)
        guiHoverBg     = c(0x3000BBFF); guiSeparator   = c(0x20FFFFFF); guiSettingText = c(0xFF90C0D0)
        guiSettingValue= c(0xFFC0F0FF); espPlayer      = c(0xFFFF5555); espFriend      = c(0xFF55FFAA)
        espItem        = c(0xFFFFDD00); espMob         = c(0xFFFF8800); fontNormal     = c(0xFFFFFFFF)
        fontHighlight  = c(0xFF00CCFF); fontShadow     = c(0x80000000); mmTitle        = c(0xFF80D0FF)
        mmSubtitle     = c(0xFF40A0D0); mmButtonBg     = c(0x50004870); mmButtonFg     = c(0xFF80D0FF)
    }

    private fun createBlood() = Theme("Blood").apply {
        guiBackground  = c(0xCC280000); guiTitleBg     = c(0xE0500000); guiTitleText   = c(0xFFFF6060)
        guiModuleText  = c(0xFFFFC0C0); guiEnabledDot  = c(0xFFFF0000); guiDisabledDot = c(0xFF700000)
        guiHoverBg     = c(0x30FF0000); guiSeparator   = c(0x20FFFFFF); guiSettingText = c(0xFFD08080)
        guiSettingValue= c(0xFFFFA0A0); espPlayer      = c(0xFFFF0000); espFriend      = c(0xFF55FF55)
        espItem        = c(0xFFFFAA00); espMob         = c(0xFFFF5500); fontNormal     = c(0xFFFFFFFF)
        fontHighlight  = c(0xFFFF6060); fontShadow     = c(0x80000000); mmTitle        = c(0xFFFF6060)
        mmSubtitle     = c(0xFFD04040); mmButtonBg     = c(0x50500000); mmButtonFg     = c(0xFFFF6060)
    }

    private fun createMidnight() = Theme("Midnight").apply {
        guiBackground  = c(0xCC0A0A18); guiTitleBg     = c(0xE0141430); guiTitleText   = c(0xFF8899CC)
        guiModuleText  = c(0xFFCCD6F0); guiEnabledDot  = c(0xFF4488FF); guiDisabledDot = c(0xFF384868)
        guiHoverBg     = c(0x184488FF); guiSeparator   = c(0x10FFFFFF); guiSettingText = c(0xFF7788AA)
        guiSettingValue= c(0xFFAABBEE); espPlayer      = c(0xFFFF4444); espFriend      = c(0xFF44FF88)
        espItem        = c(0xFFDDCC44); espMob         = c(0xFFFF8833); fontNormal     = c(0xFFCCCCDD)
        fontHighlight  = c(0xFF8899FF); fontShadow     = c(0x80000000); mmTitle        = c(0xFF8899CC)
        mmSubtitle     = c(0xFF6677AA); mmButtonBg     = c(0x50141430); mmButtonFg     = c(0xFF8899CC)
    }

    private fun createForest() = Theme("Forest").apply {
        guiBackground  = c(0xCC0C200C); guiTitleBg     = c(0xE01C3C1C); guiTitleText   = c(0xFF88CC88)
        guiModuleText  = c(0xFFC0E8C0); guiEnabledDot  = c(0xFF44BB44); guiDisabledDot = c(0xFF2E602E)
        guiHoverBg     = c(0x2044BB44); guiSeparator   = c(0x20FFFFFF); guiSettingText = c(0xFF80A880)
        guiSettingValue= c(0xFFB0E0B0); espPlayer      = c(0xFFFF6644); espFriend      = c(0xFF66FF66)
        espItem        = c(0xFFFFDD44); espMob         = c(0xFFFF8833); fontNormal     = c(0xFFE0E8D0)
        fontHighlight  = c(0xFF88FF88); fontShadow     = c(0x80000000); mmTitle        = c(0xFF88CC88)
        mmSubtitle     = c(0xFF66AA66); mmButtonBg     = c(0x501C3C1C); mmButtonFg     = c(0xFF88CC88)
    }

    private fun createSunrise() = Theme("Sunrise").apply {
        guiBackground  = c(0xCC201408); guiTitleBg     = c(0xE03C2810); guiTitleText   = c(0xFFFFAA44)
        guiModuleText  = c(0xFFFFD8B0); guiEnabledDot  = c(0xFFFF9900); guiDisabledDot = c(0xFF703800)
        guiHoverBg     = c(0x30FF9900); guiSeparator   = c(0x20FFFFFF); guiSettingText = c(0xFFD0A070)
        guiSettingValue= c(0xFFFFCC88); espPlayer      = c(0xFFFF4444); espFriend      = c(0xFF44FF88)
        espItem        = c(0xFFFFDD00); espMob         = c(0xFFFF6600); fontNormal     = c(0xFFFFF0E0)
        fontHighlight  = c(0xFFFFAA44); fontShadow     = c(0x80000000); mmTitle        = c(0xFFFFAA44)
        mmSubtitle     = c(0xFFDD8844); mmButtonBg     = c(0x503C2810); mmButtonFg     = c(0xFFFFAA44)
    }

    private fun createCyberpunk() = Theme("Cyberpunk").apply {
        guiBackground  = c(0xCC0A0018); guiTitleBg     = c(0xE0280038); guiTitleText   = c(0xFFFFDD00)
        guiModuleText  = c(0xFFE0E0FF); guiEnabledDot  = c(0xFF00FFD0); guiDisabledDot = c(0xFF480068)
        guiHoverBg     = c(0x30FFDD00); guiSeparator   = c(0x20FFFFFF); guiSettingText = c(0xFFC080D0)
        guiSettingValue= c(0xFF00FFD0); espPlayer      = c(0xFFFF0066); espFriend      = c(0xFF00FFD0)
        espItem        = c(0xFFFFDD00); espMob         = c(0xFFFF6600); fontNormal     = c(0xFFFFF0E0)
        fontHighlight  = c(0xFF00FFD0); fontShadow     = c(0x80000000); mmTitle        = c(0xFFFFDD00)
        mmSubtitle     = c(0xFF00FFD0); mmButtonBg     = c(0x50280038); mmButtonFg     = c(0xFFFFDD00)
    }

    private fun saveOne(name: String) {
        val t = themes[name] ?: return
        try {
            Files.createDirectories(dir)
            val cf = AprParser.ConfigFile()
            val mc = AprParser.ModuleConfig(t.name)
            with(mc.settings) {
                put("guiBackground", t.guiBackground.toString())
                put("guiTitleBg", t.guiTitleBg.toString())
                put("guiTitleText", t.guiTitleText.toString())
                put("guiModuleText", t.guiModuleText.toString())
                put("guiEnabledDot", t.guiEnabledDot.toString())
                put("guiDisabledDot", t.guiDisabledDot.toString())
                put("guiHoverBg", t.guiHoverBg.toString())
                put("guiSeparator", t.guiSeparator.toString())
                put("guiSettingText", t.guiSettingText.toString())
                put("guiSettingValue", t.guiSettingValue.toString())
                put("espPlayer", t.espPlayer.toString())
                put("espFriend", t.espFriend.toString())
                put("espItem", t.espItem.toString())
                put("espMob", t.espMob.toString())
                put("fontNormal", t.fontNormal.toString())
                put("fontHighlight", t.fontHighlight.toString())
                put("fontShadow", t.fontShadow.toString())
                put("mmTitle", t.mmTitle.toString())
                put("mmSubtitle", t.mmSubtitle.toString())
                put("mmButtonBg", t.mmButtonBg.toString())
                put("mmButtonFg", t.mmButtonFg.toString())
            }
            cf.modules.add(mc)
            FilesManager.writeApr(dir.resolve("$name.apr"), AprParser.serialize(cf))
        } catch (e: IOException) {
            Logger.error("[ThemeManager] Failed to save theme $name: ${e.message}")
        }
    }

    private fun saveSelected() {
        try {
            Files.createDirectories(dir)
            val cf = AprParser.ConfigFile()
            val mc = AprParser.ModuleConfig("selected")
            mc.settings["selected"] = currentName
            cf.modules.add(mc)
            FilesManager.writeApr(selectedFile, AprParser.serialize(cf))
        } catch (e: IOException) {
            Logger.error("[ThemeManager] Failed to save selected: ${e.message}")
        }
    }

    private fun loadAll() {
        try {
            Files.createDirectories(dir)
            Files.list(dir).filter { f ->
                f.toString().endsWith(".apr") && f.fileName.toString() != "_selected.apr"
            }.forEach { loadOne(it) }
            loadSelected()
        } catch (e: IOException) {
            Logger.error("[ThemeManager] Failed to list themes: ${e.message}")
        }
    }

    private fun loadOne(path: Path) {
        try {
            val text = FilesManager.readApr(path) ?: return
            if (text.isBlank()) return
            val cf = AprParser.parse(text)
            if (cf.modules.isEmpty()) return
            val mc = cf.modules[0]
            val name = mc.name
            val t = Theme(name)
            with(mc.settings) {
                t.guiBackground = intOr(get("guiBackground"), 0xA0000000)
                t.guiTitleBg = intOr(get("guiTitleBg"), 0xC81E1E28)
                t.guiTitleText = intOr(get("guiTitleText"), 0xFFDCDCFF)
                t.guiModuleText = intOr(get("guiModuleText"), 0xFFFFFFFF)
                t.guiEnabledDot = intOr(get("guiEnabledDot"), 0xFF64FF64)
                t.guiDisabledDot = intOr(get("guiDisabledDot"), 0xFF505050)
                t.guiHoverBg = intOr(get("guiHoverBg"), 0x18FFFFFF)
                t.guiSeparator = intOr(get("guiSeparator"), 0x18FFFFFF)
                t.guiSettingText = intOr(get("guiSettingText"), 0xFFFFFFFF)
                t.guiSettingValue = intOr(get("guiSettingValue"), 0xFFDCDCFF)
                t.espPlayer = intOr(get("espPlayer"), 0xFFFF5555)
                t.espFriend = intOr(get("espFriend"), 0xFF55FF55)
                t.espItem = intOr(get("espItem"), 0xFFFFFF55)
                t.espMob = intOr(get("espMob"), 0xFFFFAA00)
                t.fontNormal = intOr(get("fontNormal"), 0xFFFFFFFF)
                t.fontHighlight = intOr(get("fontHighlight"), 0xFFFFFF55)
                t.fontShadow = intOr(get("fontShadow"), 0x80000000)
                t.mmTitle = intOr(get("mmTitle"), 0xFFFFFFFF)
                t.mmSubtitle = intOr(get("mmSubtitle"), 0xFFB48CFF)
                t.mmButtonBg = intOr(get("mmButtonBg"), 0x4C140A28)
                t.mmButtonFg = intOr(get("mmButtonFg"), 0xFFFFFFFF)
            }
            themes[name] = t
        } catch (e: IOException) {
            Logger.error("[ThemeManager] Failed to load $path: ${e.message}")
        }
    }

    private fun loadSelected() {
        if (!FilesManager.exists(selectedFile)) return
        try {
            val text = FilesManager.readApr(selectedFile) ?: return
            if (text.isBlank()) return
            val cf = AprParser.parse(text)
            if (cf.modules.isNotEmpty()) {
                val sel = cf.modules[0].settings["selected"]
                if (sel != null && themes.containsKey(sel)) currentName = sel
            }
        } catch (e: IOException) {
            Logger.error("[ThemeManager] Failed to load selected: ${e.message}")
        }
    }

    private fun intOr(s: String?, def: Long): Int {
        if (s == null) return def.toInt()
        return try { s.toLong().toInt() } catch (_: NumberFormatException) { def.toInt() }
    }

    fun saveAll() {
        for (name in themes.keys) saveOne(name)
    }

    private fun c(hex: Long) = hex.toInt()

    companion object {
        @JvmStatic
        private var _instance: ThemeManagerModule? = null

        @JvmStatic
        val instance: ThemeManagerModule?
            get() = _instance

        @JvmStatic
        fun activeTheme(): Theme = instance?.active() ?: Theme("DefaultAporia")
    }
}
