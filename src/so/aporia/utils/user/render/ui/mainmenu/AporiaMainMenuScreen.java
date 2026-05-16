package so.aporia.utils.user.render.ui.mainmenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import so.aporia.utils.user.locale.LocaleManager;
import so.aporia.utils.user.render.animation.Animator;
import so.aporia.utils.user.render.animation.Easing;
import so.aporia.utils.user.render.color.ColorUtil;
import so.aporia.utils.user.render.core.AporiaRenderer;

import java.util.*;

@OnlyIn(Dist.CLIENT)
public final class AporiaMainMenuScreen extends Screen {

    private static final long LOCK_TIMEOUT_MS = 5 * 60 * 1000;

    private static final int BTN_W = 220;
    private static final int BTN_H = 32;
    private static final int BTN_GAP = 8;
    private static final int BTN_R = 6;

    private static final int C_BG = ColorUtil.rgba(8, 4, 16, 255);
    private static final int C_BTN_BG = ColorUtil.rgba(255, 255, 255, 10);
    private static final int C_BTN_HOV = ColorUtil.rgba(255, 255, 255, 22);
    private static final int C_BTN_CLICK = ColorUtil.rgba(255, 255, 255, 35);
    private static final int C_TXT = ColorUtil.rgba(255, 255, 255, 255);
    private static final int C_TXT_DIM = ColorUtil.rgba(255, 255, 255, 140);
    private static final int C_SUBTITLE = ColorUtil.rgba(180, 140, 255, 200);
    private static final int C_LOCK_TIME = ColorUtil.rgba(255, 255, 255, 255);
    private static final int C_LOCK_DATE = ColorUtil.rgba(255, 255, 255, 160);
    private static final int C_LOCK_HINT = ColorUtil.rgba(255, 255, 255, 80);
    private static final int C_STAR_WHITE = ColorUtil.rgba(200, 220, 255, 255);
    private static final int C_STAR_PURPLE = ColorUtil.rgba(150, 120, 255, 255);
    private static final int C_FLASH = ColorUtil.rgba(100, 30, 200, 255);

    private final Random rng = new Random(42);
    private final Star[] stars = new Star[120];
    private final Flash[] flashes = new Flash[6];

    private final List<MenuButton> buttons = new ArrayList<>();
    private Animator loadAnim;

    private boolean locked = false;
    private long lastActivity = 0;
    private Animator lockFadeAnim;

    public AporiaMainMenuScreen() {
        super(Component.literal("Aporia"));
        for (int i = 0; i < stars.length; i++) {
            stars[i] = new Star(rng);
        }
        for (int i = 0; i < flashes.length; i++) {
            flashes[i] = new Flash(rng);
        }
    }

