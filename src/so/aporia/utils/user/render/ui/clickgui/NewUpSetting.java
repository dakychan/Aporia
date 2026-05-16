/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.utils.user.render.ui.clickgui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import so.aporia.module.Module;
import so.aporia.module.settings.BooleanSetting;
import so.aporia.module.settings.BindSetting;
import so.aporia.module.settings.ButtonSetting;
import so.aporia.module.settings.MultiSelectSetting;
import so.aporia.module.settings.SelectSetting;
import so.aporia.module.settings.Setting;
import so.aporia.module.settings.TextSetting;
import so.aporia.utils.user.render.animation.Animator;
import so.aporia.utils.user.render.animation.Easing;
import so.aporia.utils.user.render.animation.TypeAnim;
import so.aporia.utils.user.render.color.ColorUtil;
import so.aporia.utils.user.render.core.AporiaRenderer;
import so.aporia.module.impl.render.Beautifully;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Самостоятельный попап настроек.
 * При открытии — раскрывается из точки в центре экрана.
 * При закрытии — сжимается обратно в точку.
 * Слева — тумблеры, справа — 3D превью модели игрока.
 */
public class NewUpSetting {

    private static final int PANEL_W = 340;
    private static final int PANEL_H = 230;
    private static final int R = 12;
    private static final int PAD = 10;
    private static final int LINE_H = 28;
    private static final int TOPBAR_H = 30;
    private static final int PREVIEW_W = 130;

    private static final int C_BG = ColorUtil.rgba(16, 18, 30, 210);
    private static final int C_TOPBAR = ColorUtil.rgba(28, 32, 52, 230);
    private static final int C_BORDER_OUT = ColorUtil.rgba(255, 255, 255, 35);
    private static final int C_BORDER_IN = ColorUtil.rgba(255, 255, 255, 12);
    private static final int C_TXT = ColorUtil.rgba(255, 255, 255, 220);
    private static final int C_TXT_DIM = ColorUtil.rgba(255, 255, 255, 100);
    private static final int C_TOGGLE_ON = ColorUtil.rgba(100, 200, 100, 160);
    private static final int C_TOGGLE_OFF = ColorUtil.rgba(80, 80, 80, 80);
    private static final int C_DIVIDER = ColorUtil.rgba(255, 255, 255, 30);

    private Module module;
    private int settingCount = 0;
    private final Map<Object, TypeAnim> settingAnims = new WeakHashMap<>();
    private final Map<Object, Animator> boolToggleAnims = new WeakHashMap<>();
    private final Map<Object, Animator> selectAnims = new WeakHashMap<>();
    private final Map<Object, Animator> multiSelectAnims = new WeakHashMap<>();

    private Animator openAnim = null;
    private Animator closeAnim = null;
    private float mouseX = 0f;
    private float lastMouseX = 0f;
    private float playerYRot = 0f;

    public NewUpSetting(Module module) {
        this.module = module;
        countSettings();
        this.openAnim = new Animator(400, Easing::cubicOut);
        this.openAnim.play();
    }

    private void countSettings() {
        settingCount = 0;
        for (Field f : module.getClass().getDeclaredFields()) {
            if (Setting.class.isAssignableFrom(f.getType())) settingCount++;
        }
    }

    public void startClose() {
        closeAnim = new Animator(250, Easing::cubicIn);
        closeAnim.play();
        openAnim = null;
    }

    public boolean isClosing() {
        return closeAnim != null && closeAnim.value() < 1f;
    }

    public boolean isFullyClosed() {
        return closeAnim != null && closeAnim.value() >= 0.99f;
    }

    public boolean isOpening() {
        return openAnim != null && openAnim.value() < 1f;
    }

    public boolean isReady() {
        return openAnim == null || openAnim.value() >= 0.3f;
    }

