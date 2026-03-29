package so.aporia.utils.user.render.ui.clickgui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import so.aporia.module.Module;
import so.aporia.module.settings.BooleanSetting;
import so.aporia.module.settings.BindSetting;
import so.aporia.module.settings.ButtonSetting;
import so.aporia.module.settings.MultiSelectSetting;
import so.aporia.module.settings.SelectSetting;
import so.aporia.module.settings.Setting;
import so.aporia.module.settings.TextSetting;
import so.aporia.utils.user.render.animation.Animator;
import so.aporia.utils.user.render.animation.TypeAnim;
import so.aporia.utils.user.render.color.ColorUtil;
import so.aporia.utils.user.render.core.AporiaRenderer;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Попап для отображения настроек модуля.
 */
public class SettingsPopup {

    private static final int POPUP_W = 140;
    private static final int POPUP_H_BASE = 20;
    private static final int TOPBAR_H = 0;
    private static final int PAD = 8;
    private static final int LINE_H = 24;
    private static final int R = 8;
    private static final int MAX_POPUP_H = 300;
    
    private static final int C_BG = ColorUtil.rgba(20, 22, 35, 180);
    private static final int C_BORDER_OUT = ColorUtil.rgba(255, 255, 255, 40);
    private static final int C_BORDER_IN = ColorUtil.rgba(255, 255, 255, 15);
    private static final int C_TXT = ColorUtil.rgba(255, 255, 255, 220);
    private static final int C_TOGGLE_ON = ColorUtil.rgba(100, 200, 100, 150);
    private static final int C_TOGGLE_OFF = ColorUtil.rgba(100, 100, 100, 80);
    private static final int C_DIVIDER = ColorUtil.rgba(255, 255, 255, 50);
    
    private Module module;
    private int settingCount = 0;
    private final Map<Object, TypeAnim> settingAnims = new WeakHashMap<>();
    private final Map<Object, Long> boolFlashTime = new WeakHashMap<>();
    private final Map<Object, Animator> boolToggleAnims = new WeakHashMap<>();
    private static final long BOOL_FLASH_COOLDOWN = 1000;
    
    private Animator popupAnim = null;
    private long popupOpenTime = 0;
    
    public SettingsPopup(Module module) {
        this.module = module;
        countSettings();
        this.popupAnim = Animator.slide(500);
        this.popupAnim.play();
        this.popupOpenTime = System.currentTimeMillis();
    }
    
    private void countSettings() {
        settingCount = 0;
        for (Field f : module.getClass().getDeclaredFields()) {
            if (Setting.class.isAssignableFrom(f.getType())) {
                settingCount++;
            }
        }
    }
    
    public void render(AporiaRenderer r, GuiGraphics gfx, int mx, int my, int screenW, int screenH) {
        if (module == null) return;
        
        popupAnim.update();
        float animProg = popupAnim.value();
        
        int contentH = settingCount * LINE_H + (settingCount > 0 ? (settingCount - 1) : 0) * 2;
        int popupH = POPUP_H_BASE + contentH;
        popupH = Math.min(popupH, MAX_POPUP_H);
        
        int px = (screenW - POPUP_W) / 2;
        int py = (screenH - popupH) / 2;
        
        px = Math.max(0, Math.min(px, screenW - POPUP_W));
        py = Math.max(0, Math.min(py, screenH - popupH));
        
        float scale = animProg;
        int scaledW = (int)(POPUP_W * scale);
        int scaledH = (int)(popupH * scale);
        int scaledX = px + (POPUP_W - scaledW) / 2;
        int scaledY = py + (popupH - scaledH) / 2;
        
        r.drawRectBlurred(scaledX, scaledY, scaledW, scaledH, R, C_BG, 8f);
        r.drawStroke(scaledX, scaledY, scaledW, scaledH, R, 1f, 1, 0f, C_BORDER_OUT);
        r.drawStroke(scaledX + 1, scaledY + 1, scaledW - 2, scaledH - 2, R - 1, 1f, 1, 0f, C_BORDER_IN);
        
        if (animProg > 0.95f) {
            int cx = px + PAD;
            int cw = POPUP_W - PAD * 2;
            int y = py + PAD;
            int idx = 0;
            
            for (Field f : module.getClass().getDeclaredFields()) {
                if (!Setting.class.isAssignableFrom(f.getType())) continue;
                f.setAccessible(true);
                try {
                    Setting<?> s = (Setting<?>) f.get(module);
                    renderSettingRow(r, s, cx, cw, y);
                    
                    if (idx < settingCount - 1) {
                        int divY = y + LINE_H;
                        float centerX = cx + cw / 2f + 25f;
                        float halfLen = cw / 3.5f;
                        r.drawFadeHLine(centerX, divY, halfLen, 1.0f, 0.6f, C_DIVIDER);
                    }
                    
                    y += LINE_H;
                    idx++;
                } catch (IllegalAccessException ignored) {}
            }
        } else {
            int cx = px + POPUP_W / 2;
            int cy = py + popupH / 2;
            r.drawText("regular", "секунду..", cx - 30, cy - 5, 9f, C_TXT);
        }
    }
    
