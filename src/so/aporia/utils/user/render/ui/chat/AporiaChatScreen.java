package so.aporia.utils.user.render.ui.chat;

import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import so.aporia.utils.KeyboardLayout;
import so.aporia.utils.files.impl.ChatFile;
import so.aporia.utils.user.logger.Logger;
import so.aporia.utils.user.render.animation.MessageAnim;
import so.aporia.utils.user.render.color.ColorUtil;
import so.aporia.utils.user.render.core.AporiaRenderer;
import so.aporia.utils.events.EventBus;
import so.aporia.utils.events.impl.KeyInputEvent;
import so.aporia.utils.events.impl.MouseClickEvent;
import so.aporia.utils.events.impl.MouseScrollEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom chat screen replacing vanilla ChatScreen.
 * Supports multiple chat windows, per-window prefix/suffix, message routing,
 * search, resize/drag, context menu and inline settings panel.
 */
@OnlyIn(Dist.CLIENT)
public class AporiaChatScreen extends ChatScreen {

    static final int LEFT       = 2;
    static final int BOTTOM_PAD = 2;
    static final int INPUT_H    = 14;
    static final int RADIUS     = 5;
    static final int GAP        = 3;
    static final int BOX_PAD    = 4;
    static final int LINE_H     = 10;
    static final int MAX_LINES  = 10;
    private static final int TEXT_PAD    = 8;
    private static final int MIN_W       = 80;
    private static final int RESIZE_HIT  = 6;
    private static final int DRAG_GRIP   = 8;

    private static final int C_INPUT_BG     = ColorUtil.rgba(16,  16,  32,  200);
    private static final int C_CHAT_BG      = ColorUtil.rgba(0,   0,   0,   150);
    private static final int C_HINT         = ColorUtil.rgba(120, 120, 120, 255);
    private static final int C_SEARCH_BG    = ColorUtil.rgba(20,  20,  50,  210);
    private static final int C_SEARCH_HL    = ColorUtil.rgba(255, 200, 50,  180);
    private static final int C_SCROLL_BAR   = ColorUtil.rgba(255, 255, 255,  60);
    private static final int C_SCROLL_THUMB = ColorUtil.rgba(255, 255, 255, 140);
    private static final int C_RESIZE_HINT  = ColorUtil.rgba(255, 255, 255,  30);
    private static final int C_DRAG_HINT    = ColorUtil.rgba(255, 255, 255,  15);


    /**
     * Per-window configuration: position, size, prefix/suffix, filter, scroll state
     * and the window's own message line store populated by {@link WinMgr#route}.
     */
    public static final class WinCfg {
        public String  name;
        public int     x, bottomY, w, h;
        public boolean draggable      = false;
        
        public boolean searchOnOpen   = false;
        public boolean showOnlyFilter = false;
        public boolean showOnlyServer = false;
        public boolean prefixEnabled  = true;
        
        public String  filterWords    = "";
        public String  msgPrefix      = "";
        public String  msgSuffix      = "";
        public String  prefixTriggers = "";
        
        public int     selfColor      = 0xFFADD8E6;
        public int     scrollOffset   = 0;

        public final java.util.ArrayDeque<GuiMessage.Line> lines = new java.util.ArrayDeque<>(100);
        private static final int MAX_STORED = 200;

        public WinCfg(String name, int x, int bottomY, int w, int h, boolean draggable) {
            this.name=name; this.x=x; this.bottomY=bottomY; this.w=w; this.h=h; this.draggable=draggable;
        }

        int maxLines() { return Math.max(1, (h - BOX_PAD*2) / LINE_H); }

        void addLine(GuiMessage.Line l) {
            lines.addFirst(l);
            while (lines.size() > MAX_STORED) lines.removeLast();
        }

        public void clear() { lines.clear(); scrollOffset = 0; }
    }


    /**
     * Singleton window manager. Holds the list of {@link WinCfg} windows and the active index.
     * Routes incoming {@link GuiMessage} objects to the correct windows based on filter keywords.
     */
    public static final class WinMgr {
        public static final WinMgr I = new WinMgr();
        public final List<WinCfg> wins = new ArrayList<>();
        public int active = 0;

        private WinMgr() { wins.add(makeWin("Main", 0)); }

        WinCfg get() { return wins.get(active); }

        void add(String name) {
            int idx = wins.size();
            wins.add(makeWin(name, idx));
            active = wins.size()-1;
        }

        void remove(int i) {
            if (wins.size() <= 1) return;
            wins.remove(i);
            active = Math.max(0, Math.min(active, wins.size()-1));
        }

        /**
         * Routes a new message to all matching windows.
         * Filtered windows claim messages that match their keywords;
         * unfiltered windows receive everything not claimed by any filter.
         */
        public void route(GuiMessage msg, net.minecraft.client.gui.Font font) {
            String full = msg.content().getString().toLowerCase();
            boolean isServerMessage = msg.tag() != null && msg.tag().toString().contains("System");
            
            List<WinCfg> claimants = new ArrayList<>();
            for (WinCfg w : wins) {
                if (!w.showOnlyFilter || w.filterWords.isEmpty()) continue;
                for (String kw : splitTokens(w.filterWords)) {
                    if (matchesKw(full, kw)) { claimants.add(w); break; }
                }
            }
            for (WinCfg w : wins) {
                boolean isFiltered = w.showOnlyFilter && !w.filterWords.isEmpty();
                boolean claimed    = claimants.contains(w);
                if (isFiltered && !claimed) continue;
                if (!isFiltered && !claimants.isEmpty()) continue;
                
                if (w.showOnlyServer && !isServerMessage) continue;
                
                int splitW = Math.max(10, w.w - BOX_PAD*2 - TEXT_PAD);
                List<net.minecraft.util.FormattedCharSequence> parts = msg.splitLines(font, splitW);
                for (int i = 0; i < parts.size(); i++) {
                    w.addLine(new GuiMessage.Line(msg.addedTime(), parts.get(i), msg.tag(), i == parts.size()-1));
                }
            }
        }

