package aporia.su.modules.impl.movement;

import anidumpproject.api.annotation.Native;
import lombok.Getter;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import aporia.su.events.api.EventHandler;
import aporia.su.events.impl.PacketEvent;
import aporia.su.events.impl.TickEvent;
import aporia.su.modules.module.ModuleStructure;
import aporia.su.modules.module.category.ModuleCategory;
import aporia.su.modules.module.setting.implement.BooleanSetting;
import aporia.su.util.Instance;

public class AutoSprint extends ModuleStructure {
    public static AutoSprint getInstance() {
        return Instance.get(AutoSprint.class);
    }

    private static volatile boolean serverSprintState = false;

    @Getter
    private final BooleanSetting noReset = new BooleanSetting("Не сбрасывать спринт", "Don't reset sprint for crits")
            .setValue(false);

    public AutoSprint() {
        super("AutoSprint", ModuleCategory.MOVEMENT);
        settings(noReset);
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void onPacket(PacketEvent event) {
        if (event.getType() != PacketEvent.Type.SEND)
            return;
        if (!(event.getPacket() instanceof ClientCommandC2SPacket packet))
            return;

        if (packet.getMode() == ClientCommandC2SPacket.Mode.START_SPRINTING) {
            if (serverSprintState) {
                event.cancel();
                return;
            }
            serverSprintState = true;
        } else if (packet.getMode() == ClientCommandC2SPacket.Mode.STOP_SPRINTING) {
            if (!serverSprintState) {
                event.cancel();
                return;
            }
            serverSprintState = false;
        }
    }

    public static boolean isServerSprinting() {
        return serverSprintState;
    }

    public static void resetServerState() {
        serverSprintState = false;
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent e) {
        if (mc.player == null)
            return;

        processSprint();
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void processSprint() {
        boolean horizontal = mc.player.horizontalCollision && !mc.player.collidedSoftly;
        boolean sneaking = mc.player.isSneaking() && !mc.player.isSwimming();
        boolean canSprint = !horizontal && mc.player.forwardSpeed > 0;

        if (sneaking)
            return;

        if (canSprint && !mc.player.isSprinting()) {
            mc.player.setSprinting(true);
        }
    }

    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void deactivate() {
        resetServerState();
    }
}