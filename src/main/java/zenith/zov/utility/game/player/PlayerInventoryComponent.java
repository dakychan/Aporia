package zenith.zov.utility.game.player;


import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.screen.ingame.AbstractCommandBlockScreen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.gui.screen.ingame.StructureBlockScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import zenith.zov.Zenith;
import zenith.zov.base.events.impl.player.EventMoveInput;
import zenith.zov.base.events.impl.player.EventUpdate;
import zenith.zov.base.request.ScriptManager;
import zenith.zov.client.screens.menu.MenuScreen;
import zenith.zov.utility.interfaces.IMinecraft;

import java.util.List;

@UtilityClass
public class PlayerInventoryComponent implements IMinecraft {
    public final List<KeyBinding> moveKeys = List.of(mc.options.forwardKey, mc.options.backKey, mc.options.leftKey, mc.options.rightKey, mc.options.jumpKey);
    public ScriptManager.ScriptTask script = new ScriptManager.ScriptTask();
    public boolean canMove = true;


    public void addTask(Runnable task) {
        if ( MovingUtil.hasPlayerMovement()) {
            Zenith.getInstance().getScriptManager().addTask(script);

            switch (Zenith.getInstance().getServerHandler().getServer()) {
                case "FunTime", "HolyWorld" -> {


                    script.schedule(EventUpdate.class, eventUpdate -> {
                        PlayerInventoryComponent.disableMoveKeys();
                        return true;
                    });
                    script.schedule(EventUpdate.class, eventUpdate -> {
                        task.run();
                        enableMoveKeys();
                        return true;
                    });
                    return;
                }
                case "ReallyWorld" -> {
                    if (mc.player.isOnGround()) {

                        script.schedule(EventUpdate.class, eventUpdate -> {
                            PlayerInventoryComponent.disableMoveKeys();
                            return true;
                        });

                        script.schedule(EventUpdate.class, eventUpdate -> {
                            PlayerInventoryComponent.disableMoveKeys();
                            return true;
                        });

                        script.schedule(EventUpdate.class, eventUpdate -> {
                            task.run();
                            return true;
                        });
                        script.schedule(EventUpdate.class, eventUpdate -> {
                            enableMoveKeys();
                            return true;
                        });
                        return;
                    }
                }
                case "CopyTime" -> {
                    script.schedule(EventUpdate.class, eventUpdate -> {
                        PlayerInventoryComponent.disableMoveKeys();
                        return true;
                    });
                    script.schedule(EventUpdate.class, eventUpdate -> {
                        task.run();

                        return true;
                    });
                    script.schedule(EventUpdate.class, eventUpdate -> {
                        enableMoveKeys();
                        return true;
                    });
                    return;
                }
            }
        }
        task.run();
    }

    public void disableMoveKeys() {
        canMove = false;
        unPressMoveKeys();
    }

    public void enableMoveKeys() {
        PlayerInventoryUtil.closeScreen(true);
        canMove = true;
        updateMoveKeys();
    }

    public void unPressMoveKeys() {
        moveKeys.forEach(keyBinding -> keyBinding.setPressed(false));
    }

    public void updateMoveKeys() {
        moveKeys.forEach(keyBinding -> keyBinding.setPressed(InputUtil.isKeyPressed(mc.getWindow().getHandle(), keyBinding.getDefaultKey().getCode())));
    }

    public boolean shouldSkipExecution() {
        if (mc.currentScreen == null) return false;
        
        if (PlayerIntersectionUtil.isChat(mc.currentScreen)) return false;
        if (mc.currentScreen instanceof SignEditScreen) return false;
        if (mc.currentScreen instanceof AnvilScreen) return false;
        if (mc.currentScreen instanceof AbstractCommandBlockScreen) return false;
        if (mc.currentScreen instanceof StructureBlockScreen) return false;
        // if (mc.currentScreen instanceof MenuScreen) return false;
        
        if (mc.player != null && mc.player.currentScreenHandler != null) {
            int slotCount = mc.player.currentScreenHandler.slots.size();
            return slotCount >= 27;
        }
        
        return false;
    }
}
