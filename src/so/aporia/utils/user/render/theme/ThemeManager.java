/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.utils.user.render.theme;

import com.chaos.annotation.Obfuscate;
import so.aporia.utils.files.AprParser;
import so.aporia.utils.files.FilesManager;
import so.aporia.utils.user.logger.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import com.chaos.annotation.ChaosNative;

@Obfuscate
public final class ThemeManager {

    public static final ThemeManager INSTANCE = new ThemeManager();

    private static final Path DIR = FilesManager.ROOT.resolve("themes");
    private static final Path SELECTED = DIR.resolve("_selected.apr");

    private final Map<String, Theme> themes = new LinkedHashMap<>();
    private String current;

    private ThemeManager() {}

    public void init() {
        loadAll();
        loadSelected();
        if (themes.isEmpty()) {
            registerBuiltins();
        }
        if (current == null || !themes.containsKey(current)) {
            current = themes.keySet().iterator().next();
        }
        Logger.info("[ThemeManager] Loaded " + themes.size() + " themes, active: " + current);
    }

    private void registerBuiltins() {
        themes.put("DefaultAporia", createDefault());
        themes.put("Amethyst", createAmethyst());
        themes.put("Synthwave", createSynthwave());
        themes.put("Matrix", createMatrix());
        themes.put("Ocean", createOcean());
        themes.put("Blood", createBlood());
        themes.put("Midnight", createMidnight());
        themes.put("Forest", createForest());
        themes.put("Sunrise", createSunrise());
        themes.put("Cyberpunk", createCyberpunk());
        current = "DefaultAporia";
        for (String n : themes.keySet()) saveOne(n);
        saveSelected();
    }

    // --- CRUD ---

    public void create(String name) {
        if (themes.containsKey(name)) return;
        Theme t = new Theme(name);
        t.guiBackground = 0xA0000000;
        t.guiTitleBg = 0xC81E1E28;
        t.guiTitleText = 0xFFDCDCFF;
        t.guiModuleText = 0xFFFFFFFF;
        t.guiEnabledDot = 0xFF64FF64;
        t.guiDisabledDot = 0xFF505050;
        t.guiHoverBg = 0x18FFFFFF;
        t.guiSeparator = 0x18FFFFFF;
        t.guiSettingText = 0xFFFFFFFF;
        t.guiSettingValue = 0xFFDCDCFF;
        t.espPlayer = 0xFFFF5555;
        t.espFriend = 0xFF55FF55;
        t.espItem = 0xFFFFFF55;
        t.espMob = 0xFFFFAA00;
        t.fontNormal = 0xFFFFFFFF;
        t.fontHighlight = 0xFFFFFF55;
        t.fontShadow = 0x80000000;
        t.mmTitle = 0xFFFFFFFF;
        t.mmSubtitle = 0xFFB48CFF;
        t.mmButtonBg = 0x4C140A28;
        t.mmButtonFg = 0xFFFFFFFF;
        themes.put(name, t);
    }

    public void delete(String name) {
        if (name.equals("DefaultAporia")) return;
        themes.remove(name);
        Path f = DIR.resolve(name + ".apr");
        try { Files.deleteIfExists(f); } catch (IOException ignored) {}
        if (current != null && current.equals(name)) {
            current = themes.keySet().iterator().next();
            saveSelected();
        }
    }

    public void deleteAll() {
        Iterator<String> it = themes.keySet().iterator();
        while (it.hasNext()) {
            String name = it.next();
            if (name.equals("DefaultAporia")) continue;
            Path f = DIR.resolve(name + ".apr");
            try { Files.deleteIfExists(f); } catch (IOException ignored) {}
            it.remove();
        }
        current = "DefaultAporia";
        saveSelected();
    }

    public void select(String name) {
        if (themes.containsKey(name)) {
            current = name;
            saveSelected();
        }
    }

    public Theme get(String name) {
        return themes.get(name);
    }

    public Theme active() {
        return themes.get(current);
    }

    public String activeName() {
        return current;
    }

    public Collection<Theme> getAll() {
        return themes.values();
    }

