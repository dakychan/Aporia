package so.aporia.render.ui.chat;

import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import so.aporia.render.animation.MessageAnim;
import so.aporia.render.color.ColorUtil;
import so.aporia.render.core.AporiaRenderer;
import so.aporia.utils.events.EventBus;
import so.aporia.utils.events.impl.KeyInputEvent;
import so.aporia.utils.events.impl.MouseClickEvent;
import so.aporia.utils.events.impl.MouseScrollEvent;

import java.util.List;

/**
 * Custom Aporia chat screen.
 * Supports scroll (mouse wheel / PgUp/PgDn) and Ctrl+F search.
 */
@OnlyIn(Dist.CLIENT)
public class AporiaChatScreen extends ChatScreen {

    /* ------------------------------------------------------------------ */
    /* Layout constants                                                     */
    /* ------------------------------------------------------------------ */
    static final int LEFT       = 2;
    static final int BOTTOM_PAD = 2;
    static final int INPUT_H    = 14;
    static final int RADIUS     = 5;
    static final int GAP        = 3;
    static final int BOX_PAD    = 4;
    static final int LINE_H     = 10;
    static final int MAX_LINES  = 10;
    private static final int TEXT_PAD = 8;
    private static final int MIN_W    = 80;

    /* ------------------------------------------------------------------ */
    /* Colors                                                               */
    /* ------------------------------------------------------------------ */
    private static final int COLOR_INPUT_BG    = ColorUtil.rgba(16,  16,  32,  200);
    private static final int COLOR_CHAT_BG     = ColorUtil.rgba(0,   0,   0,   150);
    private static final int COLOR_HINT        = ColorUtil.rgba(120, 120, 120, 255);
    private static final int COLOR_SEARCH_BG   = ColorUtil.rgba(20,  20,  50,  210);
    private static final int COLOR_SEARCH_HL   = ColorUtil.rgba(255, 200, 50,  180);
    private static final int COLOR_SCROLL_BAR  = ColorUtil.rgba(255, 255, 255, 60);
    private static final int COLOR_SCROLL_THUMB= ColorUtil.rgba(255, 255, 255, 140);

    /* ------------------------------------------------------------------ */
    /* State                                                                */
    /* ------------------------------------------------------------------ */
    /** How many lines we've scrolled up from the bottom (0 = newest at bottom). */
    private int scrollOffset = 0;

    /** Ctrl+F search mode. */
    private boolean searchMode = false;
    private String  searchQuery = "";
    private EditBox searchBox;

    public AporiaChatScreen(String initial, boolean isDraft) {
        super(initial, isDraft);
    }

    /* ------------------------------------------------------------------ */
    /* Helpers                                                              */
    /* ------------------------------------------------------------------ */

    private int chatWidth() {
        return net.minecraft.client.gui.components.ChatComponent.getWidth(
            this.minecraft.options.chatWidth().get()
        );
    }

    private int barWidth() {
        String val = this.input != null ? this.input.getValue() : "";
        return Math.min(Math.max(this.font.width(val) + TEXT_PAD * 2, MIN_W), chatWidth());
    }

    private List<GuiMessage.Line> allLines() {
        return this.minecraft.gui.getChat().getTrimmedMessages();
    }

    /** Max scroll = total lines - visible lines (clamped ≥ 0). */
    private int maxScroll() {
        return Math.max(0, allLines().size() - MAX_LINES);
    }

