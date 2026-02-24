package ru.module.impl.misc

import ru.module.Module
import ru.Aporia

/**
 * DiscordRPC module - Discord Rich Presence integration.
 * 
 * Shows game status in Discord with:
 * - Role information
 * - UID display
 * - Telegram and Discord links
 */
class DiscordRPC : Module("DiscordRPC", "Discord Rich Presence", C.MISC) {
    
    /**
     * Last time Discord game state was updated (in milliseconds).
     * Used for throttling updates to once per second.
     */
    private var lastUpdateTime: Long = 0
    
    /**
     * Update interval in milliseconds (1 second).
     */
    private val UPDATE_INTERVAL_MS: Long = 1000
    
    override fun onEnable() {
        /**
         * Initialize Discord RPC when module is enabled
         */
        try {
            val discordManager = Aporia.getDiscordManager()
            if (discordManager != null && !discordManager.isRunning()) {
                discordManager.init()
            }
        } catch (e: Exception) {
            aporia.cc.Logger.error("Failed to initialize Discord RPC: ${e.message}", e)
        }
    }
    
    override fun onDisable() {
        /**
         * Stop Discord RPC when module is disabled
         */
        try {
            val discordManager = Aporia.getDiscordManager()
            if (discordManager != null && discordManager.isRunning()) {
                discordManager.stopRPC()
            }
        } catch (e: Exception) {
            aporia.cc.Logger.error("Failed to stop Discord RPC: ${e.message}", e)
        }
    }
    
    override fun onTick() {
        /**
         * Update Discord game state once per second.
         * Throttled to prevent excessive updates while ensuring
         * state changes are reflected within 1 second.
         */
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdateTime >= UPDATE_INTERVAL_MS) {
            try {
                val discordManager = Aporia.getDiscordManager()
                if (discordManager != null && discordManager.isRunning()) {
                    discordManager.updateGameState()
                    lastUpdateTime = currentTime
                }
            } catch (e: Exception) {
                aporia.cc.Logger.error("Failed to update Discord game state: ${e.message}", e)
            }
        }
    }
}
