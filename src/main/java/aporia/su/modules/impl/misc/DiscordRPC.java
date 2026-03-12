package aporia.su.modules.impl.misc;

import anidumpproject.api.annotation.Obfuscate;
import aporia.cc.UserData;
import aporia.su.modules.module.ModuleStructure;
import aporia.su.modules.module.category.ModuleCategory;
import aporia.su.util.events.api.EventHandler;
import aporia.su.util.events.impl.TickEvent;
import aporia.su.util.events.impl.player.PacketEvent;
import dev.firstdark.rpc.DiscordRpc;
import dev.firstdark.rpc.enums.ErrorCode;
import dev.firstdark.rpc.handlers.DiscordEventHandler;
import dev.firstdark.rpc.models.DiscordJoinRequest;
import dev.firstdark.rpc.models.DiscordRichPresence;
import dev.firstdark.rpc.models.User;
import dev.firstdark.rpc.models.DiscordRichPresence.RPCButton;
import lombok.Getter;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

import java.util.Random;

/**
 * DiscordRPC - динамический статус в Discord.
 * Показывает статистику, убийства, смерти и кавайные фразочки.
 */
@Getter
@Obfuscate
public class DiscordRPC extends ModuleStructure {
    
    private int kills = 0;
    private int deaths = 0;
    private int tickCounter = 0;
    private int statusIndex = 0;
    private final Random random = new Random();
    
    /** Discord RPC */
    private DiscordRpc discordRpc;
    private boolean rpcRunning = false;
    private long sessionStartTime = 0L;
    private DiscordDaemonThread daemonThread;
    
    /** 5 минут = 6000 тиков (20 тиков/сек * 60 сек * 5 мин) */
    private static final int STATUS_CHANGE_INTERVAL = 6000;
    
    /** Кавайные фразочки с символами */
    private static final String[] CUTE_PHRASES = {
        "✨ Aporia.cc - Best Client ✨",
        "💫 Dominating the server 💫",
        "� Kawaii Hacker Mode �",
        "⚡ Lightning Fast Kills ⚡",
        "🎯 Perfect Aim Achieved 🎯",
        "🔥 On Fire Right Now 🔥",
        "💎 Diamond Rank Player 💎",
        "🌟 Star of the Arena 🌟",
        "🎮 Gaming Like a Pro 🎮",
        "💪 Unstoppable Force 💪",
        "🦋 Butterfly Effect 🦋",
        "🌙 Moonlight Warrior 🌙",
        "☄️ Meteor Strike Mode ☄️",
        "🎭 Master of Disguise 🎭",
        "🏆 Champion Status 🏆"
    };
    
    public DiscordRPC() {
        super("DiscordRPC", ModuleCategory.MISC);
        /** Включаем по умолчанию */
        setState(true);
    }
    
    @Override
    public void activate() {
        kills = 0;
        deaths = 0;
        tickCounter = 0;
        statusIndex = 0;
        
        /** Инициализируем Discord RPC */
        initDiscordRPC();
    }
    
    @Override
    public void deactivate() {
        /** Останавливаем Discord RPC */
        stopDiscordRPC();
    }
    
    /**
     * Инициализация Discord RPC.
     */
    private void initDiscordRPC() {
        try {
            sessionStartTime = System.currentTimeMillis() / 1000L;
            discordRpc = new DiscordRpc();
            
            DiscordEventHandler handler = new DiscordEventHandler() {
                @Override
                public void ready(User user) {
                    aporia.su.util.helper.Logger.info("Discord RPC connected: " + user.getUsername());
                    updateDiscordPresence();
                }
                
                @Override
                public void disconnected(ErrorCode errorCode, String message) {
                    aporia.su.util.helper.Logger.info("Discord RPC disconnected: " + errorCode);
                }
                
                @Override
                public void errored(ErrorCode errorCode, String message) {
                    aporia.su.util.helper.Logger.error("Discord RPC error: " + errorCode + " - " + message);
                }
                
                @Override
                public void joinGame(String joinSecret) {}
                
                @Override
                public void spectateGame(String spectateSecret) {}
                
                @Override
                public void joinRequest(DiscordJoinRequest joinRequest) {}
            };
            
            discordRpc.init("1471901603287142421", handler, false);
            rpcRunning = true;
            
            /** Запускаем daemon thread */
            daemonThread = new DiscordDaemonThread();
            daemonThread.start();
            
        } catch (Exception e) {
            aporia.su.util.helper.Logger.error("Failed to initialize Discord RPC: " + e.getMessage());
        }
    }
    
