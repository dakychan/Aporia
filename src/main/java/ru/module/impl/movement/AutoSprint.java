package ru.module.impl.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import ru.module.Module;

public class AutoSprint extends Module {
    
    private final BooleanSetting onlyForward;
    private final BooleanSetting keepSprint;
    private final ModeSetting mode;
    private final NumberSetting delay;
    
    public AutoSprint() {
        super("AutoSprint", "Автоматически активирует спринт", C.MOVEMENT);
        
        onlyForward = new BooleanSetting("Только вперед", true);
        keepSprint = new BooleanSetting("Сохранять спринт", false);
        mode = new ModeSetting("Режим", "Всегда", "Всегда", "При движении", "Умный");
        delay = new NumberSetting("Задержка", 0.0, 0.0, 10.0, 0.5);
        
        addSetting(onlyForward);
        addSetting(keepSprint);
        addSetting(mode);
        addSetting(delay);
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
        if (!keepSprint.getValue()) {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            
            if (player != null) {
                player.setSprinting(false);
            }
        }
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        
        if (player == null) return;
        
        boolean movingForward = mc.options.keyUp.isDown();
        
        if (onlyForward.getValue()) {
            if (movingForward && !player.isSprinting()) {
                player.setSprinting(true);
            }
        } else {
            boolean moving = movingForward || mc.options.keyLeft.isDown() || 
                           mc.options.keyRight.isDown() || mc.options.keyDown.isDown();
            if (moving && !player.isSprinting()) {
                player.setSprinting(true);
            }
        }
    }
}