        /** Clears all windows and rebuilds their line stores from the full chat history. */
        public void rebuild(net.minecraft.client.gui.components.ChatComponent chat,
                            net.minecraft.client.gui.Font font) {
            for (WinCfg w : wins) w.clear();
            List<GuiMessage> all = chat.getAllMessages();
            for (int i = all.size()-1; i >= 0; i--) route(all.get(i), font);
        }

        private static boolean matchesKw(String text, String kw) {
            if (kw.isEmpty()) return false;
            String[] v = KeyboardLayout.both(kw.toLowerCase());
            return text.contains(v[0]) || text.contains(v[1]);
        }

        /** Сплитит filterWords по запятой или пробелу, возвращает токены без пустых. */
        static String[] splitTokens(String filterWords) {
            return java.util.Arrays.stream(filterWords.split("[,\\s]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        }

        private static WinCfg makeWin(String name, int idx) {
            int defW = 320, defH = MAX_LINES*LINE_H + BOX_PAD*2;
            return switch (idx) {
                case 0  -> new WinCfg(name, LEFT, BOTTOM_PAD+INPUT_H+GAP, defW, defH, false);
                default -> new WinCfg(name, -1, -1, defW, defH, true);
            };
        }
    }


    /**
     * Right-click context menu with options: Edit, Add window, Delete window, and window switcher.
     */
    private static final class CtxMenu {
        boolean visible = false;
        int renderX, renderY;

        private static final int W      = 160;
        private static final int ITEM   = 14;
        private static final int PAD    = 6;
        private static final int C_BG   = ColorUtil.rgba(18,18,30,235);
        private static final int C_ITEM = ColorUtil.rgba(255,255,255,200);
        private static final int C_HOV  = ColorUtil.rgba(80,80,160,180);
        private static final int C_SEP  = ColorUtil.rgba(255,255,255,40);

        void open(int x, int y, int screenW, int screenH) {
            visible = true;
            int totalH = menuHeight(WinMgr.I.wins.size());
            renderX = (x + W > screenW) ? x - W : x;
            renderY = (y + totalH > screenH) ? y - totalH : y;
        }

        void close() { visible = false; }

        private int menuHeight(int winCount) {
            return PAD*2 + 3*ITEM + (winCount > 0 ? 4 + winCount*ITEM : 0);
        }

        int itemAt(double px, double py, int winCount) {
            if (!visible) return -1;
            int totalH = menuHeight(winCount);
            if (px<renderX||px>renderX+W||py<renderY||py>renderY+totalH) return -1;
            int rel  = (int)(py - renderY - PAD);
            int item = rel / ITEM;
            return Math.max(0, Math.min(2 + winCount, item));
        }

        void render(GuiGraphics gfx, net.minecraft.client.gui.Font font, int winCount) {
            if (!visible) return;
            int totalH = menuHeight(winCount);
            AporiaRenderer.INSTANCE.drawRect(renderX, renderY, W, totalH, 5, C_BG);
            String[] labels = {"§fEdit", "§aAdd window", "§cDelete window"};
            for (int i = 0; i < 3; i++)
                gfx.drawString(font, labels[i], renderX+PAD, renderY+PAD+i*ITEM+3, C_ITEM, false);
            if (winCount > 0) {
                int sy = renderY+PAD+3*ITEM;
                gfx.fill(renderX+PAD, sy, renderX+W-PAD, sy+1, C_SEP);
                for (int i = 0; i < winCount; i++) {
                    int iy = sy+4+i*ITEM;
                    if (i == WinMgr.I.active) gfx.fill(renderX+2, iy, renderX+W-2, iy+ITEM, C_HOV);
                    gfx.drawString(font, WinMgr.I.wins.get(i).name, renderX+PAD, iy+3, C_ITEM, false);
                }
            }
        }
    }


    /**
     * Inline settings panel rendered over the active chat window.
     * Shows 10 editable fields with scrolling support.
     * Fields: Name, Prefix, Prefix enabled, Prefix triggers, Suffix,
     *         Filter words, Filter only, Search on open, Only server, Self color.
     */
    private static final class EditPanel {
        boolean visible = false;
        int bx, by, bw, bh;
        int scrollOffset = 0;

        static final int ITEM   = 16;
        static final int PAD    = 6;
        static final int HDR_H  = ITEM + PAD;
        static final int FIELDS = 10;

        private static final int C_BG      = ColorUtil.rgba(10,  10,  24,  250);
        private static final int C_HDR     = ColorUtil.rgba(40,  40,  80,  255);
        private static final int C_LBL     = ColorUtil.rgba(150, 150, 200, 255);
        private static final int C_VAL     = ColorUtil.rgba(255, 255, 255, 255);
        private static final int C_ROW_ALT = ColorUtil.rgba(255, 255, 255,   8);
        private static final int C_SEP     = ColorUtil.rgba(255, 255, 255,  25);
        private static final int C_SCROLL  = ColorUtil.rgba(255, 255, 255,  40);
        private static final int C_THUMB   = ColorUtil.rgba(255, 255, 255, 120);

        void open()  { visible = true; scrollOffset = 0; }
        void close() { visible = false; }
        void setBox(int x, int y, int w, int h) { bx=x; by=y; bw=w; bh=h; }

        int visibleRows() { return Math.max(1, (bh - HDR_H - PAD) / ITEM); }

        void scroll(int delta) {
            int maxS = Math.max(0, FIELDS - visibleRows());
            scrollOffset = Math.max(0, Math.min(scrollOffset + delta, maxS));
        }

        void render(GuiGraphics gfx, net.minecraft.client.gui.Font font, WinCfg c) {
            if (!visible) return;
            AporiaRenderer r = AporiaRenderer.INSTANCE;
            r.drawRect(bx, by, bw, bh, RADIUS, C_BG);
            r.drawRect(bx, by, bw, HDR_H, RADIUS, C_HDR);
            gfx.drawString(font, "§fSettings — §7" + c.name, bx+PAD, by+PAD/2+3, C_VAL, false);
            String esc = "§7[Esc]";
            gfx.drawString(font, esc, bx+bw-font.width(esc)-PAD, by+PAD/2+3, C_VAL, false);
            int contentY = by + HDR_H;
            gfx.fill(bx+PAD, contentY, bx+bw-PAD, contentY+1, C_SEP);
            contentY += 1;
            String[] labels = {"Name","Prefix","Prefix enabled","Prefix triggers","Suffix",
                               "Filter words","Filter only","Search on open","Only server","Self color"};
            String[] values = {
                c.name, c.msgPrefix,
                c.prefixEnabled  ? "§aON" : "§cOFF",
                c.prefixTriggers.isEmpty() ? "§7(always)" : c.prefixTriggers,
                c.msgSuffix, c.filterWords,
                c.showOnlyFilter ? "§aON" : "§cOFF",
                c.searchOnOpen   ? "§aON" : "§cOFF",
                c.showOnlyServer ? "§aON" : "§cOFF",
                String.format("#%06X", c.selfColor & 0xFFFFFF)
            };
            int rows = visibleRows();
            int maxS = Math.max(0, FIELDS - rows);
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxS));
            gfx.enableScissor(bx, contentY, bx+bw, by+bh);
            for (int i = 0; i < rows && (i + scrollOffset) < FIELDS; i++) {
                int fi = i + scrollOffset;
                int ry = contentY + PAD/2 + i * ITEM;
                if (fi % 2 == 0) gfx.fill(bx+2, ry, bx+bw-2, ry+ITEM, C_ROW_ALT);
                gfx.drawString(font, "§7" + labels[fi], bx+PAD, ry+4, C_LBL, false);
                gfx.drawString(font, values[fi], bx+PAD+100, ry+4, C_VAL, false);
            }
            gfx.disableScissor();
            if (maxS > 0) {
                int barH   = bh - HDR_H - PAD;
                int thumbH = Math.max(10, barH * rows / FIELDS);
                int thumbY = contentY + PAD/2 + (barH - thumbH) * scrollOffset / maxS;
                gfx.fill(bx+bw-3, contentY+PAD/2, bx+bw-1, contentY+PAD/2+barH, C_SCROLL);
                gfx.fill(bx+bw-3, thumbY, bx+bw-1, thumbY+thumbH, C_THUMB);
            }
        }

        int fieldAt(double px, double py) {
            if (!visible) return -1;
            int contentY = by + HDR_H + 1 + PAD/2;
            if (px<bx||px>bx+bw||py<contentY||py>by+bh) return -1;
            int rel = (int)(py - contentY);
            int row = rel / ITEM;
            int fi  = row + scrollOffset;
            return (fi >= 0 && fi < FIELDS) ? fi : -1;
        }

        boolean onScroll(double px, double py, double dy) {
            if (!visible || px<bx||px>bx+bw||py<by||py>by+bh) return false;
            scroll(dy > 0 ? -1 : 1);
            return true;
        }
    }


