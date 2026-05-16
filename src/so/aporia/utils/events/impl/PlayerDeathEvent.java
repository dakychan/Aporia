/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.utils.events.impl;

import net.minecraft.world.entity.player.Player;

/**
 * Fired when a player dies.
 */
public final class PlayerDeathEvent {

    private final Player player;
    private final String playerName;

    public PlayerDeathEvent(Player player) {
        this.player = player;
        this.playerName = player != null ? player.getName().getString() : "unknown";
    }

    public Player player()     { return player; }
    public String playerName() { return playerName; }
}
