/*
 * Copyright (c) 2025-2026 BEVoid Project
 * Distributed under the BEVoid Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.module.impl.misc;

import net.minecraft.client.Minecraft;
import so.aporia.module.Category;
import so.aporia.module.Module;
import so.aporia.module.settings.BooleanSetting;
import so.aporia.utils.events.EventBus;
import so.aporia.utils.user.render.ui.clickgui.ClickGuiScreen;

/**
 * ClickGui модуль для управления интерфейсом.
 */
public final class ClickGui extends Module {

    private static final Minecraft mc = Minecraft.getInstance();
    
    private final BooleanSetting showSettings = new BooleanSetting("Show Settings Popup",
        "Показывать попап с настройками модулей", true);
    
    public ClickGui() {
        super("ClickGui", Category.VISUAL, 0x43);
    }
    
    @Override
    protected void onEnable() {
        EventBus.INSTANCE.register(this);
        mc.setScreen(new ClickGuiScreen());
    }

    @Override
    protected void onDisable() {
        EventBus.INSTANCE.unregister(this);
        if (mc.screen instanceof ClickGuiScreen) {
            mc.setScreen(null);
        }
    }
    
    public BooleanSetting getShowSettings() { return showSettings; }
}