    /** Resize/drag handle type returned by {@link #hitHandle}. */
    private enum Handle { NONE, DRAG, TOP, LEFT, RIGHT, TOP_LEFT, TOP_RIGHT }

    private boolean searchMode  = false;
    private String  searchQuery = "";
    private EditBox searchBox;
    private final CtxMenu   ctx  = new CtxMenu();
    private final EditPanel edit = new EditPanel();
    private int     editingField = -1;
    private EditBox fieldBox;
    private Handle  dragging = Handle.NONE;

    private boolean scrollDragging = false;
    private double scrollDragStartY = 0;
    private int scrollDragStartOffset = 0;

    private net.minecraft.network.chat.Style hoveredStyle   = null;
    private int                              hoveredLineIdx  = -1;

    public AporiaChatScreen(String initial, boolean isDraft) { super(initial, isDraft); }

    private WinCfg cfg() { return WinMgr.I.get(); }

    private int barWidth() {
        String v = this.input != null ? this.input.getValue() : "";
        return Math.min(Math.max(this.font.width(v) + TEXT_PAD*2, MIN_W), cfg().w);
    }

    private static java.util.List<GuiMessage.Line> visibleLines(WinCfg c) {
        return new java.util.ArrayList<>(c.lines);
    }

    /**
     * Walks a {@link net.minecraft.util.FormattedCharSequence} and returns the {@link net.minecraft.network.chat.Style}
     * of the glyph at pixel offset {@code relX} from the start of the line.
     */
    private net.minecraft.network.chat.Style styleAtX(net.minecraft.util.FormattedCharSequence seq, int relX) {
        net.minecraft.network.chat.Style[] found = {null};
        float[] accW = {0f};
        seq.accept((idx, style, cp) -> {
            float cw = this.font.getSplitter().stringWidth(
                net.minecraft.util.FormattedCharSequence.forward(new String(Character.toChars(cp)), style));
            if (accW[0] + cw > relX) { found[0] = style; return false; }
            accW[0] += cw;
            return true;
        });
        return found[0];
    }

