package so.aporia.module.impl.misc;

import net.minecraft.client.Minecraft;
import so.aporia.module.Category;
import so.aporia.module.Module;
import so.aporia.module.settings.BooleanSetting;
import so.aporia.module.settings.SelectSetting;
import so.aporia.utils.events.EventBus;
import so.aporia.utils.events.EventHandler;
import so.aporia.utils.events.impl.TickEvent;
import so.aporia.utils.user.logger.Logger;

/**
 * ServerHelper - помощник для различных серверов.
 * Включает AutoFlyMe для JeNr0.
 * 
 * При падении автоматически отправляет /flyme на сервере JeNr0.
 */
public final class ServerHelper extends Module {

    private static final Minecraft mc = Minecraft.getInstance();
    
    // Settings
    private final SelectSetting mode = new SelectSetting("Тип сервера", "Позволяет выбрать тип сервера")
        .value("ReallyWorld", "HolyWorld", "FunTime", "JeNr0")
        .selected("JeNr0");
    
    private final BooleanSetting autoFlyMe = new BooleanSetting("AutoFlyMe", 
        "Автоматически активирует /flyme при падении", true,
        () -> mode.isSelected("JeNr0"));
    
    // State
    private long lastFlymeAttempt = 0;
    private static final long FLYME_COOLDOWN = 100; // ms between attempts
    
    public ServerHelper() {
        super("ServerHelper", Category.MISC);
    }
    
    @Override
    protected void onEnable() {
        EventBus.INSTANCE.register(this);
        Logger.info("ServerHelper enabled - mode: " + mode.get());
    }

    @Override
    protected void onDisable() {
        EventBus.INSTANCE.unregister(this);
        Logger.info("ServerHelper disabled");
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (!autoFlyMe.isEnabled() || !mode.isSelected("JeNr0")) {
            return;
        }
        
        if (mc.player == null) {
            return;
        }
        
        // Проверяем условия для активации /flyme
        boolean isFalling = !mc.player.onGround() && mc.player.getY() < -0.5;
        boolean notFlying = !mc.player.getAbilities().flying;
        boolean hasVelocity = Math.abs(mc.player.getY()) > 0.1;
        
        if (isFalling && notFlying && hasVelocity) {
            long currentTime = System.currentTimeMillis();
            
            // Отправляем каждые FLYME_COOLDOWN пока падаем
            if (currentTime - lastFlymeAttempt >= FLYME_COOLDOWN) {
                sendFlymeCommand();
                lastFlymeAttempt = currentTime;
            }
        }
    }
    
    /**
     * Отправка команды /flyme на сервер.
     */
    private void sendFlymeCommand() {
        if (mc.player != null && mc.player.connection != null) {
            mc.player.connection.sendChat("flyme");
        }
    }
    
    // Getters for settings - used by ClickGui to access them
    public SelectSetting getMode() { return mode; }
    public BooleanSetting getAutoFlyMe() { return autoFlyMe; }
}