    public Set<String> getNames() {
        return themes.keySet();
    }

    // --- Save / Load ---

    public void saveAll() {
        for (String name : themes.keySet()) {
            saveOne(name);
        }
    }

    public void saveOne(String name) {
        Theme t = themes.get(name);
        if (t == null) return;
        try {
            Files.createDirectories(DIR);
            AprParser.ConfigFile cf = new AprParser.ConfigFile();
            AprParser.ModuleConfig mc = new AprParser.ModuleConfig(t.name);
            mc.settings.put("guiBackground", String.valueOf(t.guiBackground));
            mc.settings.put("guiTitleBg", String.valueOf(t.guiTitleBg));
            mc.settings.put("guiTitleText", String.valueOf(t.guiTitleText));
            mc.settings.put("guiModuleText", String.valueOf(t.guiModuleText));
            mc.settings.put("guiEnabledDot", String.valueOf(t.guiEnabledDot));
            mc.settings.put("guiDisabledDot", String.valueOf(t.guiDisabledDot));
            mc.settings.put("guiHoverBg", String.valueOf(t.guiHoverBg));
            mc.settings.put("guiSeparator", String.valueOf(t.guiSeparator));
            mc.settings.put("guiSettingText", String.valueOf(t.guiSettingText));
            mc.settings.put("guiSettingValue", String.valueOf(t.guiSettingValue));
            mc.settings.put("espPlayer", String.valueOf(t.espPlayer));
            mc.settings.put("espFriend", String.valueOf(t.espFriend));
            mc.settings.put("espItem", String.valueOf(t.espItem));
            mc.settings.put("espMob", String.valueOf(t.espMob));
            mc.settings.put("fontNormal", String.valueOf(t.fontNormal));
            mc.settings.put("fontHighlight", String.valueOf(t.fontHighlight));
            mc.settings.put("fontShadow", String.valueOf(t.fontShadow));
            mc.settings.put("mmTitle", String.valueOf(t.mmTitle));
            mc.settings.put("mmSubtitle", String.valueOf(t.mmSubtitle));
            mc.settings.put("mmButtonBg", String.valueOf(t.mmButtonBg));
            mc.settings.put("mmButtonFg", String.valueOf(t.mmButtonFg));
            cf.modules.add(mc);
            FilesManager.writeApr(DIR.resolve(name + ".apr"), AprParser.serialize(cf));
        } catch (IOException e) {
            Logger.error("[ThemeManager] Failed to save theme " + name + ": " + e.getMessage());
        }
    }

    private void loadAll() {
        try {
            Files.createDirectories(DIR);
            try (var files = Files.list(DIR)) {
                files.filter(f -> f.toString().endsWith(".apr") && !f.getFileName().toString().equals("_selected.apr"))
                    .forEach(this::loadOne);
            }
        } catch (IOException e) {
            Logger.error("[ThemeManager] Failed to list themes: " + e.getMessage());
        }
    }

