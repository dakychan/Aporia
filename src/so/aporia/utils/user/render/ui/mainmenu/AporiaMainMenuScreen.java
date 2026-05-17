/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

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

    private final List<MenuButton> buttons = new ArrayList<>();
    private Animator loadAnim;

    private boolean locked = false;
    private long lastActivity = 0;
    private Animator lockFadeAnim;

    private int sw, sh;
    private float titleSize, subSize, btnTextSize, clockSize, dateSize;
    private int btnW, btnH, btnGap, btnR;
    private float logoY, subY, btnStartY;

    public AporiaMainMenuScreen() {
        super(Component.literal("Aporia"));
    }

    private void calcLayout() {
        sw = this.width;
        sh = this.height;
        float scale = Minecraft.getInstance().getWindow().getGuiScale();

        float baseUnit = Math.min(sw, sh) / 100f;

        btnW = Math.max(90, Math.min(150, (int)(sw * 0.17f)));
        btnH = Math.max(24, Math.min(36, (int)(baseUnit * 3.5f)));
        btnGap = Math.max(4, (int)(baseUnit * 0.8f));
        btnR = Math.max(8, Math.min(16, (int)(baseUnit * 1.5f)));

        titleSize = Math.max(18f, Math.min(42f, sw * 0.045f));
        if (scale >= 3f) titleSize *= 0.75f;
        else if (scale >= 2f) titleSize *= 0.85f;

        subSize = Math.max(9f, Math.min(16f, sw * 0.016f));
        if (scale >= 3f) subSize *= 0.75f;
        else if (scale >= 2f) subSize *= 0.85f;

        btnTextSize = Math.max(9f, Math.min(13f, sw * 0.013f));
        if (scale >= 3f) btnTextSize *= 0.8f;
        else if (scale >= 2f) btnTextSize *= 0.9f;

        clockSize = Math.max(32f, Math.min(72f, sw * 0.07f));
        dateSize = Math.max(11f, Math.min(20f, sw * 0.02f));

        float centerY = sh / 2f;
        logoY = centerY - sh * 0.38f;
        subY = centerY - sh * 0.28f;

        String[] keys = {"menu.singleplayer", "menu.multiplayer", "menu.settings", "menu.exit"};
        int totalBtnH = keys.length * btnH + (keys.length - 1) * btnGap;
        btnStartY = centerY + sh * 0.02f - totalBtnH / 2f;

        buttons.clear();
        for (int i = 0; i < keys.length; i++) {
            int x = (sw - btnW) / 2;
            int y = (int)(btnStartY + i * (btnH + btnGap));
            buttons.add(new MenuButton(x, y, btnW, btnH, keys[i], i, i * 60));
        }
    }

    @Override
    protected void init() {
        lastActivity = System.currentTimeMillis();
        locked = false;
        calcLayout();

        loadAnim = new Animator(800, Easing::sineInOut);
        loadAnim.play();
        lockFadeAnim = new Animator(500, Easing::cubicOut);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        calcLayout();
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
            renderLockScreen(AporiaRenderer.INSTANCE, gfx, now);
            return;
        }

        AporiaRenderer r = AporiaRenderer.INSTANCE;
        r.prepareFrameBlur(Minecraft.getInstance(), 30f, 0.5f);

        loadAnim.update();
        float prog = loadAnim.value();

        float lift = (1f - prog) * sh * 0.03f;
        float logoAlpha = Math.max(0, Math.min(1, (prog - 0.1f) / 0.4f));
        float subAlpha = Math.max(0, Math.min(1, (prog - 0.3f) / 0.4f));
        float btnAlpha = Math.max(0, Math.min(1, (prog - 0.4f) / 0.5f));

        if (logoAlpha > 0.01f) {
            String title = LocaleManager.INSTANCE.get("menu.title");
            float titleW = r.getTextWidth("bold", title, titleSize);
            float titleX = (sw - titleW) / 2f;
            r.drawText("bold", title, titleX, logoY - lift, titleSize, ColorUtil.rgba(255, 255, 255, (int)(255 * logoAlpha)));
        }

        if (subAlpha > 0.01f) {
            String sub = LocaleManager.INSTANCE.get("menu.subtitle");
            float subW = r.getTextWidth("regular", sub, subSize);
            float subX = (sw - subW) / 2f;
            r.drawText("regular", sub, subX, subY - lift, subSize, ColorUtil.rgba(180, 140, 255, (int)(200 * subAlpha)));
        }

        if (btnAlpha > 0.01f) {
            for (MenuButton btn : buttons) {
                btn.render(r, gfx, mx, my, btnAlpha);
            }
        }

        String version = "Aporia v1.0";
        float verW = r.getTextWidth("regular", version, 8f);
        r.drawText("regular", version, sw - verW - 8, sh - 14, 8f, ColorUtil.rgba(255, 255, 255, (int)(80 * prog)));
    }

    private void renderLockScreen(AporiaRenderer r, GuiGraphics gfx, long now) {
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

        float floatOffset = (float)(Math.sin(now / 1500.0) * 6f);

        float timeW = r.getTextWidth("bold", timeStr, clockSize);
        float timeX = (sw - timeW) / 2f;
        float timeY = sh / 2f - clockSize * 0.6f + floatOffset;
        r.drawText("bold", timeStr, timeX, timeY, clockSize, ColorUtil.rgba(255, 255, 255, (int)(255 * alpha)));

        float dateW = r.getTextWidth("regular", dateStr, dateSize);
        float dateX = (sw - dateW) / 2f;
        float dateY = timeY + clockSize * 0.55f + floatOffset * 0.5f;
        r.drawText("regular", dateStr, dateX, dateY, dateSize, ColorUtil.rgba(255, 255, 255, (int)(160 * alpha)));

        String hint = LocaleManager.INSTANCE.get("lock.click_hint");
        float hintW = r.getTextWidth("regular", hint, 10f);
        float hintX = (sw - hintW) / 2f;
        float hintY = sh / 2f + sh * 0.12f;
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
                mc.setScreen(new net.minecraft.client.gui.screens.worldselection.SelectWorldScreen(this));
                break;
            case 1:
                mc.setScreen(new net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen(this));
                break;
            case 2:
                mc.setScreen(new net.minecraft.client.gui.screens.options.OptionsScreen(this, mc.options));
                break;
            case 3:
                mc.execute(() -> mc.stop());
                break;
        }
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override
    public void renderBackground(GuiGraphics gfx, int mx, int my, float delta) {
        float time = (float)(System.currentTimeMillis() / 1000.0);
        AporiaRenderer.INSTANCE.drawMainMenuBackground(time, this.width, this.height);
    }
    private static final class MenuButton {
        final int x, y, w, h, id;
        final String localeKey;
        Animator hoverAnim;
        Animator appearAnim;
        int appearDelay;

        MenuButton(int x, int y, int w, int h, String localeKey, int id, int appearDelay) {
            this.x = x; this.y = y; this.w = w; this.h = h;
            this.localeKey = localeKey; this.id = id;
            this.appearDelay = appearDelay;
            this.hoverAnim = new Animator(200, Easing::cubicOut);
            this.appearAnim = new Animator(400, Easing::cubicOut);
        }

        void render(AporiaRenderer r, GuiGraphics gfx, int mx, int my, float globalAlpha) {
            boolean hov = mx >= x && mx < x + w && my >= y && my < y + h;
            if (hov && !hoverAnim.isPlaying() && hoverAnim.value() < 0.99f) hoverAnim.play();
            if (!hov && !hoverAnim.isPlaying() && hoverAnim.value() > 0.01f) hoverAnim.reverse();
            hoverAnim.update();

            if (!appearAnim.isPlaying() && appearAnim.value() < 0.99f) {
                appearAnim.play();
            }
            appearAnim.update();

            float hp = hoverAnim.value();
            float ap = appearAnim.value();
            float alpha = globalAlpha * ap;

            float slideY = (1f - ap) * h * 0.5f;
            int drawY = (int)(y + slideY);

            // 1. Рендерим размытый фон (Frosted Glass)
            if (alpha > 0.01f) {
                // Передаем полупрозрачный фиолетовый цвет (наш тинт поверх блюра)
                // Альфа 0.3 (76 из 255) - это степень "заморозки" стекла. Можешь поиграть с ней.
                int glassTint = ColorUtil.rgba(20, 10, 40, (int)(76 * alpha));
                r.drawRectBlurred(x, drawY, w, h, h * 0.5f, glassTint, 10f);
            }

            float rVal = h * 0.5f;

            // 2. Стеклянная подложка (Она больше не нужна как прямоугольник, так как блюр её включает!
            // Но если хочешь добавить легкий глянец при наведении, можно оставить очень тонкий слой:
            int bgAlpha = (int)((10 + 20 * hp) * alpha);
            int bg = ColorUtil.rgba(255, 255, 255, bgAlpha);
            r.drawRect(x, drawY, w, h, rVal, bg);

            // 3. Обводка
            int borderAlpha = (int)((30 + 50 * hp) * alpha);
            r.drawStroke(x, drawY, w, h, rVal, 1f, 1, 0f, ColorUtil.rgba(255, 255, 255, borderAlpha));

            // 4. Текст
            float txtSize = Math.max(9f, Math.min(13f, h * 0.38f));
            String text = LocaleManager.INSTANCE.get(localeKey);
            float textW = r.getTextWidth("regular", text, txtSize);
            float textX = x + (w - textW) / 2f;
            float textY = drawY + (h - txtSize) / 2f - 1f;
            r.drawText("regular", text, textX, textY, txtSize, ColorUtil.rgba(255, 255, 255, (int)((180 + 75 * hp) * alpha)));
        }

        boolean clicked(int mx, int my, int button) {
            if (button != 0) return false;
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }
}
