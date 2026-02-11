package ru.module.impl.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import ru.module.Module;

public class AutoFlyMe extends Module {
    private long lastSpacePress = 0;
    private int currentRepeat = 0;
    private boolean isExecuting = false;
    private boolean wasFalling = false;
    private double lastY = 0;
    private long lastFallCommandTime = 0;
    private static final long FALL_COMMAND_COOLDOWN = 500;

    private boolean receivedCantFlyMessage = false;
    private long cantFlyMessageTime = 0;
    private static final long CANT_FLY_RETRY_DELAY = 100;
    
    private final NumberSetting timeDelay;
    private final NumberSetting repeatCount;
    private final BooleanSetting useFlyme;
    private final BooleanSetting onFall;
    private final ModeSetting mode;
    
    public AutoFlyMe() {
        super("AutoFlyMe", "Автоматически прописывает /fly или /flyme при падении", C.MISC, -1);
        
        timeDelay = new NumberSetting("Задержка", 1.0, 0.1, 2.0, 0.1);
        repeatCount = new NumberSetting("Повторы", 1.0, 1.0, 5.0, 1.0);
        useFlyme = new BooleanSetting("Использовать /flyme", false);
        onFall = new BooleanSetting("При падении", true);
        mode = new ModeSetting("Режим", "Авто", "Авто", "Ручной", "Смешанный");
        
        addSetting(timeDelay);
        addSetting(repeatCount);
        addSetting(useFlyme);
        addSetting(onFall);
        addSetting(mode);
    }

    @Override
    public void onEnable() {
        lastSpacePress = 0;
        currentRepeat = 0;
        isExecuting = false;
        wasFalling = false;
        receivedCantFlyMessage = false;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            lastY = mc.player.getY();
        }
    }

    @Override
    public void onDisable() {
        isExecuting = false;
        currentRepeat = 0;
        wasFalling = false;
        receivedCantFlyMessage = false;
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (onFall.getValue()) {
            checkFalling(mc);
        }

        if (mc.options.keyJump.isDefault() && !isExecuting) {
            isExecuting = true;
            lastSpacePress = System.currentTimeMillis();
            currentRepeat = 0;
        }
        
        if (isExecuting && currentRepeat < repeatCount.getValue().intValue()) {
            long elapsed = System.currentTimeMillis() - lastSpacePress;
            long delayMs = (long) (timeDelay.getValue() * 1000);
            if (elapsed >= delayMs * currentRepeat) {
                executeCommand();
                currentRepeat++;
                if (currentRepeat >= repeatCount.getValue().intValue()) {
                    isExecuting = false;
                }
            }
        }
        
        if (receivedCantFlyMessage) {
            long elapsed = System.currentTimeMillis() - cantFlyMessageTime;
            if (elapsed >= CANT_FLY_RETRY_DELAY) {
                if (isFalling(mc)) {
                    executeCommand();
                    cantFlyMessageTime = System.currentTimeMillis();
                } else {
                    receivedCantFlyMessage = false;
                }
            }
        }
    }
    
    private void checkFalling(Minecraft mc) {
        boolean isFalling = isFalling(mc);
        if (isFalling && !wasFalling) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastFallCommandTime >= FALL_COMMAND_COOLDOWN) {
                executeCommand();
                lastFallCommandTime = currentTime;
            }
        }
        wasFalling = isFalling;
        lastY = mc.player.getY();
    }
    
    private boolean isFalling(Minecraft mc) {
        if (mc.player == null) return false;
        return mc.player.getDeltaMovement().y < -0.1
                && !mc.player.onGround()
                && !mc.player.getAbilities().flying;
    }

    private void executeCommand() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            String command = useFlyme.getValue() ? "flyme" : "fly";
            mc.player.connection.sendCommand(command);
        }
    }

    public void onChatMessage(Component message) {
        String text = message.getString().toLowerCase();

        if (text.contains("вы не можете летать") ||
            text.contains("you cannot fly") ||
            text.contains("you can't fly") ||
            text.contains("flying is not enabled")) {
            
            receivedCantFlyMessage = true;
            cantFlyMessageTime = System.currentTimeMillis();
        }
    }
}