    private void loadOne(Path path) {
        try {
            String text = FilesManager.readApr(path);
            if (text == null || text.isBlank()) return;
            AprParser.ConfigFile cf = AprParser.parse(text);
            if (cf.modules.isEmpty()) return;
            AprParser.ModuleConfig mc = cf.modules.get(0);
            String name = mc.name;
            Theme t = new Theme(name);
            t.guiBackground = intOr(mc.settings.get("guiBackground"), 0xA0000000);
            t.guiTitleBg = intOr(mc.settings.get("guiTitleBg"), 0xC81E1E28);
            t.guiTitleText = intOr(mc.settings.get("guiTitleText"), 0xFFDCDCFF);
            t.guiModuleText = intOr(mc.settings.get("guiModuleText"), 0xFFFFFFFF);
            t.guiEnabledDot = intOr(mc.settings.get("guiEnabledDot"), 0xFF64FF64);
            t.guiDisabledDot = intOr(mc.settings.get("guiDisabledDot"), 0xFF505050);
            t.guiHoverBg = intOr(mc.settings.get("guiHoverBg"), 0x18FFFFFF);
            t.guiSeparator = intOr(mc.settings.get("guiSeparator"), 0x18FFFFFF);
            t.guiSettingText = intOr(mc.settings.get("guiSettingText"), 0xFFFFFFFF);
            t.guiSettingValue = intOr(mc.settings.get("guiSettingValue"), 0xFFDCDCFF);
            t.espPlayer = intOr(mc.settings.get("espPlayer"), 0xFFFF5555);
            t.espFriend = intOr(mc.settings.get("espFriend"), 0xFF55FF55);
            t.espItem = intOr(mc.settings.get("espItem"), 0xFFFFFF55);
            t.espMob = intOr(mc.settings.get("espMob"), 0xFFFFAA00);
            t.fontNormal = intOr(mc.settings.get("fontNormal"), 0xFFFFFFFF);
            t.fontHighlight = intOr(mc.settings.get("fontHighlight"), 0xFFFFFF55);
            t.fontShadow = intOr(mc.settings.get("fontShadow"), 0x80000000);
            t.mmTitle = intOr(mc.settings.get("mmTitle"), 0xFFFFFFFF);
            t.mmSubtitle = intOr(mc.settings.get("mmSubtitle"), 0xFFB48CFF);
            t.mmButtonBg = intOr(mc.settings.get("mmButtonBg"), 0x4C140A28);
            t.mmButtonFg = intOr(mc.settings.get("mmButtonFg"), 0xFFFFFFFF);
            themes.put(name, t);
        } catch (IOException e) {
            Logger.error("[ThemeManager] Failed to load " + path + ": " + e.getMessage());
        }
    }

    private void saveSelected() {
        try {
            Files.createDirectories(DIR);
            AprParser.ConfigFile cf = new AprParser.ConfigFile();
            AprParser.ModuleConfig mc = new AprParser.ModuleConfig("selected");
            mc.settings.put("selected", current);
            cf.modules.add(mc);
            FilesManager.writeApr(SELECTED, AprParser.serialize(cf));
        } catch (IOException e) {
            Logger.error("[ThemeManager] Failed to save selected: " + e.getMessage());
        }
    }

    private void loadSelected() {
        if (!FilesManager.exists(SELECTED)) return;
        try {
            String text = FilesManager.readApr(SELECTED);
            if (text == null || text.isBlank()) return;
            AprParser.ConfigFile cf = AprParser.parse(text);
            if (!cf.modules.isEmpty()) {
                current = cf.modules.get(0).settings.get("selected");
            }
        } catch (IOException e) {
            Logger.error("[ThemeManager] Failed to load selected: " + e.getMessage());
        }
    }

    private static int intOr(String s, int def) {
        if (s == null) return def;
        try { return (int) Long.parseLong(s); } catch (NumberFormatException e) { return def; }
    }

    // --- Default theme ---

    public static Theme createDefault() {
        Theme t = new Theme("DefaultAporia");
        t.guiBackground = 0xA0000000;
        t.guiTitleBg = 0xC81E1E28;
        t.guiTitleText = 0xFFDCDCFF;
        t.guiModuleText = 0xFFFFFFFF;
        t.guiEnabledDot = 0xFF64FF64;
        t.guiDisabledDot = 0xFF505050;
        t.guiHoverBg = 0x18FFFFFF;
        t.guiSeparator = 0x18FFFFFF;
        t.guiSettingText = 0xFFFFFFFF;
        t.guiSettingValue = 0xFFDCDCFF;
        t.espPlayer = 0xFFFF5555;
        t.espFriend = 0xFF55FF55;
        t.espItem = 0xFFFFFF55;
        t.espMob = 0xFFFFAA00;
        t.fontNormal = 0xFFFFFFFF;
        t.fontHighlight = 0xFFFFFF55;
        t.fontShadow = 0x80000000;
        t.mmTitle = 0xFFFFFFFF;
        t.mmSubtitle = 0xFFB48CFF;
        t.mmButtonBg = 0x4C140A28;
        t.mmButtonFg = 0xFFFFFFFF;
        return t;
    }

