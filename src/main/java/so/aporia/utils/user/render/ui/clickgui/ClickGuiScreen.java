package so.aporia.utils.user.render.ui.clickgui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import so.aporia.module.Category;
import so.aporia.module.Module;
import so.aporia.module.ModuleManager;
import so.aporia.utils.user.render.color.ColorUtil;
import so.aporia.utils.user.render.core.AporiaRenderer;
import so.aporia.utils.user.render.font.Fonts;

import java.util.ArrayList;
import java.util.List;

/**
 * ClickGui — BTHack-style.
 *
 *  ┌──────────────────────────────────────┐
 *  │  ⊕ APORIA                [Search..] │
 *  ├──────────┬───────────────────────────┤
 *  │ ⚔ Combat │ ┌──────────┐ ┌──────────┐│
 *  │ ↝ Move   │ │ Module A │ │ Module B ││
 *  │ ◎ Visual │ └──────────┘ └──────────┘│
 *  │ ☺ Player │ ┌──────────┐             │
 *  │ ◉ World  │ │ Module C │             │
 *  │ ··· Misc │ └──────────┘             │
 *  └──────────┴───────────────────────────┘
 *
 * Modules shown as pill cards in a 2-column grid.
 * Category icons from the categoryicons font.
 * Sliding highlight rect animates between categories.
 */
@OnlyIn(Dist.CLIENT)
public final class ClickGuiScreen extends Screen {

    /* ── Size ────────────────────────────────────────────────── */
    private static final float W_FRAC = 0.38f;
    private static final float H_FRAC = 0.45f;
    private static final int   MIN_W  = 260;
    private static final int   MIN_H  = 180;

    /* ── Layout ──────────────────────────────────────────────── */
    private static final int HEADER_H  = 32;
    private static final int SIDEBAR_W = 100;
    private static final int CAT_H     = 30;
    private static final int CAT_GAP   = 2;
    private static final int PAD       = 10;
    private static final int HIT       = 5;
    private static final int PANEL_R   = 12;
    private static final int CAT_R     = 6;
    private static final int SEARCH_H  = 18;
    private static final int SEARCH_W  = 100;
    private static final float ANIM_SPEED = 0.18f;

    /* module card grid */
    private static final int CARD_H   = 30;
    private static final int CARD_R   = 6;  /* moderate rounding */
    private static final int CARD_GAP = 6;
    private static final int CARD_PAD = 8;

    /* ── Colors ──────────────────────────────────────────────── */
    private static final int C_BG          = ColorUtil.rgba( 18,  22,  32, 245);
    private static final int C_HEADER      = ColorUtil.rgba( 22,  27,  40, 255);
    private static final int C_SIDEBAR     = ColorUtil.rgba( 20,  25,  37, 255);
    private static final int C_DIV         = ColorUtil.rgba(255, 255, 255,  14);
    private static final int C_TITLE       = ColorUtil.rgba(255, 255, 255, 255);
    private static final int C_CAT_TXT     = ColorUtil.rgba(130, 150, 190, 255);
    private static final int C_CAT_ACT_TXT = ColorUtil.rgba(210, 225, 255, 255);
    private static final int C_CAT_HOV_TXT = ColorUtil.rgba(175, 195, 230, 255);
    private static final int C_CAT_SLIDE   = ColorUtil.rgba( 80, 100, 160,  55);
    private static final int C_CARD_BG     = ColorUtil.rgba( 28,  34,  50, 255);
    private static final int C_CARD_BDR    = ColorUtil.rgba( 55,  70, 110, 180);
    private static final int C_CARD_ON_BDR = ColorUtil.rgba( 90, 140, 255, 220);
    private static final int C_CARD_HOV    = ColorUtil.rgba( 35,  43,  62, 255);
    private static final int C_MOD_TXT     = ColorUtil.rgba(200, 210, 230, 255);
    private static final int C_MOD_ON_TXT  = ColorUtil.rgba(220, 235, 255, 255);
    private static final int C_SRCH_BG     = ColorUtil.rgba( 25,  31,  46, 255);
    private static final int C_SRCH_BDR    = ColorUtil.rgba( 55,  70, 110, 160);
    private static final int C_SRCH_BDR_F  = ColorUtil.rgba( 90, 140, 255, 200);
    private static final int C_SRCH_TXT    = ColorUtil.rgba(175, 185, 205, 255);
    private static final int C_SRCH_PH     = ColorUtil.rgba( 80,  95, 130, 255);