    public void render(AporiaRenderer r, GuiGraphics gfx, int mx, int my, int screenW, int screenH) {
        if (module == null) return;

        mouseX = mx;

        if (closeAnim != null) {
            closeAnim.update();
            float s = 1f - closeAnim.value();
            int cx = screenW / 2;
            int cy = screenH / 2;
            int sw = (int)(PANEL_W * s);
            int sh = (int)(PANEL_H * s);
            if (sw > 2 && sh > 2) {
                r.drawRectBlurred(cx - sw / 2, cy - sh / 2, sw, sh, (int)(R * s), C_BG, 5f);
                r.drawStroke(cx - sw / 2, cy - sh / 2, sw, sh, (int)(R * s), 1f, 1, 0f, C_BORDER_OUT);
            }
            return;
        }

        if (openAnim != null) openAnim.update();
        float prog = openAnim != null ? openAnim.value() : 1f;

        int px = (screenW - PANEL_W) / 2;
        int py = (screenH - PANEL_H) / 2;
        int scaledW = (int)(PANEL_W * prog);
        int scaledH = (int)(PANEL_H * prog);
        int scaledX = px + (PANEL_W - scaledW) / 2;
        int scaledY = py + (PANEL_H - scaledH) / 2;

        if (Beautifully.isBlurEnabled()) {
            r.drawRectBlurred(scaledX, scaledY, scaledW, scaledH, R, C_BG, 5f);
        } else {
            r.drawRect(scaledX, scaledY, scaledW, scaledH, R, C_BG);
        }
        r.drawStroke(scaledX, scaledY, scaledW, scaledH, R, 1f, 1, 0f, C_BORDER_OUT);
        r.drawStroke(scaledX + 1, scaledY + 1, scaledW - 2, scaledH - 2, R - 1, 1f, 1, 0f, C_BORDER_IN);

        int settingsW = PANEL_W - PREVIEW_W - PAD * 2;
        int dividerX = px + settingsW + PAD;

        r.drawRect(px, py, PANEL_W, TOPBAR_H, R, C_TOPBAR);
        r.drawRect(px, py + TOPBAR_H - 2, PANEL_W, 2, 0, C_DIVIDER);
        r.drawText("bold", module.name(), px + PAD, py + (TOPBAR_H - 11) / 2f, 11f, C_TXT);
        String esc = "§7[Esc]";
        r.drawText("regular", esc, px + PANEL_W - r.getTextWidth("regular", "§7[Esc]", 8f) - PAD, py + (TOPBAR_H - 8) / 2f, 8f, C_TXT_DIM);

        r.drawRect(dividerX, py + TOPBAR_H, 1, PANEL_H - TOPBAR_H, 0, C_DIVIDER);

        if (prog > 0.3f) {
            int alpha = Math.min(255, (int)((prog - 0.3f) / 0.7f * 255));
            renderSettings(r, px, py, settingsW, alpha);
            renderPlayerPreview(r, gfx, dividerX + PAD, py + TOPBAR_H + PAD, PREVIEW_W - PAD * 2, PANEL_H - TOPBAR_H - PAD * 2, alpha);
        }
    }

    private void renderSettings(AporiaRenderer r, int px, int py, int settingsW, int alpha) {
        int cx = px + PAD;
        int y = py + TOPBAR_H + PAD;

        for (Field f : module.getClass().getDeclaredFields()) {
            if (!Setting.class.isAssignableFrom(f.getType())) continue;
            f.setAccessible(true);
            try {
                Setting<?> s = (Setting<?>) f.get(module);
                renderSettingRow(r, s, cx, settingsW - PAD, y, alpha);
                y += LINE_H;
            } catch (IllegalAccessException ignored) {}
        }
    }

