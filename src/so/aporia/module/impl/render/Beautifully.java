/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.module.impl.render;

import so.aporia.module.Category;
import so.aporia.module.Module;
import so.aporia.module.settings.BooleanSetting;
import so.aporia.module.settings.MultiSelectSetting;
import so.aporia.utils.events.EventBus;
import so.aporia.utils.events.EventHandler;
import so.aporia.utils.events.impl.RenderHudEvent;
import net.minecraft.client.Minecraft;
import so.aporia.utils.user.render.core.AporiaRenderer;

public final class Beautifully extends Module {

    public final BooleanSetting blur = new BooleanSetting("Blur", "Enable blur on UI elements", true);
    public final MultiSelectSetting features = new MultiSelectSetting("Features", "Toggle UI features")
        .options("Custom Chat");

    public Beautifully() {
        super("Beautifully", Category.VISUAL);
    }

    @Override protected void onEnable()  { EventBus.INSTANCE.register(this); }
    @Override protected void onDisable() { EventBus.INSTANCE.unregister(this); }

    public static boolean isBlurEnabled() {
        var mod = so.aporia.module.ModuleManager.INSTANCE.get("Beautifully");
        return mod != null && mod.isEnabled() && ((Beautifully) mod).blur.isEnabled();
    }

    public static boolean isCustomChatEnabled() {
        var mod = so.aporia.module.ModuleManager.INSTANCE.get("Beautifully");
        if (mod == null || !mod.isEnabled()) return true;
        return ((Beautifully) mod).features.isSelected("Custom Chat");
    }

    @EventHandler
    public void onRenderHud(RenderHudEvent e) {
        if (blur.isEnabled()) {
            AporiaRenderer.INSTANCE.prepareFrameBlur(Minecraft.getInstance(), 15f, 0.75f);
        }
    }
}