    private void renderSettingRow(AporiaRenderer r, Setting<?> s, int cx, int cw, int y) {
        if (s instanceof BooleanSetting bs) {
            TypeAnim ta = settingAnims.computeIfAbsent(s, k -> {
                TypeAnim a = new TypeAnim(110, 170);
                a.snap(bs.name());
                return a;
            });
            
            Animator toggleAnim = boolToggleAnims.computeIfAbsent(s, k -> new Animator(500, so.aporia.utils.user.render.animation.Easing::elasticOut));
            
            long now = System.currentTimeMillis();
            long lastFlash = boolFlashTime.getOrDefault(s, 0L);
            boolean inFlash = (now - lastFlash) < BOOL_FLASH_COOLDOWN;
            
            String displayText = ta.update();
            
            if (!ta.isRunning() && !inFlash && !displayText.equals(bs.name())) {
                ta.setTarget(bs.name());
            }
            
            r.drawText("regular", displayText, cx, y + (LINE_H - 9) / 2f - 1, 9f, C_TXT);
            
            int toggleW = 22;
            int toggleH = 10;
            int toggleX = cx + cw - toggleW - 8 - 2;
            int toggleY = y + (LINE_H - toggleH) / 2 + 1 - 1;
            
            int bgColor = bs.isEnabled() ? C_TOGGLE_ON : C_TOGGLE_OFF;
            r.drawRect(toggleX, toggleY, toggleW, toggleH, toggleH / 2, bgColor);
            
            toggleAnim.update();
            float toggleProg = toggleAnim.value();
            
            int dotSize = 13;
            int dotX = bs.isEnabled() ? toggleX + toggleW - dotSize / 2 - 1 : toggleX - dotSize / 2 + 1;
            int dotY = toggleY + toggleH / 2 - dotSize / 2 - 1;
            
            int animDotX = (int)(dotX + (toggleProg - 0.5f) * 4f);
            r.drawRect(animDotX, dotY, dotSize, dotSize, dotSize / 2, ColorUtil.rgba(255, 255, 255, 240));
        } else if (s instanceof SelectSetting ss) {
            r.drawText("regular", s.name(), cx, y + (LINE_H - 9) / 2f - 1, 9f, C_TXT);
            
            int valueW = 60;
            int valueH = 14;
            int valueX = cx + cw - valueW - 8;
            int valueY = y + (LINE_H - valueH) / 2;
            r.drawRect(valueX, valueY, valueW, valueH, 4, ColorUtil.rgba(40, 40, 60, 120));
            r.drawText("regular", ss.get(), valueX + 6, valueY + (valueH - 8) / 2, 8f, ColorUtil.rgba(255, 255, 255, 200));
        } else if (s instanceof TextSetting ts) {
            r.drawText("regular", s.name(), cx, y + (LINE_H - 9) / 2f - 1, 9f, C_TXT);
            
            int inputW = 70;
            int inputH = 14;
            int inputX = cx + cw - inputW - 8;
            int inputY = y + (LINE_H - inputH) / 2;
            r.drawRectBlurred(inputX, inputY, inputW, inputH, 4, ColorUtil.rgba(30, 30, 50, 150), 15f);
            String text = ts.get().isEmpty() ? "..." : ts.get();
            r.drawText("regular", text, inputX + 6, inputY + (inputH - 8) / 2, 8f, ColorUtil.rgba(255, 255, 255, ts.get().isEmpty() ? 100 : 200));
        } else if (s instanceof BindSetting bs2) {
            r.drawText("regular", s.name(), cx, y + (LINE_H - 9) / 2f - 1, 9f, C_TXT);
            
            int keyW = 50;
            int keyH = 14;
            int keyX = cx + cw - keyW - 8;
            int keyY = y + (LINE_H - keyH) / 2;
            r.drawRect(keyX, keyY, keyW, keyH, 4, ColorUtil.rgba(50, 50, 70, 120));
            String keyName = bs2.isBound() ? keyName(bs2.getKey()) : "None";
            r.drawText("regular", keyName, keyX + 6, keyY + (keyH - 8) / 2, 8f, ColorUtil.rgba(255, 255, 255, 200));
        } else if (s instanceof ButtonSetting btn) {
            int btnW = cw - 16;
            int btnH = 18;
            int btnX = cx + 8;
            int btnY = y + (LINE_H - btnH) / 2;
            r.drawRect(btnX, btnY, btnW, btnH, 5, ColorUtil.rgba(60, 120, 200, 150));
            r.drawText("regular", s.name(), btnX + btnW / 2 - r.getTextWidth("regular", s.name(), 9f) / 2, btnY + (btnH - 9) / 2, 9f, C_TXT);
        } else if (s instanceof MultiSelectSetting mss) {
            r.drawText("regular", s.name(), cx, y + (LINE_H - 9) / 2f - 1, 9f, C_TXT);
            
            int valueW = 70;
            int valueH = 14;
            int valueX = cx + cw - valueW - 8;
            int valueY = y + (LINE_H - valueH) / 2;
            r.drawRect(valueX, valueY, valueW, valueH, 4, ColorUtil.rgba(40, 40, 60, 120));
            
            String selected = String.join(", ", mss.get());
            if (selected.isEmpty()) selected = "None";
            r.drawText("regular", selected, valueX + 6, valueY + (valueH - 8) / 2, 7f, ColorUtil.rgba(255, 255, 255, 150));
        } else {
            r.drawText("regular", s.name(), cx, y + (LINE_H - 9) / 2f - 1, 9f, C_TXT);
        }
    }
    