    private void renderSettingRow(AporiaRenderer r, Setting<?> s, int cx, int cw, int y, int alpha) {
        float a = alpha / 255f;

        if (s instanceof BooleanSetting bs) {
            TypeAnim ta = settingAnims.computeIfAbsent(s, k -> { TypeAnim a2 = new TypeAnim(110, 170); a2.snap(bs.name()); return a2; });
            Animator tog = boolToggleAnims.computeIfAbsent(s, k -> new Animator(500, Easing::elasticOut));

            String txt = ta.update();
            if (!ta.isRunning() && !txt.equals(bs.name())) ta.setTarget(bs.name());

            r.drawText("regular", txt, cx, y + (LINE_H - 9) / 2f - 1, 9f, ColorUtil.rgba(255, 255, 255, (int)(220 * a)));

            int tW = 24, tH = 12, tX = cx + cw - tW - 4, tY = y + (LINE_H - tH) / 2;
            r.drawRect(tX, tY, tW, tH, tH / 2, bs.isEnabled() ? C_TOGGLE_ON : C_TOGGLE_OFF);
            tog.update();
            int dot = 14;
            int dX = bs.isEnabled() ? tX + tW - dot / 2 - 1 : tX - dot / 2 + 1;
            int animX = (int)(dX + (tog.value() - 0.5f) * 4f);
            r.drawRect(animX, tY + tH / 2 - dot / 2, dot, dot, dot / 2, ColorUtil.rgba(255, 255, 255, (int)(240 * a)));

        } else if (s instanceof SelectSetting ss) {
            TypeAnim ta = settingAnims.computeIfAbsent(s, k -> { TypeAnim a2 = new TypeAnim(110, 170); a2.snap(ss.get()); return a2; });
            Animator sa = selectAnims.computeIfAbsent(s, k -> new Animator(400, Easing::cubicOut));
            String txt = ta.update();
            if (!ta.isRunning() && !txt.equals(ss.get())) ta.setTarget(ss.get());

            r.drawText("regular", s.name(), cx, y + (LINE_H - 9) / 2f - 1, 9f, ColorUtil.rgba(255, 255, 255, (int)(220 * a)));
            int vW = 60, vH = 16, vX = cx + cw - vW - 4, vY = y + (LINE_H - vH) / 2;
            sa.update();
            r.drawRectBlurred(vX, vY, vW, vH, 4, ColorUtil.rgba(30, 30, 50, (int)((120 + sa.value() * 80) * a)), 5f);
            r.drawText("regular", txt, vX + 6, vY + (vH - 9) / 2f + 1, 8f, ColorUtil.rgba(255, 255, 255, (int)(200 * a)));

        } else if (s instanceof TextSetting ts) {
            r.drawText("regular", s.name(), cx, y + (LINE_H - 9) / 2f - 1, 9f, ColorUtil.rgba(255, 255, 255, (int)(220 * a)));
            int iW = 70, iH = 16, iX = cx + cw - iW - 4, iY = y + (LINE_H - iH) / 2;
            r.drawRectBlurred(iX, iY, iW, iH, 4, ColorUtil.rgba(30, 30, 50, (int)(150 * a)), 1f);
            String txt = ts.get().isEmpty() ? "..." : ts.get();
            r.drawText("regular", txt, iX + 6, iY + (iH - 9) / 2f + 1, 8f, ColorUtil.rgba(255, 255, 255, ts.get().isEmpty() ? (int)(100 * a) : (int)(200 * a)));

        } else if (s instanceof BindSetting bs2) {
            r.drawText("regular", s.name(), cx, y + (LINE_H - 9) / 2f - 1, 9f, ColorUtil.rgba(255, 255, 255, (int)(220 * a)));
            int kW = 50, kH = 16, kX = cx + cw - kW - 4, kY = y + (LINE_H - kH) / 2;
            r.drawRectBlurred(kX, kY, kW, kH, 4, ColorUtil.rgba(30, 30, 50, (int)(150 * a)), 5f);
            String kn = bs2.isBound() ? so.aporia.utils.user.input.KeyCodeMap.getName(bs2.getKey()) : "None";
            r.drawText("regular", kn, kX + 6, kY + (kH - 9) / 2f + 1, 8f, ColorUtil.rgba(255, 255, 255, (int)(200 * a)));

        } else if (s instanceof ButtonSetting btn) {
            int bW = cw - 8, bH = 20, bX = cx + 4, bY = y + (LINE_H - bH) / 2;
            r.drawRect(bX, bY, bW, bH, 5, ColorUtil.rgba(60, 120, 200, (int)(150 * a)));
            r.drawText("regular", s.name(), bX + bW / 2 - r.getTextWidth("regular", s.name(), 9f) / 2, bY + (bH - 9) / 2f + 1, 9f, ColorUtil.rgba(255, 255, 255, (int)(220 * a)));

        } else if (s instanceof MultiSelectSetting mss) {
            TypeAnim ta = settingAnims.computeIfAbsent(s, k -> { TypeAnim a2 = new TypeAnim(110, 170); a2.snap(String.join(", ", mss.getSelected())); return a2; });
            Animator ma = multiSelectAnims.computeIfAbsent(s, k -> new Animator(400, Easing::cubicOut));
            String txt = ta.update();
            String cur = String.join(", ", mss.getSelected());
            if (!ta.isRunning() && !txt.equals(cur)) ta.setTarget(cur);

            r.drawText("regular", s.name(), cx, y + (LINE_H - 9) / 2f - 1, 9f, ColorUtil.rgba(255, 255, 255, (int)(220 * a)));
            int vW = 70, vH = 16, vX = cx + cw - vW - 4, vY = y + (LINE_H - vH) / 2;
            ma.update();
            r.drawRectBlurred(vX, vY, vW, vH, 4, ColorUtil.rgba(30, 30, 50, (int)((120 + ma.value() * 80) * a)), 1f);
            r.drawText("regular", txt.isEmpty() ? "None" : txt, vX + 6, vY + (vH - 9) / 2f + 1, 7f, ColorUtil.rgba(255, 255, 255, (int)(200 * a)));
        } else {
            r.drawText("regular", s.name(), cx, y + (LINE_H - 9) / 2f - 1, 9f, ColorUtil.rgba(255, 255, 255, (int)(220 * a)));
        }
    }

