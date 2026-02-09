package zenith.zov.client.modules.impl.player;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.block.Block;
import net.minecraft.util.hit.BlockHitResult;
import zenith.zov.base.events.impl.player.EventUpdate;
import zenith.zov.client.modules.api.Category;
import zenith.zov.client.modules.api.Module;
import zenith.zov.client.modules.api.ModuleAnnotation;

@ModuleAnnotation(name = "AutoTool", category = Category.PLAYER, description = "Выбирает лучший инструмент для добычи блоков")
public final class AutoTool extends Module {

    public static final AutoTool INSTANCE = new AutoTool();
    
    private int previousSlot = -1;

    private AutoTool() {
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.player.isCreative()) {
            previousSlot = -1;
            return;
        }

        if (mc.interactionManager.isBreakingBlock() && previousSlot == -1) {
            previousSlot = mc.player.getInventory().getSelectedSlot();
        }

        if (mc.interactionManager.isBreakingBlock()) {
            int toolSlot = findOptimalTool();
            if (toolSlot != -1) {
                mc.player.getInventory().setSelectedSlot(toolSlot);
            }
        } else {
            if (previousSlot != -1) {
                mc.player.getInventory().setSelectedSlot(previousSlot);
                previousSlot = -1;
            }
        }
    }

    private int findOptimalTool() {
        if (mc.player == null || mc.world == null) return 0;

        if (mc.crosshairTarget instanceof BlockHitResult blockHitResult) {
            Block block = mc.world.getBlockState(blockHitResult.getBlockPos()).getBlock();
            return findTool(block);
        }
        return -1;
    }

    private int findTool(Block block) {
        int bestSlot = -1;
        float bestSpeed = 1.0f;

        for (int i = 0; i < 9; i++) {
            float speed = getMiningSpeed(i, block);

            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    private float getMiningSpeed(int slot, Block block) {
        if (mc.player == null) return 0.0f;
        return mc.player.getInventory().getStack(slot).getMiningSpeedMultiplier(block.getDefaultState());
    }

    @Override
    public void onDisable() {
        previousSlot = -1;
        super.onDisable();
    }
}