    private void resolvePosition(WinCfg c, int idx) {
        if (c.x != -1) return;
        int defW = c.w, defH = c.h;
        switch (idx % 4) {
            case 1 -> { c.x = this.width-defW-LEFT; c.bottomY = BOTTOM_PAD+INPUT_H+GAP; }
            case 2 -> { c.x = LEFT;                 c.bottomY = this.height-defH-GAP*2; }
            case 3 -> { c.x = this.width-defW-LEFT; c.bottomY = this.height-defH-GAP*2; }
            default-> { c.x = this.width/2-defW/2;  c.bottomY = this.height/2-defH/2;   }
        }
    }

    private int boxX()   { WinCfg c = cfg(); resolvePosition(c, WinMgr.I.active); return c.x; }
    private int boxY()   { WinCfg c = cfg(); return this.height - c.bottomY - c.h; }
    private int boxW()   { return cfg().w; }
    private int boxH()   { return cfg().h; }
    private int inputY() { return this.height - BOTTOM_PAD - INPUT_H; }


    @Override
    protected void init() {
        this.historyPos = this.minecraft.gui.getChat().getRecentChat().size();
        for (int i = 1; i < WinMgr.I.wins.size(); i++) resolvePosition(WinMgr.I.wins.get(i), i);

        WinCfg c  = cfg();
        int    iy = inputY();
        this.input = new EditBox(
            this.minecraft.fontFilterFishy,
            c.x+TEXT_PAD, iy+(INPUT_H-9)/2, c.w-TEXT_PAD*2, 9,
            Component.translatable("chat.editBox")
        ) {
            @Override protected MutableComponent createNarrationMessage() {
                return super.createNarrationMessage()
                    .append(AporiaChatScreen.this.commandSuggestions.getNarrationMessage());
            }
        };
        this.input.setMaxLength(256);
        this.input.setBordered(false);
        this.input.setValue(this.initial);
        this.input.setResponder(s -> {
            this.commandSuggestions.setAllowSuggestions(true);
            this.commandSuggestions.updateCommandInfo();
            this.isDraft = false;
        });
        this.input.setCanLoseFocus(false);
        this.addRenderableWidget(this.input);

        this.searchBox = new EditBox(this.font, c.x+TEXT_PAD, 0, c.w-TEXT_PAD*2, 9, Component.literal(""));
        this.searchBox.setMaxLength(64);
        this.searchBox.setBordered(false);
        this.searchBox.setResponder(s -> searchQuery = s);
        this.searchBox.setVisible(false);
        this.addRenderableWidget(this.searchBox);

        this.fieldBox = new EditBox(this.font, 0, 0, 100, 9, Component.literal(""));
        this.fieldBox.setMaxLength(128);
        this.fieldBox.setBordered(false);
        this.fieldBox.setVisible(false);
        this.addRenderableWidget(this.fieldBox);

        this.commandSuggestions = new CommandSuggestions(
            this.minecraft, this, this.input, this.font,
            false, false, 1, 10, true, -805306368);
        this.commandSuggestions.setAllowHiding(false);
        this.commandSuggestions.setAllowSuggestions(false);
        this.commandSuggestions.updateCommandInfo();

        if (cfg().searchOnOpen) openSearch();
        WinMgr.I.rebuild(this.minecraft.gui.getChat(), this.font);
    }


    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float delta) {
        AporiaRenderer r = AporiaRenderer.INSTANCE;
        int active = WinMgr.I.active;
        for (int i = 0; i < WinMgr.I.wins.size(); i++) {
            if (i != active) renderWindowContent(gfx, r, WinMgr.I.wins.get(i), mouseX, mouseY, false);
        }
        renderWindowContent(gfx, r, cfg(), mouseX, mouseY, true);

        int bx = boxX(), iy = inputY(), bW = barWidth();
        r.drawRect(bx, iy, bW, INPUT_H, RADIUS, C_INPUT_BG);
        gfx.enableScissor(bx, iy, bx+bW, iy+INPUT_H);
        this.input.render(gfx, mouseX, mouseY, delta);
        gfx.disableScissor();
        if (this.input.getValue().isEmpty() && !searchMode)
            gfx.drawString(this.font, "Message...", bx+TEXT_PAD, iy+(INPUT_H-8)/2, C_HINT, false);

        this.commandSuggestions.render(gfx, mouseX, mouseY);

        if (hoveredStyle != null && hoveredStyle.getClickEvent() instanceof net.minecraft.network.chat.ClickEvent.OpenUrl url) {
            gfx.setTooltipForNextFrame(this.font,
                java.util.List.of(net.minecraft.util.FormattedCharSequence.forward(
                    url.uri().toString(), net.minecraft.network.chat.Style.EMPTY)),
                mouseX, mouseY);
        }

        if (edit.visible) {
            edit.render(gfx, this.font, cfg());
            if (editingField >= 0) renderFieldEditor(gfx, r, edit.bx, edit.by, edit.bw);
        }
        ctx.render(gfx, this.font, WinMgr.I.wins.size());
    }