    private void renderPlayerPreview(AporiaRenderer r, GuiGraphics gfx, int x, int y, int w, int h, int alpha) {
        float a = alpha / 255f;
        r.drawRect(x, y, w, h, 6, ColorUtil.rgba(0, 0, 0, (int)(40 * a)));
        r.drawStroke(x, y, w, h, 6, 1f, 1, 0f, ColorUtil.rgba(255, 255, 255, (int)(20 * a)));

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        playerYRot += (mouseX - lastMouseX) * 0.5f;
        lastMouseX = mouseX;

        float cx = x + w / 2f;
        float cy = y + h / 2f + 5f;
        float size = Math.min(w, h) * 0.85f;
        renderEntity(gfx, cx, cy, size, playerYRot, mc.player);
    }

    private void renderEntity(GuiGraphics gfx, float cx, float cy, float size, float yRot, Player player) {
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        EntityRenderer<? super Player, ?> renderer = dispatcher.getRenderer(player);
        EntityRenderState state = renderer.createRenderState(player, 1f);
        state.lightCoords = 15728880;
        state.shadowPieces.clear();
        state.outlineColor = 0;

        if (state instanceof LivingEntityRenderState ls) {
            ls.bodyRot = 180f + yRot;
            ls.yRot = yRot;
            if (ls.pose != Pose.FALL_FLYING) ls.xRot = 0f;
            ls.boundingBoxWidth = ls.boundingBoxWidth / ls.scale;
            ls.boundingBoxHeight = ls.boundingBoxHeight / ls.scale;
            ls.scale = 1f;
        }

        Quaternionf rot = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf tilt = new Quaternionf().rotateX(0f);
        rot.mul(tilt);
        Vector3f off = new Vector3f(0f, state.boundingBoxHeight / 2f + size * 0.05f, 0f);
        int hs = (int)(size / 2);
        int x1 = (int)(cx - hs);
        int y1 = (int)(cy - hs);
        int x2 = (int)(cx + hs);
        int y2 = (int)(cy + hs);

        gfx.submitEntityRenderState(state, 15728880, off, rot, tilt, x1, y1, x2, y2);
    }

    public boolean mouseClicked(int mx, int my, int screenW, int screenH, int button) {
        if (isClosing()) return false;
        if (!isReady()) return false;

        float prog = openAnim != null ? openAnim.value() : 1f;
        int px = (screenW - PANEL_W) / 2;
        int py = (screenH - PANEL_H) / 2;
        int scaledW = (int)(PANEL_W * prog);
        int scaledH = (int)(PANEL_H * prog);
        int scaledX = px + (PANEL_W - scaledW) / 2;
        int scaledY = py + (PANEL_H - scaledH) / 2;

        if (mx < scaledX || mx >= scaledX + scaledW || my < scaledY || my >= scaledY + scaledH) return false;

        int settingsW = PANEL_W - PREVIEW_W - PAD * 2;
        int cx = px + PAD;
        int y = py + TOPBAR_H + PAD;

        for (Field f : module.getClass().getDeclaredFields()) {
            if (!Setting.class.isAssignableFrom(f.getType())) continue;
            f.setAccessible(true);
            try {
                Setting<?> s = (Setting<?>) f.get(module);
                if (mx >= cx && mx < cx + settingsW - PAD && my >= y && my < y + LINE_H) {
                    if (s instanceof BooleanSetting bs) {
                        bs.toggle();
                        Animator tog = boolToggleAnims.get(s);
                        if (tog != null) { tog.reset(); tog.play(); }
                        TypeAnim ta = settingAnims.get(s);
                        if (ta != null && !ta.isRunning()) ta.setTarget(bs.isEnabled() ? "Включено!" : "Выключено!");
                    } else if (s instanceof SelectSetting ss) {
                        ss.setSelectedIndex((ss.getSelectedIndex() + 1) % ss.getOptions().size());
                        Animator sa = selectAnims.get(s);
                        if (sa != null) { sa.reset(); sa.play(); }
                    } else if (s instanceof MultiSelectSetting mss) {
                        List<String> opts = mss.getOptions();
                        if (!opts.isEmpty()) {
                            List<String> cur = mss.getSelected();
                            String cv = cur.isEmpty() ? opts.get(0) : cur.get(0);
                            String next = opts.get((opts.indexOf(cv) + 1) % opts.size());
                            mss.setSelected(java.util.Arrays.asList(next));
                            Animator ma = multiSelectAnims.get(s);
                            if (ma != null) { ma.reset(); ma.play(); }
                        }
                    } else if (s instanceof ButtonSetting btn) {
                        btn.click();
                    }
                    return true;
                }
                y += LINE_H;
            } catch (IllegalAccessException ignored) {}
        }
        return true;
    }

    public boolean keyPressed(int scancode) {
        if (scancode == 1) { startClose(); return true; }
        return false;
    }

    public Module getModule() { return module; }
    public void close() { module = null; }
    public boolean isOpen() { return module != null; }
}