    // --- Builtin themes ---

    public static Theme createAmethyst() {
        Theme t = new Theme("Amethyst");
        t.guiBackground  = 0xCC1A0A2E;
        t.guiTitleBg     = 0xE02D1060;
        t.guiTitleText   = 0xFFD4A0FF;
        t.guiModuleText  = 0xFFE8D0FF;
        t.guiEnabledDot  = 0xFFB060FF;
        t.guiDisabledDot = 0xFF503070;
        t.guiHoverBg     = 0x30A050FF;
        t.guiSeparator   = 0x20FFFFFF;
        t.guiSettingText = 0xFFC8B0E0;
        t.guiSettingValue= 0xFFE8D0FF;
        t.espPlayer      = 0xFFFF55FF;
        t.espFriend      = 0xFF55FFAA;
        t.espItem        = 0xFFFFD700;
        t.espMob         = 0xFFFF6600;
        t.fontNormal     = 0xFFFFFFFF;
        t.fontHighlight  = 0xFFFFAAFF;
        t.fontShadow     = 0x80000000;
        t.mmTitle        = 0xFFFFD0FF;
        t.mmSubtitle     = 0xFFD080FF;
        t.mmButtonBg     = 0x4C2A0055;
        t.mmButtonFg     = 0xFFFFD0FF;
        return t;
    }

    public static Theme createSynthwave() {
        Theme t = new Theme("Synthwave");
        t.guiBackground  = 0xCC0A0020;
        t.guiTitleBg     = 0xE01A0050;
        t.guiTitleText   = 0xFFFF6EB4;
        t.guiModuleText  = 0xFFE0E0FF;
        t.guiEnabledDot  = 0xFFFF00AA;
        t.guiDisabledDot = 0xFF401060;
        t.guiHoverBg     = 0x30FF00AA;
        t.guiSeparator   = 0x20FFFFFF;
        t.guiSettingText = 0xFFC080D0;
        t.guiSettingValue= 0xFF80D0FF;
        t.espPlayer      = 0xFFFF0080;
        t.espFriend      = 0xFF00FFAA;
        t.espItem        = 0xFFFFCC00;
        t.espMob         = 0xFFFF4400;
        t.fontNormal     = 0xFFFFFFFF;
        t.fontHighlight  = 0xFFFF00AA;
        t.fontShadow     = 0x80000000;
        t.mmTitle        = 0xFFFF6EB4;
        t.mmSubtitle     = 0xFF4ADEFF;
        t.mmButtonBg     = 0x4C1A0050;
        t.mmButtonFg     = 0xFFFF6EB4;
        return t;
    }

    public static Theme createMatrix() {
        Theme t = new Theme("Matrix");
        t.guiBackground  = 0xCC001000;
        t.guiTitleBg     = 0xE0003000;
        t.guiTitleText   = 0xFF00FF41;
        t.guiModuleText  = 0xFFAAFFAA;
        t.guiEnabledDot  = 0xFF00FF41;
        t.guiDisabledDot = 0xFF005000;
        t.guiHoverBg     = 0x3000FF41;
        t.guiSeparator   = 0x20FFFFFF;
        t.guiSettingText = 0xFF80C080;
        t.guiSettingValue= 0xFF00FF41;
        t.espPlayer      = 0xFFFF3333;
        t.espFriend      = 0xFF00FF41;
        t.espItem        = 0xFFFFFF00;
        t.espMob         = 0xFFFF8800;
        t.fontNormal     = 0xFF00FF41;
        t.fontHighlight  = 0xFFFFFFFF;
        t.fontShadow     = 0x80000000;
        t.mmTitle        = 0xFF00FF41;
        t.mmSubtitle     = 0xFF80FF80;
        t.mmButtonBg     = 0x4C003000;
        t.mmButtonFg     = 0xFF00FF41;
        return t;
    }