    /**
     * Renders background, messages, scrollbar, search bar and resize hints for one window.
     * Overlays (edit panel, context menu) are rendered separately in {@link #render}.
     */
    private void renderWindowContent(GuiGraphics gfx, AporiaRenderer r, WinCfg c, int mouseX, int mouseY, boolean isActive) {
        resolvePosition(c, WinMgr.I.wins.indexOf(c));
        int bx = c.x, by = this.height-c.bottomY-c.h, bw = c.w, bh = c.h;
        if (isActive) edit.setBox(bx, by, bw, bh);
        if (isActive && edit.visible) {
            renderResizeHints(gfx, c, bx, by, bw, bh, mouseX, mouseY);
            return;
        }
        int msgCount = Math.min(c.lines.size(), c.maxLines());
        int actualH  = msgCount == 0 ? 0 : Math.min(bh, msgCount * LINE_H + BOX_PAD * 2);
        int actualBy = by + bh - actualH;
        if (actualH > 0) r.drawRect(bx, actualBy, bw, actualH, RADIUS, C_CHAT_BG);
        gfx.enableScissor(bx, actualBy, bx+bw, by+bh);
        if (isActive) renderMessages(gfx, c, by+BOX_PAD, bx+BOX_PAD);
        else          renderMessagesPassive(gfx, c, by+BOX_PAD, bx+BOX_PAD);
        gfx.disableScissor();
        renderScrollBar(gfx, c, bx, by, bw, bh);
        renderResizeHints(gfx, c, bx, by, bw, bh, mouseX, mouseY);
        if (isActive && searchMode) {
            int sy = by - GAP - INPUT_H;
            r.drawRect(bx, sy, Math.min(bw, 200), INPUT_H, RADIUS, C_SEARCH_BG);
            if (searchQuery.isEmpty())
                gfx.drawString(this.font, "§7Search...", bx+TEXT_PAD, sy+(INPUT_H-8)/2, 0xFFFFFFFF, false);
            this.searchBox.setX(bx+TEXT_PAD);
            this.searchBox.setY(sy+(INPUT_H-9)/2);
            this.searchBox.setWidth(Math.min(bw, 200) - TEXT_PAD*2);
            this.searchBox.setVisible(true);
            this.searchBox.render(gfx, 0, 0, 0);
        }
    }

    private void renderMessages(GuiGraphics gfx, WinCfg c, int boxTopY, int textX) {
        int total = c.lines.size();
        int maxL  = c.maxLines();
        int maxS  = Math.max(0, total - maxL);
        c.scrollOffset = Math.max(0, Math.min(c.scrollOffset, maxS));
        int count   = Math.min(maxL, total - c.scrollOffset);
        int bottomY = boxTopY + maxL * LINE_H - LINE_H;
        boolean doSearch = searchMode && !searchQuery.isEmpty();
        int i = 0;
        for (GuiMessage.Line line : c.lines) {
            if (i >= c.scrollOffset + count) break;
            if (i < c.scrollOffset) { i++; continue; }
            int visIdx = i - c.scrollOffset;
            int lineY  = bottomY - visIdx * LINE_H;
            long key   = lineKey(line);
            MessageAnim anim = HudChatRenderer.anims.computeIfAbsent(key, k -> new MessageAnim(0f));
            anim.setAlphaTarget(1f); anim.tick();
            int tx = textX + (int) anim.slideX();
            if (doSearch && matchesQuery(plainText(line.content()), searchQuery))
                AporiaRenderer.INSTANCE.drawRect(c.x+1, lineY-1, c.w-2, LINE_H, 2, C_SEARCH_HL);
            gfx.drawString(this.font, line.content(), tx, lineY,
                ColorUtil.rgba(255, 255, 255, (int)(255 * anim.alpha())), false);
            i++;
        }
    }

    /** Renders messages for a non-active (background) window without animations. */
    private void renderMessagesPassive(GuiGraphics gfx, WinCfg c, int boxTopY, int textX) {
        List<GuiMessage.Line> visible = visibleLines(c);
        int maxL    = c.maxLines();
        int count   = Math.min(maxL, visible.size());
        int bottomY = boxTopY + maxL * LINE_H - LINE_H;
        for (int i = 0; i < count; i++) {
            int lineY = bottomY - i * LINE_H;
            gfx.drawString(this.font, visible.get(i).content(), textX, lineY, 0xAAFFFFFF, false);
        }
    }

    private void renderScrollBar(GuiGraphics gfx, WinCfg c, int bx, int by, int bw, int bh) {
        int total  = c.lines.size();
        int maxL   = c.maxLines();
        int maxS   = Math.max(0, total - maxL);
        if (maxS <= 0) return;
        int barX   = bx + bw - 3;
        int barH   = bh - BOX_PAD * 2;
        int thumbH = Math.max(10, barH * maxL / Math.max(1, total));
        int thumbY = by + BOX_PAD + (barH - thumbH) * (maxS - c.scrollOffset) / maxS;
        gfx.fill(barX, by+BOX_PAD, barX+2, by+BOX_PAD+barH, C_SCROLL_BAR);
        gfx.fill(barX, thumbY,     barX+2, thumbY+thumbH,    C_SCROLL_THUMB);
    }