    /* ── Panel state ─────────────────────────────────────────── */
    private float px, py, pw, ph;
    private Category active = Category.values()[0];
    private float animY;

    /* search */
    private String  search        = "";
    private boolean searchFocused = false;

    /* drag */
    private boolean dragging;
    private float   dox, doy;

    /* resize */
    private boolean resizing;
    private boolean resizeL, resizeR, resizeT, resizeB;
    private float   rStartMx, rStartMy, rStartPx, rStartPy, rStartPw, rStartPh;

    public ClickGuiScreen() {
        super(Component.literal("ClickGui"));
    }

    @Override
    protected void init() {
        if (pw == 0) {
            pw = Math.max(MIN_W, this.width  * W_FRAC);
            ph = Math.max(MIN_H, this.height * H_FRAC);
            px = (this.width  - pw) / 2f;
            py = (this.height - ph) / 2f;
            animY = catTargetY(active);
        }
    }

    /* ── Render ──────────────────────────────────────────────── */

    @Override
    public void render(GuiGraphics gfx, int mx, int my, float delta) {
        AporiaRenderer r = AporiaRenderer.INSTANCE;

        px = Math.max(0, Math.min(px, this.width  - pw));
        py = Math.max(0, Math.min(py, this.height - ph));

        int ipx = (int) px, ipy = (int) py, ipw = (int) pw, iph = (int) ph;

        animY += (catTargetY(active) - animY) * ANIM_SPEED;

        /* main bg */
        r.drawRect(ipx, ipy, ipw, iph, PANEL_R, C_BG);

        /* header */
        r.drawRect(ipx, ipy, ipw, HEADER_H + PANEL_R, PANEL_R, C_HEADER);
        r.drawRect(ipx, ipy + HEADER_H, ipw, PANEL_R, 0, C_HEADER);
        r.drawText("bold", "APORIA", ipx + PAD, ipy + (HEADER_H - 10) / 2f, 10f, C_TITLE);

        /* search */
        int sfx = ipx + ipw - SEARCH_W - PAD;
        int sfy = ipy + (HEADER_H - SEARCH_H) / 2;
        int sr  = SEARCH_H / 2;
        r.drawRect(sfx, sfy, SEARCH_W, SEARCH_H, sr, C_SRCH_BG);
        int bdr = searchFocused ? C_SRCH_BDR_F : C_SRCH_BDR;
        r.drawStroke(sfx, sfy, SEARCH_W, SEARCH_H, sr, 1f, 1, 0f, bdr);
        gfx.enableScissor(sfx + 6, sfy, sfx + SEARCH_W - 6, sfy + SEARCH_H);
        if (search.isEmpty() && !searchFocused) {
            r.drawText("regular", "Search...", sfx + 8, sfy + (SEARCH_H - 7) / 2f, 7f, C_SRCH_PH);
        } else {
            String cur = search + (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0 ? "|" : "");
            r.drawText("regular", cur, sfx + 8, sfy + (SEARCH_H - 7) / 2f, 7f, C_SRCH_TXT);
        }
        gfx.disableScissor();

        /* divider */
        r.drawRect(ipx, ipy + HEADER_H, ipw, 1, 0, C_DIV);

        /* sidebar bg */
        r.drawRect(ipx, ipy + HEADER_H + 1, SIDEBAR_W, iph - HEADER_H - 1, 0, C_SIDEBAR);
        r.drawRect(ipx, ipy + iph - PANEL_R, SIDEBAR_W, PANEL_R, 0, C_SIDEBAR);

        /* sidebar divider */
        r.drawRect(ipx + SIDEBAR_W, ipy + HEADER_H + 1, 1, iph - HEADER_H - 1, 0, C_DIV);

        /* sliding highlight */
        int sidebarTop = ipy + HEADER_H + 1;
        r.drawRect(ipx + 4, sidebarTop + animY, SIDEBAR_W - 8, CAT_H, CAT_R, C_CAT_SLIDE);

        /* sidebar */
        gfx.enableScissor(ipx, sidebarTop, ipx + SIDEBAR_W, ipy + iph);
        renderSidebar(r, ipx, sidebarTop, mx, my);
        gfx.disableScissor();

        /* module cards */
        int cx = ipx + SIDEBAR_W + 1;
        int cy = ipy + HEADER_H + 1;
        int cw = ipw - SIDEBAR_W - 1;
        int ch = iph - HEADER_H - 1;
        gfx.enableScissor(cx, cy, cx + cw, cy + ch);
        renderCards(r, cx, cy, cw, mx, my);
        gfx.disableScissor();
    }

