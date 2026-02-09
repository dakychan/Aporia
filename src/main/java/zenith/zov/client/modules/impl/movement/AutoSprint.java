package zenith.zov.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;


import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import zenith.zov.base.events.impl.player.EventUpdate;
import zenith.zov.client.modules.api.Category;
import zenith.zov.client.modules.api.Module;
import zenith.zov.client.modules.api.ModuleAnnotation;
import zenith.zov.utility.game.player.PlayerIntersectionUtil;

import java.util.List;

@ModuleAnnotation(name = "AutoSprint", category = Category.MOVEMENT, description = "Автоматически включает спринт")
public final class AutoSprint extends Module {
    public static final AutoSprint INSTANCE = new AutoSprint();
    private AutoSprint() {
    }
    @EventTarget
    public void onUpdate(EventUpdate event) {
        mc.options.sprintKey.setPressed(true);
    }
}