    public static Theme createOcean() {
        Theme t = new Theme("Ocean");
        t.guiBackground  = 0xCC001828;
        t.guiTitleBg     = 0xE0003050;
        t.guiTitleText   = 0xFF80D0FF;
        t.guiModuleText  = 0xFFD0F0FF;
        t.guiEnabledDot  = 0xFF00AAFF;
        t.guiDisabledDot = 0xFF004060;
        t.guiHoverBg     = 0x3000AAFF;
        t.guiSeparator   = 0x20FFFFFF;
        t.guiSettingText = 0xFF90C0D0;
        t.guiSettingValue= 0xFFC0F0FF;
        t.espPlayer      = 0xFFFF5555;
        t.espFriend      = 0xFF55FFAA;
        t.espItem        = 0xFFFFDD00;
        t.espMob         = 0xFFFF8800;
        t.fontNormal     = 0xFFFFFFFF;
        t.fontHighlight  = 0xFF00CCFF;
        t.fontShadow     = 0x80000000;
        t.mmTitle        = 0xFF80D0FF;
        t.mmSubtitle     = 0xFF40A0D0;
        t.mmButtonBg     = 0x4C003860;
        t.mmButtonFg     = 0xFF80D0FF;
        return t;
    }

    public static Theme createBlood() {
        Theme t = new Theme("Blood");
        t.guiBackground  = 0xCC200000;
        t.guiTitleBg     = 0xE0400000;
        t.guiTitleText   = 0xFFFF6060;
        t.guiModuleText  = 0xFFFFC0C0;
        t.guiEnabledDot  = 0xFFFF0000;
        t.guiDisabledDot = 0xFF600000;
        t.guiHoverBg     = 0x30FF0000;
        t.guiSeparator   = 0x20FFFFFF;
        t.guiSettingText = 0xFFD08080;
        t.guiSettingValue= 0xFFFFA0A0;
        t.espPlayer      = 0xFFFF0000;
        t.espFriend      = 0xFF55FF55;
        t.espItem        = 0xFFFFAA00;
        t.espMob         = 0xFFFF5500;
        t.fontNormal     = 0xFFFFFFFF;
        t.fontHighlight  = 0xFFFF6060;
        t.fontShadow     = 0x80000000;
        t.mmTitle        = 0xFFFF6060;
        t.mmSubtitle     = 0xFFD04040;
        t.mmButtonBg     = 0x4C400000;
        t.mmButtonFg     = 0xFFFF6060;
        return t;
    }

    public static Theme createMidnight() {
        Theme t = new Theme("Midnight");
        t.guiBackground  = 0xCC080810;
        t.guiTitleBg     = 0xE0101028;
        t.guiTitleText   = 0xFF8899CC;
        t.guiModuleText  = 0xFFCCD6F0;
        t.guiEnabledDot  = 0xFF4488FF;
        t.guiDisabledDot = 0xFF334466;
        t.guiHoverBg     = 0x184488FF;
        t.guiSeparator   = 0x10FFFFFF;
        t.guiSettingText = 0xFF7788AA;
        t.guiSettingValue= 0xFFAABBEE;
        t.espPlayer      = 0xFFFF4444;
        t.espFriend      = 0xFF44FF88;
        t.espItem        = 0xFFDDCC44;
        t.espMob         = 0xFFFF8833;
        t.fontNormal     = 0xFFCCCCDD;
        t.fontHighlight  = 0xFF8899FF;
        t.fontShadow     = 0x80000000;
        t.mmTitle        = 0xFF8899CC;
        t.mmSubtitle     = 0xFF6677AA;
        t.mmButtonBg     = 0x4C101028;
        t.mmButtonFg     = 0xFF8899CC;
        return t;
    }

