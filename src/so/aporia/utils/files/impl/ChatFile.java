package so.aporia.utils.files.impl;

import so.aporia.utils.files.FilesManager;
import so.aporia.utils.user.render.ui.chat.AporiaChatScreen.WinCfg;
import so.aporia.utils.user.render.ui.chat.AporiaChatScreen.WinMgr;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * ChatFile — сохранение/загрузка состояния чат-окон в chat.apr.
 *
 * Хранит:
 *   - список окон (позиция, размер, все настройки edit panel, фильтры)
 *   - активный индекс
 */
public final class ChatFile {

    private static final Path FILE = FilesManager.ROOT.resolve("chat.apr");

    private ChatFile() {}

    /** Сериализуемое представление одного окна. */
    public static final class WinData {
        public String  name          = "Main";
        public int     x             = -1;
        public int     bottomY       = -1;
        public int     w             = 320;
        public int     h             = 120;
        public boolean draggable     = false;
        public boolean searchOnOpen  = false;
        public boolean showOnlyFilter= false;
        public boolean showOnlyServer= false;
        public String  filterWords   = "";
        public String  msgPrefix     = "";
        public String  msgSuffix     = "";
        public String  prefixTriggers= "";
        public boolean prefixEnabled = true;
        public int     selfColor     = 0xFFADD8E6;
        public int     scrollOffset  = 0;
    }

    /** Корневой объект файла. */
    public static final class ChatData {
        public int           active = 0;
        public List<WinData> wins   = new ArrayList<>();
    }

    /** Сохраняет текущее состояние WinMgr в chat.apr. */
    public static void save() throws IOException {
        ChatData data = new ChatData();
        data.active = WinMgr.I.active;
        for (WinCfg c : WinMgr.I.wins) {
            WinData d = new WinData();
            d.name           = c.name;
            d.x              = c.x;
            d.bottomY        = c.bottomY;
            d.w              = c.w;
            d.h              = c.h;
            d.draggable      = c.draggable;
            d.searchOnOpen   = c.searchOnOpen;
            d.showOnlyFilter = c.showOnlyFilter;
            d.showOnlyServer = c.showOnlyServer;
            d.filterWords    = c.filterWords;
            d.msgPrefix      = c.msgPrefix;
            d.msgSuffix      = c.msgSuffix;
            d.prefixTriggers = c.prefixTriggers;
            d.prefixEnabled  = c.prefixEnabled;
            d.selfColor      = c.selfColor;
            d.scrollOffset   = c.scrollOffset;
            data.wins.add(d);
        }
        String json = toJson(data);
        FilesManager.writeApr(FILE, json);
    }

    /** Загружает chat.apr и применяет к WinMgr. Если файла нет — ничего не делает. */
    public static void load() throws IOException {
        if (!FilesManager.exists(FILE)) return;
        String json = FilesManager.readApr(FILE);
        ChatData data = fromJson(json);
        if (data == null || data.wins == null || data.wins.isEmpty()) return;
        WinMgr.I.wins.clear();
        for (WinData d : data.wins) {
            WinCfg c = new WinCfg(d.name, d.x, d.bottomY, d.w, d.h, d.draggable);
            c.searchOnOpen   = d.searchOnOpen;
            c.showOnlyFilter = d.showOnlyFilter;
            c.showOnlyServer = d.showOnlyServer;
            c.filterWords    = d.filterWords;
            c.msgPrefix      = d.msgPrefix;
            c.msgSuffix      = d.msgSuffix;
            c.prefixTriggers = d.prefixTriggers;
            c.prefixEnabled  = d.prefixEnabled;
            c.selfColor      = d.selfColor;
            c.scrollOffset   = d.scrollOffset;
            WinMgr.I.wins.add(c);
        }
        WinMgr.I.active = Math.max(0, Math.min(data.active, WinMgr.I.wins.size() - 1));
    }

    private static final com.google.gson.Gson GSON =
        new com.google.gson.GsonBuilder().setPrettyPrinting().create();

    private static String toJson(ChatData data) {
        return GSON.toJson(data);
    }

    private static ChatData fromJson(String json) {
        try {
            return GSON.fromJson(json, ChatData.class);
        } catch (Exception e) {
            return null;
        }
    }
}
