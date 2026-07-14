/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.utils.files.impl;

import com.chaos.annotation.Obfuscate;
import so.aporia.module.Module;
import so.aporia.module.ModuleManager;
import so.aporia.module.settings.*;
import so.aporia.utils.files.AprParser;
import so.aporia.utils.files.FilesManager;
import so.aporia.utils.user.logger.Logger;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.*;

import com.chaos.annotation.ChaosNative;

/**
 * ConfigFile — сохранение/загрузка конфигов всех модулей в config.apr.
 * <p>
 * Использует кастомный .apr язык парсера:
 * <pre>
 * module 'AutoEZ' (
 *   bind = R
 *   setting 'Enabled' = T
 *   setting 'Delay' = 1.5
 * )
 * </pre>
 */
@Obfuscate
public final class ConfigFile {

    private static final Path FILE = FilesManager.ROOT.resolve("config.apr");
    private static volatile boolean dirty = false;
    private static final Set<String> activatedModules = new HashSet<>();
    private static Thread autoSaveThread;
    private static final long AUTO_SAVE_INTERVAL_MS = 30000;

    private ConfigFile() {}

    private static void startAutoSave() {
        if (autoSaveThread != null && autoSaveThread.isAlive()) return;
        autoSaveThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(AUTO_SAVE_INTERVAL_MS);
                    if (dirty) {
                        save();
                    }
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    Logger.error("Auto-save error: " + e.getMessage());
                }
            }
        }, "Aporia-ConfigAutoSave");
        autoSaveThread.setDaemon(true);
        autoSaveThread.start();
    }

    public static void markModuleActivated(Module mod) {
        activatedModules.add(mod.name());
    }

    /** Сохраняет все модули в config.apr */
    public static void save() {
        try {
            AprParser.ConfigFile cf = new AprParser.ConfigFile();

            for (Module mod : ModuleManager.INSTANCE.getAll()) {
                AprParser.ModuleConfig mc = new AprParser.ModuleConfig(mod.name());
                mc.bind = mod.keybind();
                mc.active = mod.isEnabled();

                for (Field f : mod.getClass().getDeclaredFields()) {
                    if (!Setting.class.isAssignableFrom(f.getType())) continue;
                    f.setAccessible(true);
                    try {
                        Setting<?> s = (Setting<?>) f.get(mod);
                        String val = settingToString(s);
                        if (val != null) {
                            mc.settings.put(s.name(), val);
                        }
                    } catch (IllegalAccessException ignored) {}
                }

                cf.modules.add(mc);
            }

            String text = AprParser.serialize(cf);
            FilesManager.writeApr(FILE, text);
            dirty = false;
            Logger.success("Config saved");
        } catch (IOException e) {
            Logger.error("Failed to save config: " + e.getMessage());
        }
    }

    /** Загружает config.apr и применяет ко всем модулям */
    public static void load() {
        try {
            if (FilesManager.exists(FILE)) {
                String text = FilesManager.readApr(FILE);
                AprParser.ConfigFile cf = AprParser.parse(text);

                for (AprParser.ModuleConfig mc : cf.modules) {
                    Module mod = ModuleManager.INSTANCE.get(mc.name);
                    if (mod == null) continue;

                    activatedModules.add(mod.name());

                    if (mc.bind >= 0) {
                        mod.setKeybind(mc.bind);
                    }

                    // Apply settings BEFORE enabling module
                    for (Field f : mod.getClass().getDeclaredFields()) {
                        if (!Setting.class.isAssignableFrom(f.getType())) continue;
                        f.setAccessible(true);
                        try {
                            Setting<?> s = (Setting<?>) f.get(mod);
                            String val = mc.settings.get(s.name());
                            if (val != null) {
                                applySetting(s, val);
                            }
                        } catch (IllegalAccessException ignored) {}
                    }

                    // Sync module.keybind from BindSetting (in case bind= was missing in config)
                    for (Field f : mod.getClass().getDeclaredFields()) {
                        if (!Setting.class.isAssignableFrom(f.getType())) continue;
                        f.setAccessible(true);
                        try {
                            Setting<?> s = (Setting<?>) f.get(mod);
                            if (s instanceof BindSetting) {
                                int bk = ((BindSetting) s).getKey();
                                if (bk >= 0 && bk != mod.keybind()) {
                                    mod.setKeybind(bk);
                                }
                            }
                        } catch (IllegalAccessException ignored) {}
                    }

                    if (mc.active) {
                        try {
                            mod.enable();
                        } catch (Exception e) {
                            Logger.warn("Deferred enable of " + mod.name() + ": " + e.getMessage());
                        }
                    }
                }

                Logger.success("Config loaded");
            }
            startAutoSave();
        } catch (Exception e) {
            Logger.error("Failed to load config: " + e.getMessage());
        }
    }

    /** Помечает конфиг как изменённый */
    public static void markDirty() {
        dirty = true;
    }

    /** Проверяет есть ли несохранённые изменения */
    public static boolean isDirty() {
        return dirty;
    }

    private static String settingToString(Setting<?> s) {
        if (s instanceof BooleanSetting bs) {
            return bs.isEnabled() ? "T" : "F";
        } else if (s instanceof SliderSetting ss) {
            return String.valueOf(ss.get());
        } else if (s instanceof TextSetting ts) {
            return "'" + ts.get().replace("'", "\\'") + "'";
        } else if (s instanceof SelectSetting ss) {
            return "'" + ss.get().replace("'", "\\'") + "'";
        } else if (s instanceof MultiSelectSetting mss) {
            List<String> sel = mss.getSelected();
            return "[" + String.join(", ", sel) + "]";
        } else if (s instanceof BindSetting bs2) {
            return AprParser.keyToString(bs2.getKey());
        } else if (s instanceof ColorSetting cs) {
            return "'" + cs.toHexString() + "'";
        }
        return null;
    }

    private static void applySetting(Setting<?> s, String val) {
        if (s instanceof BooleanSetting bs) {
            bs.set(val.equalsIgnoreCase("T") || val.equalsIgnoreCase("true"));
        } else if (s instanceof SliderSetting ss) {
            try {
                ss.setValue(Double.parseDouble(val));
            } catch (NumberFormatException ignored) {}
        } else if (s instanceof TextSetting ts) {
            ts.set(stripQuotes(val));
        } else if (s instanceof SelectSetting ss) {
            String option = stripQuotes(val);
            List<String> opts = ss.getOptions();
            for (int i = 0; i < opts.size(); i++) {
                if (opts.get(i).equals(option)) {
                    ss.setSelectedIndex(i);
                    break;
                }
            }
        } else if (s instanceof MultiSelectSetting mss) {
            if (val.startsWith("[") && val.endsWith("]")) {
                String inner = val.substring(1, val.length() - 1);
                List<String> items = Arrays.asList(inner.split(",\\s*"));
                mss.setSelected(items);
            }
        } else if (s instanceof BindSetting bs2) {
            bs2.setKey(AprParser.parseKey(val));
        } else if (s instanceof ColorSetting cs) {
            try {
                String hex = stripQuotes(val);
                if (hex.startsWith("#")) hex = hex.substring(1);
                cs.set((int)Long.parseLong(hex, 16));
            } catch (NumberFormatException ignored) {}
        }
    }

    private static String stripQuotes(String s) {
        if (s.length() >= 2 && s.startsWith("'") && s.endsWith("'")) {
            return s.substring(1, s.length() - 1).replace("\\'", "'");
        }
        return s;
    }
}
