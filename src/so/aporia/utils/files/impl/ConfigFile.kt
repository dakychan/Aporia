package so.aporia.utils.files.impl

import com.chaos.annotation.ChaosNative
import com.chaos.annotation.Obfuscate
import so.aporia.module.Module
import so.aporia.module.ModuleManager
import so.aporia.module.settings.*
import so.aporia.utils.files.AprParser
import so.aporia.utils.files.FilesManager
import so.aporia.utils.user.logger.Logger

/**
 * ConfigFile — save/load module configs to config.apr.
 */
@Obfuscate
@ChaosNative
object ConfigFile {

    private val FILE = FilesManager.ROOT.resolve("config.apr")
    @Volatile private var dirty = false
    private val activatedModules = mutableSetOf<String>()
    private var autoSaveThread: Thread? = null
    private const val AUTO_SAVE_INTERVAL_MS = 30000L

    private fun startAutoSave() {
        if (autoSaveThread?.isAlive == true) return
        autoSaveThread = Thread({
            while (!Thread.currentThread().isInterrupted) {
                try {
                    Thread.sleep(AUTO_SAVE_INTERVAL_MS)
                    if (dirty) save()
                } catch (_: InterruptedException) { break }
                catch (e: Exception) { Logger.error("Auto-save error: ${e.message}") }
            }
        }, "Aporia-ConfigAutoSave").apply { isDaemon = true; start() }
    }

    @JvmStatic
    fun stopAutoSave() {
        autoSaveThread?.interrupt()
        autoSaveThread = null
    }

    @JvmStatic
    fun markModuleActivated(mod: Module) {
        activatedModules.add(mod.name)
    }

    @JvmStatic
    fun save() {
        try {
            val cf = AprParser.ConfigFile()
            for (mod in ModuleManager.getAll()) {
                val mc = AprParser.ModuleConfig(mod.name)
                mc.bind = mod.keybind
                mc.active = mod.isEnabled
                for (f in mod.javaClass.declaredFields) {
                    if (!Setting::class.java.isAssignableFrom(f.type)) continue
                    f.isAccessible = true
                    try {
                        val s = f.get(mod) as? Setting<*> ?: continue
                        settingToString(s)?.let { mc.settings[s.name] = it }
                    } catch (_: IllegalAccessException) {}
                }
                cf.modules.add(mc)
            }
            FilesManager.writeApr(FILE, AprParser.serialize(cf))
            dirty = false
            Logger.success("Config saved")
        } catch (e: Exception) {
            Logger.error("Failed to save config: ${e.message}")
        }
    }

    @JvmStatic
    fun load() {
        try {
            if (FilesManager.exists(FILE)) {
                val text = FilesManager.readApr(FILE)
                val cf = AprParser.parse(text)
                for (mc in cf.modules) {
                    val mod = ModuleManager.get(mc.name) ?: continue
                    activatedModules.add(mod.name)
                    if (mc.bind >= 0) mod.keybind = mc.bind

                    for (f in mod.javaClass.declaredFields) {
                        if (!Setting::class.java.isAssignableFrom(f.type)) continue
                        f.isAccessible = true
                        try {
                            val s = f.get(mod) as? Setting<*> ?: continue
                            val v = mc.settings[s.name] ?: continue
                            applySetting(s, v)
                        } catch (_: IllegalAccessException) {}
                    }

                    for (f in mod.javaClass.declaredFields) {
                        if (!Setting::class.java.isAssignableFrom(f.type)) continue
                        f.isAccessible = true
                        try {
                            val s = f.get(mod)
                            if (s is BindSetting) {
                                val bk = s.getKey()
                                if (bk >= 0 && bk != mod.keybind) mod.keybind = bk
                            }
                        } catch (_: IllegalAccessException) {}
                    }

                    if (mc.active) {
                        try { mod.enable() }
                        catch (e: Exception) { Logger.warn("Deferred enable of ${mod.name}: ${e.message}") }
                    }
                }
                Logger.success("Config loaded")
            }
            startAutoSave()
        } catch (e: Exception) {
            Logger.error("Failed to load config: ${e.message}")
        }
    }

    @JvmStatic fun markDirty() { dirty = true }
    @JvmStatic fun isDirty(): Boolean = dirty

    private fun settingToString(s: Setting<*>): String? = when (s) {
        is BooleanSetting -> if (s.isEnabled) "T" else "F"
        is SliderSetting -> s.get().toString()
        is TextSetting -> "'${s.get().replace("'", "\\'")}'"
        is SelectSetting -> "'${s.get().replace("'", "\\'")}'"
        is MultiSelectSetting -> "[${s.getSelected().joinToString(", ")}]"
        is BindSetting -> AprParser.keyToString(s.getKey())
        is ColorSetting -> "'${s.toHexString()}'"
        else -> null
    }

    private fun applySetting(s: Setting<*>, v: String) {
        when (s) {
            is BooleanSetting -> s.set(v.equals("T", true) || v.equals("true", true))
            is SliderSetting -> try { s.setValue(v.toDouble()) } catch (_: NumberFormatException) {}
            is TextSetting -> s.set(stripQuotes(v))
            is SelectSetting -> {
                val opt = stripQuotes(v)
                s.getOptions().forEachIndexed { i, o -> if (o == opt) { s.setSelectedIndex(i); return } }
            }
            is MultiSelectSetting -> if (v.startsWith("[") && v.endsWith("]")) {
                s.setSelected(v.substring(1, v.length - 1).split(",\\s*".toRegex()))
            }
            is BindSetting -> s.setKey(AprParser.parseKey(v))
            is ColorSetting -> try {
                var hex = stripQuotes(v); if (hex.startsWith("#")) hex = hex.substring(1)
                s.set(hex.toLong(16).toInt())
            } catch (_: NumberFormatException) {}
        }
    }

    private fun stripQuotes(s: String): String =
        if (s.length >= 2 && s.startsWith("'") && s.endsWith("'")) s.substring(1, s.length - 1).replace("\\'", "'") else s
}
