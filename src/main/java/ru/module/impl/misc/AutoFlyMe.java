package ru.module.impl.misc;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import ru.module.Module;

public class AutoFlyMe extends Module {
    private float timeDelay = 1.0f; // Задержка в секундах (0.1 - 2.0)
    private int repeatCount = 1; // Сколько раз повторять (1 - 5)
    private boolean useFlyme = false; // false = /fly, true = /flyme
    
    private long lastSpacePress = 0;
    private int currentRepeat = 0;
    private boolean isExecuting = false;
    
    // Для отслеживания падения
    private boolean wasFalling = false;
    private double lastY = 0;
    private long lastFallCommandTime = 0;
    private static final long FALL_COMMAND_COOLDOWN = 500; // Кулдаун между командами при падении
    
    // Для обработки сообщения "Вы не можете летать"
    private boolean receivedCantFlyMessage = false;
    private long cantFlyMessageTime = 0;
    private static final long CANT_FLY_RETRY_DELAY = 100; // Задержка перед повтором после сообщения
    
    public AutoFlyMe() {
        super("AutoFlyMe", "Автоматически прописывает /fly или /flyme при падении", C.MISC, -1);
    }

    @Override
    public void onEnable() {
        lastSpacePress = 0;
        currentRepeat = 0;
        isExecuting = false;
        wasFalling = false;
        receivedCantFlyMessage = false;
        
        MinecraftClient mc = MinecraftClient.getInstance();
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
        
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        
        // Проверяем падение игрока
        checkFalling(mc);
        
        // Проверяем нажатие пробела (старая логика)
        if (mc.options.jumpKey.isPressed() && !isExecuting) {
            isExecuting = true;
            lastSpacePress = System.currentTimeMillis();
            currentRepeat = 0;
        }
        
        // Выполняем команду с задержкой (старая логика)
        if (isExecuting && currentRepeat < repeatCount) {
            long elapsed = System.currentTimeMillis() - lastSpacePress;
            long delayMs = (long) (timeDelay * 1000);
            
            if (elapsed >= delayMs * currentRepeat) {
                executeCommand();
                currentRepeat++;
                
                if (currentRepeat >= repeatCount) {
                    isExecuting = false;
                }
            }
        }
        
        // Повторная попытка после сообщения "Вы не можете летать"
        if (receivedCantFlyMessage) {
            long elapsed = System.currentTimeMillis() - cantFlyMessageTime;
            if (elapsed >= CANT_FLY_RETRY_DELAY) {
                // Проверяем, все еще падает ли игрок
                if (isFalling(mc)) {
                    executeCommand();
                    cantFlyMessageTime = System.currentTimeMillis();
                } else {
                    receivedCantFlyMessage = false;
                }
            }
        }
    }
    
    private void checkFalling(MinecraftClient mc) {
        boolean isFalling = isFalling(mc);
        
        // Если начал падать
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
    
    private boolean isFalling(MinecraftClient mc) {
        if (mc.player == null) return false;
        
        // Игрок падает если:
        // 1. Его вертикальная скорость отрицательная
        // 2. Он не на земле
        // 3. Он не в воде
        // 4. Он не летает в креативе
        
        boolean isMovingDown = mc.player.getVelocity().y < -0.1;
        boolean notOnGround = !mc.player.isOnGround();
        boolean notInWater = !mc.player.isTouchingWater();
        boolean notFlying = !mc.player.getAbilities().flying;
        
        return isMovingDown && notOnGround && notInWater && notFlying;
    }
    
    private void executeCommand() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            String command = useFlyme ? "flyme" : "fly";
            mc.player.networkHandler.sendChatCommand(command);
        }
    }
    
    /**
     * Вызывается когда игрок получает сообщение в чат.
     * Нужно вызвать этот метод из mixin'а для обработки сообщений.
     */
    public void onChatMessage(Text message) {
        String text = message.getString().toLowerCase();
        
        // Проверяем различные варианты сообщения о невозможности летать
        if (text.contains("вы не можете летать") || 
            text.contains("you cannot fly") ||
            text.contains("you can't fly") ||
            text.contains("flying is not enabled")) {
            
            receivedCantFlyMessage = true;
            cantFlyMessageTime = System.currentTimeMillis();
        }
    }
    
    // Геттеры и сеттеры для настроек
    public float getTimeDelay() {
        return timeDelay;
    }
    
    public void setTimeDelay(float timeDelay) {
        this.timeDelay = Math.max(0.1f, Math.min(2.0f, timeDelay));
    }
    
    public int getRepeatCount() {
        return repeatCount;
    }
    
    public void setRepeatCount(int repeatCount) {
        this.repeatCount = Math.max(1, Math.min(5, repeatCount));
    }
    
    public boolean isUseFlyme() {
        return useFlyme;
    }
    
    public void setUseFlyme(boolean useFlyme) {
        this.useFlyme = useFlyme;
    }
}
