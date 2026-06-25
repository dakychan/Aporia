package so.aporia.utils.files

import com.chaos.annotation.Obfuscate
import so.aporia.utils.user.input.KeyCodeMap
import java.util.LinkedHashMap
import java.util.regex.Pattern

@Obfuscate
object AprParser {

    class ModuleConfig(@JvmField val name: String) {
        @JvmField val settings = linkedMapOf<String, String>()
        @JvmField var bind = -1
        @JvmField var active = false
    }

    class ConfigFile {
        @JvmField val modules = mutableListOf<ModuleConfig>()
    }

    @JvmStatic
    fun parse(input: String): ConfigFile {
        val result = ConfigFile()
        val text = stripComments(input)

        val modulePattern = Pattern.compile("module\\s+'([^']+)'\\s*\\(", Pattern.MULTILINE)
        val m = modulePattern.matcher(text)

        while (m.find()) {
            val modName = m.group(1)
            val start = m.end()
            val end = findMatchingParen(text, start)
            if (end < 0) continue

            val body = text.substring(start, end).trim()
            val mc = ModuleConfig(modName)
            parseBody(body, mc)
            result.modules.add(mc)
        }

        return result
    }

    @JvmStatic
    fun serialize(cf: ConfigFile): String {
        val sb = StringBuilder()
        for (mc in cf.modules) {
            sb.append("module '").append(mc.name).append("' (\n")
            sb.append("  active = ").append(if (mc.active) "T" else "F").append("\n")
            if (mc.bind >= 0) {
                sb.append("  bind = ").append(keyToString(mc.bind)).append("\n")
            }
            for ((key, value) in mc.settings) {
                sb.append("  setting '").append(key).append("' = ").append(value).append("\n")
            }
            sb.append(")\n\n")
        }
        return sb.toString()
    }

    private fun parseBody(body: String, mc: ModuleConfig) {
        for (line in body.split("\n")) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

            var m = Pattern.compile("^active\\s*=\\s*(.+)$").matcher(trimmed)
            if (m.find()) {
                mc.active = m.group(1)!!.trim().equals("T", ignoreCase = true) || m.group(1)!!.trim().equals("true", ignoreCase = true)
                continue
            }

            m = Pattern.compile("^bind\\s*=\\s*(.+)$").matcher(trimmed)
            if (m.find()) {
                mc.bind = parseKey(m.group(1)!!.trim())
                continue
            }

            m = Pattern.compile("^setting\\s+'([^']+)'\\s*=\\s*(.+)$").matcher(trimmed)
            if (m.find()) {
                mc.settings[m.group(1)!!] = m.group(2)!!.trim()
            }
        }
    }

    private fun findMatchingParen(text: String, start: Int): Int {
        var depth = 1
        for (i in start until text.length) {
            val c = text[i]
            if (c == '(') depth++
            else if (c == ')') {
                depth--
                if (depth == 0) return i
            }
        }
        return -1
    }

    private fun stripComments(input: String): String = input.replace("#[^\n]*".toRegex(), "")

    @JvmStatic
    fun keyToString(scancode: Int): String = KeyCodeMap.getName(scancode)

    @JvmStatic
    fun parseKey(keyStr: String): Int {
        if (keyStr.equals("none", ignoreCase = true) || keyStr.equals("null", ignoreCase = true)) return -1
        return KeyCodeMap.getScancode(keyStr)
    }
}
