/*
 * Copyright (c) 2025-2026 BEVoid Project
 * Distributed under the BEVoid Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.module.impl.misc;

import so.aporia.module.Category;
import so.aporia.module.Module;
import so.aporia.module.ModuleManager;
import so.aporia.module.settings.*;
import so.aporia.utils.files.FilesManager;
import so.aporia.utils.user.logger.Logger;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.*;

public final class AutoConfig extends Module {

    private static final Path CONFIG_PATH = FilesManager.ROOT.resolve("config.apr");

    public static float    guiX = -1, guiY = -1, guiW = 0, guiH = 0;
    public static String   guiCategory = null;

    private final BooleanSetting autoSave = new BooleanSetting(
        "AutoSave", "Автосохранение при изменении", true
    );

    public AutoConfig() {
        super("AutoConfig", Category.MISC);
    }

    @Override
    protected void onEnable() {
        load();
        Logger.success("AutoConfig loaded");
    }

    @Override
    protected void onDisable() {
        if (autoSave.isEnabled()) save();
    }

    public void save() {
        Map<String, String> cfg = new LinkedHashMap<>();
        cfg.put("gui.x", String.valueOf(guiX));
        cfg.put("gui.y", String.valueOf(guiY));
        cfg.put("gui.w", String.valueOf(guiW));
        cfg.put("gui.h", String.valueOf(guiH));
        if (guiCategory != null) cfg.put("gui.category", guiCategory);
        for (Module m : ModuleManager.INSTANCE.getAll()) {
            String p = key(m.name());
            cfg.put(p + ".enabled", String.valueOf(m.isEnabled()));
            cfg.put(p + ".keybind", String.valueOf(m.keybind()));
            for (Field f : m.getClass().getDeclaredFields()) {
                if (!Setting.class.isAssignableFrom(f.getType())) continue;
                f.setAccessible(true);
                try {
                    Setting<?> s = (Setting<?>) f.get(m);
                    String sk = p + ".s." + f.getName();
                    if      (s instanceof BooleanSetting bs)     cfg.put(sk, String.valueOf(bs.isEnabled()));
                    else if (s instanceof SelectSetting ss)       cfg.put(sk, ss.get());
                    else if (s instanceof TextSetting ts)         cfg.put(sk, ts.get());
                    else if (s instanceof BindSetting bs)         cfg.put(sk, String.valueOf(bs.getKey()));
                    else if (s instanceof MultiSelectSetting ms)  cfg.put(sk, String.join(",", ms.get()));
                } catch (IllegalAccessException ignored) {}
            }
        }
        try {
            StringBuilder sb = new StringBuilder("# Aporia config\n");
            for (Map.Entry<String, String> e : cfg.entrySet())
                sb.append(e.getKey()).append('=').append(e.getValue()).append('\n');
            FilesManager.writeApr(CONFIG_PATH, sb.toString());
            Logger.info("Config saved → " + CONFIG_PATH);
        } catch (IOException e) {
            Logger.error("Save failed: " + e.getMessage());
        }
    }

    public void load() {
        if (!FilesManager.exists(CONFIG_PATH)) { Logger.info("No config, using defaults"); return; }
        Map<String, String> cfg = new LinkedHashMap<>();
        try {
            String raw = FilesManager.readApr(CONFIG_PATH);
            for (String line : raw.split("\n")) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq > 0) cfg.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
            }
        } catch (IOException e) {
            Logger.error("Load failed: " + e.getMessage()); return;
        }
        guiX = parseFloat(cfg.get("gui.x"), -1);
        guiY = parseFloat(cfg.get("gui.y"), -1);
        guiW = parseFloat(cfg.get("gui.w"), 0);
        guiH = parseFloat(cfg.get("gui.h"), 0);
        guiCategory = cfg.get("gui.category");
        for (Module m : ModuleManager.INSTANCE.getAll()) {
            String p = key(m.name());

            Boolean en = parseBool(cfg.get(p + ".enabled"));
            if (en != null) { if (en && !m.isEnabled()) m.enable(); else if (!en && m.isEnabled()) m.disable(); }

            String kb = cfg.get(p + ".keybind");
            if (kb != null) { try { m.setKeybind(Integer.parseInt(kb)); } catch (NumberFormatException ignored) {} }

            for (Field f : m.getClass().getDeclaredFields()) {
                if (!Setting.class.isAssignableFrom(f.getType())) continue;
                f.setAccessible(true);
                try {
                    Setting<?> s = (Setting<?>) f.get(m);
                    String val = cfg.get(p + ".s." + f.getName());
                    if (val == null) continue;
                    if (s instanceof BooleanSetting bs) {
                        Boolean b = parseBool(val); if (b != null) bs.set(b);
                    } else if (s instanceof SelectSetting ss) {
                        ss.selected(val);
                    } else if (s instanceof TextSetting ts) {
                        ts.set(val);
                    } else if (s instanceof BindSetting bs) {
                        try { bs.setKey(Integer.parseInt(val)); } catch (NumberFormatException ignored) {}
                    } else if (s instanceof MultiSelectSetting ms) {
                        ms.get().clear();
                        if (!val.isEmpty()) for (String opt : val.split(",")) ms.get().add(opt.trim());
                    }
                } catch (IllegalAccessException ignored) {}
            }
        }
        Logger.info("Config loaded ← " + CONFIG_PATH);
    }

    private static String key(String name) { return name.replace(" ", "_"); }
    private static Boolean parseBool(String s) {
        if ("true".equalsIgnoreCase(s)) return true;
        if ("false".equalsIgnoreCase(s)) return false;
        return null;
    }
    private static float parseFloat(String s, float def) {
        if (s == null) return def;
        try { return Float.parseFloat(s); } catch (NumberFormatException e) { return def; }
    }

    public BooleanSetting getAutoSave() { return autoSave; }
}
