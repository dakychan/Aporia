package so.aporia.utils.user.render.ui.clickgui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import so.aporia.module.Module;
import so.aporia.module.settings.BooleanSetting;
import so.aporia.module.settings.BindSetting;
import so.aporia.module.settings.SelectSetting;
import so.aporia.module.settings.Setting;
import so.aporia.module.settings.TextSetting;
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
    private static final int POPUP_H_BASE = 30;
    private static final int TOPBAR_H = 0;
    private static final int PAD = 8;
    private static final int LINE_H = 24;
    private static final int R = 8;
    
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
    
    public SettingsPopup(Module module) {
        this.module = module;
        countSettings();
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
        
        int popupH = POPUP_H_BASE + settingCount * LINE_H;
        int px = (screenW - POPUP_W) / 2;
        int py = (screenH - popupH) / 2;
        
        px = Math.max(0, Math.min(px, screenW - POPUP_W));
        py = Math.max(0, Math.min(py, screenH - popupH));
        
        r.drawRectBlurred(px, py, POPUP_W, popupH, R, C_BG, 8f);
        r.drawStroke(px, py, POPUP_W, popupH, R, 1f, 1, 0f, C_BORDER_OUT);
        r.drawStroke(px + 1, py + 1, POPUP_W - 2, popupH - 2, R - 1, 1f, 1, 0f, C_BORDER_IN);
        
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
                    float centerX = cx + cw / 2f;
                    float halfLen = cw / 2.5f;
                    r.drawFadeHLine(centerX, divY, halfLen, 1.5f, 1f, C_DIVIDER);
                }
                
                y += LINE_H;
                idx++;
            } catch (IllegalAccessException ignored) {}
        }
    }
    
    private void renderSettingRow(AporiaRenderer r, Setting<?> s, int cx, int cw, int y) {
        if (s instanceof BooleanSetting bs) {
            TypeAnim ta = settingAnims.computeIfAbsent(s, k -> {
                TypeAnim a = new TypeAnim(110, 170);
                a.snap(bs.name());
                return a;
            });
            
            String displayText = ta.update();
            if (!ta.isRunning() && !displayText.equals(bs.name())) {
                ta.snap(bs.name());
            }
            r.drawText("regular", displayText, cx, y + (LINE_H - 9) / 2f - 1, 9f, C_TXT);
            
            int toggleW = 22;
            int toggleH = 10;
            int toggleX = cx + cw - toggleW - 8 - 2;
            int toggleY = y + (LINE_H - toggleH) / 2 + 1 - 1;
            
            int bgColor = bs.isEnabled() ? C_TOGGLE_ON : C_TOGGLE_OFF;
            r.drawRect(toggleX, toggleY, toggleW, toggleH, toggleH / 2, bgColor);
            
            int dotSize = 13;
            int dotX = bs.isEnabled() ? toggleX + toggleW - dotSize / 2 - 1 : toggleX - dotSize / 2 + 1;
            int dotY = toggleY + toggleH / 2 - dotSize / 2 - 1;
            r.drawRect(dotX, dotY, dotSize, dotSize, dotSize / 2, ColorUtil.rgba(255, 255, 255, 240));
        } else if (s instanceof SelectSetting ss) {
            r.drawText("regular", s.name(), cx, y + (LINE_H - 9) / 2f - 1, 9f, C_TXT);
            r.drawText("regular", ss.get(), cx + cw - 40, y + (LINE_H - 9) / 2f - 1, 8f, ColorUtil.rgba(255, 255, 255, 150));
        } else if (s instanceof TextSetting ts) {
            r.drawText("regular", s.name(), cx, y + (LINE_H - 9) / 2f - 1, 9f, C_TXT);
            r.drawText("regular", ts.get(), cx + cw - 40, y + (LINE_H - 9) / 2f - 1, 8f, ColorUtil.rgba(255, 255, 255, 150));
        } else if (s instanceof BindSetting bs2) {
            r.drawText("regular", s.name(), cx, y + (LINE_H - 9) / 2f - 1, 9f, C_TXT);
            String keyName = bs2.isBound() ? keyName(bs2.getKey()) : "None";
            r.drawText("regular", keyName, cx + cw - 40, y + (LINE_H - 9) / 2f - 1, 8f, ColorUtil.rgba(255, 255, 255, 150));
        } else {
            r.drawText("regular", s.name(), cx, y + (LINE_H - 9) / 2f - 1, 9f, C_TXT);
        }
    }
    
    private static String keyName(int key) {
        return net.minecraft.client.KeyMapping.createNameSupplier("key.keyboard." + key).get().getString().toUpperCase();
    }
    
    public boolean mouseClicked(int mx, int my, int screenW, int screenH) {
        int popupH = POPUP_H_BASE + settingCount * LINE_H;
        int px = (screenW - POPUP_W) / 2;
        int py = (screenH - popupH) / 2;
        
        px = Math.max(0, Math.min(px, screenW - POPUP_W));
        py = Math.max(0, Math.min(py, screenH - popupH));
        
        if (mx < px || mx >= px + POPUP_W || my < py || my >= py + popupH) {
            return false;
        }
        
        int cx = px + PAD;
        int cw = POPUP_W - PAD * 2;
        int y = py + PAD;
        
        for (Field f : module.getClass().getDeclaredFields()) {
            if (!Setting.class.isAssignableFrom(f.getType())) continue;
            f.setAccessible(true);
            try {
                Setting<?> s = (Setting<?>) f.get(module);
                if (mx >= cx && mx < cx + cw && my >= y && my < y + LINE_H) {
                    if (s instanceof BooleanSetting bs) {
                        bs.toggle();
                        TypeAnim ta = settingAnims.get(s);
                        if (ta != null && !ta.isRunning()) {
                            String want = bs.isEnabled() ? "Включено!" : "Выключено!";
                            ta.setTarget(want);
                        }
                    }
                    return true;
                }
                y += LINE_H;
            } catch (IllegalAccessException ignored) {}
        }
        
        return true;
    }
    
    public Module getModule() {
        return module;
    }
}
