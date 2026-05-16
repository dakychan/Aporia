/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.utils.events.impl;

import com.mojang.blaze3d.vertex.PoseStack;

/** Fired each frame when the world is being rendered, after terrain and entities. */
public final class WorldRenderEvent {

    private final PoseStack poseStack;
    private final float partialTick;

    public WorldRenderEvent(PoseStack poseStack, float partialTick) {
        this.poseStack = poseStack;
        this.partialTick = partialTick;
    }

    public PoseStack poseStack() { return poseStack; }
    public float partialTick() { return partialTick; }
}
