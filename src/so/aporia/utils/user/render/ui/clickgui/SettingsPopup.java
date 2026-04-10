/*
 * Copyright (c) 2025-2026 BEVoid Project
 * Distributed under the BEVoid Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

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
import java.util.ArrayList;
import java.util.List;
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
    private final Map<Object, Animator> selectAnims = new WeakHashMap<>();
    private final Map<Object, Animator> multiSelectAnims = new WeakHashMap<>();
    private static final long BOOL_FLASH_COOLDOWN = 1000;
    
    private Animator popupAnim = null;
    private long popupOpenTime = 0;
    private GuiGraphics currentGfx = null;
    private int textScrollOffset = 0;
    
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
        
        currentGfx = gfx;
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
        
        // r.drawRectBlurred(scaledX, scaledY, scaledW, scaledH, R, C_BG, 8f);
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
            
            if (optionsMenu != null) {
                optionsMenu.render(r, optionsMenuX, optionsMenuY);
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
            TypeAnim ta = settingAnims.computeIfAbsent(s, k -> {
                TypeAnim a = new TypeAnim(110, 170);
                a.snap(ss.get());
                return a;
            });
            
            Animator selectAnim = selectAnims.computeIfAbsent(s, k -> new Animator(400, so.aporia.utils.user.render.animation.Easing::cubicOut));
            
            String displayText = ta.update();
            if (!ta.isRunning() && !displayText.equals(ss.get())) {
                ta.setTarget(ss.get());
            }
            
            r.drawText("regular", s.name(), cx, y + (LINE_H - 9) / 2f - 1, 9f, C_TXT);
            
            int valueW = 60;
            int valueH = 14;
            int valueX = cx + cw - valueW - 8;
            int valueY = y + (LINE_H - valueH) / 2;
            
            selectAnim.update();
            float selectProg = selectAnim.value();
            int bgAlpha = (int)(120 + selectProg * 80);
            // r.drawRectBlurred(valueX, valueY, valueW, valueH, 4, ColorUtil.rgba(30, 30, 50, bgAlpha), 15f);
            r.drawText("regular", displayText, valueX + 6, valueY + (valueH - 9) / 2f + 1, 8f, ColorUtil.rgba(255, 255, 255, 200));
        } else if (s instanceof TextSetting ts) {
            r.drawText("regular", s.name(), cx, y + (LINE_H - 9) / 2f - 1, 9f, C_TXT);
            
            int inputW = 70;
            int inputH = 14;
            int inputX = cx + cw - inputW - 8;
            int inputY = y + (LINE_H - inputH) / 2;
            
            boolean isFocused = textEditingField != null && textEditingModule != null;
            try {
                if (isFocused) {
                    textEditingField.setAccessible(true);
                    TextSetting checkTs = (TextSetting) textEditingField.get(textEditingModule);
                    isFocused = checkTs == ts;
                }
            } catch (IllegalAccessException ignored) {}
            
            int bgColor = isFocused ? ColorUtil.rgba(40, 60, 100, 180) : ColorUtil.rgba(30, 30, 50, 150);
            // r.drawRectBlurred(inputX, inputY, inputW, inputH, 4, bgColor, 15f);
            
            String text = ts.get().isEmpty() ? "..." : ts.get();
            int textColor = isFocused ? ColorUtil.rgba(255, 255, 255, 255) : ColorUtil.rgba(255, 255, 255, ts.get().isEmpty() ? 100 : 200);
            
            int textX = inputX + 6;
            int textY = (int)(inputY + (inputH - 9) / 2f + 1);
            
            if (currentGfx != null) {
                currentGfx.enableScissor(inputX + 2, inputY, inputX + inputW - 2, inputY + inputH);
            }
            
            if (isFocused) {
                int textWidth = (int) r.getTextWidth("regular", text, 8f);
                int maxWidth = inputW - 12;
                
                if (textWidth > maxWidth) {
                    textScrollOffset = Math.max(0, textWidth - maxWidth);
                } else {
                    textScrollOffset = 0;
                }
                
                r.drawText("regular", text, textX - textScrollOffset, textY, 8f, textColor);
            } else {
                r.drawText("regular", text, textX, textY, 8f, textColor);
            }
            
            if (currentGfx != null) {
                currentGfx.disableScissor();
            }
        } else if (s instanceof BindSetting bs2) {
            r.drawText("regular", s.name(), cx, y + (LINE_H - 9) / 2f - 1, 9f, C_TXT);
            
            int keyW = 50;
            int keyH = 14;
            int keyX = cx + cw - keyW - 8;
            int keyY = y + (LINE_H - keyH) / 2;
            
            boolean isBound = bindingField != null && bindingModule != null;
            try {
                if (isBound) {
                    bindingField.setAccessible(true);
                    BindSetting checkBs = (BindSetting) bindingField.get(bindingModule);
                    isBound = checkBs == bs2;
                }
            } catch (IllegalAccessException ignored) {}
            
            int bgColor = isBound ? ColorUtil.rgba(40, 60, 100, 180) : ColorUtil.rgba(30, 30, 50, 150);
            // r.drawRectBlurred(keyX, keyY, keyW, keyH, 4, bgColor, 15f);
            String keyName = bs2.isBound() ? keyName(bs2.getKey()) : "None";
            
            int keyWidth = (int) r.getTextWidth("regular", keyName, 8f);
            int maxWidth = keyW - 12;
            int keyScrollOffset = 0;
            
            if (keyWidth > maxWidth) {
                keyScrollOffset = Math.max(0, keyWidth - maxWidth);
            }
            
            if (currentGfx != null) {
                currentGfx.enableScissor(keyX + 2, keyY, keyX + keyW - 2, keyY + keyH);
            }
            r.drawText("regular", keyName, keyX + 6 - keyScrollOffset, keyY + (keyH - 9) / 2f + 1, 8f, ColorUtil.rgba(255, 255, 255, 200));
            if (currentGfx != null) {
                currentGfx.disableScissor();
            }
        } else if (s instanceof ButtonSetting btn) {
            int btnW = cw - 16;
            int btnH = 18;
            int btnX = cx + 8;
            int btnY = y + (LINE_H - btnH) / 2;
            r.drawRect(btnX, btnY, btnW, btnH, 5, ColorUtil.rgba(60, 120, 200, 150));
            r.drawText("regular", s.name(), btnX + btnW / 2 - r.getTextWidth("regular", s.name(), 9f) / 2, btnY + (btnH - 9) / 2f + 1, 9f, C_TXT);
        } else if (s instanceof MultiSelectSetting mss) {
            TypeAnim ta = settingAnims.computeIfAbsent(s, k -> {
                TypeAnim a = new TypeAnim(110, 170);
                a.snap(String.join(", ", mss.getSelected()));
                return a;
            });
            
            Animator multiAnim = multiSelectAnims.computeIfAbsent(s, k -> new Animator(400, so.aporia.utils.user.render.animation.Easing::cubicOut));
            
            String displayText = ta.update();
            String current = String.join(", ", mss.getSelected());
            if (!ta.isRunning() && !displayText.equals(current)) {
                ta.setTarget(current);
            }
            
            r.drawText("regular", s.name(), cx, y + (LINE_H - 9) / 2f - 1, 9f, C_TXT);
            
            int valueW = 70;
            int valueH = 14;
            int valueX = cx + cw - valueW - 8;
            int valueY = y + (LINE_H - valueH) / 2;
            
            multiAnim.update();
            float multiProg = multiAnim.value();
            int bgAlpha = (int)(120 + multiProg * 80);
            // r.drawRectBlurred(valueX, valueY, valueW, valueH, 4, ColorUtil.rgba(30, 30, 50, bgAlpha), 15f);
            
            String selected = displayText.isEmpty() ? "None" : displayText;
            r.drawText("regular", selected, valueX + 6, valueY + (valueH - 9) / 2f + 1, 7f, ColorUtil.rgba(255, 255, 255, 200));
        } else {
            r.drawText("regular", s.name(), cx, y + (LINE_H - 9) / 2f - 1, 9f, C_TXT);
        }
    }
    
    private static String keyName(int key) {
        return so.aporia.utils.user.input.KeyCodeMap.getName(key);
    }
    
    public boolean mouseClicked(int mx, int my, int screenW, int screenH, int button) {
        // Обрабатываем колесико отдельно для BindSetting
        if (button == 2) {
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
            
            if (mx >= scaledX && mx < scaledX + scaledW && my >= scaledY && my < scaledY + scaledH) {
                int cx = scaledX + PAD;
                int cw = scaledW - PAD * 2;
                int y = scaledY + PAD;
                
                for (Field f : module.getClass().getDeclaredFields()) {
                    if (!Setting.class.isAssignableFrom(f.getType())) continue;
                    f.setAccessible(true);
                    try {
                        Setting<?> s = (Setting<?>) f.get(module);
                        if (mx >= cx && mx < cx + cw && my >= y && my < y + LINE_H) {
                            if (s instanceof BindSetting bs2) {
                                bs2.setKey(2);  // 2 = MOUSE3 (колесико)
                                // so.aporia.utils.files.impl.ConfigFile.markModuleModified(module);
                                return true;
                            }
                            return false;
                        }
                        y += LINE_H;
                    } catch (IllegalAccessException ignored) {}
                }
            }
            return false;
        }

        // Игнорируем другие кнопки кроме ЛКМ и ПКМ
        if (button > 1) {
            return false;
        }

        if (optionsMenu != null) {
            if (optionsMenu.setting instanceof SelectSetting ss) {
                if (optionsMenu.mouseClicked(mx, my, ss)) {
                    optionsMenu = null;
                    Animator anim = selectAnims.get(ss);
                    if (anim != null) {
                        anim.reset();
                        anim.play();
                    }
                    return true;
                }
            } else if (optionsMenu.setting instanceof MultiSelectSetting mss) {
                if (optionsMenu.mouseClicked(mx, my, mss)) {
                    optionsMenu = null;
                    Animator anim = multiSelectAnims.get(mss);
                    if (anim != null) {
                        anim.reset();
                        anim.play();
                    }
                    return true;
                }
            }
            optionsMenu = null;
            return true;
        }
        
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
                        // so.aporia.utils.files.impl.ConfigFile.markModuleModified(module);
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
                            // so.aporia.utils.files.impl.ConfigFile.markModuleModified(module);
                            Animator anim = selectAnims.get(s);
                            if (anim != null) {
                                anim.reset();
                                anim.play();
                            }
                        } else if (button == 1) {
                            optionsMenu = new OptionsMenu(this, ss, ss.getOptions());
                            optionsMenuX = mx;
                            optionsMenuY = my;
                        }
                    } else if (s instanceof MultiSelectSetting mss) {
                        if (button == 0) {
                            List<String> opts = mss.getOptions();
                            if (!opts.isEmpty()) {
                                List<String> current = mss.getSelected();
                                String currentVal = current.isEmpty() ? opts.get(0) : current.get(0);
                                int idx = opts.indexOf(currentVal);
                                String next = opts.get((idx + 1) % opts.size());
                                mss.setSelected(java.util.Arrays.asList(next));
                                // so.aporia.utils.files.impl.ConfigFile.markModuleModified(module);
                                Animator anim = multiSelectAnims.get(s);
                                if (anim != null) {
                                    anim.reset();
                                    anim.play();
                                }
                            }
                        } else if (button == 1) {
                            optionsMenu = new OptionsMenu(this, mss, mss.getOptions());
                            optionsMenuX = mx;
                            optionsMenuY = my;
                        }
                    } else if (s instanceof ButtonSetting btn) {
                        btn.click();
                        // so.aporia.utils.files.impl.ConfigFile.markModuleModified(module);
                    } else if (s instanceof TextSetting ts) {
                        textEditingField = f;
                        textEditingModule = module;
                    } else if (s instanceof BindSetting bs2) {
                        bindingField = f;
                        bindingModule = module;
                    }
                    return true;
                }
                y += LINE_H;
            } catch (IllegalAccessException ignored) {}
        }
        
        // Если клик был внутри попапа, закрываем его (но не для колесика)
        return true;
    }
    
    public boolean isOpen() {
        return module != null;
    }
    
    public void close() {
        module = null;
        bindingField = null;
        bindingModule = null;
        textEditingField = null;
        textEditingModule = null;
    }
    
    private Field bindingField = null;
    private Module bindingModule = null;
    private Field textEditingField = null;
    private Module textEditingModule = null;
    private OptionsMenu optionsMenu = null;
    private int optionsMenuX = 0;
    private int optionsMenuY = 0;
    
    public boolean keyPressed(int scancode) {
        if (scancode == 1) {
            if (textEditingField != null && textEditingModule != null) {
                textEditingField = null;
                textEditingModule = null;
                return true;
            }
            if (bindingField != null && bindingModule != null) {
                bindingField = null;
                bindingModule = null;
                return true;
            }
            return false;
        }
        
        if (textEditingField != null && textEditingModule != null) {
            if (scancode == 14) {
                try {
                    textEditingField.setAccessible(true);
                    TextSetting ts = (TextSetting) textEditingField.get(textEditingModule);
                    ts.backspace();
                    // so.aporia.utils.files.impl.ConfigFile.markModuleModified(textEditingModule);
                    return true;
                } catch (IllegalAccessException ignored) {}
            } else {
                String keyName = so.aporia.utils.user.input.KeyCodeMap.getName(scancode);
                if (keyName.length() == 1 && keyName.matches("[A-Z0-9]")) {
                    try {
                        textEditingField.setAccessible(true);
                        TextSetting ts = (TextSetting) textEditingField.get(textEditingModule);
                        char c = keyName.charAt(0);
                        
                        boolean shiftPressed = org.lwjgl.glfw.GLFW.glfwGetKey(org.lwjgl.glfw.GLFW.glfwGetCurrentContext(), 340) == org.lwjgl.glfw.GLFW.GLFW_PRESS ||
                                             org.lwjgl.glfw.GLFW.glfwGetKey(org.lwjgl.glfw.GLFW.glfwGetCurrentContext(), 344) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
                        
                        if (c >= 'A' && c <= 'Z') {
                            if (!shiftPressed) {
                                c = (char)(c + 32);
                            }
                        }
                        
                        ts.appendChar(c);
                        // so.aporia.utils.files.impl.ConfigFile.markModuleModified(textEditingModule);
                        return true;
                    } catch (IllegalAccessException ignored) {}
                }
            }
        }
        if (bindingField != null && bindingModule != null) {
            try {
                bindingField.setAccessible(true);
                BindSetting bs = (BindSetting) bindingField.get(bindingModule);
                bs.setKey(scancode);
                // so.aporia.utils.files.impl.ConfigFile.markModuleModified(bindingModule);
                bindingField = null;
                bindingModule = null;
                return true;
            } catch (IllegalAccessException ignored) {}
        }
        return false;
    }
    
    public boolean charTyped(char c, int modifiers) {
        if (textEditingField != null && textEditingModule != null) {
            try {
                textEditingField.setAccessible(true);
                TextSetting ts = (TextSetting) textEditingField.get(textEditingModule);
                if (c >= 32 && c < 127) {
                    ts.appendChar(c);
                    // so.aporia.utils.files.impl.ConfigFile.markModuleModified(textEditingModule);
                    return true;
                }
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
    
    private static class OptionsMenu {
        private Object setting;
        private List<String> options;
        private SettingsPopup popup;
        
        OptionsMenu(SettingsPopup popup, Object setting, List<String> options) {
            this.popup = popup;
            this.setting = setting;
            this.options = new ArrayList<>(options);
        }
        
        void render(AporiaRenderer r, int x, int y) {
            int itemH = 16;
            int menuW = 80;
            int menuH = options.size() * itemH + 4;
            
            // r.drawRectBlurred(x, y, menuW, menuH, 4, ColorUtil.rgba(20, 22, 35, 200), 8f);
            r.drawStroke(x, y, menuW, menuH, 4, 1f, 1, 0f, ColorUtil.rgba(255, 255, 255, 40));
            
            for (int i = 0; i < options.size(); i++) {
                int itemY = y + 2 + i * itemH;
                r.drawText("regular", options.get(i), x + 6, itemY + (itemH - 8) / 2, 8f, ColorUtil.rgba(255, 255, 255, 200));
            }
        }
        
        boolean mouseClicked(int mx, int my, SelectSetting ss) {
            int itemH = 16;
            int menuW = 80;
            int menuH = options.size() * itemH + 4;
            
            if (mx < popup.optionsMenuX || mx >= popup.optionsMenuX + menuW || my < popup.optionsMenuY || my >= popup.optionsMenuY + menuH) {
                return false;
            }
            
            int idx = (my - popup.optionsMenuY - 2) / itemH;
            if (idx >= 0 && idx < options.size()) {
                ss.setSelectedIndex(idx);
                // so.aporia.utils.files.impl.ConfigFile.markModuleModified(popup.module);
                return true;
            }
            return false;
        }
        
        boolean mouseClicked(int mx, int my, MultiSelectSetting mss) {
            int itemH = 16;
            int menuW = 80;
            int menuH = options.size() * itemH + 4;
            
            if (mx < popup.optionsMenuX || mx >= popup.optionsMenuX + menuW || my < popup.optionsMenuY || my >= popup.optionsMenuY + menuH) {
                return false;
            }
            
            int idx = (my - popup.optionsMenuY - 2) / itemH;
            if (idx >= 0 && idx < options.size()) {
                mss.setSelected(java.util.Arrays.asList(options.get(idx)));
                // so.aporia.utils.files.impl.ConfigFile.markModuleModified(popup.module);
                return true;
            }
            return false;
        }
    }
}
