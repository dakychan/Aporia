package ru.help.discord;

import aporia.cc.user.UserData;
import dev.firstdark.rpc.DiscordRpc;
import dev.firstdark.rpc.enums.ErrorCode;
import dev.firstdark.rpc.handlers.DiscordEventHandler;
import dev.firstdark.rpc.models.DiscordJoinRequest;
import dev.firstdark.rpc.models.DiscordRichPresence;
import dev.firstdark.rpc.models.User;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.resources.Identifier;
import ru.Aporia;

import java.io.IOException;

/**
 * Discord RPC Manager using dev.firstdark.rpc library.
 * Manages Discord Rich Presence integration.
 */
@Setter
@Getter
public class DiscordManager {
    private final DiscordDaemonThread discordDaemonThread = new DiscordDaemonThread();
    private boolean running = false;
    private DiscordInfo info = new DiscordInfo("Unknown", "", "");
    private Identifier avatarId;
    private DiscordRpc discordRpc;
    UserData.UserDataClass userData = UserData.getUserData();
    
    /**
     * Session start timestamp in seconds.
     * Preserved across all Discord status updates to maintain accurate session time.
     */
    private long sessionStartTime = 0;
    /**
     * Initialize Discord RPC with application ID 1471901603287142421.
     */
    public void init() {
        try {
            /**
             * Record session start time for persistent Discord timestamp.
             */
            sessionStartTime = System.currentTimeMillis() / 1000;
            
            discordRpc = new DiscordRpc();
            
            DiscordEventHandler handler = new DiscordEventHandler() {
                @Override
                public void ready(User user) {
                    Aporia.getDiscordManager().setInfo(new DiscordInfo(
                        user.getUsername(),
                        "https://cdn.discordapp.com/avatars/" + user.getUserId() + "/" + user.getAvatar() + ".png",
                        user.getUserId()
                    ));
                    
                    /**
                     * Use updatePresence() to ensure consistent timestamp usage.
                     */
                    updatePresence("В меню");
                }
                
                @Override
                public void disconnected(ErrorCode errorCode, String message) {
                    aporia.cc.Logger.INSTANCE.info("Discord RPC disconnected: " + errorCode + " - " + message);
                }
                
                @Override
                public void errored(ErrorCode errorCode, String message) {
                    aporia.cc.Logger.INSTANCE.error("Discord RPC error: " + errorCode + " - " + message, null);
                }
                
                @Override
                public void joinGame(String joinSecret) {
                    /**
                     * Not implemented - join game functionality not needed
                     */
                }
                
                @Override
                public void spectateGame(String spectateSecret) {
                    /**
                     * Not implemented - spectate game functionality not needed
                     */
                }
                
                @Override
                public void joinRequest(DiscordJoinRequest joinRequest) {
                    /**
                     * Not implemented - join request functionality not needed
                     */
                }
            };
            
            discordRpc.init("1471901603287142421", handler, false);
            running = true;
            discordDaemonThread.start();

        } catch (Exception e) {
            aporia.cc.Logger.INSTANCE.error("Failed to initialize Discord RPC: " + e.getMessage(), e);
        }
    }

    /**
     * Detect current game state.
     * 
     * @return Game state description
     */
    private String detectGameState() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        
        if (mc.level == null) {
            return "В меню";
        }
        
        if (mc.isLocalServer()) {
            return "Одиночная игра";
        }
        
        net.minecraft.client.multiplayer.ServerData serverData = mc.getCurrentServer();
        if (serverData != null) {
            return serverData.ip;
        }
        
        return "В игре";
    }
    
    /**
     * Update Discord presence while preserving session start time.
     * 
     * @param state Current game state description
     */
    public void updatePresence(String state) {
        if (!running || discordRpc == null) {
            return;
        }
        
        DiscordRichPresence presence = DiscordRichPresence.builder()
            .startTimestamp(sessionStartTime)
            .state(state)
            .details("UUID : " + userData.getUuid())
            .largeImageKey("aporia")
            .button(DiscordRichPresence.RPCButton.of("Discord", "https://discord.gg/TPdfGKs7B3"))
            .build();
        
        discordRpc.updatePresence(presence);
    }
    
    /**
     * Update Discord status with current game state.
     * Detects whether the player is in menu, single player, or multiplayer
     * and updates Discord Rich Presence accordingly.
     * Safe to call frequently - includes null checks for Discord RPC state.
     */
    public void updateGameState() {
        if (!running || discordRpc == null) {
            return;
        }
        
        String state = detectGameState();
        updatePresence(state);
    }
    
    /**
     * Stop Discord RPC.
     */
    public void stopRPC() {
        if (discordRpc != null) {
            discordRpc.shutdown();
        }
        this.running = false;
    }
    
    /**
     * Check if Discord RPC is running.
     * 
     * @return true if running
     */
    public boolean isRunning() {
        return this.running;
    }

    /**
     * Load avatar texture from Discord.
     * 
     * @throws IOException if avatar cannot be loaded
     */
    /* public void load() throws IOException {
        if (avatarId == null && info != null && !info.avatarUrl().isEmpty()) {
            avatarId = ru.files.avatar.BufferUtil.registerDynamicTexture("avatar-", ru.files.avatar.BufferUtil.getHeadFromURL(info.avatarUrl()));
        }
    } */

    /**
     * Background thread for Discord RPC callbacks.
     */
    private class DiscordDaemonThread extends Thread {
        @Override
        public void run() {
            this.setName("Discord-RPC");

            try {
                while (Aporia.getDiscordManager().isRunning()) {
                    if (discordRpc != null) {
                        discordRpc.runCallbacks();
                    }
                    Thread.sleep(15000);
                }
            } catch (Exception exception) {
                aporia.cc.Logger.INSTANCE.error("Discord RPC thread error: " + exception.getMessage(), exception);
                stopRPC();
            }
            super.run();
        }
    }

    /**
     * Discord user information record.
     */
    public record DiscordInfo(String userName, String avatarUrl, String userId) {}
}