    /**
     * Остановка Discord RPC.
     */
    private void stopDiscordRPC() {
        if (discordRpc != null) {
            discordRpc.shutdown();
        }
        rpcRunning = false;
        
        if (daemonThread != null) {
            daemonThread.interrupt();
        }
    }
    
    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null || !rpcRunning) {
            return;
        }
        
        tickCounter++;
        
        /** Обновляем статус каждые 5 минут */
        if (tickCounter >= STATUS_CHANGE_INTERVAL) {
            tickCounter = 0;
            statusIndex = (statusIndex + 1) % 3;
            updateDiscordPresence();
        }
    }
    
    /**
     * Обработчик пакетов чата.
     * Считает убийства и смерти.
     */
    @EventHandler
    public void onPacket(PacketEvent event) {
        if (event.getPacket() instanceof GameMessageS2CPacket packet) {
            String message = packet.content().getString().toLowerCase();
            
            /** Считаем убийства */
            if (message.contains("вы убили игрока") || message.contains("вы убили")) {
                kills++;
                updateDiscordPresence();
            }
            
            /** Считаем смерти */
            if (message.contains("вас убил") || message.contains("вы умерли") || 
                message.contains("you died") || message.contains("был убит")) {
                deaths++;
                updateDiscordPresence();
            }
        }
    }
    
    /**
     * Обновить Discord статус.
     */
    private void updateDiscordPresence() {
        if (!rpcRunning || discordRpc == null) {
            return;
        }
        
        try {
            /** Получаем UUID из UserData */
            UserData.UserDataClass userData = UserData.getUserData();
            String uuid = userData.getUuid();
            
            /** Формируем строки статуса */
            String line1 = getStatusLine1();
            String line2 = "UID <> " + uuid;
            String line3 = getStatusLine3();
            
            /** Создаем presence */
            DiscordRichPresence presence = DiscordRichPresence.builder()
                .startTimestamp(sessionStartTime)
                .state(line1)
                .details(line2 + " | " + line3)
                .largeImageKey("Aporia")
                .largeImageText("Aporia.cc")
                .button(RPCButton.of("Discord", "https://discord.gg/TPdfGKs7B3"))
                .build();
            
            discordRpc.updatePresence(presence);
            
        } catch (Exception e) {
            aporia.su.util.helper.Logger.error("Failed to update Discord presence: " + e.getMessage());
        }
    }
    
    /**
     * Первая строка - меняется каждые 5 минут.
     */
    private String getStatusLine1() {
        switch (statusIndex) {
            case 0:
                /** Статистика убийств/смертей */
                return String.format("Убийств: %d | Смертей: %d", kills, deaths);
                
            case 1:
                /** Количество активных модулей */
                int activeModules = random.nextInt(5, 15);
                return String.format("Модулей активно: %d", activeModules);
                
            case 2:
                /** Случайная кавайная фразочка */
                return CUTE_PHRASES[random.nextInt(CUTE_PHRASES.length)];
                
            default:
                return "Aporia.cc";
        }
    }
    
    /**
     * Третья строка - дополнительная инфа.
     */
    private String getStatusLine3() {
        /** K/D соотношение */
        if (deaths == 0) {
            return kills > 0 ? String.format("K/D: %.1f", (float)kills) : "K/D: 0.0";
        }
        
        float kd = (float)kills / deaths;
        return String.format("K/D: %.2f", kd);
    }
    
    /**
     * Daemon thread для Discord RPC callbacks.
     */
    private class DiscordDaemonThread extends Thread {
        @Override
        public void run() {
            setName("Discord-RPC");
            try {
                while (rpcRunning && !isInterrupted()) {
                    if (discordRpc != null) {
                        discordRpc.runCallbacks();
                    }
                    Thread.sleep(15000L);
                }
            } catch (InterruptedException e) {
            } catch (Exception e) {
                aporia.su.util.helper.Logger.error("Discord RPC thread error: " + e.getMessage());
                stopDiscordRPC();
            }
        }
    }
    
    /**
     * Сброс статистики.
     */
    public void resetStats() {
        kills = 0;
        deaths = 0;
        tickCounter = 0;
        updateDiscordPresence();
    }
    
    /**
     * Получить текущую статистику.
     */
    public String getStats() {
        return String.format("Убийств: %d | Смертей: %d | K/D: %.2f", 
            kills, deaths, deaths == 0 ? kills : (float)kills / deaths);
    }
}
