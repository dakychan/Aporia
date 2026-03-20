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
 * ClickGui — three-column layout inspired by modern client GUIs.
 *
 *  ┌──────────────┬──────────────────────┬──────────────────┐
 *  │  sidebar     │  [Category] [Search] │  Settings panel  │
 *  │  • Combat    │  ──────────────────  │  (opens on RMB)  │
 *  │    Move      │  [Mod] [key]  [⚙]   │                  │
 *  │    Visual    │  [Mod]               │                  │
 *  │    Player    │  [Mod]               │                  │
 *  └──────────────┴──────────────────────┴──────────────────┘
 *
 * Sidebar: category list with animated selection highlight.
 * Module list: single-column cards with name, keybind, settings icon.
 * Settings panel: opens on RMB on a module card (placeholder for now).
 */
@OnlyIn(Dist.CLIENT)
public final class ClickGuiScreen extends Screen {

    private static final float W_FRAC = 0.52f;
    private static final float H_FRAC = 0.50f;
    private static final int   MIN_W  = 380;
    private static final int   MIN_H  = 220;

    private static final int SIDEBAR_W  = 130;
    private static final int MODULES_W  = 200;
    private static final int CAT_H      = 28;
    private static final int CAT_GAP    = 2;
    private static final int CAT_PAD    = 10;
    private static final int TOPBAR_H   = 36;
    private static final int PAD        = 10;
    private static final int HIT        = 5;
    private static final int PANEL_R    = 10;
    private static final int CAT_R      = 6;
    private static final int CARD_H     = 32;
    private static final int CARD_R     = 7;
    private static final int CARD_GAP   = 4;
    private static final int SEARCH_H   = 20;
    private static final int SEARCH_W   = 130;
    private static final float ANIM_SPD = 0.16f;

    private static final int C_BG           = ColorUtil.rgba( 18,  21,  30, 252);
    private static final int C_SIDEBAR      = ColorUtil.rgba( 22,  26,  38, 255);
    private static final int C_TOPBAR       = ColorUtil.rgba( 22,  26,  38, 255);
    private static final int C_DIV          = ColorUtil.rgba(255, 255, 255,  12);
    private static final int C_CAT_SLIDE    = ColorUtil.rgba( 70,  90, 150,  60);
    private static final int C_CAT_TXT      = ColorUtil.rgba(110, 130, 170, 255);
    private static final int C_CAT_ACT      = ColorUtil.rgba(210, 225, 255, 255);
    private static final int C_CAT_HOV      = ColorUtil.rgba(160, 180, 220, 255);
    private static final int C_CARD_BG      = ColorUtil.rgba( 26,  31,  46, 255);
    private static final int C_CARD_HOV     = ColorUtil.rgba( 33,  40,  58, 255);
    private static final int C_CARD_ON      = ColorUtil.rgba( 30,  38,  60, 255);
    private static final int C_CARD_BDR     = ColorUtil.rgba( 50,  65, 105, 160);
    private static final int C_CARD_ON_BDR  = ColorUtil.rgba( 80, 130, 255, 200);
    private static final int C_MOD_TXT      = ColorUtil.rgba(185, 200, 225, 255);
    private static final int C_MOD_ON_TXT   = ColorUtil.rgba(220, 235, 255, 255);
    private static final int C_MOD_DIM      = ColorUtil.rgba(100, 115, 145, 255);
    private static final int C_TOPBAR_TXT   = ColorUtil.rgba(160, 175, 210, 255);
    private static final int C_TOPBAR_ACT   = ColorUtil.rgba(210, 225, 255, 255);
    private static final int C_SRCH_BG      = ColorUtil.rgba( 28,  34,  50, 255);
    private static final int C_SRCH_BDR     = ColorUtil.rgba( 50,  65, 105, 140);
    private static final int C_SRCH_BDR_F   = ColorUtil.rgba( 80, 130, 255, 180);
    private static final int C_SRCH_TXT     = ColorUtil.rgba(175, 190, 215, 255);
    private static final int C_SRCH_PH      = ColorUtil.rgba( 75,  90, 125, 255);
    private static final int C_SETTINGS_BG  = ColorUtil.rgba( 20,  24,  36, 255);
    private static final int C_SETTINGS_TXT = ColorUtil.rgba(140, 155, 185, 255);

