package so.aporia.utils.user.render.ui.clickgui;

import net.minecraft.client.Minecraft;
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
import so.aporia.utils.user.render.animation.Animator;
import so.aporia.utils.user.render.animation.Easing;
import so.aporia.utils.user.render.color.ColorUtil;
import so.aporia.utils.user.render.core.AporiaRenderer;
import so.aporia.utils.user.render.font.Fonts;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class ClickGuiScreen extends Screen {

    private static final int SIDEBAR_W = 130;
    private static final int TOPBAR_H  = 34;
    private static final int CAT_H     = 28;
    private static final int CAT_PAD   = 12;
    private static final int CARD_H    = 26;
    private static final int CARD_R    = 6;
    private static final int CARD_GAP  = 3;
    private static final int PAD       = 8;
    private static final int PANEL_R   = 16;
    private static final int CAT_R     = 8;
    private static final int SEARCH_H  = 20;
    private static final int SEARCH_W  = 130;
    private static final int HIT       = 6;
    private static final int MIN_W     = 340;
    private static final int MIN_H     = 180;
    private static final float W_FRAC  = 0.38f;
    private static final float H_FRAC  = 0.42f;

    private static final int C_PANEL_TINT  = ColorUtil.rgba(255, 255, 255,  18);
    private static final int C_CAT_TXT     = ColorUtil.rgba(255, 255, 255, 140);
    private static final int C_CAT_ACT     = ColorUtil.rgba(255, 255, 255, 255);
    private static final int C_CAT_HOV     = ColorUtil.rgba(255, 255, 255, 210);
    private static final int C_DIV         = ColorUtil.rgba(255, 255, 255,  16);
    private static final int C_CARD_TINT   = ColorUtil.rgba(255, 255, 255,  12);
    private static final int C_CARD_HOV    = ColorUtil.rgba(255, 255, 255,  28);
    private static final int C_CARD_ON     = ColorUtil.rgba(255, 255, 255,  40);
    private static final int C_TXT         = ColorUtil.rgba(255, 255, 255, 180);
    private static final int C_TXT_ON      = ColorUtil.rgba(255, 255, 255, 255);
    private static final int C_TXT_DIM     = ColorUtil.rgba(255, 255, 255, 100);
    private static final int C_TOPBAR_TXT  = ColorUtil.rgba(255, 255, 255, 255);
    private static final int C_SRCH_BG     = ColorUtil.rgba(255, 255, 255,  18);
    private static final int C_SRCH_BDR    = ColorUtil.rgba(255, 255, 255,  40);
    private static final int C_SRCH_BDR_F  = ColorUtil.rgba(255, 255, 255, 120);
    private static final int C_SRCH_TXT    = ColorUtil.rgba(255, 255, 255, 220);
    private static final int C_SRCH_PH     = ColorUtil.rgba(255, 255, 255,  80);
    private static final int C_GLASS_OUT   = ColorUtil.rgba(255, 255, 255,  30);
    private static final int C_GLASS_IN    = ColorUtil.rgba(255, 255, 255,  12);

    private float px, py, pw, ph;
    private Category active = Category.values()[0];
    private Module selected = null;
    private float animSelY  = -1;

    private final Map<Category, Animator> hoverAnims = new EnumMap<>(Category.class);
    private final java.util.WeakHashMap<Module, Animator> modHoverAnims = new java.util.WeakHashMap<>();

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
            pw = Math.max(MIN_W, this.width  * W_FRAC);
            ph = Math.max(MIN_H, this.height * H_FRAC);
            px = (this.width  - pw) / 2f;
            py = (this.height - ph) / 2f;
        }
        for (Category c : Category.values())
            hoverAnims.computeIfAbsent(c, k -> new Animator(200, Easing::cubicOut));
        if (animSelY < 0) animSelY = catY(active);
        AporiaRenderer.INSTANCE.resetDebugFlags();
    }

    @Override
    public void render(GuiGraphics gfx, int mx, int my, float delta) {
        AporiaRenderer r = AporiaRenderer.INSTANCE;

        r.prepareBlur(Minecraft.getInstance(), 30f, 0.75f);

        px = Math.max(0, Math.min(px, this.width  - pw));
        py = Math.max(0, Math.min(py, this.height - ph));

        int ipx = (int)px, ipy = (int)py, ipw = (int)pw, iph = (int)ph;
        int cx  = ipx + SIDEBAR_W;
        int cw  = ipw - SIDEBAR_W;
        int by  = ipy + TOPBAR_H;
        int bh  = iph - TOPBAR_H;

        float targetSelY = catY(active);
        animSelY += (targetSelY - animSelY) * 0.18f;

        r.drawRectBlurred(ipx, ipy, ipw, iph, PANEL_R, C_PANEL_TINT, 20f);

        r.drawStroke(ipx,     ipy,     ipw,     iph,     PANEL_R,     1f, 1, 0f, C_GLASS_OUT);
        r.drawStroke(ipx + 1, ipy + 1, ipw - 2, iph - 2, PANEL_R - 1, 1f, 1, 0f, C_GLASS_IN);

        r.drawRect(cx, ipy, 1, iph, 0, C_DIV);
        r.drawRect(cx, by,  cw, 1,  0, C_DIV);

        gfx.enableScissor(ipx, ipy, cx, ipy + iph);
        renderSidebar(r, ipx, ipy, iph, mx, my);
        gfx.disableScissor();

        renderTopbar(r, gfx, cx, ipy, cw);

        int modX = cx + 1;
        int modW = selected != null ? 200 : cw - 1;
        gfx.enableScissor(modX, by + 1, modX + modW, ipy + iph);
        renderModules(r, gfx, modX, by, modW, mx, my, by + 1, modX + modW, ipy + iph);
        gfx.disableScissor();

        if (selected != null) {
            int sx = modX + modW;
            int sw = cw - 1 - modW;
            r.drawRect(sx, by, 1, bh, 0, C_DIV);
            gfx.enableScissor(sx + 1, by + 1, sx + 1 + sw, ipy + iph);
            renderSettings(r, sx + 1, by, sw);
            gfx.disableScissor();
        }
    }

    private void renderSidebar(AporiaRenderer r, int sx, int sy, int sh, int mx, int my) {
        Category[] cats = Category.values();
        for (Category cat : cats) {
            int cy   = (int)catY(cat);
            boolean act = cat == active;
            boolean hov = mx >= sx && mx < sx + SIDEBAR_W && my >= cy && my < cy + CAT_H;

            Animator anim = hoverAnims.get(cat);
            if (anim != null) {
                if (hov  && !anim.isPlaying() && anim.value() < 0.99f) anim.play();
                if (!hov && !anim.isPlaying() && anim.value() > 0.01f) anim.reverse();
                anim.update();
            }

            int col = act ? C_CAT_ACT : (hov ? C_CAT_HOV : C_CAT_TXT);

            float iconSize = 13f;
            float txtSize  = 9f;
            float startX   = sx + 20f;

            r.drawText(Fonts.CATICONS, String.valueOf(cat.icon), startX, cy + (CAT_H - iconSize) / 2f, iconSize, col);
            r.drawText("regular", capitalize(cat.name()), startX + iconSize + 6f, cy + (CAT_H - txtSize) / 2f, txtSize, col);

            float prog = anim != null ? anim.value() : (act ? 1f : 0f);
            if (prog > 0.01f) {
                float lineY   = cy + CAT_H - 2f;
                float centerX = sx + SIDEBAR_W / 2f;
                float halfLen = SIDEBAR_W / 2f - CAT_PAD;
                int   lineCol = act
                    ? ColorUtil.rgba(190, 210, 255, 170)
                    : ColorUtil.rgba(140, 165, 215, 110);
                r.drawFadeHLine(centerX, lineY, halfLen, 1.5f, prog, lineCol);
            }
        }
    }

    private void renderTopbar(AporiaRenderer r, GuiGraphics gfx, int tx, int ty, int tw) {
        r.drawText("bold", capitalize(active.name()),
            tx + PAD, ty + (TOPBAR_H - 11) / 2f, 11f, C_TOPBAR_TXT);

        int sfx = tx + tw - SEARCH_W - PAD;
        int sfy = ty + (TOPBAR_H - SEARCH_H) / 2;
        int sr  = SEARCH_H / 2;
        r.drawRect(sfx, sfy, SEARCH_W, SEARCH_H, sr, C_SRCH_BG);
        r.drawStroke(sfx, sfy, SEARCH_W, SEARCH_H, sr, 1f, 1, 0f,
            searchFocused ? C_SRCH_BDR_F : C_SRCH_BDR);

        gfx.enableScissor(sfx + 6, sfy, sfx + SEARCH_W - 6, sfy + SEARCH_H);
        if (search.isEmpty() && !searchFocused) {
            r.drawText("regular", "Search...", sfx + 8, sfy + (SEARCH_H - 9) / 2f, 9f, C_SRCH_PH);
        } else {
            String cur = search + (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0 ? "|" : "");
            r.drawText("regular", cur, sfx + 8, sfy + (SEARCH_H - 9) / 2f, 9f, C_SRCH_TXT);
        }
        gfx.disableScissor();
    }

    private void renderModules(AporiaRenderer r, GuiGraphics gfx, int mx2, int my2, int mw, int hx, int hy,
                               int scY1, int scX2, int scY2) {
        List<Module> mods = filtered();
        int cols = 2;
        int colW = (mw - PAD * (cols + 1)) / cols;
        int col  = 0;
        int rowY = my2 + PAD;

        for (Module m : mods) {
            int cardX = mx2 + PAD + col * (colW + PAD);
            int y     = rowY;
            boolean on  = m.isEnabled();
            boolean hov = hx >= cardX && hx < cardX + colW && hy >= y && hy < y + CARD_H;
            boolean sel = m == selected;

            Animator ha = modHoverAnims.computeIfAbsent(m, k -> new Animator(180, Easing::cubicOut));
            if (hov && !ha.isPlaying() && ha.value() < 0.99f) ha.play();
            if (!hov && !ha.isPlaying() && ha.value() > 0.01f) ha.reverse();
            ha.update();
            float hp = ha.value();

            r.drawRectBlurred(cardX, y, colW, CARD_H, CARD_R,
                sel ? C_CARD_ON : (hov ? C_CARD_HOV : C_CARD_TINT), 8f);

            int outlineAlpha = (int)(20 + 50 * hp) + (on ? 60 : 0);
            int outlineColor = ColorUtil.rgba(255, 255, 255, outlineAlpha);
            r.drawStroke(cardX, y, colW, CARD_H, CARD_R, 1f, 1, 0f, outlineColor);

            r.drawText("regular", m.name(),
                cardX + PAD, y + (CARD_H - 9) / 2f, 9f, on ? C_TXT_ON : C_TXT);

            col++;
            if (col >= cols) { col = 0; rowY += CARD_H + CARD_GAP; }
        }
    }

    private void renderSettings(AporiaRenderer r, int sx, int sy, int sw) {
        if (selected == null) return;
        r.drawText("bold", selected.name(), sx + PAD, sy + PAD, 8f, C_TXT_ON);
        r.drawText("regular", "Right-click to close", sx + PAD, sy + PAD + 14, 6f, C_TXT_DIM);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean b) {
        int mx = (int)e.x(), my = (int)e.y();
        int ipx = (int)px, ipy = (int)py, ipw = (int)pw, iph = (int)ph;
        int bodyY = ipy + TOPBAR_H;

        if (e.button() == 0) {
            int sfx = ipx + ipw - SEARCH_W - PAD + SIDEBAR_W;
            int cx  = ipx + SIDEBAR_W;
            int cw  = ipw - SIDEBAR_W;
            sfx = cx + cw - SEARCH_W - PAD;
            int sfy = ipy + (TOPBAR_H - SEARCH_H) / 2;
            if (mx >= sfx && mx < sfx + SEARCH_W && my >= sfy && my < sfy + SEARCH_H) {
                searchFocused = true; return true;
            }
            searchFocused = false;

            boolean el = mx >= ipx - HIT && mx < ipx + HIT;
            boolean er = mx >= ipx + ipw - HIT && mx < ipx + ipw + HIT;
            boolean et = my >= ipy - HIT && my < ipy + HIT;
            boolean eb = my >= ipy + iph - HIT && my < ipy + iph + HIT;
            if ((el || er || et || eb) && mx >= ipx - HIT && mx <= ipx + ipw + HIT
                    && my >= ipy - HIT && my <= ipy + iph + HIT) {
                resizing = true;
                resizeL = el; resizeR = er; resizeT = et; resizeB = eb;
                rStartMx = mx; rStartMy = my;
                rStartPx = px; rStartPy = py; rStartPw = pw; rStartPh = ph;
                return true;
            }

            if (mx >= cx && mx < ipx + ipw && my >= ipy && my < bodyY) {
                dragging = true; dox = mx - px; doy = my - py; return true;
            }

            for (Category cat : Category.values()) {
                int cy = (int)catY(cat);
                if (mx >= ipx && mx < ipx + SIDEBAR_W && my >= cy && my < cy + CAT_H) {
                    active = cat; search = ""; selected = null; return true;
                }
            }

            int modX = cx + 1;
            int modW = selected != null ? 200 : ipw - SIDEBAR_W - 1;
            int cols = 2;
            int colW = (modW - PAD * (cols + 1)) / cols;
            int col2 = 0, rowY = bodyY + PAD;
            for (Module m : filtered()) {
                int cardX = modX + PAD + col2 * (colW + PAD);
                if (mx >= cardX && mx < cardX + colW && my >= rowY && my < rowY + CARD_H) {
                    m.toggle(); return true;
                }
                col2++;
                if (col2 >= cols) { col2 = 0; rowY += CARD_H + CARD_GAP; }
            }
        }

        if (e.button() == 1) {
            int cx  = (int)px + SIDEBAR_W + 1;
            int modW = selected != null ? 200 : (int)pw - SIDEBAR_W - 1;
            int y = (int)py + TOPBAR_H + PAD;
            for (Module m : filtered()) {
                int cardX = cx + PAD, cardW = modW - PAD * 2;
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
        float mx = (float)e.x(), my = (float)e.y();
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
        if (searchFocused) { search += (char)e.codepoint(); return true; }
        return super.charTyped(e);
    }

    /** Y position of a category row in screen coords */
    private float catY(Category cat) {
        Category[] cats = Category.values();
        int CAT_GAP = 2;
        int totalH = cats.length * CAT_H + (cats.length - 1) * CAT_GAP;
        int startY = (int)py + ((int)ph - totalH) / 2;
        for (int i = 0; i < cats.length; i++)
            if (cats[i] == cat) return startY + i * (CAT_H + CAT_GAP);
        return startY;
    }

    private List<Module> filtered() {
        List<Module> base = ModuleManager.INSTANCE.getByCategory(active);
        if (search.isEmpty()) return base;
        List<Module> out = new ArrayList<>();
        String q = search.toLowerCase();
        for (Module m : base) if (m.name().toLowerCase().contains(q)) out.add(m);
        return out;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    private static String keyName(int key) {
        return net.minecraft.client.KeyMapping.createNameSupplier("key.keyboard." + key)
            .get().getString().toUpperCase();
    }

    @Override public boolean isPauseScreen()     { return false; }
    @Override public boolean isAllowedInPortal() { return true; }
    @Override public void renderBackground(GuiGraphics gfx, int mx, int my, float d) {}
}
