/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.utils.files;

import java.util.*;
import java.util.regex.*;

/**
 * Parser for .apr config language.
 * <p>
 * Syntax:
 * <pre>
 * module 'Name' (
 *   setting 'key' = T, F
 *   bind = SHIFT
 *   number 'key' = 1.5
 *   text 'key' = 'hello'
 *   select 'key' = 'option1'
 * )
 * </pre>
 */
public final class AprParser {

    private AprParser() {}

    /** Parsed representation of a module config block. */
    public static final class ModuleConfig {
        public final String name;
        public final Map<String, String> settings = new LinkedHashMap<>();
        public int bind = -1;
        public boolean active = false;

        public ModuleConfig(String name) { this.name = name; }
    }

    /** Parsed representation of a full config file. */
    public static final class ConfigFile {
        public final List<ModuleConfig> modules = new ArrayList<>();
    }

    /**
     * Parses .apr config language text into a ConfigFile.
     */
    public static ConfigFile parse(String input) {
        ConfigFile result = new ConfigFile();
        String text = stripComments(input);

        Pattern modulePattern = Pattern.compile(
            "module\\s+'([^']+)'\\s*\\(",
            Pattern.MULTILINE
        );
        Matcher m = modulePattern.matcher(text);

        while (m.find()) {
            String modName = m.group(1);
            int start = m.end();
            int end = findMatchingParen(text, start);
            if (end < 0) continue;

            String body = text.substring(start, end).trim();
            ModuleConfig mc = new ModuleConfig(modName);
            parseBody(body, mc);
            result.modules.add(mc);
        }

        return result;
    }

    /**
     * Serializes a ConfigFile back into .apr language text.
     */
    public static String serialize(ConfigFile cf) {
        StringBuilder sb = new StringBuilder();
        for (ModuleConfig mc : cf.modules) {
            sb.append("module '").append(mc.name).append("' (\n");
            sb.append("  active = ").append(mc.active ? "T" : "F").append("\n");
            if (mc.bind >= 0) {
                sb.append("  bind = ").append(keyToString(mc.bind)).append("\n");
            }
            for (Map.Entry<String, String> e : mc.settings.entrySet()) {
                sb.append("  setting '").append(e.getKey()).append("' = ").append(e.getValue()).append("\n");
            }
            sb.append(")\n\n");
        }
        return sb.toString();
    }

    private static void parseBody(String body, ModuleConfig mc) {
        String[] lines = body.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            // active = T, F
            Pattern activePattern = Pattern.compile("^active\\s*=\\s*(.+)$");
            Matcher am = activePattern.matcher(line);
            if (am.find()) {
                mc.active = am.group(1).trim().equalsIgnoreCase("T") || am.group(1).trim().equalsIgnoreCase("true");
                continue;
            }

            // bind = KEY
            Pattern bindPattern = Pattern.compile("^bind\\s*=\\s*(.+)$");
            Matcher bm = bindPattern.matcher(line);
            if (bm.find()) {
                mc.bind = parseKey(bm.group(1).trim());
                continue;
            }

            // setting 'key' = value
            Pattern settingPattern = Pattern.compile("^setting\\s+'([^']+)'\\s*=\\s*(.+)$");
            Matcher sm = settingPattern.matcher(line);
            if (sm.find()) {
                String key = sm.group(1);
                String val = sm.group(2).trim();
                mc.settings.put(key, val);
            }
        }
    }

    private static int findMatchingParen(String text, int start) {
        int depth = 1;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private static String stripComments(String input) {
        return input.replaceAll("#[^\n]*", "");
    }

    /**
     * Converts a GLFW scancode to a string representation.
     */
    public static String keyToString(int scancode) {
        return so.aporia.utils.user.input.KeyCodeMap.getName(scancode);
    }

    /**
     * Parses a key string back to a GLFW scancode.
     */
    public static int parseKey(String keyStr) {
        if (keyStr.equalsIgnoreCase("none") || keyStr.equalsIgnoreCase("null")) return -1;
        return so.aporia.utils.user.input.KeyCodeMap.getScancode(keyStr);
    }
}
