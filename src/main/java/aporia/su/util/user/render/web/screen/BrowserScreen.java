package aporia.su.util.user.render.web.screen;

import aporia.cc.OsManager;
import aporia.su.util.user.render.web.WebRendererConfig;
import aporia.su.util.user.render.web.WebRendererInstance;
import aporia.su.util.user.render.web.WebRendererManager;
import aporia.su.util.files.FilesManager;
import aporia.su.util.interfaces.IMinecraft;
import aporia.su.util.user.render.Render2D;
import aporia.su.util.user.render.font.Fonts;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Fullscreen in-game browser with a floating, draggable tab bar.
 * Tabs are persisted to {@code browser.apr} on close.
 */
public class BrowserScreen extends Screen implements IMinecraft {

    private static final int   BAR_H     = 26;
    private static final int   TAB_H     = 20;
    private static final int   TAB_MAX_W = 160;
    private static final int   TAB_MIN_W = 60;
    private static final int   PLUS_W    = 22;
    private static final int   CLOSE_W   = 12;
    private static final float FONT_SIZE = 6.5f;

    private static final int C_BAR        = new Color(12, 12, 16, 230).getRGB();
    private static final int C_TAB_ACTIVE = new Color(30, 30, 44, 255).getRGB();
    private static final int C_TAB_HOVER  = new Color(22, 22, 32, 255).getRGB();
    private static final int C_TAB_IDLE   = new Color(16, 16, 24, 200).getRGB();
    private static final int C_ACCENT     = new Color(90, 90, 220, 255).getRGB();
    private static final int C_TEXT       = new Color(215, 215, 225, 255).getRGB();
    private static final int C_TEXT_DIM   = new Color(110, 110, 130, 255).getRGB();
    private static final int C_CLOSE_HOV  = new Color(220, 60, 60, 255).getRGB();
    private static final int C_OUTLINE    = new Color(40, 40, 58, 180).getRGB();

    private static final Path   CONFIG_DIR  = OsManager.mainDirectory.resolve("configs");
    private static final String CONFIG_NAME = "browser";

    private final WebRendererManager rendererManager = WebRendererManager.INSTANCE;
    private final List<Tab> tabs = new ArrayList<>();
    private int activeTab = 0;

    private float barX, barY;
    private boolean draggingBar = false;
    private double dragStartMx, dragStartMy;
    private float  dragStartBx, dragStartBy;

    private boolean editingUrl = false;
    private String  urlBuf     = "";
    private int     urlCursor  = 0;

    private double mx, my;

    public BrowserScreen(String url) {
        super(Text.literal("Browser"));
        loadState();
        if (tabs.isEmpty()) tabs.add(new Tab(url));
    }

    @Override
    protected void init() {
        rendererManager.initialize();
        if (barX == 0 && barY == 0) {
            barX = (width - Math.min(TAB_MAX_W * tabs.size() + PLUS_W + 10, width * 0.7f)) / 2f;
            barY = 2;
        }
        for (Tab tab : tabs) tab.ensureInstance(rendererManager, width, height);
        focusActive();
    }

    @Override
    public void removed() {
        super.removed();
        saveState();
        for (Tab tab : tabs) tab.close();
        tabs.clear();
        rendererManager.shutdown();
    }

    @Override public boolean shouldPause() { return false; }


    /** Total height occupied by the floating bar + url row below it. */
    private static final int URL_H    = 14;
    private static final int URL_PAD  = 3;

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        mx = mouseX; my = mouseY;

        for (Tab tab : tabs) tab.updateAnim(delta);

        Tab active = activeTab();
        if (active != null && active.instance != null) {
            active.instance.render(ctx, 0, 0, width, height, delta);
        } else {
            Render2D.rect(0, 0, width, height, new Color(10, 10, 14, 255).getRGB());
            if (!rendererManager.hasRealEngine()) {
                Fonts.REGULAR.draw("MCEF not installed", 12, 12, FONT_SIZE, C_TEXT_DIM);
            }
        }