    private void renderSidebar(AporiaRenderer r, int sx, int sy, int mx, int my) {
        int y = sy + 4;
        for (Category cat : Category.values()) {
            boolean act = cat == active;
            boolean hov = !act && mx >= sx && mx < sx + SIDEBAR_W && my >= y && my < y + CAT_H;
            int col = act ? C_CAT_ACT_TXT : (hov ? C_CAT_HOV_TXT : C_CAT_TXT);

            /* icon from categoryicons font */
            String icon = String.valueOf(cat.icon);
            r.drawText(Fonts.CATICONS, icon, sx + PAD, y + (CAT_H - 11) / 2f, 11f, col);

            /* label */
            r.drawText("regular", capitalize(cat.name()), sx + PAD + 16, y + (CAT_H - 7) / 2f, 7f, col);
            y += CAT_H + CAT_GAP;
        }
    }

    private void renderCards(AporiaRenderer r, int cx, int cy, int cw, int mx, int my) {
        List<Module> mods = filteredModules();
        int cols    = 2;
        int cardW   = (cw - PAD * (cols + 1)) / cols;
        int x0      = cx + PAD;
        int y       = cy + PAD;

        for (int i = 0; i < mods.size(); i++) {
            Module m   = mods.get(i);
            int col    = i % cols;
            int row    = i / cols;
            int cardX  = x0 + col * (cardW + CARD_GAP);
            int cardY  = y  + row * (CARD_H + CARD_GAP);

            boolean on  = m.isEnabled();
            boolean hov = mx >= cardX && mx < cardX + cardW && my >= cardY && my < cardY + CARD_H;

            int bg  = hov ? C_CARD_HOV : C_CARD_BG;
            int bdr = on  ? C_CARD_ON_BDR : C_CARD_BDR;

            /* card bg */
            r.drawRect(cardX, cardY, cardW, CARD_H, CARD_R, bg);

            /* corners-only stroke via shader — borderMode=2, fade=0.9 */
            r.drawStroke(cardX, cardY, cardW, CARD_H, CARD_R, 1.5f, 2, 0.9f, bdr);

            /* module name */
            int txtCol = on ? C_MOD_ON_TXT : C_MOD_TXT;
            r.drawText("regular", m.name(), cardX + CARD_PAD, cardY + (CARD_H - 7) / 2f, 7f, txtCol);
        }
    }