    @Override
    protected void init() {
        lastActivity = System.currentTimeMillis();
        locked = false;

        loadAnim = new Animator(800, Easing::sineInOut);
        loadAnim.play();
        lockFadeAnim = new Animator(500, Easing::cubicOut);

        buttons.clear();
        String[] keys = {"menu.singleplayer", "menu.multiplayer", "menu.settings", "menu.exit"};
        int totalH = keys.length * BTN_H + (keys.length - 1) * BTN_GAP;
        int startY = this.height / 2 + 40 - totalH / 2;

        for (int i = 0; i < keys.length; i++) {
            int x = (this.width - BTN_W) / 2;
            int y = startY + i * (BTN_H + BTN_GAP);
            buttons.add(new MenuButton(x, y, BTN_W, BTN_H, keys[i], i));
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mx, int my, float delta) {
        long now = System.currentTimeMillis();

        if (!locked && now - lastActivity > LOCK_TIMEOUT_MS) {
            locked = true;
            lockFadeAnim = new Animator(500, Easing::cubicOut);
            lockFadeAnim.play();
        }
        if (locked) {
            renderLockScreen(gfx, mx, my, now);
            return;
        }

        AporiaRenderer r = AporiaRenderer.INSTANCE;

        gfx.fill(0, 0, this.width, this.height, C_BG);

        updateAndRenderBg(r, now);

        loadAnim.update();
        float prog = loadAnim.value();

        float logoAlpha = Math.max(0, Math.min(1, (prog - 0.1f) / 0.4f));
        float subAlpha = Math.max(0, Math.min(1, (prog - 0.3f) / 0.4f));
        float btnAlpha = Math.max(0, Math.min(1, (prog - 0.4f) / 0.5f));

        if (logoAlpha > 0.01f) {
            String title = LocaleManager.INSTANCE.get("menu.title");
            float titleW = r.getTextWidth("bold", title, 36f);
            float titleX = (this.width - titleW) / 2f;
            float titleY = this.height / 2f - 80f;
            r.drawText("bold", title, titleX, titleY, 36f, ColorUtil.rgba(255, 255, 255, (int)(255 * logoAlpha)));
        }

        if (subAlpha > 0.01f) {
            String sub = LocaleManager.INSTANCE.get("menu.subtitle");
            float subW = r.getTextWidth("regular", sub, 12f);
            float subX = (this.width - subW) / 2f;
            float subY = this.height / 2f - 38f;
            r.drawText("regular", sub, subX, subY, 12f, ColorUtil.rgba(180, 140, 255, (int)(200 * subAlpha)));
        }

        if (btnAlpha > 0.01f) {
            for (MenuButton btn : buttons) {
                btn.render(r, gfx, mx, my, btnAlpha);
            }
        }

        String version = "Aporia v1.0";
        float verW = r.getTextWidth("regular", version, 8f);
        r.drawText("regular", version, this.width - verW - 10, this.height - 18, 8f, ColorUtil.rgba(255, 255, 255, (int)(80 * prog)));
    }

    private void updateAndRenderBg(AporiaRenderer r, long now) {
        float t = now / 1000f;

        for (Flash f : flashes) {
            f.update(t, rng);
            if (f.alpha > 0.01f) {
                int col = ColorUtil.rgba(100, 30, 200, (int)(f.alpha * 60));
                float size = f.size * (1f - f.alpha) * 200f + 2f;
                r.drawCircle((int)(f.x * this.width), (int)(f.y * this.height), (int)size, col);
            }
        }

        for (Star s : stars) {
            s.update(t);
            if (s.alpha > 0.01f) {
                int col;
                if (s.colorType == 0) {
                    col = ColorUtil.rgba(200, 220, 255, (int)(s.alpha * 180));
                } else if (s.colorType == 1) {
                    col = ColorUtil.rgba(150, 120, 255, (int)(s.alpha * 150));
                } else {
                    col = ColorUtil.rgba(255, 255, 255, (int)(s.alpha * 200));
                }
                r.drawRect((int)(s.x * this.width) - 1, (int)(s.y * this.height) - 1, 2, 2, 0, col);
            }
        }
    }

    private void renderLockScreen(GuiGraphics gfx, int mx, int my, long now) {
        AporiaRenderer r = AporiaRenderer.INSTANCE;

        gfx.fill(0, 0, this.width, this.height, C_BG);
        updateAndRenderBg(r, now);

        lockFadeAnim.update();
        float alpha = lockFadeAnim.value();

        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;
        int month = cal.get(Calendar.MONTH) + 1;
        int year = cal.get(Calendar.YEAR);

        String timeStr = String.format(LocaleManager.INSTANCE.get("lock.time_format"), hour, minute);
        String dayName = LocaleManager.INSTANCE.get("lock.day_" + dayOfWeek);
        String dateStr = String.format("%s, %02d.%04d", dayName, month, year);

        float timeW = r.getTextWidth("bold", timeStr, 64f);
        float timeX = (this.width - timeW) / 2f;
        float timeY = this.height / 2f - 50f;
        r.drawText("bold", timeStr, timeX, timeY, 64f, ColorUtil.rgba(255, 255, 255, (int)(255 * alpha)));

        float dateW = r.getTextWidth("regular", dateStr, 16f);
        float dateX = (this.width - dateW) / 2f;
        float dateY = timeY + 45f;
        r.drawText("regular", dateStr, dateX, dateY, 16f, ColorUtil.rgba(255, 255, 255, (int)(160 * alpha)));

        String hint = LocaleManager.INSTANCE.get("lock.click_hint");
        float hintW = r.getTextWidth("regular", hint, 10f);
        float hintX = (this.width - hintW) / 2f;
        float hintY = this.height / 2f + 80f;
        float pulse = (float)(Math.sin(now / 800.0) * 0.3 + 0.7);
        r.drawText("regular", hint, hintX, hintY, 10f, ColorUtil.rgba(255, 255, 255, (int)(80 * alpha * pulse)));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean b) {
        lastActivity = System.currentTimeMillis();

        if (locked) {
            locked = false;
            return true;
        }

        int mx = (int)e.x(), my = (int)e.y();

        for (MenuButton btn : buttons) {
            if (btn.clicked(mx, my, e.button())) {
                onButtonAction(btn.id);
                return true;
            }
        }

        return super.mouseClicked(e, b);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        lastActivity = System.currentTimeMillis();
        return super.mouseDragged(e, dx, dy);
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        lastActivity = System.currentTimeMillis();

        if (locked) {
            locked = false;
            return true;
        }

        return super.keyPressed(e);
    }

    @Override
    public boolean charTyped(CharacterEvent e) {
        lastActivity = System.currentTimeMillis();
        return super.charTyped(e);
    }

    private void onButtonAction(int id) {
        Minecraft mc = Minecraft.getInstance();
        switch (id) {
            case 0:
                mc.setScreen(new net.minecraft.client.gui.screens.TitleScreen());
                mc.execute(() -> mc.setScreen(new net.minecraft.client.gui.screens.worldselection.SelectWorldScreen(new net.minecraft.client.gui.screens.TitleScreen())));
                break;
            case 1:
                mc.setScreen(new net.minecraft.client.gui.screens.TitleScreen());
                mc.execute(() -> mc.setScreen(new net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen(new net.minecraft.client.gui.screens.TitleScreen())));
                break;
            case 2:
                mc.setScreen(new net.minecraft.client.gui.screens.TitleScreen());
                mc.execute(() -> mc.setScreen(new net.minecraft.client.gui.screens.OptionsScreen(new net.minecraft.client.gui.screens.TitleScreen())));
                break;
            case 3:
                mc.execute(() -> mc.stop());
                break;
        }
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void renderBackground(GuiGraphics gfx, int mx, int my, float d) {}

    private static final class Star {
        float x, y, baseAlpha, alpha, twinkleSpeed;
        int colorType;

        Star(Random rng) {
            x = rng.nextFloat();
            y = rng.nextFloat();
            baseAlpha = 0.3f + rng.nextFloat() * 0.7f;
            twinkleSpeed = 0.5f + rng.nextFloat() * 2f;
            colorType = rng.nextInt(3);
        }

        void update(float t) {
            alpha = baseAlpha * (0.5f + 0.5f * (float)Math.sin(t * twinkleSpeed + x * 100));
        }
    }

    private static final class Flash {
        float x, y, alpha, size;
        float timer;
        boolean active;

        Flash(Random rng) {
            x = rng.nextFloat();
            y = rng.nextFloat();
            size = 0.5f + rng.nextFloat() * 1.5f;
            timer = rng.nextFloat() * 15f;
            active = false;
            alpha = 0;
        }

        void update(float t, Random rng) {
            timer -= 0.016f;
            if (timer <= 0 && !active) {
                timer = 5f + rng.nextFloat() * 10f;
                x = rng.nextFloat();
                y = rng.nextFloat();
                active = true;
                alpha = 1f;
            }
            if (active) {
                alpha -= 0.02f;
                if (alpha <= 0) {
                    alpha = 0;
                    active = false;
                }
            }
        }
    }

    private static final class MenuButton {
        final int x, y, w, h, id;
        final String localeKey;
        Animator hoverAnim;

        MenuButton(int x, int y, int w, int h, String localeKey, int id) {
            this.x = x; this.y = y; this.w = w; this.h = h;
            this.localeKey = localeKey; this.id = id;
            this.hoverAnim = new Animator(200, Easing::cubicOut);
        }

        void render(AporiaRenderer r, GuiGraphics gfx, int mx, int my, float alpha) {
            boolean hov = mx >= x && mx < x + w && my >= y && my < y + h;
            if (hov && !hoverAnim.isPlaying() && hoverAnim.value() < 0.99f) hoverAnim.play();
            if (!hov && !hoverAnim.isPlaying() && hoverAnim.value() > 0.01f) hoverAnim.reverse();
            hoverAnim.update();

            float hp = hoverAnim.value();
            int bgAlpha = (int)(10 + 12 * hp);
            int bg = ColorUtil.rgba(255, 255, 255, (int)(bgAlpha * alpha));

            r.drawRectBlurred(x, y, w, h, BTN_R, bg, 5f);
            int borderAlpha = (int)((15 + 25 * hp) * alpha);
            r.drawStroke(x, y, w, h, BTN_R, 1f, 1, 0f, ColorUtil.rgba(255, 255, 255, borderAlpha));

            String text = LocaleManager.INSTANCE.get(localeKey);
            float textW = r.getTextWidth("regular", text, 11f);
            float textX = x + (w - textW) / 2f;
            float textY = y + (h - 11) / 2f;
            r.drawText("regular", text, textX, textY, 11f, ColorUtil.rgba(255, 255, 255, (int)((180 + 75 * hp) * alpha)));
        }

        boolean clicked(int mx, int my, int button) {
            if (button != 0) return false;
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }
}