        renderTabBar();
        renderUrlBar();
    }

    private void renderTabBar() {
        int tabCount = tabs.size();
        float tabW   = tabWidth(tabCount);
        float totalW = tabBarWidth(tabCount, tabW);

        Render2D.rect(barX, barY, totalW, BAR_H, C_BAR, 6f);
        Render2D.outline(barX, barY, totalW, BAR_H, 0.5f, C_OUTLINE, 6f);

        float tx = barX + 2;
        for (int i = 0; i < tabCount; i++) {
            Tab tab = tabs.get(i);
            boolean isActive = (i == activeTab);
            boolean hovered  = isTabHovered(tx, tabW);
            float   anim     = tab.animValue;

            int bg = isActive ? C_TAB_ACTIVE : (hovered ? C_TAB_HOVER : C_TAB_IDLE);
            float tabY  = barY + (BAR_H - TAB_H) / 2f;
            float drawW = tabW * anim;
            float drawH = TAB_H * anim;
            float drawY = tabY + (TAB_H - drawH) / 2f;

            Render2D.rect(tx, drawY, drawW, drawH, bg, 4f);

            if (isActive) {
                Render2D.rect(tx + 2, barY + BAR_H - 3, (tabW - 4) * anim, 2, C_ACCENT, 1f);
            }

            if (anim > 0.4f) {
                float cx = tx + tabW - CLOSE_W - 3;
                float cy = barY + (BAR_H - CLOSE_W) / 2f;
                boolean closeHov = mx >= cx && mx < cx + CLOSE_W && my >= cy && my < cy + CLOSE_W;
                Fonts.BOLD.drawCentered("×", cx + CLOSE_W / 2f,
                    barY + (BAR_H - Fonts.BOLD.getHeight(8f)) / 2f, 8f,
                    closeHov ? C_CLOSE_HOV : C_TEXT_DIM);

                String title = tab.displayTitle();
                float maxTW = tabW - CLOSE_W - 10;
                while (title.length() > 1 && Fonts.REGULAR.getWidth(title, FONT_SIZE) > maxTW)
                    title = title.substring(0, title.length() - 1);
                if (!tab.displayTitle().equals(title)) title += "…";
                Fonts.REGULAR.draw(title, tx + 5,
                    barY + (BAR_H - Fonts.REGULAR.getHeight(FONT_SIZE)) / 2f, FONT_SIZE,
                    isActive ? C_TEXT : C_TEXT_DIM);
            }

            tx += tabW + 1;
        }

        float plusX = barX + totalW - PLUS_W - 2;
        float plusY = barY + (BAR_H - TAB_H) / 2f;
        boolean plusHov = mx >= plusX && mx < plusX + PLUS_W && my >= plusY && my < plusY + TAB_H;
        Render2D.rect(plusX, plusY, PLUS_W, TAB_H, plusHov ? C_TAB_HOVER : C_TAB_IDLE, 4f);
        Fonts.BOLD.drawCentered("+", plusX + PLUS_W / 2f,
            barY + (BAR_H - Fonts.BOLD.getHeight(8f)) / 2f, 8f,
            plusHov ? C_TEXT : C_TEXT_DIM);
    }

    /** URL bar rendered below the tab bar, centered under it. */
    private void renderUrlBar() {
        int tabCount = tabs.size();
        float tabW   = tabWidth(tabCount);
        float totalW = tabBarWidth(tabCount, tabW);

        float urlW = Math.min(totalW, Math.max(160f, totalW * 0.7f));
        float urlX = barX + (totalW - urlW) / 2f;
        float urlY = barY + BAR_H + URL_PAD;

        boolean urlHov = mx >= urlX && mx < urlX + urlW && my >= urlY && my < urlY + URL_H;
        int urlBg = editingUrl ? new Color(18, 18, 30, 255).getRGB()
                               : (urlHov ? C_TAB_HOVER : new Color(12, 12, 18, 210).getRGB());
        Render2D.rect(urlX, urlY, urlW, URL_H, urlBg, 4f);
        Render2D.outline(urlX, urlY, urlW, URL_H, 0.5f, editingUrl ? C_ACCENT : C_OUTLINE, 4f);

        String display = editingUrl ? urlBuf : currentUrl();
        while (display.length() > 1 && Fonts.REGULAR.getWidth(display, 5f) > urlW - 10)
            display = display.substring(0, display.length() - 1);
        Fonts.REGULAR.draw(display, urlX + 5, urlY + (URL_H - Fonts.REGULAR.getHeight(5f)) / 2f,
            5f, editingUrl ? C_TEXT : C_TEXT_DIM);
    }


    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double cmx = click.x(), cmy = click.y();
        int btn = click.button();

        if (isInBar(cmx, cmy) && (btn == 2 || btn == 1)) {
            draggingBar = true;
            dragStartMx = cmx; dragStartMy = cmy;
            dragStartBx = barX; dragStartBy = barY;
            return true;
        }

        if (isInBar(cmx, cmy) && btn == 0) {
            editingUrl = false;
            int tabCount = tabs.size();
            float tabW = tabWidth(tabCount);
            float totalW = tabBarWidth(tabCount, tabW);

            float plusX = barX + totalW - PLUS_W - 2;
            float plusY = barY + (BAR_H - TAB_H) / 2f;
            if (cmx >= plusX && cmx < plusX + PLUS_W && cmy >= plusY && cmy < plusY + TAB_H) {
                openNewTab("https://google.com");
                return true;
            }

            float tx = barX + 2;
            for (int i = 0; i < tabCount; i++) {
                if (cmx >= tx && cmx < tx + tabW) {
                    float cx = tx + tabW - CLOSE_W - 3;
                    float cy = barY + (BAR_H - CLOSE_W) / 2f;
                    if (cmx >= cx && cmx < cx + CLOSE_W && cmy >= cy && cmy < cy + CLOSE_W) {
                        closeTab(i); return true;
                    }
                    switchTab(i); return true;
                }
                tx += tabW + 1;
            }
            return true;
        }

        // url bar click (below tab bar)
        if (btn == 0 && isInUrlBar(cmx, cmy)) {
            editingUrl = true;
            urlBuf = currentUrl();
            urlCursor = urlBuf.length();
            return true;
        }

        if (!isInAnyHud(cmx, cmy)) {
            editingUrl = false;
            Tab active = activeTab();
            if (active != null && active.instance != null) {
                active.instance.setFocused(true);
                active.instance.mouseButton(cmx, cmy, btn, true, mods());
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(Click click) {
        draggingBar = false;
        if (!isInAnyHud(click.x(), click.y())) {
            Tab active = activeTab();
            if (active != null && active.instance != null) {
                active.instance.mouseButton(click.x(), click.y(), click.button(), false, mods());
                return true;
            }
        }
        return false;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        mx = mouseX; my = mouseY;
        if (draggingBar) {
            barX = dragStartBx + (float)(mouseX - dragStartMx);
            barY = dragStartBy + (float)(mouseY - dragStartMy);
            barX = Math.max(0, Math.min(barX, width - 40));
            barY = Math.max(0, Math.min(barY, height - BAR_H));
            return;
        }
        if (!isInAnyHud(mouseX, mouseY)) {
            Tab active = activeTab();
            if (active != null && active.instance != null)
                active.instance.mouseMove(mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double h, double v) {
        if (!isInAnyHud(mouseX, mouseY)) {
            Tab active = activeTab();
            if (active != null && active.instance != null) { active.instance.scroll(h, v); return true; }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        if (draggingBar) {
            barX = dragStartBx + (float)(click.x() - dragStartMx);
            barY = dragStartBy + (float)(click.y() - dragStartMy);
            barX = Math.max(0, Math.min(barX, width - 40));
            barY = Math.max(0, Math.min(barY, height - BAR_H));
            return true;
        }
        if (!isInAnyHud(click.x(), click.y())) {
            Tab active = activeTab();
            if (active != null && active.instance != null) {
                active.instance.mouseMove(click.x(), click.y());
                return true;
            }
        }
        return false;
    }


    @Override
    public boolean keyPressed(KeyInput input) {
        int key = input.key();
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (editingUrl) { editingUrl = false; return true; }
            close(); return true;
        }
        if (editingUrl) return handleUrlKey(key);
        Tab active = activeTab();
        if (active != null && active.instance != null) {
            active.instance.keyEvent(key, input.scancode(), input.modifiers(), true);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyReleased(KeyInput input) {
        if (!editingUrl) {
            Tab active = activeTab();
            if (active != null && active.instance != null) {
                active.instance.keyEvent(input.key(), input.scancode(), input.modifiers(), false);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (editingUrl) {
            String ch = new String(Character.toChars(input.codepoint()));
            urlBuf = urlBuf.substring(0, urlCursor) + ch + urlBuf.substring(urlCursor);
            urlCursor++;
            return true;
        }
        Tab active = activeTab();
        if (active != null && active.instance != null) { active.instance.charTyped(input.codepoint()); return true; }
        return false;
    }

    @Override
    public void resize(int w, int h) {
        super.resize(w, h);
        for (Tab tab : tabs) if (tab.instance != null) tab.instance.resize(w, h);
    }

    private boolean handleUrlKey(int key) {
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            String url = urlBuf.trim();
            if (!url.contains("://")) url = "https://" + url;
            Tab active = activeTab();
            if (active != null) {
                active.url = url;
                active.title = "";
                if (active.instance != null) active.instance.loadUrl(url);
            }
            editingUrl = false;
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE && urlCursor > 0) {
            urlBuf = urlBuf.substring(0, urlCursor - 1) + urlBuf.substring(urlCursor);
            urlCursor--;
        } else if (key == GLFW.GLFW_KEY_DELETE && urlCursor < urlBuf.length()) {
            urlBuf = urlBuf.substring(0, urlCursor) + urlBuf.substring(urlCursor + 1);
        } else if (key == GLFW.GLFW_KEY_LEFT  && urlCursor > 0) urlCursor--;
        else if (key == GLFW.GLFW_KEY_RIGHT && urlCursor < urlBuf.length()) urlCursor++;
        else if (key == GLFW.GLFW_KEY_HOME) urlCursor = 0;
        else if (key == GLFW.GLFW_KEY_END)  urlCursor = urlBuf.length();
        return true;
    }

    private void openNewTab(String url) {
        Tab tab = new Tab(url);
        tab.ensureInstance(rendererManager, width, height);
        tabs.add(tab);
        switchTab(tabs.size() - 1);
    }

    private void closeTab(int i) {
        if (tabs.size() == 1) { close(); return; }
        tabs.get(i).close();
        tabs.remove(i);
        if (activeTab >= tabs.size()) activeTab = tabs.size() - 1;
        focusActive();
    }

    private void switchTab(int i) {
        Tab prev = activeTab();
        if (prev != null && prev.instance != null) prev.instance.setFocused(false);
        activeTab = i;
        focusActive();
    }

    private void focusActive() {
        Tab t = activeTab();
        if (t != null && t.instance != null) t.instance.setFocused(true);
    }

    private Tab activeTab() {
        if (tabs.isEmpty() || activeTab < 0 || activeTab >= tabs.size()) return null;
        return tabs.get(activeTab);
    }

    private String currentUrl() {
        Tab t = activeTab();
        return t == null ? "" : t.url;
    }

    private float tabWidth(int count) {
        float avail = Math.min(width * 0.7f, 800f) - PLUS_W - 6;
        return Math.min(TAB_MAX_W, Math.max(TAB_MIN_W, avail / Math.max(1, count)));
    }

    private float tabBarWidth(int count, float tabW) {
        return tabW * count + count - 1 + PLUS_W + 4;
    }

    private float tabBarWidth() {
        int count = tabs.size();
        float tabW = tabWidth(count);
        return tabBarWidth(count, tabW);
    }

    private boolean isTabHovered(float tx, float tabW) {
        return mx >= tx && mx < tx + tabW && my >= barY && my < barY + BAR_H;
    }

    private boolean isInBar(double x, double y) {
        float totalW = tabBarWidth();
        return x >= barX && x < barX + totalW && y >= barY && y < barY + BAR_H;
    }

    private boolean isInUrlBar(double x, double y) {
        int tabCount = tabs.size();
        float tabW   = tabWidth(tabCount);
        float totalW = tabBarWidth(tabCount, tabW);
        float urlW   = Math.min(totalW, Math.max(160f, totalW * 0.7f));
        float urlX   = barX + (totalW - urlW) / 2f;
        float urlY   = barY + BAR_H + URL_PAD;
        return x >= urlX && x < urlX + urlW && y >= urlY && y < urlY + URL_H;
    }

    private boolean isInAnyHud(double x, double y) {
        return isInBar(x, y) || isInUrlBar(x, y);
    }

    private int mods() {
        int m = 0;
        long h = mc.getWindow().getHandle();
        if (GLFW.glfwGetKey(h, GLFW.GLFW_KEY_LEFT_SHIFT)    == GLFW.GLFW_PRESS ||
            GLFW.glfwGetKey(h, GLFW.GLFW_KEY_RIGHT_SHIFT)   == GLFW.GLFW_PRESS) m |= 0x1;
        if (GLFW.glfwGetKey(h, GLFW.GLFW_KEY_LEFT_CONTROL)  == GLFW.GLFW_PRESS ||
            GLFW.glfwGetKey(h, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS) m |= 0x2;
        if (GLFW.glfwGetKey(h, GLFW.GLFW_KEY_LEFT_ALT)      == GLFW.GLFW_PRESS ||
            GLFW.glfwGetKey(h, GLFW.GLFW_KEY_RIGHT_ALT)     == GLFW.GLFW_PRESS) m |= 0x4;
        return m;
    }


    private void saveState() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("barX", barX);
            root.addProperty("barY", barY);
            root.addProperty("active", activeTab);
            JsonArray arr = new JsonArray();
            for (Tab t : tabs) {
                JsonObject o = new JsonObject();
                o.addProperty("url", t.url);
                o.addProperty("title", t.title);
                arr.add(o);
            }
            root.add("tabs", arr);
            FilesManager.createFile(CONFIG_DIR, FilesManager.FileFormat.APR, CONFIG_NAME, root.toString(), FilesManager.CheckMode.ALWAYS);
        } catch (Exception ignored) {}
    }

    private void loadState() {
        try {
            Path p = FilesManager.getFilePath(CONFIG_DIR, CONFIG_NAME, FilesManager.FileFormat.APR);
            if (!FilesManager.exists(p)) return;
            String raw = FilesManager.readFile(p);
            if (raw == null || raw.isBlank()) return;
            JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
            barX = root.has("barX") ? root.get("barX").getAsFloat() : 0;
            barY = root.has("barY") ? root.get("barY").getAsFloat() : 0;
            int savedActive = root.has("active") ? root.get("active").getAsInt() : 0;
            if (root.has("tabs")) {
                for (var el : root.getAsJsonArray("tabs")) {
                    JsonObject o = el.getAsJsonObject();
                    Tab t = new Tab(o.get("url").getAsString());
                    t.title = o.has("title") ? o.get("title").getAsString() : "";
                    tabs.add(t);
                }
            }
            activeTab = Math.min(savedActive, Math.max(0, tabs.size() - 1));
        } catch (Exception ignored) {}
    }


    /** Single browser tab: holds URL, display title, and the renderer instance. */
    private class Tab {
        String url;
        String title = "";
        float  animValue = 0f;
        WebRendererInstance instance;

        Tab(String url) { this.url = url; }

        void ensureInstance(WebRendererManager mgr, int w, int h) {
            if (instance != null) return;
            WebRendererConfig cfg = WebRendererConfig.builder().url(url).transparent(false).build();
            instance = mgr.create(cfg);
            if (instance != null) instance.resize(w, h);
        }

        /** Hostname extracted from URL, or raw URL if parsing fails. */
        String displayTitle() {
            if (!title.isEmpty()) return title;
            try {
                String u = url.contains("://") ? url : "https://" + url;
                String host = new java.net.URI(u).getHost();
                return host != null ? host : url;
            } catch (Exception e) { return url; }
        }

        void updateAnim(float delta) {
            animValue = Math.min(1f, animValue + delta * 0.15f);
        }

        void close() {
            if (instance != null) { instance.close(); instance = null; }
        }
    }
}
