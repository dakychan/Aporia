package aporia.su.modules.impl.movement;

import anidumpproject.api.annotation.Native;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.Blocks;
import aporia.su.events.api.EventHandler;
import aporia.su.events.impl.TickEvent;
import aporia.su.modules.module.ModuleStructure;
import aporia.su.modules.module.category.ModuleCategory;
import aporia.su.modules.module.setting.implement.SelectSetting;
import aporia.su.util.Instance;
import aporia.su.util.move.MoveUtil;
import aporia.su.util.string.PlayerInteractionHelper;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class NoWeb extends ModuleStructure {
    public static NoWeb getInstance() {
        return Instance.get(NoWeb.class);
    }

    public final SelectSetting webMode = new SelectSetting("Режим", "Выберите режим обхода").value("Grim");

    public NoWeb() {
        super("NoWeb", "No Web", ModuleCategory.MOVEMENT);
        settings(webMode);
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent e) {
        if (PlayerInteractionHelper.isPlayerInBlock(Blocks.COBWEB)) {
            double[] speed = MoveUtil.calculateDirection(0.35);
            mc.player.addVelocity(speed[0], 0, speed[1]);
            mc.player.setVelocity(speed[0], mc.options.jumpKey.isPressed() ? 0.65f : mc.options.sneakKey.isPressed() ? -0.65f : 0, speed[1]);
        }
    }
}