    private float px, py, pw, ph;
    private Category active   = Category.values()[0];
    private Module   selected = null;
    private float    animY    = 0;

    private String  search        = "";
    private boolean searchFocused = false;

    private boolean dragging;
    private float   dox, doy;
    private boolean resizing;
    private boolean resizeL, resizeR, resizeT, resizeB;
    private float   rStartMx, rStartMy, rStartPx, rStartPy, rStartPw, rStartPh;

    public ClickGuiScreen() { super(Component.literal("ClickGui")); }

    @Override
    protected void init() {
        if (pw == 0) {
            pw = Math.max(MIN_W, this.width * W_FRAC);
            ph = Math.max(MIN_H, this.height * H_FRAC);
            px = (this.width - pw) / 2f;
            py = (this.height - ph) / 2f;
            animY = catTargetY(active, (int) py, (int) ph);
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mx, int my, float delta) {
        AporiaRenderer r = AporiaRenderer.INSTANCE;

        px = Math.max(0, Math.min(px, this.width  - pw));
        py = Math.max(0, Math.min(py, this.height - ph));

        int ipx = (int) px, ipy = (int) py, ipw = (int) pw, iph = (int) ph;
        int contentX = ipx + SIDEBAR_W;
        int contentW = ipw - SIDEBAR_W;
        int bodyY    = ipy + TOPBAR_H;
        int bodyH    = iph - TOPBAR_H;

        animY += (catTargetY(active, ipy, iph) - animY) * ANIM_SPD;

        /* main bg */
        r.drawRect(ipx, ipy, ipw, iph, PANEL_R, C_BG);

        /* sidebar bg — stops before top-left and bottom-left corners */
        r.drawRect(ipx, ipy + PANEL_R, SIDEBAR_W, iph - PANEL_R * 2, 0, C_SIDEBAR);
        r.drawRect(ipx + PANEL_R, ipy + iph - PANEL_R, SIDEBAR_W - PANEL_R, PANEL_R, 0, C_SIDEBAR);
        r.drawRect(ipx + PANEL_R, ipy, SIDEBAR_W - PANEL_R, PANEL_R, 0, C_SIDEBAR);

        /* topbar bg — right of sidebar only, top-right corner rounded */
        r.drawRect(contentX, ipy, contentW, TOPBAR_H + PANEL_R, PANEL_R, C_TOPBAR);
        r.drawRect(contentX, bodyY, contentW, PANEL_R, 0, C_TOPBAR);

        /* dividers */
        r.drawRect(contentX, ipy, 1, iph, 0, C_DIV);
        r.drawRect(contentX, bodyY, contentW, 1, 0, C_DIV);

        /* sidebar: animated highlight + category list */
        animY += (catTargetY(active, ipy, iph) - animY) * ANIM_SPD;
        r.drawRect(ipx + 4, animY, SIDEBAR_W - 8, CAT_H, CAT_R, C_CAT_SLIDE);
        gfx.enableScissor(ipx, ipy, contentX, ipy + iph);
        renderSidebar(r, ipx, ipy, iph, mx, my);
        gfx.disableScissor();

        /* topbar: category name + search */
        renderTopbar(r, gfx, contentX, ipy, contentW);

        /* module list */
        int modX = contentX + 1;
        int modW = selected != null ? MODULES_W : contentW - 1;
        gfx.enableScissor(modX, bodyY + 1, modX + modW, ipy + iph);
        renderModules(r, modX, bodyY, modW, mx, my);
        gfx.disableScissor();

        /* settings panel */
        if (selected != null) {
            int sx = modX + modW;
            int sw = contentW - 1 - modW;
            r.drawRect(sx, bodyY, 1, bodyH, 0, C_DIV);
            gfx.enableScissor(sx + 1, bodyY + 1, sx + 1 + sw, ipy + iph);
            renderSettings(r, gfx, sx + 1, bodyY, sw);
            gfx.disableScissor();
        }
    }

    private void renderTopbar(AporiaRenderer r, GuiGraphics gfx, int ipx, int ipy, int ipw) {
        String catName = capitalize(active.name());
        r.drawText("bold", catName, ipx + PAD, ipy + (TOPBAR_H - 9) / 2f, 9f, C_TOPBAR_ACT);

        int sfx = ipx + ipw - SEARCH_W - PAD;
        int sfy = ipy + (TOPBAR_H - SEARCH_H) / 2;
        int sr  = SEARCH_H / 2;
        r.drawRect(sfx, sfy, SEARCH_W, SEARCH_H, sr, C_SRCH_BG);
        r.drawStroke(sfx, sfy, SEARCH_W, SEARCH_H, sr, 1f, 1, 0f, searchFocused ? C_SRCH_BDR_F : C_SRCH_BDR);
        gfx.enableScissor(sfx + 6, sfy, sfx + SEARCH_W - 6, sfy + SEARCH_H);
        if (search.isEmpty() && !searchFocused) {
            r.drawText("regular", "Search...", sfx + 8, sfy + (SEARCH_H - 7) / 2f, 7f, C_SRCH_PH);
        } else {
            String cur = search + (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0 ? "|" : "");
            r.drawText("regular", cur, sfx + 8, sfy + (SEARCH_H - 7) / 2f, 7f, C_SRCH_TXT);
        }
        gfx.disableScissor();
    }

    private void renderSidebar(AporiaRenderer r, int sx, int sy, int sHeight, int mx, int my) {
        Category[] cats = Category.values();
        int totalH = cats.length * CAT_H + (cats.length - 1) * CAT_GAP;
        int startY = sy + (sHeight - totalH) / 2;
        int y = startY;
        for (Category cat : cats) {
            boolean act = cat == active;
            boolean hov = !act && mx >= sx && mx < sx + SIDEBAR_W && my >= y && my < y + CAT_H;
            int col = act ? C_CAT_ACT : (hov ? C_CAT_HOV : C_CAT_TXT);
            String icon = String.valueOf(cat.icon);
            r.drawText(Fonts.CATICONS, icon, sx + CAT_PAD, y + (CAT_H - 11) / 2f, 11f, col);
            r.drawText("regular", capitalize(cat.name()), sx + CAT_PAD + 18, y + (CAT_H - 7) / 2f, 7f, col);
            y += CAT_H + CAT_GAP;
        }
    }

    private void renderModules(AporiaRenderer r, int cx, int cy, int cw, int mx, int my) {
        List<Module> mods = filteredModules();
        int y = cy + PAD;
        for (Module m : mods) {
            int cardX = cx + PAD;
            int cardW = cw - PAD * 2;
            boolean on  = m.isEnabled();
            boolean hov = mx >= cardX && mx < cardX + cardW && my >= y && my < y + CARD_H;
            boolean sel = m == selected;

            int bg  = sel ? C_CARD_ON : (hov ? C_CARD_HOV : C_CARD_BG);
            int bdr = (on || sel) ? C_CARD_ON_BDR : C_CARD_BDR;

            r.drawRect(cardX, y, cardW, CARD_H, CARD_R, bg);
            r.drawStroke(cardX, y, cardW, CARD_H, CARD_R, 1f, 1, 0f, bdr);

            int txtCol = on ? C_MOD_ON_TXT : C_MOD_TXT;
            r.drawText("regular", m.name(), cardX + PAD, y + (CARD_H - 7) / 2f, 7f, txtCol);

            String kb = m.keybind() > 0 ? keyName(m.keybind()) : "";
            if (!kb.isEmpty())
                r.drawText("regular", kb, cardX + cardW - PAD - r.getTextWidth("regular", kb, 6f), y + (CARD_H - 6) / 2f, 6f, C_MOD_DIM);

            y += CARD_H + CARD_GAP;
        }
    }

    private void renderSettings(AporiaRenderer r, GuiGraphics gfx, int sx, int sy, int sw) {
        if (selected == null) return;
        r.drawText("bold", selected.name(), sx + PAD, sy + PAD, 8f, C_MOD_ON_TXT);
        r.drawText("regular", "Right-click to close", sx + PAD, sy + PAD + 14, 6f, C_SETTINGS_TXT);
    }


    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean b) {
        int mx = (int) e.x(), my = (int) e.y();
        int ipx = (int) px, ipy = (int) py, ipw = (int) pw, iph = (int) ph;
        int bodyY = ipy + TOPBAR_H;

        if (e.button() == 0) {
            int sfx = ipx + ipw - SEARCH_W - PAD;
            int sfy = ipy + (TOPBAR_H - SEARCH_H) / 2;
            if (mx >= sfx && mx < sfx + SEARCH_W && my >= sfy && my < sfy + SEARCH_H) {
                searchFocused = true; return true;
            }
            searchFocused = false;

            boolean el = mx >= ipx - HIT && mx < ipx + HIT;
            boolean er = mx >= ipx + ipw - HIT && mx < ipx + ipw + HIT;
            boolean et = my >= ipy - HIT && my < ipy + HIT;
            boolean eb = my >= ipy + iph - HIT && my < ipy + iph + HIT;
            boolean inX = mx >= ipx - HIT && mx <= ipx + ipw + HIT;
            boolean inY = my >= ipy - HIT && my <= ipy + iph + HIT;
            if ((el || er || et || eb) && inX && inY) {
                resizing = true;
                resizeL = el; resizeR = er; resizeT = et; resizeB = eb;
                rStartMx = mx; rStartMy = my;
                rStartPx = px; rStartPy = py; rStartPw = pw; rStartPh = ph;
                return true;
            }

            if (mx >= ipx && mx < ipx + ipw && my >= ipy && my < bodyY && mx >= (int)px + SIDEBAR_W) {
                dragging = true; dox = mx - px; doy = my - py; return true;
            }

            Category[] catArr = Category.values();
            int totalCH = catArr.length * CAT_H + (catArr.length - 1) * CAT_GAP;
            int catSY   = ipy + (iph - totalCH) / 2;
            for (Category cat : catArr) {
                if (mx >= ipx && mx < ipx + SIDEBAR_W && my >= catSY && my < catSY + CAT_H) {
                    active = cat; search = ""; selected = null; return true;
                }
                catSY += CAT_H + CAT_GAP;
            }

            int modX = ipx + SIDEBAR_W + 1;
            int modW = selected != null ? MODULES_W : ipw - SIDEBAR_W - 1;
            List<Module> mods = filteredModules();
            int y = bodyY + PAD;
            for (Module m : mods) {
                int cardX = modX + PAD;
                int cardW = modW - PAD * 2;
                if (mx >= cardX && mx < cardX + cardW && my >= y && my < y + CARD_H) {
                    m.toggle(); return true;
                }
                y += CARD_H + CARD_GAP;
            }
        }

        if (e.button() == 1) {
            int modX = (int) px + SIDEBAR_W + 1;
            int modW = selected != null ? MODULES_W : (int) pw - SIDEBAR_W - 1;
            List<Module> mods = filteredModules();
            int y = (int) py + TOPBAR_H + PAD;
            for (Module m : mods) {
                int cardX = modX + PAD;
                int cardW = modW - PAD * 2;
                if (mx >= cardX && mx < cardX + cardW && my >= y && my < y + CARD_H) {
                    selected = (selected == m) ? null : m; return true;
                }
                y += CARD_H + CARD_GAP;
            }
            selected = null;
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

    private float catTargetY(Category cat, int panelY, int panelH) {
        Category[] cats = Category.values();
        int totalH = cats.length * CAT_H + (cats.length - 1) * CAT_GAP;
        int startY = panelY + (panelH - totalH) / 2;
        for (int i = 0; i < cats.length; i++) if (cats[i] == cat) return startY + i * (CAT_H + CAT_GAP);
        return startY;
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

    private static String keyName(int key) {
        return net.minecraft.client.KeyMapping.createNameSupplier("key.keyboard." + key).get().getString().toUpperCase();
    }

    @Override public boolean isPauseScreen()     { return false; }
    @Override public boolean isAllowedInPortal() { return true; }
    @Override public void renderBackground(GuiGraphics gfx, int mx, int my, float d) {}
}
