package so.aporia.render.ui.chat;

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
import so.aporia.render.animation.MessageAnim;
import so.aporia.render.color.ColorUtil;
import so.aporia.render.core.AporiaRenderer;
import so.aporia.utils.KeyboardLayout;
import so.aporia.utils.events.EventBus;
import so.aporia.utils.events.impl.KeyInputEvent;
import so.aporia.utils.events.impl.MouseClickEvent;
import so.aporia.utils.events.impl.MouseScrollEvent;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class AporiaChatScreen extends ChatScreen {

    /* ============================================================ */
    /* Layout constants                                              */
    /* ============================================================ */
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

    /* Colors */
    private static final int C_INPUT_BG     = ColorUtil.rgba(16,  16,  32,  200);
    private static final int C_CHAT_BG      = ColorUtil.rgba(0,   0,   0,   150);
    private static final int C_HINT         = ColorUtil.rgba(120, 120, 120, 255);
    private static final int C_SEARCH_BG    = ColorUtil.rgba(20,  20,  50,  210);
    private static final int C_SEARCH_HL    = ColorUtil.rgba(255, 200, 50,  180);
    private static final int C_SCROLL_BAR   = ColorUtil.rgba(255, 255, 255,  60);
    private static final int C_SCROLL_THUMB = ColorUtil.rgba(255, 255, 255, 140);
    private static final int C_RESIZE_HINT  = ColorUtil.rgba(255, 255, 255,  30);
    private static final int C_DRAG_HINT    = ColorUtil.rgba(255, 255, 255,  15);

    /* ============================================================ */
    /* WinCfg — per-window config                                   */
    /* ============================================================ */
    public static final class WinCfg {
        public String  name;
        public int     x, bottomY, w, h;
        public boolean draggable      = false;
        public boolean searchOnOpen   = false;
        public boolean showOnlyFilter = false;
        public String  filterWords    = "";
        public String  msgPrefix      = "";
        public String  msgSuffix      = "";
        public int     selfColor      = 0xFFADD8E6;
        public int     scrollOffset   = 0;

        /* Own message store — filled by WinMgr.route() */
        public final java.util.ArrayDeque<GuiMessage.Line> lines = new java.util.ArrayDeque<>(100);
        private static final int MAX_STORED = 200;

        WinCfg(String name, int x, int bottomY, int w, int h, boolean draggable) {
            this.name=name; this.x=x; this.bottomY=bottomY; this.w=w; this.h=h; this.draggable=draggable;
        }
        int maxLines() { return Math.max(1, (h - BOX_PAD*2) / LINE_H); }

        void addLine(GuiMessage.Line l) {
            lines.addFirst(l);
            while (lines.size() > MAX_STORED) lines.removeLast();
        }
        public void clear() { lines.clear(); scrollOffset = 0; }
    }

    /* ============================================================ */
    /* WinMgr — window list                                         */
    /* ============================================================ */
    public static final class WinMgr {
        public static final WinMgr I = new WinMgr();
        public final List<WinCfg> wins = new ArrayList<>();
        int active = 0;
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
         * Routes a new GuiMessage to the correct windows.
         * Called from ChatComponent.addMessage — before trimmedMessages is updated.
         * Each window gets its own split lines based on its own width.
         * A filtered window claims messages matching its keywords;
         * Main (and other unfiltered windows) get everything NOT claimed by any filter.
         */
        public void route(GuiMessage msg, net.minecraft.client.gui.Font font) {
            String full = msg.content().getString().toLowerCase();

            /* Determine which filtered windows claim this message */
            List<WinCfg> claimants = new ArrayList<>();
            for (WinCfg w : wins) {
                if (!w.showOnlyFilter || w.filterWords.isEmpty()) continue;
                for (String kw : w.filterWords.split(",")) {
                    if (matchesKw(full, kw.trim())) { claimants.add(w); break; }
                }
            }

            for (WinCfg w : wins) {
                boolean isFiltered = w.showOnlyFilter && !w.filterWords.isEmpty();
                boolean claimed = claimants.contains(w);
                /* Filtered window: only if it claimed the message */
                if (isFiltered && !claimed) continue;
                /* Unfiltered window: skip if any other window claimed it */
                if (!isFiltered && !claimants.isEmpty()) continue;

                int splitW = Math.max(10, w.w - BOX_PAD*2 - TEXT_PAD);
                List<net.minecraft.util.FormattedCharSequence> parts = msg.splitLines(font, splitW);
                /*
                 * Visual order: parts.get(0) = top line, parts.get(last) = bottom line.
                 * c.lines index-0 = newest displayed line = bottom visual line.
                 * So we addFirst bottom→top: iterate forward, each addFirst pushes previous down.
                 * Result: lines[0]=bottom(last part), lines[1]=second-to-last, etc.
                 */
                for (int i = 0; i < parts.size(); i++) {
                    w.addLine(new GuiMessage.Line(msg.addedTime(), parts.get(i), msg.tag(), i == parts.size()-1));
                }
            }
        }

        /** Rebuild all window line stores from allMessages (e.g. after rescale or clear). */
        public void rebuild(net.minecraft.client.gui.components.ChatComponent chat,
                            net.minecraft.client.gui.Font font) {
            for (WinCfg w : wins) w.clear();
            List<GuiMessage> all = chat.getAllMessages();
            /* allMessages is newest-first; rebuild in reverse so route adds oldest first */
            for (int i = all.size()-1; i >= 0; i--) route(all.get(i), font);
        }

        private static boolean matchesKw(String text, String kw) {
            if (kw.isEmpty()) return false;
            String[] v = so.aporia.utils.KeyboardLayout.both(kw.toLowerCase());
            return text.contains(v[0]) || text.contains(v[1]);
        }

        /** Spawn positions: 0=bottom-left, 1=bottom-right, 2=top-left, 3=top-right, rest=center */
        private static WinCfg makeWin(String name, int idx) {
            int defW = 320, defH = MAX_LINES*LINE_H + BOX_PAD*2;
            boolean draggable = idx > 0;
            return switch (idx) {
                case 0 -> new WinCfg(name, LEFT, BOTTOM_PAD+INPUT_H+GAP, defW, defH, false);
                default -> new WinCfg(name, -1, -1, defW, defH, true);
            };
        }
    }

    /* ============================================================ */
    /* CtxMenu                                                       */
    /* ============================================================ */
    private static final class CtxMenu {
        boolean visible = false;
        int renderX, renderY;

        private static final int W    = 160;
        private static final int ITEM = 14;
        private static final int PAD  = 6;
        private static final int C_BG   = ColorUtil.rgba(18,18,30,235);
        private static final int C_ITEM = ColorUtil.rgba(255,255,255,200);
        private static final int C_HOV  = ColorUtil.rgba(80,80,160,180);
        private static final int C_SEP  = ColorUtil.rgba(255,255,255,40);

        void open(int x, int y, int screenW, int screenH) {
            visible=true;
            int winCount = WinMgr.I.wins.size();
            int totalH = menuHeight(winCount);
            renderX = (x + W > screenW) ? x - W : x;
            renderY = (y + totalH > screenH) ? y - totalH : y;
        }
        void close() { visible=false; }

        private int menuHeight(int winCount) {
            return PAD*2 + 3*ITEM + (winCount>0 ? 4+winCount*ITEM : 0);
        }

        int itemAt(double px, double py, int winCount) {
            if (!visible) return -1;
            int totalH = menuHeight(winCount);
            if (px<renderX||px>renderX+W||py<renderY||py>renderY+totalH) return -1;
            int rel = (int)(py - renderY - PAD);
            int item = rel / ITEM;
            return Math.max(0, Math.min(2 + winCount, item));
        }

        void render(GuiGraphics gfx, net.minecraft.client.gui.Font font, int winCount) {
            if (!visible) return;
            int totalH = menuHeight(winCount);
            AporiaRenderer.INSTANCE.drawRect(renderX, renderY, W, totalH, 5, C_BG);
            String[] labels = {"§fEdit", "§aAdd window", "§cDelete window"};
            for (int i=0; i<3; i++)
                gfx.drawString(font, labels[i], renderX+PAD, renderY+PAD+i*ITEM+3, C_ITEM, false);
            if (winCount > 0) {
                int sy = renderY+PAD+3*ITEM;
                gfx.fill(renderX+PAD, sy, renderX+W-PAD, sy+1, C_SEP);
                for (int i=0; i<winCount; i++) {
                    int iy = sy+4+i*ITEM;
                    if (i==WinMgr.I.active) gfx.fill(renderX+2,iy,renderX+W-2,iy+ITEM,C_HOV);
                    gfx.drawString(font, WinMgr.I.wins.get(i).name, renderX+PAD, iy+3, C_ITEM, false);
                }
            }
        }
    }

    /* ============================================================ */
    /* EditPanel — renders IN-PLACE over the chat box               */
    /* ============================================================ */
    private static final class EditPanel {
        boolean visible = false;
        int bx, by, bw, bh;

        static final int ITEM    = 14;
        static final int PAD     = 6;
        static final int FIELDS  = 7;
        /* Height needed to show all fields */
        static int panelHeight() { return ITEM + PAD*2 + FIELDS * ITEM + PAD; }

        private static final int C_BG  = ColorUtil.rgba(10, 10, 24, 250);
        private static final int C_HDR = ColorUtil.rgba(40, 40, 80, 255);
        private static final int C_LBL = ColorUtil.rgba(150, 150, 200, 255);
        private static final int C_VAL = ColorUtil.rgba(255, 255, 255, 255);
        private static final int C_ROW = ColorUtil.rgba(255, 255, 255, 8);
        private static final int C_SEP = ColorUtil.rgba(255, 255, 255, 25);

        void open()  { visible = true; }
        void close() { visible = false; }

        void setBox(int x, int y, int w, int h) { bx=x; by=y; bw=w; bh=h; }

        void render(GuiGraphics gfx, net.minecraft.client.gui.Font font, WinCfg c) {
            if (!visible) return;
            AporiaRenderer r = AporiaRenderer.INSTANCE;
            /* Always render with enough height for all fields, anchored to window top */
            int ph = panelHeight();
            r.drawRect(bx, by, bw, ph, RADIUS, C_BG);
            r.drawRect(bx, by, bw, ITEM+PAD, RADIUS, C_HDR);
            gfx.drawString(font, "§fSettings — §7"+c.name, bx+PAD, by+PAD/2+2, C_VAL, false);
            String esc = "§7[Esc]";
            gfx.drawString(font, esc, bx+bw-font.width(esc)-PAD, by+PAD/2+2, C_VAL, false);
            int y = by + ITEM + PAD*2;
            gfx.fill(bx+PAD, y-2, bx+bw-PAD, y-1, C_SEP);
            row(gfx, font, 0, y, "Name",           c.name);           y+=ITEM;
            row(gfx, font, 1, y, "Prefix",          c.msgPrefix);      y+=ITEM;
            row(gfx, font, 2, y, "Suffix",          c.msgSuffix);      y+=ITEM;
            row(gfx, font, 3, y, "Filter words",    c.filterWords);    y+=ITEM;
            row(gfx, font, 4, y, "Filter only",     c.showOnlyFilter ? "§aON" : "§cOFF"); y+=ITEM;
            row(gfx, font, 5, y, "Search on open",  c.searchOnOpen   ? "§aON" : "§cOFF"); y+=ITEM;
            row(gfx, font, 6, y, "Self color",      String.format("#%06X", c.selfColor & 0xFFFFFF));
        }

        private void row(GuiGraphics gfx, net.minecraft.client.gui.Font font, int idx, int y, String lbl, String val) {
            if (idx % 2 == 0) gfx.fill(bx+2, y, bx+bw-2, y+ITEM, C_ROW);
            gfx.drawString(font, "§7"+lbl, bx+PAD, y+3, C_LBL, false);
            gfx.drawString(font, val,       bx+PAD+95, y+3, C_VAL, false);
        }

        /** Returns field index at screen pos, -1 if none. Fields 4 and 5 are boolean toggles. */
        int fieldAt(double px, double py) {
            if (!visible) return -1;
            int ph = panelHeight();
            if (px<bx||px>bx+bw||py<by||py>by+ph) return -1;
            int headerH = ITEM+PAD*2;
            int rel = (int)(py - by - headerH);
            if (rel < 0) return -1;
            int f = rel / ITEM;
            return (f >= 0 && f < FIELDS) ? f : -1;
        }
    }

    /* ============================================================ */
    /* Resize/drag enum                                              */
    /* ============================================================ */
    private enum Handle { NONE, DRAG, TOP, LEFT, RIGHT, TOP_LEFT, TOP_RIGHT }

    /* ============================================================ */
    /* Instance state                                                */
    /* ============================================================ */
    private boolean searchMode  = false;
    private String  searchQuery = "";
    private EditBox searchBox;
    private final CtxMenu   ctx  = new CtxMenu();
    private final EditPanel edit = new EditPanel();
    private int     editingField = -1;
    private EditBox fieldBox;
    private Handle  dragging = Handle.NONE;

    public AporiaChatScreen(String initial, boolean isDraft) { super(initial, isDraft); }

    /* ============================================================ */
    /* Helpers                                                       */
    /* ============================================================ */
    private WinCfg cfg() { return WinMgr.I.get(); }

    private int barWidth() {
        String v = this.input!=null ? this.input.getValue() : "";
        return Math.min(Math.max(this.font.width(v)+TEXT_PAD*2, MIN_W), cfg().w);
    }

    /** Returns the routed lines for window c (already filtered by WinMgr.route). */
    private static List<GuiMessage.Line> visibleLines(WinCfg c) {
        return new ArrayList<>(c.lines);
    }

    private void resolvePosition(WinCfg c, int idx) {
        if (c.x != -1) return;
        int defW=c.w, defH=c.h;
        switch (idx % 4) {
            case 1 -> { c.x=this.width-defW-LEFT; c.bottomY=BOTTOM_PAD+INPUT_H+GAP; }
            case 2 -> { c.x=LEFT;                 c.bottomY=this.height-defH-GAP*2; }
            case 3 -> { c.x=this.width-defW-LEFT; c.bottomY=this.height-defH-GAP*2; }
            default-> { c.x=this.width/2-defW/2;  c.bottomY=this.height/2-defH/2; }
        }
    }

    private int boxX()  { WinCfg c=cfg(); resolvePosition(c, WinMgr.I.active); return c.x; }
    private int boxY()  { WinCfg c=cfg(); return this.height-c.bottomY-c.h; }
    private int boxW()  { return cfg().w; }
    private int boxH()  { return cfg().h; }
    private int inputY(){ return this.height-BOTTOM_PAD-INPUT_H; }

    /* ============================================================ */
    /* Init                                                          */
    /* ============================================================ */
    @Override
    protected void init() {
        this.historyPos = this.minecraft.gui.getChat().getRecentChat().size();
        for (int i=1; i<WinMgr.I.wins.size(); i++) resolvePosition(WinMgr.I.wins.get(i), i);

        WinCfg c = cfg();
        int iy = inputY();
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
        this.searchBox.setResponder(s -> searchQuery=s);
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

        /* Populate window lines from existing chat history */
        WinMgr.I.rebuild(this.minecraft.gui.getChat(), this.font);
    }

    /* ============================================================ */
    /* Render                                                        */
    /* ============================================================ */
    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float delta) {
        AporiaRenderer r = AporiaRenderer.INSTANCE;

        /* 1. Render all non-active windows first */
        int active = WinMgr.I.active;
        for (int i=0; i<WinMgr.I.wins.size(); i++) {
            if (i != active) renderWindowContent(gfx, r, WinMgr.I.wins.get(i), mouseX, mouseY, false);
        }
        /* 2. Render active window content (no overlays yet) */
        renderWindowContent(gfx, r, cfg(), mouseX, mouseY, true);

        /* 3. Input bar */
        int bx=boxX(), iy=inputY(), bW=barWidth();
        r.drawRect(bx, iy, bW, INPUT_H, RADIUS, C_INPUT_BG);
        gfx.enableScissor(bx, iy, bx+bW, iy+INPUT_H);
        this.input.render(gfx, mouseX, mouseY, delta);
        gfx.disableScissor();
        if (this.input.getValue().isEmpty() && !searchMode)
            gfx.drawString(this.font, "Message...", bx+TEXT_PAD, iy+(INPUT_H-8)/2, C_HINT, false);

        this.commandSuggestions.render(gfx, mouseX, mouseY);

        /* 4. Overlays ALWAYS last — on top of everything */
        if (edit.visible) {
            edit.render(gfx, this.font, cfg());
            if (editingField >= 0) renderFieldEditor(gfx, r, edit.bx, edit.by, edit.bw);
        }
        ctx.render(gfx, this.font, WinMgr.I.wins.size());
    }

    /** Renders only the chat box content (background + messages + scrollbar + search + resize hints). No overlays. */
    private void renderWindowContent(GuiGraphics gfx, AporiaRenderer r, WinCfg c, int mouseX, int mouseY, boolean isActive) {
        resolvePosition(c, WinMgr.I.wins.indexOf(c));
        int bx=c.x, by=this.height-c.bottomY-c.h, bw=c.w, bh=c.h;

        /* Keep edit panel coords in sync with active window */
        if (isActive) edit.setBox(bx, by, bw, bh);

        /* If edit panel open — skip message rendering for active window */
        if (isActive && edit.visible) {
            renderResizeHints(gfx, c, bx, by, bw, bh, mouseX, mouseY);
            return;
        }

        r.drawRect(bx, by, bw, bh, RADIUS, C_CHAT_BG);
        gfx.enableScissor(bx, by, bx+bw, by+bh);
        if (isActive) renderMessages(gfx, c, by+BOX_PAD, bx+BOX_PAD);
        else          renderMessagesPassive(gfx, c, by+BOX_PAD, bx+BOX_PAD);
        gfx.disableScissor();

        renderScrollBar(gfx, c, bx, by, bw, bh);
        renderResizeHints(gfx, c, bx, by, bw, bh, mouseX, mouseY);

        if (isActive && searchMode) {
            int sy = by-GAP-INPUT_H;
            r.drawRect(bx, sy, Math.min(bw,200), INPUT_H, RADIUS, C_SEARCH_BG);
            if (searchQuery.isEmpty())
                gfx.drawString(this.font, "§7Search...", bx+TEXT_PAD, sy+(INPUT_H-8)/2, 0xFFFFFFFF, false);
            this.searchBox.setX(bx+TEXT_PAD);
            this.searchBox.setY(sy+(INPUT_H-9)/2);
            this.searchBox.setWidth(Math.min(bw,200)-TEXT_PAD*2);
            this.searchBox.setVisible(true);
            this.searchBox.render(gfx, 0, 0, 0);
        }
    }

    private void renderMessages(GuiGraphics gfx, WinCfg c, int boxTopY, int textX) {
        List<GuiMessage.Line> visible = visibleLines(c);
        int total = visible.size();
        int maxL  = c.maxLines();
        int maxS  = Math.max(0, total - maxL);
        c.scrollOffset = Math.max(0, Math.min(c.scrollOffset, maxS));

        int count   = Math.min(maxL, total - c.scrollOffset);
        int bottomY = boxTopY + maxL * LINE_H - LINE_H;

        for (int i = 0; i < count; i++) {
            GuiMessage.Line line = visible.get(c.scrollOffset + i);
            String pt = plainText(line.content());
            boolean matched = searchMode && !searchQuery.isEmpty() && matchesQuery(pt, searchQuery);
            long key = lineKey(line);
            MessageAnim anim = HudChatRenderer.anims.computeIfAbsent(key, k -> new MessageAnim(0f));
            anim.setAlphaTarget(1f); anim.tick();
            int lineY = bottomY - i * LINE_H;
            int tx    = textX + (int) anim.slideX();
            if (matched) AporiaRenderer.INSTANCE.drawRect(c.x+1, lineY-1, c.w-2, LINE_H, 2, C_SEARCH_HL);
            gfx.drawString(this.font, line.content(), tx, lineY,
                ColorUtil.rgba(255, 255, 255, (int)(255 * anim.alpha())), false);
        }
    }

    /** Passive window: render latest lines with routing filter applied. */
    private void renderMessagesPassive(GuiGraphics gfx, WinCfg c, int boxTopY, int textX) {
        List<GuiMessage.Line> visible = visibleLines(c);
        int maxL  = c.maxLines();
        int count = Math.min(maxL, visible.size());
        int bottomY = boxTopY + maxL * LINE_H - LINE_H;
        for (int i = 0; i < count; i++) {
            int lineY = bottomY - i * LINE_H;
            gfx.drawString(this.font, visible.get(i).content(), textX, lineY, 0xAAFFFFFF, false);
        }
    }

    private void renderScrollBar(GuiGraphics gfx, WinCfg c, int bx, int by, int bw, int bh) {
        int total = c.lines.size();
        int maxL  = c.maxLines();
        int maxS  = Math.max(0, total - maxL);
        if (maxS <= 0) return;
        int barX   = bx + bw - 3;
        int barH   = bh - BOX_PAD * 2;
        int thumbH = Math.max(10, barH * maxL / Math.max(1, total));
        int thumbY = by + BOX_PAD + (barH - thumbH) * (maxS - c.scrollOffset) / maxS;
        gfx.fill(barX, by+BOX_PAD, barX+2, by+BOX_PAD+barH, C_SCROLL_BAR);
        gfx.fill(barX, thumbY,     barX+2, thumbY+thumbH,    C_SCROLL_THUMB);
    }

    private void renderResizeHints(GuiGraphics gfx, WinCfg c, int bx, int by, int bw, int bh, int mx, int my) {
        Handle h=hitHandle(mx,my,c,bx,by,bw,bh);
        if (h==Handle.NONE) return;
        switch(h) {
            case DRAG      -> gfx.fill(bx,by,bx+bw,by+DRAG_GRIP,C_DRAG_HINT);
            case TOP       -> gfx.fill(bx,by,bx+bw,by+2,C_RESIZE_HINT);
            case LEFT      -> gfx.fill(bx,by,bx+2,by+bh,C_RESIZE_HINT);
            case RIGHT     -> gfx.fill(bx+bw-2,by,bx+bw,by+bh,C_RESIZE_HINT);
            case TOP_LEFT  -> { gfx.fill(bx,by,bx+bw,by+2,C_RESIZE_HINT); gfx.fill(bx,by,bx+2,by+bh,C_RESIZE_HINT); }
            case TOP_RIGHT -> { gfx.fill(bx,by,bx+bw,by+2,C_RESIZE_HINT); gfx.fill(bx+bw-2,by,bx+bw,by+bh,C_RESIZE_HINT); }
            default -> {}
        }
    }

    private void renderFieldEditor(GuiGraphics gfx, AporiaRenderer r, int bx, int by, int bw) {
        int headerH = EditPanel.ITEM + EditPanel.PAD*2;
        int fieldY  = by + headerH + editingField * EditPanel.ITEM;
        int ex = bx + EditPanel.PAD + 95;
        int ew = bw - EditPanel.PAD*2 - 95;
        r.drawRect(ex-2, fieldY+1, ew+4, EditPanel.ITEM-2, 3, ColorUtil.rgba(30,30,70,240));
        this.fieldBox.setX(ex); this.fieldBox.setY(fieldY+3);
        this.fieldBox.setWidth(ew);
        this.fieldBox.setVisible(true);
        this.fieldBox.render(gfx, 0, 0, 0);
    }

    /* ============================================================ */
    /* Hit-test                                                      */
    /* ============================================================ */
    private Handle hitHandle(double mx, double my, WinCfg c, int bx, int by, int bw, int bh) {
        boolean onTop   = my>=by-RESIZE_HIT && my<=by+RESIZE_HIT;
        boolean onLeft  = mx>=bx-RESIZE_HIT && mx<=bx+RESIZE_HIT;
        boolean onRight = mx>=bx+bw-RESIZE_HIT && mx<=bx+bw+RESIZE_HIT;
        boolean inBox   = mx>=bx && mx<=bx+bw && my>=by && my<=by+bh;
        if (onTop && onLeft)  return Handle.TOP_LEFT;
        if (onTop && onRight) return Handle.TOP_RIGHT;
        if (onTop && inBox)   return Handle.TOP;
        if (onLeft && inBox)  return Handle.LEFT;
        if (onRight && inBox) return Handle.RIGHT;
        if (c.draggable && inBox && my<=by+DRAG_GRIP) return Handle.DRAG;
        return Handle.NONE;
    }

    /* ============================================================ */
    /* Mouse                                                         */
    /* ============================================================ */
    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean b) {
        double mx=e.x(), my=e.y(); int btn=e.button();
        EventBus.INSTANCE.post(new MouseClickEvent(mx,my,btn,MouseClickEvent.Action.PRESS));

        /* Editing a text field — commit on click outside */
        if (editingField>=0) { commitFieldEdit(); return true; }

        /* Edit panel is open */
        if (edit.visible) {
            int f=edit.fieldAt(mx,my);
            if (f>=0) {
                /* FIX: fields 4 and 5 are boolean toggles — no EditBox */
                if (f==4) {
                    cfg().showOnlyFilter=!cfg().showOnlyFilter;
                    WinMgr.I.rebuild(this.minecraft.gui.getChat(), this.font);
                    return true;
                }
                if (f==5) { cfg().searchOnOpen  =!cfg().searchOnOpen;   return true; }
                startFieldEdit(f);
                return true;
            }
            edit.close();
            return true;
        }

        /* Context menu is open */
        if (ctx.visible) {
            int item=ctx.itemAt(mx,my,WinMgr.I.wins.size());
            if (item==0) { ctx.close(); edit.open(); return true; }
            if (item==1) { ctx.close(); addWindow(); return true; }
            if (item==2) { ctx.close(); removeWindow(); return true; }
            if (item>=3) { WinMgr.I.active=item-3; ctx.close(); return true; }
            ctx.close(); return true;
        }

        /* RMB inside chat box → open context menu */
        if (btn==1) {
            int bx=boxX(),by=boxY(),bw=boxW(),bh=boxH();
            if (mx>=bx&&mx<=bx+bw&&my>=by&&my<=by+bh) {
                ctx.open((int)mx,(int)my,this.width,this.height);
                return true;
            }
        }

        /* Resize / drag handles */
        WinCfg c=cfg(); int bx=boxX(),by=boxY(),bw=boxW(),bh=boxH();
        Handle h=hitHandle(mx,my,c,bx,by,bw,bh);
        if (h!=Handle.NONE && btn==0) { dragging=h; return true; }

        return super.mouseClicked(e,b);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (dragging!=Handle.NONE && e.button()==0) {
            WinCfg c=cfg(); int minW=80, minH=LINE_H*2+BOX_PAD*2;
            boolean widthChanged = false;
            switch(dragging) {
                /* DRAG: move the window — x moves with mouse, bottomY is distance from screen bottom */
                case DRAG -> { c.x+=(int)dx; c.bottomY-=(int)dy; }
                /* TOP: resize height only — bottom edge stays fixed, top edge moves with mouse */
                case TOP  -> { int nh=Math.max(minH, c.h-(int)dy); c.h=nh; }
                /* LEFT: resize width — right edge stays, left edge moves */
                case LEFT -> { int nw=Math.max(minW,c.w-(int)dx); widthChanged=nw!=c.w; c.x+=c.w-nw; c.w=nw; }
                /* RIGHT: resize width — left edge stays, right edge moves */
                case RIGHT -> { int nw=Math.max(minW,c.w+(int)dx); widthChanged=nw!=c.w; c.w=nw; }
                /* Corners: combine top + left/right */
                case TOP_LEFT  -> { int nh=Math.max(minH,c.h-(int)dy); c.h=nh; int nw=Math.max(minW,c.w-(int)dx); widthChanged=nw!=c.w; c.x+=c.w-nw; c.w=nw; }
                case TOP_RIGHT -> { int nh=Math.max(minH,c.h-(int)dy); c.h=nh; int nw=Math.max(minW,c.w+(int)dx); widthChanged=nw!=c.w; c.w=nw; }
                default -> {}
            }
            if (widthChanged) WinMgr.I.rebuild(this.minecraft.gui.getChat(), this.font);
            return true;
        }
        return super.mouseDragged(e,dx,dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent e) {
        if (dragging!=Handle.NONE) { dragging=Handle.NONE; return true; }
        return super.mouseReleased(e);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        EventBus.INSTANCE.post(new MouseScrollEvent(mx,my,dx,dy));
        if (commandSuggestions.mouseScrolled(dy)) return true;
        WinCfg c = cfg();
        int maxS = Math.max(0, c.lines.size() - c.maxLines());
        c.scrollOffset = Math.max(0, Math.min(c.scrollOffset + (dy > 0 ? 1 : -1), maxS));
        return true;
    }

    /* ============================================================ */
    /* Keyboard                                                      */
    /* ============================================================ */
    @Override
    public boolean keyPressed(KeyEvent e) {
        EventBus.INSTANCE.post(new KeyInputEvent(e.key(),e.scancode(),e.modifiers(),KeyInputEvent.Action.PRESS));
        if (editingField>=0) {
            if (e.key()==257) { commitFieldEdit(); return true; }
            if (e.key()==256) { cancelFieldEdit(); return true; }
            return fieldBox.keyPressed(e);
        }
        if (edit.visible) { if (e.key()==256) edit.close(); return true; }
        if (e.key()==70 && (e.modifiers()&2)!=0) { toggleSearch(); return true; }
        if (e.key()==256 && searchMode) { closeSearch(); return true; }
        if (searchMode) return searchBox.keyPressed(e);
        return super.keyPressed(e);
    }

    @Override
    public boolean charTyped(CharacterEvent e) {
        if (editingField>=0) return fieldBox.charTyped(e);
        if (searchMode)      return searchBox.charTyped(e);
        return super.charTyped(e);
    }

    /* ============================================================ */
    /* handleChatInput — apply prefix/suffix from active window     */
    /* ============================================================ */
    @Override
    public void handleChatInput(String msg, boolean addToHistory) {
        WinCfg c = cfg();
        /* Prepend prefix and append suffix if set */
        if (!c.msgPrefix.isEmpty()) msg = c.msgPrefix + msg;
        if (!c.msgSuffix.isEmpty()) msg = msg + c.msgSuffix;
        super.handleChatInput(msg, addToHistory);
    }

    /* ============================================================ */
    /* Window management                                             */
    /* ============================================================ */
    private void addWindow() {
        WinMgr.I.add("Window "+(WinMgr.I.wins.size()+1));
        resolvePosition(WinMgr.I.get(), WinMgr.I.active);
    }
    private void removeWindow() { WinMgr.I.remove(WinMgr.I.active); }

    /* ============================================================ */
    /* Field editing                                                 */
    /* ============================================================ */
    private void startFieldEdit(int field) {
        editingField=field;
        WinCfg c=cfg();
        String cur=switch(field){
            case 0->c.name; case 1->c.msgPrefix; case 2->c.msgSuffix; case 3->c.filterWords;
            case 6->String.format("#%06X",c.selfColor&0xFFFFFF); default->"";
        };
        fieldBox.setValue(cur); fieldBox.setFocused(true); this.setFocused(fieldBox);
    }

    private void commitFieldEdit() {
        String v=fieldBox.getValue().trim(); WinCfg c=cfg();
        switch(editingField){
            case 0->c.name=v;
            case 1->c.msgPrefix=v;
            case 2->c.msgSuffix=v;
            case 3->{ c.filterWords=v; WinMgr.I.rebuild(this.minecraft.gui.getChat(), this.font); }
            case 6->{ try{c.selfColor=(int)(Long.parseLong(v.replace("#",""),16))|0xFF000000;}catch(Exception ignored){} }
        }
        cancelFieldEdit();
    }

    private void cancelFieldEdit() {
        editingField=-1; fieldBox.setVisible(false); fieldBox.setFocused(false); this.setFocused(this.input);
    }

    /* ============================================================ */
    /* Search                                                        */
    /* ============================================================ */
    private void toggleSearch() { searchMode=!searchMode; if(searchMode) openSearch(); else closeSearch(); }
    private void openSearch() {
        searchMode=true; searchQuery=""; searchBox.setValue("");
        searchBox.setFocused(true); this.input.setFocused(false); this.setFocused(searchBox);
    }
    private void closeSearch() {
        searchMode=false; searchQuery=""; searchBox.setVisible(false);
        searchBox.setFocused(false); this.input.setFocused(true); this.setFocused(this.input);
    }

    /* ============================================================ */
    /* Utils                                                         */
    /* ============================================================ */
    private static boolean matchesQuery(String text, String query) {
        if (query.isEmpty()) return false;
        String[] v=KeyboardLayout.both(query.toLowerCase()); String t=text.toLowerCase();
        return t.contains(v[0])||t.contains(v[1]);
    }

    static String plainText(net.minecraft.util.FormattedCharSequence seq) {
        StringBuilder sb=new StringBuilder();
        seq.accept((i,style,cp)->{sb.appendCodePoint(cp);return true;});
        return sb.toString();
    }

    static long lineKey(GuiMessage.Line line) {
        return ((long)line.addedTime()<<32)|(line.content().hashCode()&0xFFFFFFFFL);
    }

    @Override public void renderBackground(GuiGraphics gfx, int mx, int my, float d) {}
    @Override public boolean isPauseScreen()     { return false; }
    @Override public boolean isAllowedInPortal() { return true; }
}
