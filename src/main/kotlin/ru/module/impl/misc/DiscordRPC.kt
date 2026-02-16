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
            ru.files.Logger.error("Failed to initialize Discord RPC: ${e.message}", e)
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
            ru.files.Logger.error("Failed to stop Discord RPC: ${e.message}", e)
        }
    }
    
    override fun onTick() {
        /**
         * No tick logic needed - Discord RPC runs in background thread
         */
    }
}
