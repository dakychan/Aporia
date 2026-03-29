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
    private static final int LINE_H    = 20;

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
    private static final int C_SET_BG      = ColorUtil.rgba(255, 255, 255,  10);
    private static final int C_SET_H       = ColorUtil.rgba(255, 255, 255,  20);

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
    
    private SettingsPopup settingsPopup = null;
    private Animator guiLoadAnim = null;
    private Animator guiOpenAnim = null;
    private Animator contentAnim = null;
    private long guiOpenTime = 0;
    private boolean loadingShown = false;
    private boolean guiInitialized = false;
    
    private long lastBlurInvalidateTime = 0;
    private static final long BLUR_INVALIDATE_THROTTLE_MS = 100;

    public ClickGuiScreen() { super(Component.literal("ClickGui")); }

    @Override
    protected void init() {
        if (pw == 0) {
            pw = Math.max(MIN_W, this.width  * W_FRAC);
            ph = Math.max(MIN_H, this.height * H_FRAC);
            px = (this.width  - pw) / 2f;
            py = (this.height - ph) / 2f;
            guiOpenAnim = Animator.slide(600);
            guiOpenAnim.play();
            contentAnim = new Animator(800, Easing::sineInOut);
            contentAnim.play();
            guiLoadAnim = new Animator(600, Easing::sineInOut);
            guiLoadAnim.play();
            guiOpenTime = System.currentTimeMillis();
            loadingShown = false;
            guiInitialized = false;
            AporiaRenderer.INSTANCE.invalidateBlurCache();
        }
        for (Category c : Category.values())
            hoverAnims.computeIfAbsent(c, k -> new Animator(200, Easing::cubicOut));
        if (animSelY < 0) animSelY = catY(active);
        AporiaRenderer.INSTANCE.resetDebugFlags();
    }

    @Override
    public void render(GuiGraphics gfx, int mx, int my, float delta) {
        AporiaRenderer r = AporiaRenderer.INSTANCE;
        r.prepareFrameBlur(Minecraft.getInstance(), 30f, 0.75f);

        px = Math.max(0, Math.min(px, this.width  - pw));
        py = Math.max(0, Math.min(py, this.height - ph));

        int ipx = (int)px, ipy = (int)py, ipw = (int)pw, iph = (int)ph;
        int cx  = ipx + SIDEBAR_W;
        int cw  = ipw - SIDEBAR_W;
        int by  = ipy + TOPBAR_H;
        int bh  = iph - TOPBAR_H;

        float targetSelY = catY(active);
        animSelY += (targetSelY - animSelY) * 0.18f;

        guiOpenAnim.update();
        float openProg = guiOpenAnim.value();
        
        float scale = openProg;
        int scaledW = (int)(ipw * scale);
        int scaledH = (int)(iph * scale);
        int scaledX = ipx + (ipw - scaledW) / 2;
        int scaledY = ipy + (iph - scaledH) / 2;

        r.drawRectBlurred(scaledX, scaledY, scaledW, scaledH, PANEL_R, C_PANEL_TINT, 20f);
        r.drawStroke(scaledX,     scaledY,     scaledW,     scaledH,     PANEL_R,     1f, 1, 0f, C_GLASS_OUT);
        r.drawStroke(scaledX + 1, scaledY + 1, scaledW - 2, scaledH - 2, PANEL_R - 1, 1f, 1, 0f, C_GLASS_IN);

        r.drawRect(cx, ipy, 1, iph, 0, C_DIV);
        r.drawRect(cx, by,  cw, 1,  0, C_DIV);

        guiLoadAnim.update();
        float loadProg = guiLoadAnim.value();
        
        contentAnim.update();
        float contentAlpha = contentAnim.value();
        
        boolean isLoading = contentAlpha < 1f && !loadingShown && !guiInitialized;
        
        if (isLoading) {
            int offsetX = (int)(loadProg * 30f);
            r.drawText("bold", "Загружаю", ipx + ipw / 2f - 40 + offsetX, ipy + iph / 2f - 20, 12f, C_TXT_ON);
            renderLoadingDots(r, ipx + ipw / 2f + 20 + offsetX, ipy + iph / 2f - 20);
            
            gfx.enableScissor(scaledX, scaledY, scaledX + scaledW, scaledY + scaledH);
            renderSidebar(r, scaledX, scaledY, scaledH, mx, my, contentAlpha);
            gfx.disableScissor();
        } else {
            if (!guiInitialized) {
                guiInitialized = true;
                loadingShown = true;
            }
            
            if (contentAlpha > 0.01f) {
                gfx.enableScissor(scaledX, scaledY, scaledX + scaledW, scaledY + scaledH);
                renderSidebar(r, scaledX, scaledY, scaledH, mx, my, contentAlpha);
                renderProfile(r, gfx, ipx, ipy, ipw, iph, contentAlpha);
                gfx.disableScissor();

                float modAlpha = Math.max(0, (contentAlpha - 0.3f) / 0.7f);
                if (modAlpha > 0.01f) {
                    renderTopbar(r, gfx, cx, scaledY, cw, modAlpha);

                    int modX = cx + 1;
                    int modW = cw - 1;
                    gfx.enableScissor(modX, by + 1, modX + modW, scaledY + scaledH);
                    renderModules(r, gfx, modX, by, modW, mx, my, by + 1, modX + modW, scaledY + scaledH, modAlpha);
                    gfx.disableScissor();
                }
            }
        }
        if (settingsPopup != null) {
            settingsPopup.render(r, gfx, mx, my, this.width, this.height);
        }
    }

    private void renderLoadingDots(AporiaRenderer r, float x, float y) {
        long elapsed = System.currentTimeMillis() - guiOpenTime;
        float cycle = (elapsed % 1500) / 1500f;
        
        for (int i = 0; i < 3; i++) {
            float alpha = 0.3f + 0.7f * Math.max(0, (float)Math.sin((cycle - i * 0.15f) * (float)Math.PI));
            r.drawText("regular", ".", x + i * 8, y, 10f, ColorUtil.rgba(255, 255, 255, (int)(alpha * 255)));
        }
    }

    private void renderProfile(AporiaRenderer r, GuiGraphics gfx, int px, int py, int pw, int ph, float alpha) {
        if (alpha < 0.01f) return;

        try {
            var discordRPC = ModuleManager.INSTANCE.get("Discord RPC");
            if (discordRPC == null || !discordRPC.isEnabled()) return;

            var discordModule = (so.aporia.module.impl.misc.DiscordRPCModule) discordRPC;
            
            discordModule.loadAvatarOnRenderThread();
            
            var discordUser = discordModule.getDiscordUser();
            if (discordUser == null) return;

            int profileH = 50;
            int profileX = px + 8;
            int profileY = py + ph - profileH - 18 + 15 + 2 + 3 + 2;
            int profileW = SIDEBAR_W - 16;

            int divCol = ColorUtil.rgba(255, 255, 255, (int)(16 * alpha));
            r.drawRect(px, profileY - 1, SIDEBAR_W, 2, 0, divCol);

            int avatarX = profileX + 6;
            int avatarY = profileY + 6;
            int avatarSize = 32;
            
            var avatarId = discordModule.getAvatarId();
            if (avatarId != null) {
                r.drawImage(avatarX, avatarY, avatarSize, avatarSize, avatarId, 5);
            } else {
                r.drawRect(avatarX, avatarY, avatarSize, avatarSize, 4, ColorUtil.rgba(100, 100, 100, (int)(100 * alpha)));
            }

            String username = discordUser.username();
            int textX = avatarX + avatarSize + 8;
            int textY = profileY + 8;
            r.drawText("bold", username, textX, textY, 9f, ColorUtil.rgba(255, 255, 255, (int)(255 * alpha)));

            String uuid = aporia.cc.UserData.getUserUUID(username);
            r.drawText("regular", uuid, textX, textY + 14, 7f, ColorUtil.rgba(255, 255, 255, (int)(140 * alpha)));

        } catch (Exception e) {
        }
    }

    private void renderSidebar(AporiaRenderer r, int sx, int sy, int sh, int mx, int my, float alpha) {
        Category[] cats = Category.values();
        for (int catIdx = 0; catIdx < cats.length; catIdx++) {
            Category cat = cats[catIdx];
            int cy   = (int)catY(cat);
            boolean act = cat == active;
            boolean hov = mx >= sx && mx < sx + SIDEBAR_W && my >= cy && my < cy + CAT_H;

            Animator anim = hoverAnims.get(cat);
            if (anim != null) {
                if (hov  && !anim.isPlaying() && anim.value() < 0.99f) anim.play();
                if (!hov && !anim.isPlaying() && anim.value() > 0.01f) anim.reverse();
                anim.update();
            }

            float staggerDelay = catIdx * 0.18f;
            float catAlpha = Math.max(0, Math.min(1f, (alpha - staggerDelay) / (1f - staggerDelay)));
            
            if (catAlpha < 0.01f) continue;
            
            int col = act ? C_CAT_ACT : (hov ? C_CAT_HOV : C_CAT_TXT);
            col = ColorUtil.rgba((col >> 16) & 0xFF, (col >> 8) & 0xFF, col & 0xFF, (int)(((col >> 24) & 0xFF) * catAlpha));

            float iconSize = 13f;
            float txtSize  = 9f;
            float startX   = sx + 20f;
            float offsetX = (1f - catAlpha) * -150f;

            r.drawText(Fonts.CATICONS, String.valueOf(cat.icon), startX + offsetX, cy + (CAT_H - iconSize) / 2f, iconSize, col);
            r.drawText("regular", capitalize(cat.name()), startX + iconSize + 6f + offsetX, cy + (CAT_H - txtSize) / 2f, txtSize, col);

            float prog = anim != null ? anim.value() : (act ? 1f : 0f);
            if (prog > 0.01f && catAlpha > 0.01f) {
                float lineY   = cy + CAT_H - 2f;
                float centerX = sx + SIDEBAR_W / 2f + 25f;
                float halfLen = SIDEBAR_W / 2.5f - CAT_PAD;
                int   lineCol = ColorUtil.rgba(255, 255, 255, (int)(60 * prog * catAlpha));
                r.drawFadeHLine(centerX, lineY, halfLen, 1.0f, prog * 0.7f, lineCol);
            }
        }
    }

    private void renderTopbar(AporiaRenderer r, GuiGraphics gfx, int tx, int ty, int tw, float alpha) {
        r.drawText("bold", capitalize(active.name()),
                tx + PAD, ty + (TOPBAR_H - 11) / 2f, 11f, ColorUtil.rgba(255, 255, 255, (int)(255 * alpha)));

        int sfx = tx + tw - SEARCH_W - PAD;
        int sfy = ty + (TOPBAR_H - SEARCH_H) / 2;
        int sr  = SEARCH_H / 2;
        int bgCol = ColorUtil.rgba((C_SRCH_BG >> 16) & 0xFF, (C_SRCH_BG >> 8) & 0xFF, C_SRCH_BG & 0xFF, (int)(((C_SRCH_BG >> 24) & 0xFF) * alpha));
        r.drawRect(sfx, sfy, SEARCH_W, SEARCH_H, sr, bgCol);
        int bdrCol = ColorUtil.rgba(255, 255, 255, (int)(40 * alpha));
        r.drawStroke(sfx, sfy, SEARCH_W, SEARCH_H, sr, 1f, 1, 0f, bdrCol);

        gfx.enableScissor(sfx + 6, sfy, sfx + SEARCH_W - 6, sfy + SEARCH_H);
        if (search.isEmpty() && !searchFocused) {
            r.drawText("regular", "Search...", sfx + 8, sfy + (SEARCH_H - 9) / 2f, 9f, ColorUtil.rgba(255, 255, 255, (int)(80 * alpha)));
        } else {
            String cur = search + (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0 ? "|" : "");
            r.drawText("regular", cur, sfx + 8, sfy + (SEARCH_H - 9) / 2f, 9f, ColorUtil.rgba(255, 255, 255, (int)(220 * alpha)));
        }
        gfx.disableScissor();
    }

    private void renderModules(AporiaRenderer r, GuiGraphics gfx, int mx2, int my2, int mw, int hx, int hy,
                               int scY1, int scX2, int scY2, float contentAlpha) {
        List<Module> mods = filtered();
        int cols = 2;
        int colW = (mw - PAD * (cols + 1)) / cols;
        int col  = 0;
        int rowY = my2 + PAD;

        for (int modIdx = 0; modIdx < mods.size(); modIdx++) {
            Module m = mods.get(modIdx);
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

            float staggerDelay = modIdx * 0.05f;
            float modAlpha = Math.max(0, Math.min(1f, (contentAlpha - staggerDelay) / (1f - staggerDelay)));
            float offsetY = (1f - modAlpha) * -20f;

            int tintAlpha = (int)(12 * modAlpha);
            int hovAlpha = (int)(28 * modAlpha);
            int onAlpha = (int)(40 * modAlpha);
            int cardTint = sel ? ColorUtil.rgba(255, 255, 255, onAlpha) : (hov ? ColorUtil.rgba(255, 255, 255, hovAlpha) : ColorUtil.rgba(255, 255, 255, tintAlpha));
            
            r.drawRectBlurred(cardX, (int)(y + offsetY), colW, CARD_H, CARD_R, cardTint, 8f);

            int outlineAlpha = (int)((20 + 50 * hp) * modAlpha) + (on ? (int)(60 * modAlpha) : 0);
            int outlineColor = ColorUtil.rgba(255, 255, 255, outlineAlpha);
            r.drawStroke(cardX, (int)(y + offsetY), colW, CARD_H, CARD_R, 1f, 1, 0f, outlineColor);

            r.drawText("regular", m.name(),
                    cardX + PAD, (int)(y + offsetY + (CARD_H - 9) / 2f - 1), 9f, ColorUtil.rgba(255, 255, 255, (int)((on ? 255 : 180) * modAlpha)));

            col++;
            if (col >= cols) { col = 0; rowY += CARD_H + CARD_GAP; }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean b) {
        int mx = (int)e.x(), my = (int)e.y();
        int ipx = (int)px, ipy = (int)py, ipw = (int)pw, iph = (int)ph;
        int bodyY = ipy + TOPBAR_H;

        if (settingsPopup != null) {
            boolean consumed = settingsPopup.mouseClicked(mx, my, this.width, this.height, e.button());
            if (consumed) {
                return true;
            }
            settingsPopup.close();
            settingsPopup = null;
            invalidateBlurThrottled();
            return true;
        }

        if (e.button() == 0) {
            int cx  = ipx + SIDEBAR_W;
            int cw  = ipw - SIDEBAR_W;
            int sfx = cx + cw - SEARCH_W - PAD;
            int sfy = ipy + (TOPBAR_H - SEARCH_H) / 2;
            if (mx >= sfx && mx < sfx + SEARCH_W && my >= sfy && my < sfy + SEARCH_H) {
                searchFocused = true; return true;
            }
            searchFocused = false;

            if (mx >= cx && mx < ipx + ipw && my >= ipy && my < bodyY) {
                dragging = true; dox = mx - px; doy = my - py; return true;
            }

            for (Category cat : Category.values()) {
                int cy = (int)catY(cat);
                if (mx >= ipx && mx < ipx + SIDEBAR_W && my >= cy && my < cy + CAT_H) {
                    active = cat; search = ""; selected = null;
                    invalidateBlurThrottled();
                    return true;
                }
            }

            int modX = cx + 1;
            int modW = cw - 1;
            int cols = 2;
            int colW = (modW - PAD * (cols + 1)) / cols;
            int col2 = 0, rowY = bodyY + PAD;
            for (Module m : filtered()) {
                int cardX = modX + PAD + col2 * (colW + PAD);
                if (mx >= cardX && mx < cardX + colW && my >= rowY && my < rowY + CARD_H) {
                    m.toggle(); 
                    invalidateBlurThrottled();
                    return true;
                }
                col2++;
                if (col2 >= cols) { col2 = 0; rowY += CARD_H + CARD_GAP; }
            }
        }

        if (e.button() == 1) {
            int cx  = (int)px + SIDEBAR_W + 1;
            int modW = (int)pw - SIDEBAR_W - 1;
            int y = (int)py + TOPBAR_H + PAD;
            int cols = 2;
            int colW = (modW - PAD * (cols + 1)) / cols;
            int col2 = 0;
            for (Module m : filtered()) {
                int cardX = cx + PAD + col2 * (colW + PAD);
                if (mx >= cardX && mx < cardX + colW && my >= y && my < y + CARD_H) {
                    if (settingsPopup != null && settingsPopup.getModule() == m) {
                        settingsPopup.close();
                        settingsPopup = null;
                        invalidateBlurThrottled();
                    } else {
                        if (settingsPopup != null) {
                            settingsPopup.close();
                        }
                        settingsPopup = new SettingsPopup(m);
                        invalidateBlurThrottled();
                    }
                    return true;
                }
                col2++;
                if (col2 >= cols) { col2 = 0; y += CARD_H + CARD_GAP; }
            }
        }

        return super.mouseClicked(e, b);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        float mx = (float)e.x(), my = (float)e.y();
        if (dragging) { px = mx - dox; py = my - doy; return true; }
        return super.mouseDragged(e, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent e) {
        dragging = false;
        return super.mouseReleased(e);
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        if (e.key() == 256) {
            if (settingsPopup != null) {
                settingsPopup.close();
                settingsPopup = null;
                AporiaRenderer.INSTANCE.invalidateBlurCache();
                return true;
            }
        }
        if (settingsPopup != null && settingsPopup.keyPressed(e.key())) {
            return true;
        }
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
        int CAT_GAP = 0;
        int totalH = cats.length * CAT_H + (cats.length - 1) * CAT_GAP;
        
        var discordRPC = ModuleManager.INSTANCE.get("Discord RPC");
        int offset = (discordRPC != null && discordRPC.isEnabled()) ? -20 : 3;
        
        int startY = (int)py + ((int)ph - totalH) / 2 + offset;
        for (int i = 0; i < cats.length; i++)
            if (cats[i] == cat) return startY + i * (CAT_H + CAT_GAP);
        return startY;
    }

    private List<Module> filteredCache = null;
    private String lastSearchQuery = "";
    private Category lastActiveCategory = null;
    
    private List<Module> filtered() {
        List<Module> base = ModuleManager.INSTANCE.getByCategory(active);
        
        if (active != lastActiveCategory || !search.equals(lastSearchQuery)) {
            lastActiveCategory = active;
            lastSearchQuery = search;
            
            if (search.isEmpty()) {
                filteredCache = base;
            } else {
                filteredCache = new ArrayList<>();
                String q = search.toLowerCase();
                for (Module m : base) if (m.name().toLowerCase().contains(q)) filteredCache.add(m);
            }
        }
        
        return filteredCache != null ? filteredCache : base;
    }

    private void invalidateBlurThrottled() {
        long now = System.currentTimeMillis();
        if (now - lastBlurInvalidateTime >= BLUR_INVALIDATE_THROTTLE_MS) {
            AporiaRenderer.INSTANCE.invalidateBlurCache();
            lastBlurInvalidateTime = now;
        }
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

    @Override
    public void onClose() {
        AporiaRenderer.INSTANCE.invalidateBlurCache();
        super.onClose();
    }
}
