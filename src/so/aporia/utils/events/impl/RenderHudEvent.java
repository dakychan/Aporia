/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.utils.events.impl;

import net.minecraft.client.gui.GuiGraphics;

/** Fired each frame when the HUD is being rendered. */
public final class RenderHudEvent {

    private final GuiGraphics graphics;
    private final float       partialTick;

    public RenderHudEvent(GuiGraphics graphics, float partialTick) {
        this.graphics    = graphics;
        this.partialTick = partialTick;
    }

    public GuiGraphics graphics()    { return graphics; }
    public float       partialTick() { return partialTick; }
}