    private void clampScroll() {
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll()));
    }

    /* ------------------------------------------------------------------ */
    /* Init                                                                 */
    /* ------------------------------------------------------------------ */

    @Override
    protected void init() {
        this.historyPos = this.minecraft.gui.getChat().getRecentChat().size();

        int fullW  = chatWidth();
        int inputY = this.height - BOTTOM_PAD - INPUT_H;

        this.input = new EditBox(
            this.minecraft.fontFilterFishy,
            LEFT + TEXT_PAD, inputY + (INPUT_H - 9) / 2,
            fullW - TEXT_PAD * 2, 9,
            Component.translatable("chat.editBox")
        ) {
            @Override
            protected MutableComponent createNarrationMessage() {
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

        /* Search box — hidden until Ctrl+F. */
        int searchY = this.height - BOTTOM_PAD - INPUT_H - GAP - MAX_LINES * LINE_H - BOX_PAD * 2 - GAP - INPUT_H;
        this.searchBox = new EditBox(
            this.font, LEFT + TEXT_PAD, searchY + (INPUT_H - 9) / 2,
            200 - TEXT_PAD * 2, 9,
            Component.literal("Search")
        );
        this.searchBox.setMaxLength(64);
        this.searchBox.setBordered(false);
        this.searchBox.setResponder(s -> searchQuery = s);
        this.searchBox.setVisible(false);
        this.addRenderableWidget(this.searchBox);

        this.commandSuggestions = new CommandSuggestions(
            this.minecraft, this, this.input, this.font,
            false, false, 1, 10, true, -805306368
        );
        this.commandSuggestions.setAllowHiding(false);
        this.commandSuggestions.setAllowSuggestions(false);
        this.commandSuggestions.updateCommandInfo();
    }

    /* ------------------------------------------------------------------ */
    /* Render                                                               */
    /* ------------------------------------------------------------------ */

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float delta) {
        AporiaRenderer r = AporiaRenderer.INSTANCE;

        int inputY = this.height - BOTTOM_PAD - INPUT_H;
        int fullW  = chatWidth();
        int bW     = barWidth();
        int boxH   = MAX_LINES * LINE_H + BOX_PAD * 2;
        int boxY   = inputY - GAP - boxH;

        /* Chat box background. */
        r.drawRect(LEFT, boxY, fullW, boxH, RADIUS, COLOR_CHAT_BG);

        /* Messages. */
        gfx.enableScissor(LEFT, boxY, LEFT + fullW, boxY + boxH);
        renderMessages(gfx, boxY + BOX_PAD, LEFT + BOX_PAD);
        gfx.disableScissor();

        /* Scroll bar (only when scrolled). */
        if (maxScroll() > 0) renderScrollBar(gfx, boxY, boxH, fullW);

        /* Search bar. */
        if (searchMode) renderSearchBar(gfx, r, boxY);

        /* Input background. */
        r.drawRect(LEFT, inputY, bW, INPUT_H, RADIUS, COLOR_INPUT_BG);

        gfx.enableScissor(LEFT, inputY, LEFT + bW, inputY + INPUT_H);
        this.input.render(gfx, mouseX, mouseY, delta);
        gfx.disableScissor();

        if (this.input.getValue().isEmpty() && !searchMode) {
            gfx.drawString(this.font, "Message...",
                LEFT + TEXT_PAD, inputY + (INPUT_H - 8) / 2, COLOR_HINT, false);
        }

        this.commandSuggestions.render(gfx, mouseX, mouseY);
    }

    /** Extracts plain text from a {@link net.minecraft.util.FormattedCharSequence} — works for any language. */
    private static String plainText(net.minecraft.util.FormattedCharSequence seq) {
        StringBuilder sb = new StringBuilder();
        seq.accept((idx, style, cp) -> { sb.appendCodePoint(cp); return true; });
        return sb.toString();
    }

    private void renderMessages(GuiGraphics gfx, int boxY, int textX) {
        List<GuiMessage.Line> lines = allLines();
        int total = lines.size();

        /* When searching — jump to first match. */
        if (searchMode && !searchQuery.isEmpty()) {
            for (int idx = 0; idx < total; idx++) {
                if (matchesQuery(plainText(lines.get(idx).content()), searchQuery)) {
                    scrollOffset = Math.max(0, Math.min(idx, maxScroll()));
                    break;
                }
            }
        }

        clampScroll();
        int count = Math.min(MAX_LINES, total - scrollOffset);
        if (count <= 0) return;

        for (int i = 0; i < count; i++) {
            int lineIdx = scrollOffset + i;
            GuiMessage.Line line = lines.get(lineIdx);

            boolean matched = searchMode && !searchQuery.isEmpty()
                && matchesQuery(plainText(line.content()), searchQuery);

            long key = lineKey(line);
            MessageAnim anim = HudChatRenderer.anims.computeIfAbsent(key, k -> new MessageAnim(0f));
            anim.setAlphaTarget(1f);
            anim.tick();

            int lineY = boxY + (MAX_LINES - 1 - i) * LINE_H;
            int tx    = textX + (int) anim.slideX();

            if (matched) {
                AporiaRenderer.INSTANCE.drawRect(LEFT + 1, lineY - 1, chatWidth() - 2, LINE_H, 2, COLOR_SEARCH_HL);
            }

            int color = ColorUtil.rgba(255, 255, 255, (int)(255 * anim.alpha()));
            gfx.drawString(this.font, line.content(), tx, lineY, color, false);
        }
    }

    /** True if {@code text} contains the query in either keyboard layout. */
    private static boolean matchesQuery(String text, String query) {
        String[] v = so.aporia.utils.KeyboardLayout.both(query.toLowerCase());
        String t = text.toLowerCase();
        return t.contains(v[0]) || t.contains(v[1]);
    }

    private void renderScrollBar(GuiGraphics gfx, int boxY, int boxH, int fullW) {
        int barX   = LEFT + fullW - 3;
        int barH   = boxH - BOX_PAD * 2;
        int total  = allLines().size();
        int thumbH = Math.max(10, barH * MAX_LINES / Math.max(1, total));
        /*
         * scrollOffset=0 → newest at bottom → thumb at BOTTOM.
         * scrollOffset=maxScroll → oldest visible → thumb at TOP.
         * So invert: thumbY moves UP as scrollOffset increases.
         */
        int thumbY = boxY + BOX_PAD + (barH - thumbH) * (maxScroll() - scrollOffset) / Math.max(1, maxScroll());

        gfx.fill(barX, boxY + BOX_PAD, barX + 2, boxY + BOX_PAD + barH, COLOR_SCROLL_BAR);
        gfx.fill(barX, thumbY, barX + 2, thumbY + thumbH, COLOR_SCROLL_THUMB);
    }

    private void renderSearchBar(GuiGraphics gfx, AporiaRenderer r, int boxY) {
        int searchBarY = boxY - GAP - INPUT_H;
        r.drawRect(LEFT, searchBarY, 200, INPUT_H, RADIUS, COLOR_SEARCH_BG);

        /* Placeholder when empty. */
        if (searchQuery.isEmpty()) {
            gfx.drawString(this.font, "§7Search...",
                LEFT + TEXT_PAD, searchBarY + (INPUT_H - 8) / 2, 0xFFFFFFFF, false);
        }

        /* Position and render the EditBox (it draws the typed text). */
        this.searchBox.setX(LEFT + TEXT_PAD);
        this.searchBox.setY(searchBarY + (INPUT_H - 9) / 2);
        this.searchBox.setWidth(200 - TEXT_PAD * 2);
        this.searchBox.setVisible(true);
        this.searchBox.render(gfx, 0, 0, 0);
    }

    /* ------------------------------------------------------------------ */
    /* Input                                                                */
    /* ------------------------------------------------------------------ */

    @Override
    public boolean keyPressed(KeyEvent e) {
        /* Fire global event. */
        KeyInputEvent.Action action = KeyInputEvent.Action.PRESS;
        EventBus.INSTANCE.post(new KeyInputEvent(e.key(), e.scancode(), e.modifiers(), action));

        /* Ctrl+F — toggle search. */
        if (e.key() == 70 && (e.modifiers() & 2) != 0) { /* F + CTRL */
            toggleSearch();
            return true;
        }

        /* Escape closes search first, then screen. */
        if (e.key() == 256 && searchMode) {
            closeSearch();
            return true;
        }

        /* If search mode, route typing to searchBox. */
        if (searchMode && searchBox.isFocused()) {
            return searchBox.keyPressed(e);
        }

        return super.keyPressed(e);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        EventBus.INSTANCE.post(new MouseScrollEvent(mx, my, dx, dy));

        if (commandSuggestions.mouseScrolled(dy)) return true;
        /* scroll up (dy>0) → show older messages → increase offset */
        int delta = dy > 0 ? 1 : -1;
        scrollOffset = Math.max(0, Math.min(scrollOffset + delta, maxScroll()));
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean b) {
        EventBus.INSTANCE.post(new MouseClickEvent(e.x(), e.y(), e.button(), MouseClickEvent.Action.PRESS));
        return super.mouseClicked(e, b);
    }

    /* ------------------------------------------------------------------ */
    /* Search helpers                                                       */
    /* ------------------------------------------------------------------ */

    private void toggleSearch() {
        searchMode = !searchMode;
        if (searchMode) {
            searchQuery = "";
            searchBox.setValue("");
            searchBox.setFocused(true);
            this.input.setFocused(false);
            this.setFocused(searchBox);
        } else {
            closeSearch();
        }
    }

    private void closeSearch() {
        searchMode = false;
        searchQuery = "";
        searchBox.setVisible(false);
        searchBox.setFocused(false);
        this.input.setFocused(true);
        this.setFocused(this.input);
    }

    /* ------------------------------------------------------------------ */
    /* Misc overrides                                                       */
    /* ------------------------------------------------------------------ */

    @Override
    public void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float delta) {}

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public boolean isAllowedInPortal() { return true; }

    static long lineKey(GuiMessage.Line line) {
        return ((long) line.addedTime() << 32) | (line.content().hashCode() & 0xFFFFFFFFL);
    }
}
