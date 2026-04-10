/*
 * Copyright (c) 2025-2026 BEVoid Project
 * Distributed under the BEVoid Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.utils.events.impl;

import net.minecraft.network.protocol.Packet;

/**
 * Fired when a packet is sent or received.
 * Direction.INBOUND  — server → client (received)
 * Direction.OUTBOUND — client → server (sent)
 */
public final class PacketEvent {

    public enum Direction { INBOUND, OUTBOUND }

    private final Packet<?> packet;
    private final Direction direction;
    private boolean cancelled;

    public PacketEvent(Packet<?> packet, Direction direction) {
        this.packet    = packet;
        this.direction = direction;
    }

    public Packet<?>  packet()    { return packet; }
    public Direction  direction() { return direction; }

    public void    cancel()        { cancelled = true; }
    public boolean isCancelled()   { return cancelled; }
}
