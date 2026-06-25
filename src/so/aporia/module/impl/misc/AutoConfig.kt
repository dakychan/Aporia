package so.aporia.module.impl.misc

import com.chaos.annotation.Obfuscate
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.ModuleManager
import so.aporia.module.settings.*
import so.aporia.utils.files.FilesManager
import so.aporia.utils.user.logger.Logger
import java.io.IOException

@Obfuscate
class AutoConfig : Module("AutoConfig", Category.MISC) {

    val autoSave = BooleanSetting("AutoSave", "Автосохранение при изменении", true)

    override fun onEnable() {
        load()
        Logger.success("AutoConfig loaded")
    }

    override fun onDisable() {
        if (autoSave.isEnabled) save()
    }

    fun save() {
        val cfg = linkedMapOf<String, String>()
        for (m in ModuleManager.getAll()) {
            val p = key(m.name)
            cfg["$p.enabled"] = m.isEnabled.toString()
            cfg["$p.keybind"] = m.keybind.toString()
        }
        try {
            val sb = StringBuilder("# Aporia config\n")
            for ((k, v) in cfg) sb.appendLine("$k=$v")
            FilesManager.writeApr(CONFIG_PATH, sb.toString())
            Logger.info("Config saved → $CONFIG_PATH")
        } catch (e: IOException) {
            Logger.error("Save failed: ${e.message}")
        }
    }

    fun load() {
        if (!FilesManager.exists(CONFIG_PATH)) { Logger.info("No config, using defaults"); return }
        val cfg = linkedMapOf<String, String>()
        try {
            val raw = FilesManager.readApr(CONFIG_PATH)
            for (line in raw.split("\n")) {
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue
                val eq = trimmed.indexOf('=')
                if (eq > 0) cfg[trimmed.substring(0, eq).trim()] = trimmed.substring(eq + 1).trim()
            }
        } catch (e: IOException) { Logger.error("Load failed: ${e.message}"); return }
        for (m in ModuleManager.getAll()) {
            val p = key(m.name)
            val en = cfg["$p.enabled"]?.lowercase()?.toBooleanStrictOrNull()
            if (en != null) { if (en && !m.isEnabled) m.enable() else if (!en && m.isEnabled) m.disable() }
            val kb = cfg["$p.keybind"]
            if (kb != null) { try { m.keybind = kb.toInt() } catch (_: NumberFormatException) {} }
        }
        Logger.info("Config loaded ← $CONFIG_PATH")
    }

    private fun key(name: String) = name.replace(" ", "_")

    companion object {
        private val CONFIG_PATH = FilesManager.ROOT.resolve("config.apr")
    }
}