    private void renderResizeHints(GuiGraphics gfx, WinCfg c, int bx, int by, int bw, int bh, int mx, int my) {
        Handle h = hitHandle(mx, my, c, bx, by, bw, bh);
        if (h == Handle.NONE) return;
        switch (h) {
            case DRAG      -> gfx.fill(bx, by, bx+bw, by+DRAG_GRIP, C_DRAG_HINT);
            case TOP       -> gfx.fill(bx, by, bx+bw, by+2, C_RESIZE_HINT);
            case LEFT      -> gfx.fill(bx, by, bx+2, by+bh, C_RESIZE_HINT);
            case RIGHT     -> gfx.fill(bx+bw-2, by, bx+bw, by+bh, C_RESIZE_HINT);
            case TOP_LEFT  -> { gfx.fill(bx, by, bx+bw, by+2, C_RESIZE_HINT); gfx.fill(bx, by, bx+2, by+bh, C_RESIZE_HINT); }
            case TOP_RIGHT -> { gfx.fill(bx, by, bx+bw, by+2, C_RESIZE_HINT); gfx.fill(bx+bw-2, by, bx+bw, by+bh, C_RESIZE_HINT); }
            default -> {}
        }
    }

    private void renderFieldEditor(GuiGraphics gfx, AporiaRenderer r, int bx, int by, int bw) {
        int rows   = edit.visibleRows();
        int fi     = editingField - edit.scrollOffset;
        if (fi < 0 || fi >= rows) return;
        int fieldY = by + EditPanel.HDR_H + 1 + EditPanel.PAD/2 + fi * EditPanel.ITEM;
        int ex     = bx + EditPanel.PAD + 100;
        int ew     = bw - EditPanel.PAD*2 - 100;
        r.drawRect(ex-2, fieldY+1, ew+4, EditPanel.ITEM-2, 3, ColorUtil.rgba(30, 30, 70, 240));
        this.fieldBox.setX(ex); this.fieldBox.setY(fieldY+4);
        this.fieldBox.setWidth(ew);
        this.fieldBox.setVisible(true);
        this.fieldBox.render(gfx, 0, 0, 0);
    }


    /**
     * Returns the resize/drag handle at screen position (mx, my) for the given window.
     * Draggable windows return {@link Handle#DRAG} for the full body (excluding resize edges).
     */
    private Handle hitHandle(double mx, double my, WinCfg c, int bx, int by, int bw, int bh) {
        boolean onTop   = my >= by-RESIZE_HIT && my <= by+RESIZE_HIT;
        boolean onLeft  = mx >= bx-RESIZE_HIT && mx <= bx+RESIZE_HIT;
        boolean onRight = mx >= bx+bw-RESIZE_HIT && mx <= bx+bw+RESIZE_HIT;
        boolean inBox   = mx >= bx && mx <= bx+bw && my >= by && my <= by+bh;
        if (onTop && onLeft)  return Handle.TOP_LEFT;
        if (onTop && onRight) return Handle.TOP_RIGHT;
        if (onTop && inBox)   return Handle.TOP;
        if (onLeft && inBox)  return Handle.LEFT;
        if (onRight && inBox) return Handle.RIGHT;
        if (c.draggable && inBox && !onTop && !onLeft && !onRight) return Handle.DRAG;
        return Handle.NONE;
    }

    @Override
    public void mouseMoved(double mx, double my) {
        updateHover(mx, my);
    }

