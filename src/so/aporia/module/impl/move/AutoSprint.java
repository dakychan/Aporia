/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.module.impl.move;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import so.aporia.module.Category;
import so.aporia.module.Module;
import so.aporia.utils.events.EventBus;
import so.aporia.utils.events.EventHandler;
import so.aporia.utils.events.impl.TickEvent;

public final class AutoSprint extends Module {

    private static final Minecraft mc = Minecraft.getInstance();

    public AutoSprint() {
        super("AutoSprint", Category.MOVE);
    }

    @Override
    protected void onEnable() {
        EventBus.INSTANCE.register(this);
    }

    @Override
    protected void onDisable() {
        EventBus.INSTANCE.unregister(this);
        if (mc.player != null) {
            mc.player.setSprinting(false);
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null) return;

        // Спринтим только если игрок двигается вперёд
        if (isMovingForward()
                && !mc.player.isUsingItem()
                && mc.player.getFoodData().getFoodLevel() > 6
                && !mc.player.isSwimming()
                && !mc.player.isPassenger()) {

            mc.player.setSprinting(true);
        }
    }

    private boolean isMovingForward() {
        return mc.player.zza > 0;
    }
}