    /* ── Mouse ───────────────────────────────────────────────── */

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean b) {
        int mx = (int) e.x(), my = (int) e.y();

        if (e.button() == 0) {
            /* search */
            int sfx = (int)(px + pw) - SEARCH_W - PAD;
            int sfy = (int) py + (HEADER_H - SEARCH_H) / 2;
            if (mx >= sfx && mx < sfx + SEARCH_W && my >= sfy && my < sfy + SEARCH_H) {
                searchFocused = true; return true;
            }
            searchFocused = false;

            /* resize */
            boolean el = mx >= (int) px - HIT        && mx < (int) px + HIT;
            boolean er = mx >= (int)(px + pw) - HIT  && mx < (int)(px + pw) + HIT;
            boolean et = my >= (int) py - HIT         && my < (int) py + HIT;
            boolean eb = my >= (int)(py + ph) - HIT   && my < (int)(py + ph) + HIT;
            boolean inX = mx >= (int) px - HIT && mx <= (int)(px + pw) + HIT;
            boolean inY = my >= (int) py - HIT && my <= (int)(py + ph) + HIT;
            if ((el || er || et || eb) && inX && inY) {
                resizing = true;
                resizeL = el; resizeR = er; resizeT = et; resizeB = eb;
                rStartMx = mx; rStartMy = my;
                rStartPx = px; rStartPy = py; rStartPw = pw; rStartPh = ph;
                return true;
            }

            /* header drag */
            if (mx >= (int) px && mx < (int)(px + pw) && my >= (int) py && my < (int)(py + HEADER_H)) {
                dragging = true; dox = mx - px; doy = my - py; return true;
            }

            /* sidebar */
            int sy = (int) py + HEADER_H + 1 + 4;
            for (Category cat : Category.values()) {
                if (mx >= (int) px && mx < (int) px + SIDEBAR_W && my >= sy && my < sy + CAT_H) {
                    active = cat; search = ""; return true;
                }
                sy += CAT_H + CAT_GAP;
            }

            /* card click */
            int cx   = (int) px + SIDEBAR_W + 1;
            int cy   = (int) py + HEADER_H + 1;
            int cw   = (int) pw - SIDEBAR_W - 1;
            int cols = 2;
            int cardW = (cw - PAD * (cols + 1)) / cols;
            int x0   = cx + PAD;
            int y0   = cy + PAD;
            List<Module> mods = filteredModules();
            for (int i = 0; i < mods.size(); i++) {
                int col   = i % cols;
                int row   = i / cols;
                int cardX = x0 + col * (cardW + CARD_GAP);
                int cardY = y0 + row * (CARD_H + CARD_GAP);
                if (mx >= cardX && mx < cardX + cardW && my >= cardY && my < cardY + CARD_H) {
                    mods.get(i).toggle(); return true;
                }
            }
        }

        return super.mouseClicked(e, b);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        float mx = (float) e.x(), my = (float) e.y();
        if (dragging) { px = mx - dox; py = my - doy; return true; }
        if (resizing) {
            float ddx = mx - rStartMx, ddy = my - rStartMy;
            if (resizeR) pw = Math.max(MIN_W, rStartPw + ddx);
            if (resizeB) ph = Math.max(MIN_H, rStartPh + ddy);
            if (resizeL) { float nw = Math.max(MIN_W, rStartPw - ddx); px = rStartPx + (rStartPw - nw); pw = nw; }
            if (resizeT) { float nh = Math.max(MIN_H, rStartPh - ddy); py = rStartPy + (rStartPh - nh); ph = nh; }
            return true;
        }
        return super.mouseDragged(e, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent e) {
        dragging = false; resizing = false;
        return super.mouseReleased(e);
    }

    /* ── Keyboard ────────────────────────────────────────────── */

    @Override
    public boolean keyPressed(KeyEvent e) {
        if (searchFocused) {
            if (e.key() == 259 && !search.isEmpty()) { search = search.substring(0, search.length() - 1); return true; }
            if (e.key() == 256) { searchFocused = false; return true; }
        }
        return super.keyPressed(e);
    }

    @Override
    public boolean charTyped(CharacterEvent e) {
        if (searchFocused) { search += (char) e.codepoint(); return true; }
        return super.charTyped(e);
    }

    /* ── Helpers ─────────────────────────────────────────────── */

    private float catTargetY(Category cat) {
        Category[] cats = Category.values();
        for (int i = 0; i < cats.length; i++) if (cats[i] == cat) return 4 + i * (CAT_H + CAT_GAP);
        return 4;
    }

    private List<Module> filteredModules() {
        List<Module> base = ModuleManager.INSTANCE.getByCategory(active);
        if (search.isEmpty()) return base;
        List<Module> out = new ArrayList<>();
        String q = search.toLowerCase();
        for (Module m : base) if (m.name().toLowerCase().contains(q)) out.add(m);
        return out;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.charAt(0) + s.substring(1).toLowerCase();
    }

    @Override public boolean isPauseScreen()     { return false; }
    @Override public boolean isAllowedInPortal() { return true; }
    @Override public void renderBackground(GuiGraphics gfx, int mx, int my, float d) {}
}