    private static String keyName(int key) {
        return net.minecraft.client.KeyMapping.createNameSupplier("key.keyboard." + key).get().getString().toUpperCase();
    }
    
    public boolean mouseClicked(int mx, int my, int screenW, int screenH, int button) {
        int contentH = settingCount * LINE_H + (settingCount > 0 ? (settingCount - 1) : 0) * 2;
        int popupH = POPUP_H_BASE + contentH;
        popupH = Math.min(popupH, MAX_POPUP_H);
        
        int px = (screenW - POPUP_W) / 2;
        int py = (screenH - popupH) / 2;
        
        px = Math.max(0, Math.min(px, screenW - POPUP_W));
        py = Math.max(0, Math.min(py, screenH - popupH));
        
        float scale = popupAnim.value();
        int scaledW = (int)(POPUP_W * scale);
        int scaledH = (int)(popupH * scale);
        int scaledX = px + (POPUP_W - scaledW) / 2;
        int scaledY = py + (popupH - scaledH) / 2;
        
        if (mx < scaledX || mx >= scaledX + scaledW || my < scaledY || my >= scaledY + scaledH) {
            return false;
        }
        
        int cx = scaledX + PAD;
        int cw = scaledW - PAD * 2;
        int y = scaledY + PAD;
        
        for (Field f : module.getClass().getDeclaredFields()) {
            if (!Setting.class.isAssignableFrom(f.getType())) continue;
            f.setAccessible(true);
            try {
                Setting<?> s = (Setting<?>) f.get(module);
                if (mx >= cx && mx < cx + cw && my >= y && my < y + LINE_H) {
                    if (s instanceof BooleanSetting bs) {
                        bs.toggle();
                        boolFlashTime.put(s, System.currentTimeMillis());
                        Animator toggleAnim = boolToggleAnims.get(s);
                        if (toggleAnim != null) {
                            toggleAnim.reset();
                            toggleAnim.play();
                        }
                        TypeAnim ta = settingAnims.get(s);
                        if (ta != null && !ta.isRunning()) {
                            String want = bs.isEnabled() ? "Включено!" : "Выключено!";
                            ta.setTarget(want);
                        }
                    } else if (s instanceof SelectSetting ss) {
                        if (button == 0) {
                            int idx = ss.getSelectedIndex();
                            ss.setSelectedIndex((idx + 1) % ss.getOptions().size());
                        }
                    } else if (s instanceof ButtonSetting btn) {
                        btn.click();
                    } else if (s instanceof BindSetting bs2) {
                        bindingField = f;
                        bindingModule = module;
                    }
                    return true;
                }
                y += LINE_H;
            } catch (IllegalAccessException ignored) {}
        }
        
        return true;
    }
    
    public boolean isOpen() {
        return module != null;
    }
    
    public void close() {
        module = null;
        bindingField = null;
        bindingModule = null;
    }
    
    private Field bindingField = null;
    private Module bindingModule = null;
    
    public boolean keyPressed(int key) {
        if (bindingField != null && bindingModule != null) {
            try {
                bindingField.setAccessible(true);
                BindSetting bs = (BindSetting) bindingField.get(bindingModule);
                bs.setKey(key);
                bindingField = null;
                bindingModule = null;
                return true;
            } catch (IllegalAccessException ignored) {}
        }
        return false;
    }
    
    public Module getModule() {
        return module;
    }
    
    public boolean isAnimationFinished() {
        return popupAnim != null && !popupAnim.isPlaying();
    }
}