    public static Theme createForest() {
        Theme t = new Theme("Forest");
        t.guiBackground  = 0xCC0A180A;
        t.guiTitleBg     = 0xE0183018;
        t.guiTitleText   = 0xFF88CC88;
        t.guiModuleText  = 0xFFC0E8C0;
        t.guiEnabledDot  = 0xFF44AA44;
        t.guiDisabledDot = 0xFF2A552A;
        t.guiHoverBg     = 0x2044AA44;
        t.guiSeparator   = 0x20FFFFFF;
        t.guiSettingText = 0xFF80A880;
        t.guiSettingValue= 0xFFB0E0B0;
        t.espPlayer      = 0xFFFF6644;
        t.espFriend      = 0xFF66FF66;
        t.espItem        = 0xFFFFDD44;
        t.espMob         = 0xFFFF8833;
        t.fontNormal     = 0xFFE0E8D0;
        t.fontHighlight  = 0xFF88FF88;
        t.fontShadow     = 0x80000000;
        t.mmTitle        = 0xFF88CC88;
        t.mmSubtitle     = 0xFF66AA66;
        t.mmButtonBg     = 0x4C183018;
        t.mmButtonFg     = 0xFF88CC88;
        return t;
    }

    public static Theme createSunrise() {
        Theme t = new Theme("Sunrise");
        t.guiBackground  = 0xCC1A1008;
        t.guiTitleBg     = 0xE0302010;
        t.guiTitleText   = 0xFFFFAA44;
        t.guiModuleText  = 0xFFFFD8B0;
        t.guiEnabledDot  = 0xFFFF8800;
        t.guiDisabledDot = 0xFF663300;
        t.guiHoverBg     = 0x30FF8800;
        t.guiSeparator   = 0x20FFFFFF;
        t.guiSettingText = 0xFFD0A070;
        t.guiSettingValue= 0xFFFFCC88;
        t.espPlayer      = 0xFFFF4444;
        t.espFriend      = 0xFF44FF88;
        t.espItem        = 0xFFFFDD00;
        t.espMob         = 0xFFFF6600;
        t.fontNormal     = 0xFFFFF0E0;
        t.fontHighlight  = 0xFFFFAA44;
        t.fontShadow     = 0x80000000;
        t.mmTitle        = 0xFFFFAA44;
        t.mmSubtitle     = 0xFFDD8844;
        t.mmButtonBg     = 0x4C302010;
        t.mmButtonFg     = 0xFFFFAA44;
        return t;
    }

    public static Theme createCyberpunk() {
        Theme t = new Theme("Cyberpunk");
        t.guiBackground  = 0xCC080010;
        t.guiTitleBg     = 0xE0200030;
        t.guiTitleText   = 0xFFFFDD00;
        t.guiModuleText  = 0xFFE0E0FF;
        t.guiEnabledDot  = 0xFF00FFC8;
        t.guiDisabledDot = 0xFF400060;
        t.guiHoverBg     = 0x30FFDD00;
        t.guiSeparator   = 0x20FFFFFF;
        t.guiSettingText = 0xFFC080D0;
        t.guiSettingValue= 0xFF00FFC8;
        t.espPlayer      = 0xFFFF0066;
        t.espFriend      = 0xFF00FFC8;
        t.espItem        = 0xFFFFDD00;
        t.espMob         = 0xFFFF6600;
        t.fontNormal     = 0xFFFFF0E0;
        t.fontHighlight  = 0xFF00FFC8;
        t.fontShadow     = 0x80000000;
        t.mmTitle        = 0xFFFFDD00;
        t.mmSubtitle     = 0xFF00FFC8;
        t.mmButtonBg     = 0x4C200030;
        t.mmButtonFg     = 0xFFFFDD00;
        return t;
    }

    // --- Theme class ---

    public static final class Theme {
        private String name;

        // GUI
        public int guiBackground;
        public int guiTitleBg;
        public int guiTitleText;
        public int guiModuleText;
        public int guiEnabledDot;
        public int guiDisabledDot;
        public int guiHoverBg;
        public int guiSeparator;
        public int guiSettingText;
        public int guiSettingValue;

        // ESP
        public int espPlayer;
        public int espFriend;
        public int espItem;
        public int espMob;

        // Font
        public int fontNormal;
        public int fontHighlight;
        public int fontShadow;

        // MainMenu
        public int mmTitle;
        public int mmSubtitle;
        public int mmButtonBg;
        public int mmButtonFg;

        Theme(String name) {
            this.name = name;
        }

        public String name() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