    /** Recomputes the hovered message style from the active window's line list. */
    private void updateHover(double mx, double my) {
        WinCfg c  = cfg();
        int bx = boxX(), by = boxY(), bw = boxW(), bh = boxH();
        if (mx < bx || mx > bx+bw || my < by || my > by+bh) {
            hoveredStyle = null; hoveredLineIdx = -1; return;
        }
        int maxL    = c.maxLines();
        int total   = c.lines.size();
        int count   = Math.min(maxL, total - c.scrollOffset);
        int bottomY = by + BOX_PAD + maxL * LINE_H - LINE_H;
        int i = 0;
        for (GuiMessage.Line line : c.lines) {
            if (i >= c.scrollOffset + count) break;
            if (i < c.scrollOffset) { i++; continue; }
            int visIdx = i - c.scrollOffset;
            int lineY  = bottomY - visIdx * LINE_H;
            if (my >= lineY && my < lineY + LINE_H) {
                hoveredLineIdx = visIdx;
                hoveredStyle   = styleAtX(line.content(), (int)mx - (bx + BOX_PAD));
                return;
            }
            i++;
        }
        hoveredStyle = null; hoveredLineIdx = -1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean b) {
        double mx = e.x(), my = e.y(); int btn = e.button();
        EventBus.INSTANCE.post(new MouseClickEvent(mx, my, btn, MouseClickEvent.Action.PRESS));

        if (editingField >= 0) { commitFieldEdit(); return true; }

        if (edit.visible) {
            int f = edit.fieldAt(mx, my);
            if (f >= 0) {
                if (f == 2) { cfg().prefixEnabled = !cfg().prefixEnabled; return true; }
                if (f == 6) { cfg().showOnlyFilter = !cfg().showOnlyFilter; WinMgr.I.rebuild(this.minecraft.gui.getChat(), this.font); return true; }
                if (f == 7) { cfg().searchOnOpen = !cfg().searchOnOpen; return true; }
                if (f == 8) { cfg().showOnlyServer = !cfg().showOnlyServer; WinMgr.I.rebuild(this.minecraft.gui.getChat(), this.font); return true; }
                startFieldEdit(f);
                return true;
            }
            edit.close();
            return true;
        }

        if (ctx.visible) {
            int item = ctx.itemAt(mx, my, WinMgr.I.wins.size());
            if (item == 0) { ctx.close(); edit.open(); return true; }
            if (item == 1) { ctx.close(); addWindow(); return true; }
            if (item == 2) { ctx.close(); removeWindow(); return true; }
            if (item >= 3) { WinMgr.I.active = item-3; ctx.close(); return true; }
            ctx.close(); return true;
        }

        if (btn == 1) {
            int bx = boxX(), by = boxY(), bw = boxW(), bh = boxH();
            if (mx >= bx && mx <= bx+bw && my >= by && my <= by+bh) {
                ctx.open((int)mx, (int)my, this.width, this.height);
                return true;
            }
        }
        WinCfg c = cfg(); int bx = boxX(), by = boxY(), bw = boxW(), bh = boxH();
        if (btn == 0) {
            int total = c.lines.size();
            int maxL = c.maxLines();
            int maxS = Math.max(0, total - maxL);
            if (maxS > 0) {
                int barX = bx + bw - 3;
                int barH = bh - BOX_PAD * 2;
                int thumbH = Math.max(10, barH * maxL / Math.max(1, total));
                int thumbY = by + BOX_PAD + (barH - thumbH) * (maxS - c.scrollOffset) / maxS;
                if (mx >= barX - 2 && mx <= barX + 4) {
                    int scrollAreaTop = by + BOX_PAD;
                    int scrollAreaBottom = scrollAreaTop + barH;
                    if (my >= scrollAreaTop && my <= scrollAreaBottom) {
                        if (my >= thumbY && my <= thumbY + thumbH) {
                            scrollDragging = true;
                            scrollDragStartY = my;
                            scrollDragStartOffset = c.scrollOffset;
                            return true;
                        }
                        float relY = (float)(my - scrollAreaTop) / (float) barH;
                        int targetOffset = (int) (relY * maxS);
                        c.scrollOffset = Math.max(0, Math.min(targetOffset, maxS));
                        return true;
                    }
                }
            }
        }
        Handle h = hitHandle(mx, my, c, bx, by, bw, bh);
        if (h != Handle.NONE && btn == 0) { dragging = h; return true; }
        if (btn == 0 && hoveredStyle != null) {
            if (hoveredStyle.getClickEvent() != null) {
                defaultHandleGameClickEvent(hoveredStyle.getClickEvent(), this.minecraft, this);
                this.initial = this.input.getValue();
                return true;
            } else if (hoveredStyle.getInsertion() != null && this.minecraft.hasShiftDown()) {
                this.insertText(hoveredStyle.getInsertion(), false);
                return true;
            }
        }
        return super.mouseClicked(e, b);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (dragging != Handle.NONE && e.button() == 0) {
            WinCfg c = cfg(); int minW = 80, minH = LINE_H*2 + BOX_PAD*2;
            boolean widthChanged = false;
            switch (dragging) {
                case DRAG -> {
                    c.x = Math.max(0, Math.min(c.x + (int)dx, this.width - c.w));
                    int newBy = Math.max(0, Math.min(c.bottomY - (int)dy, this.height - c.h));
                    c.bottomY = newBy;
                }
                case TOP       -> { int nh = Math.max(minH, c.h-(int)dy); c.h = nh; }
                case LEFT      -> { int nw = Math.max(minW, c.w-(int)dx); widthChanged = nw!=c.w; c.x += c.w-nw; c.w = nw; }
                case RIGHT     -> { int nw = Math.max(minW, c.w+(int)dx); widthChanged = nw!=c.w; c.w = nw; }
                case TOP_LEFT  -> { int nh = Math.max(minH, c.h-(int)dy); c.h = nh; int nw = Math.max(minW, c.w-(int)dx); widthChanged = nw!=c.w; c.x += c.w-nw; c.w = nw; }
                case TOP_RIGHT -> { int nh = Math.max(minH, c.h-(int)dy); c.h = nh; int nw = Math.max(minW, c.w+(int)dx); widthChanged = nw!=c.w; c.w = nw; }
                default -> {}
            }
            if (widthChanged) WinMgr.I.rebuild(this.minecraft.gui.getChat(), this.font);
            return true;
        }
        if (scrollDragging && e.button() == 0) {
            WinCfg c = cfg();
            int total = c.lines.size();
            int maxL = c.maxLines();
            int maxS = Math.max(0, total - maxL);
            if (maxS > 0) {
                int bx = boxX(), by = boxY(), bw = boxW(), bh = boxH();
                int barH = bh - BOX_PAD * 2;
                float deltaY = (float) ((e.y() - scrollDragStartY) / barH);
                int newOffset = scrollDragStartOffset + (int)(deltaY * maxS);
                c.scrollOffset = Math.max(0, Math.min(newOffset, maxS));
            }
            return true;
        }
        return super.mouseDragged(e, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent e) {
        if (dragging != Handle.NONE) { dragging = Handle.NONE; return true; }
        if (scrollDragging) { scrollDragging = false; return true; }
        return super.mouseReleased(e);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        EventBus.INSTANCE.post(new MouseScrollEvent(mx, my, dx, dy));
        if (edit.onScroll(mx, my, dy)) return true;
        if (commandSuggestions.mouseScrolled(dy)) return true;
        WinCfg c = cfg();
        int maxS = Math.max(0, c.lines.size() - c.maxLines());
        c.scrollOffset = Math.max(0, Math.min(c.scrollOffset + (dy > 0 ? 1 : -1), maxS));
        return true;
    }


    @Override
    public boolean keyPressed(KeyEvent e) {
        EventBus.INSTANCE.post(new KeyInputEvent(e.key(), e.scancode(), e.modifiers(), KeyInputEvent.Action.PRESS));
        if (editingField >= 0) {
            if (e.key() == 257) { commitFieldEdit(); return true; }
            if (e.key() == 256) { cancelFieldEdit(); return true; }
            return fieldBox.keyPressed(e);
        }
        if (edit.visible) { if (e.key() == 256) edit.close(); return true; }
        if (e.key() == 70 && (e.modifiers() & 2) != 0) { toggleSearch(); return true; }
        if (e.key() == 256 && searchMode) { closeSearch(); return true; }
        if (searchMode) return searchBox.keyPressed(e);
        return super.keyPressed(e);
    }

    @Override
    public boolean charTyped(CharacterEvent e) {
        if (editingField >= 0) return fieldBox.charTyped(e);
        if (searchMode)        return searchBox.charTyped(e);
        return super.charTyped(e);
    }

    /**
     * Applies prefix and suffix from the active window config before sending the message.
     * Prefix rules:
     * <ul>
     *   <li>Never applied to {@code /commands}.</li>
     *   <li>If {@code prefixTriggers} is non-empty and the message starts with a trigger char,
     *       the prefix is inserted after that char: {@code triggerChar + prefix + " " + rest}.</li>
     *   <li>Otherwise the prefix is prepended: {@code prefix + " " + msg}.</li>
     * </ul>
     * Raw text is saved to history before decoration.
     */
    @Override
    public void handleChatInput(String msg, boolean addToHistory) {
        WinCfg c = cfg();
        if (addToHistory) this.minecraft.gui.getChat().addRecentChat(msg);
        boolean isCommand = !msg.isEmpty() && msg.charAt(0) == '/';
        if (!isCommand && c.prefixEnabled && !c.msgPrefix.isEmpty()) {
            if (!c.prefixTriggers.isEmpty() && c.prefixTriggers.indexOf(msg.charAt(0)) >= 0) {
                String rest = msg.length() > 1 ? msg.substring(1) : "";
                msg = msg.charAt(0) + c.msgPrefix + (rest.isEmpty() ? "" : " " + rest);
            } else {
                msg = c.msgPrefix + " " + msg;
            }
        }
        if (!c.msgSuffix.isEmpty()) msg = msg + " " + c.msgSuffix;
        super.handleChatInput(msg, false);
    }

    private void addWindow() {
        WinMgr.I.add("Window " + (WinMgr.I.wins.size() + 1));
        resolvePosition(WinMgr.I.get(), WinMgr.I.active);
        trySave();
    }

    private void removeWindow() {
        WinMgr.I.remove(WinMgr.I.active);
        trySave();
    }

    private void startFieldEdit(int field) {
        editingField = field;
        WinCfg c = cfg();
        String cur = switch (field) {
            case 0 -> c.name;
            case 1 -> c.msgPrefix;
            case 3 -> c.prefixTriggers;
            case 4 -> c.msgSuffix;
            case 5 -> c.filterWords;
            case 9 -> String.format("#%06X", c.selfColor & 0xFFFFFF);
            default -> "";
        };
        fieldBox.setValue(cur); fieldBox.setFocused(true); this.setFocused(fieldBox);
    }

    private void commitFieldEdit() {
        String v = fieldBox.getValue().trim(); WinCfg c = cfg();
        switch (editingField) {
            case 0 -> c.name = v;
            case 1 -> c.msgPrefix = v;
            case 3 -> c.prefixTriggers = v;
            case 4 -> c.msgSuffix = v;
            case 5 -> { c.filterWords = v; WinMgr.I.rebuild(this.minecraft.gui.getChat(), this.font); }
            case 9 -> { try { c.selfColor = (int)(Long.parseLong(v.replace("#",""), 16)) | 0xFF000000; } catch (Exception ignored) {} }
        }
        cancelFieldEdit();
        trySave();
    }

    private void cancelFieldEdit() {
        editingField = -1; fieldBox.setVisible(false); fieldBox.setFocused(false); this.setFocused(this.input);
    }

    private void toggleSearch() { searchMode = !searchMode; if (searchMode) openSearch(); else closeSearch(); }

    private void openSearch() {
        searchMode = true; searchQuery = ""; searchBox.setValue("");
        searchBox.setFocused(true); this.input.setFocused(false); this.setFocused(searchBox);
    }

    private void closeSearch() {
        searchMode = false; searchQuery = ""; searchBox.setVisible(false);
        searchBox.setFocused(false); this.input.setFocused(true); this.setFocused(this.input);
    }

    private static boolean matchesQuery(String text, String query) {
        if (query.isEmpty()) return false;
        String[] v = KeyboardLayout.both(query.toLowerCase()); String t = text.toLowerCase();
        return t.contains(v[0]) || t.contains(v[1]);
    }

    /** Extracts plain text from a {@link net.minecraft.util.FormattedCharSequence} (strips formatting). */
    static String plainText(net.minecraft.util.FormattedCharSequence seq) {
        StringBuilder sb = new StringBuilder();
        seq.accept((i, style, cp) -> { sb.appendCodePoint(cp); return true; });
        return sb.toString();
    }

    /** Stable key for a message line used to track animation state across frames. */
    static long lineKey(GuiMessage.Line line) {
        return ((long) line.addedTime() << 32) | (line.content().hashCode() & 0xFFFFFFFFL);
    }

    @Override public void renderBackground(GuiGraphics gfx, int mx, int my, float d) {}
    @Override public boolean isPauseScreen()     { return false; }
    @Override public boolean isAllowedInPortal() { return true; }

    @Override
    public void onClose() {
        trySave();
        super.onClose();
    }

    private static void trySave() {
        try {
            ChatFile.save();
        } catch (Exception e) {
            Logger.error("ChatFile save failed: " + e.getMessage());
        }
    }
}
