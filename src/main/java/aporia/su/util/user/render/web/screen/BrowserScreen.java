package aporia.su.util.user.render.web.screen;

import aporia.su.util.user.render.web.WebRendererConfig;
import aporia.su.util.user.render.web.WebRendererInstance;
import aporia.su.util.user.render.web.WebRendererManager;
import aporia.su.Initialization;
import aporia.su.util.interfaces.IMinecraft;
import aporia.su.util.user.render.Render2D;
import aporia.su.util.user.render.font.FontRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class BrowserScreen extends Screen implements IMinecraft {

    private static final int TAB_BAR_H = 22;
    private static final int TAB_W     = 140;
    private static final int TAB_H     = 18;
    private static final int CLOSE_W   = 14;
    private static final int BAR_COLOR = 0xFF141418;
    private static final int TAB_ACTIVE_COLOR  = 0xFF1E1E26;
    private static final int TAB_HOVER_COLOR   = 0xFF1A1A22;
    private static final int TAB_NORMAL_COLOR  = 0xFF111116;
    private static final int ACCENT_COLOR      = 0xFF6060FF;
    private static final int TEXT_COLOR        = 0xFFCCCCCC;
    private static final int TEXT_DIM_COLOR    = 0xFF888888;

    private final WebRendererManager rendererManager = WebRendererManager.INSTANCE;

    // tab state
    private final List<Tab> tabs = new ArrayList<>();
    private int activeTab = 0;

    // url bar editing
    private boolean editingUrl = false;
    private String urlEditBuffer = "";
    private int urlCursor = 0;

    // hover tracking
    private double mouseX, mouseY;

    public BrowserScreen(String url) {
        super(Text.literal("Browser"));
        tabs.add(new Tab(url));
    }

    // ── lifecycle ────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        rendererManager.initialize();
        for (Tab tab : tabs) {
            if (tab.instance == null) {
                tab.instance = rendererManager.create(tab.config);
                if (tab.instance != null) {
                    tab.instance.resize(width, height - TAB_BAR_H);
                    tab.instance.setFocused(true);
                }
            }
        }
    }

    @Override
    public void removed() {
        super.removed();
        for (Tab tab : tabs) tab.close();
        tabs.clear();
        rendererManager.shutdown();
    }

    @Override
    public boolean shouldPause() { return false; }

    // ── render ───────────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        mouseX = mx; mouseY = my;

        // browser content
        Tab active = activeTab();
        if (active != null && active.instance != null) {
            active.instance.render(ctx, 0, TAB_BAR_H, width, height - TAB_BAR_H, delta);
        } else {
            ctx.fill(0, TAB_BAR_H, width, height, 0xFF101018);
            if (!rendererManager.hasRealEngine()) {
                ctx.drawTextWithShadow(textRenderer,
                    Text.literal("MCEF not installed"), 12, TAB_BAR_H + 12, 0xFFD0D0D0);
            }
        }

        // tab bar background
        Render2D.rect(0, 0, width, TAB_BAR_H, BAR_COLOR);

        // tabs
        FontRenderer font = getFont();
        int tabX = 2;
        for (int i = 0; i < tabs.size(); i++) {
            Tab tab = tabs.get(i);
            boolean isActive = (i == activeTab);
            boolean hovered = mx >= tabX && mx < tabX + TAB_W && my >= 2 && my < 2 + TAB_H;

            int bg = isActive ? TAB_ACTIVE_COLOR : (hovered ? TAB_HOVER_COLOR : TAB_NORMAL_COLOR);
            Render2D.rect(tabX, 2, TAB_W, TAB_H, bg, 4f, 4f, 0f, 0f);

            if (isActive) {
                Render2D.rect(tabX, TAB_H + 1, TAB_W, 1, ACCENT_COLOR);
            }

            // title text
            String title = tab.title.isEmpty() ? tab.config.initialUrl() : tab.title;
            if (title.length() > 18) title = title.substring(0, 17) + "…";
            int textColor = isActive ? TEXT_COLOR : TEXT_DIM_COLOR;
            if (font != null) {
                font.drawText("regular", title, tabX + 5, 2 + (TAB_H - 7) / 2f, 7f, textColor);
            } else {
                ctx.drawTextWithShadow(textRenderer, Text.literal(title), tabX + 5, 7, textColor);
            }

            // close button
            int cx = tabX + TAB_W - CLOSE_W - 2;
            boolean closeHovered = mx >= cx && mx < cx + CLOSE_W && my >= 2 && my < 2 + TAB_H;
            int closeColor = closeHovered ? 0xFFFF5555 : TEXT_DIM_COLOR;
            if (font != null) {
                font.drawCenteredText("regular", "×", cx + CLOSE_W / 2f, 2 + (TAB_H - 7) / 2f, 9f, closeColor);
            } else {
                ctx.drawTextWithShadow(textRenderer, Text.literal("×"), cx + 3, 7, closeColor);
            }

            tabX += TAB_W + 2;
        }

        // new tab button
        boolean newTabHovered = mx >= tabX && mx < tabX + 18 && my >= 2 && my < 2 + TAB_H;
        Render2D.rect(tabX, 2, 18, TAB_H, newTabHovered ? TAB_HOVER_COLOR : TAB_NORMAL_COLOR, 4f);
        if (font != null) {
            font.drawCenteredText("regular", "+", tabX + 9, 2 + (TAB_H - 7) / 2f, 9f, TEXT_DIM_COLOR);
        } else {
            ctx.drawTextWithShadow(textRenderer, Text.literal("+"), tabX + 5, 7, TEXT_DIM_COLOR);
        }

        // url bar (right side)
        renderUrlBar(ctx, mx, my, font);
    }

    private void renderUrlBar(DrawContext ctx, int mx, int my, FontRenderer font) {
        int urlX = (tabs.size() + 1) * (TAB_W + 2) + 6;
        int urlW = width - urlX - 4;
        if (urlW < 40) return;

        boolean hovered = mx >= urlX && mx < urlX + urlW && my >= 2 && my < 2 + TAB_H;
        int urlBg = editingUrl ? 0xFF1E1E2E : (hovered ? 0xFF1A1A22 : 0xFF111116);
        Render2D.rect(urlX, 2, urlW, TAB_H, urlBg, 4f);
        if (editingUrl) {
            Render2D.outline(urlX, 2, urlW, TAB_H, 1f, ACCENT_COLOR, 4f);
        }

        String display = editingUrl ? urlEditBuffer : currentUrl();
        if (display.length() > 60) display = display.substring(0, 59) + "…";
        if (font != null) {
            font.drawText("regular", display, urlX + 6, 2 + (TAB_H - 7) / 2f, 7f,
                editingUrl ? TEXT_COLOR : TEXT_DIM_COLOR);
        } else {
            ctx.drawTextWithShadow(textRenderer, Text.literal(display), urlX + 6, 7,
                editingUrl ? TEXT_COLOR : TEXT_DIM_COLOR);
        }
    }

    // ── mouse ────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x(), my = click.y();
        int btn = click.button();

        if (my < TAB_BAR_H) {
            // check close buttons
            int tabX = 2;
            for (int i = 0; i < tabs.size(); i++) {
                int cx = tabX + TAB_W - CLOSE_W - 2;
                if (btn == 0 && mx >= cx && mx < cx + CLOSE_W && my >= 2 && my < 2 + TAB_H) {
                    closeTab(i);
                    return true;
                }
                if (btn == 0 && mx >= tabX && mx < tabX + TAB_W && my >= 2 && my < 2 + TAB_H) {
                    switchTab(i);
                    return true;
                }
                tabX += TAB_W + 2;
            }
            // new tab button
            if (btn == 0 && mx >= tabX && mx < tabX + 18 && my >= 2 && my < 2 + TAB_H) {
                openNewTab("https://google.com");
                return true;
            }
            // url bar
            int urlX = (tabs.size() + 1) * (TAB_W + 2) + 6;
            int urlW = width - urlX - 4;
            if (btn == 0 && mx >= urlX && mx < urlX + urlW && my >= 2 && my < 2 + TAB_H) {
                editingUrl = true;
                urlEditBuffer = currentUrl();
                urlCursor = urlEditBuffer.length();
                return true;
            }
            editingUrl = false;
            return true;
        }

        editingUrl = false;
        Tab active = activeTab();
        if (active != null && active.instance != null) {
            active.instance.setFocused(true);
            active.instance.mouseButton(mx, my - TAB_BAR_H, btn, true, getCurrentModifiers());
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (click.y() >= TAB_BAR_H) {
            Tab active = activeTab();
            if (active != null && active.instance != null) {
                active.instance.mouseButton(click.x(), click.y() - TAB_BAR_H, click.button(), false, getCurrentModifiers());
                return true;
            }
        }
        return false;
    }

    @Override
    public void mouseMoved(double mx, double my) {
        mouseX = mx; mouseY = my;
        if (my >= TAB_BAR_H) {
            Tab active = activeTab();
            if (active != null && active.instance != null)
                active.instance.mouseMove(mx, my - TAB_BAR_H);
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        if (my >= TAB_BAR_H) {
            Tab active = activeTab();
            if (active != null && active.instance != null) {
                active.instance.scroll(h, v);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        if (click.y() >= TAB_BAR_H) {
            Tab active = activeTab();
            if (active != null && active.instance != null) {
                active.instance.mouseMove(click.x(), click.y() - TAB_BAR_H);
                return true;
            }
        }
        return false;
    }

    // ── keyboard ─────────────────────────────────────────────────────────────

    @Override
    public boolean keyPressed(KeyInput input) {
        int key = input.key();
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (editingUrl) { editingUrl = false; return true; }
            close(); return true;
        }
        if (editingUrl) {
            return handleUrlKey(key, input.modifiers());
        }
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
            urlEditBuffer = urlEditBuffer.substring(0, urlCursor) + ch + urlEditBuffer.substring(urlCursor);
            urlCursor++;
            return true;
        }
        Tab active = activeTab();
        if (active != null && active.instance != null) {
            active.instance.charTyped(input.codepoint());
            return true;
        }
        return false;
    }

    @Override
    public void resize(int w, int h) {
        super.resize(w, h);
        for (Tab tab : tabs) {
            if (tab.instance != null) tab.instance.resize(w, h - TAB_BAR_H);
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private boolean handleUrlKey(int key, int mods) {
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            String url = urlEditBuffer.trim();
            if (!url.contains("://")) url = "https://" + url;
            Tab active = activeTab();
            if (active != null) {
                active.config = WebRendererConfig.builder().url(url).transparent(false).build();
                if (active.instance != null) active.instance.loadUrl(url);
            }
            editingUrl = false;
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE && urlCursor > 0) {
            urlEditBuffer = urlEditBuffer.substring(0, urlCursor - 1) + urlEditBuffer.substring(urlCursor);
            urlCursor--;
            return true;
        }
        if (key == GLFW.GLFW_KEY_LEFT && urlCursor > 0) { urlCursor--; return true; }
        if (key == GLFW.GLFW_KEY_RIGHT && urlCursor < urlEditBuffer.length()) { urlCursor++; return true; }
        if (key == GLFW.GLFW_KEY_HOME) { urlCursor = 0; return true; }
        if (key == GLFW.GLFW_KEY_END) { urlCursor = urlEditBuffer.length(); return true; }
        return true;
    }

    private void openNewTab(String url) {
        Tab tab = new Tab(url);
        tab.instance = rendererManager.create(tab.config);
        if (tab.instance != null) {
            tab.instance.resize(width, height - TAB_BAR_H);
            tab.instance.setFocused(true);
        }
        tabs.add(tab);
        activeTab = tabs.size() - 1;
    }

    private void closeTab(int i) {
        if (tabs.size() == 1) { close(); return; }
        tabs.get(i).close();
        tabs.remove(i);
        if (activeTab >= tabs.size()) activeTab = tabs.size() - 1;
    }

    private void switchTab(int i) {
        if (activeTab == i) return;
        Tab prev = activeTab();
        if (prev != null && prev.instance != null) prev.instance.setFocused(false);
        activeTab = i;
        Tab next = activeTab();
        if (next != null && next.instance != null) next.instance.setFocused(true);
    }

    private Tab activeTab() {
        if (tabs.isEmpty() || activeTab < 0 || activeTab >= tabs.size()) return null;
        return tabs.get(activeTab);
    }

    private String currentUrl() {
        Tab t = activeTab();
        return t == null ? "" : t.config.initialUrl();
    }

    private FontRenderer getFont() {
        try {
            return Initialization.getInstance().getManager().getRenderCore().getFontRenderer();
        } catch (Exception e) { return null; }
    }

    private int getCurrentModifiers() {
        int m = 0;
        long h = mc.getWindow().getHandle();
        if (GLFW.glfwGetKey(h, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS ||
            GLFW.glfwGetKey(h, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS) m |= 0x1;
        if (GLFW.glfwGetKey(h, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS ||
            GLFW.glfwGetKey(h, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS) m |= 0x2;
        if (GLFW.glfwGetKey(h, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS ||
            GLFW.glfwGetKey(h, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS) m |= 0x4;
        return m;
    }

    // ── Tab inner class ───────────────────────────────────────────────────────

    private class Tab {
        WebRendererConfig config;
        WebRendererInstance instance;
        String title = "";

        Tab(String url) {
            this.config = WebRendererConfig.builder().url(url).transparent(false).build();
        }

        void close() {
            if (instance != null) { instance.close(); instance = null; }
        }
    }
}
