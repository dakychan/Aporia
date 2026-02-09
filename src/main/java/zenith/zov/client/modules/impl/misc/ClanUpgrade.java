package zenith.zov.client.modules.impl.misc;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.hit.BlockHitResult;
import com.darkmagician6.eventapi.EventTarget;
import zenith.zov.base.events.impl.player.EventUpdate;
import zenith.zov.client.modules.api.Category;
import zenith.zov.client.modules.api.Module;
import zenith.zov.client.modules.api.ModuleAnnotation;
import zenith.zov.utility.game.other.MessageUtil;
import zenith.zov.utility.interfaces.IMinecraft;


//POWERED BY H0NEY


@ModuleAnnotation(name = "ClanUpgrade", category = Category.MISC, description = "Автоматическое улучшение клана")
public final class ClanUpgrade extends Module implements IMinecraft {
    public static final ClanUpgrade INSTANCE = new ClanUpgrade();
    private static final float PITCH_FOR_PLACING = 90.0F;
    private static final float YAW_FOR_PLACING = 0.0F;
    private float originalPitch = 0.0f;
    private float originalYaw = 0.0f;
    private boolean rotationChanged = false;

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            return;
        }



        ItemStack heldItem = mc.player.getStackInHand(Hand.MAIN_HAND);
        if (heldItem.getItem() != Items.REDSTONE) {
            MessageUtil.displayError("Возьмите редстоун в руку!");
            return;
        }

        BlockPos targetPos = mc.player.getBlockPos();
        BlockState stateAtTargetPos = mc.world.getBlockState(targetPos);

        if (!rotationChanged) {
            originalPitch = mc.player.getPitch();
            originalYaw = mc.player.getYaw();
            rotationChanged = true;
        }

        mc.player.setPitch(PITCH_FOR_PLACING);
        mc.player.setYaw(YAW_FOR_PLACING);



        if (stateAtTargetPos.isAir()) {
            tryPlaceRedstone(targetPos);
        } else if (stateAtTargetPos.getBlock() == Blocks.REDSTONE_WIRE) {
            tryBreakRedstone(targetPos);
        }
    }

    private void tryPlaceRedstone(BlockPos posToPlaceAt) {
        BlockPos supportBlockPos = posToPlaceAt.down();
        BlockState supportBlockState = mc.world.getBlockState(supportBlockPos);

        if (supportBlockState.isSolid()) {
            Vec3d hitVec = new Vec3d(
                    supportBlockPos.getX() + 0.5,
                    supportBlockPos.getY() + 1.0,
                    supportBlockPos.getZ() + 0.5
            );

            BlockHitResult rayTraceResult = new BlockHitResult(
                    hitVec,
                    net.minecraft.util.math.Direction.UP,
                    supportBlockPos,
                    false
            );

            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, rayTraceResult);
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }

    private void tryBreakRedstone(BlockPos posToBreak) {
        mc.interactionManager.attackBlock(posToBreak, net.minecraft.util.math.Direction.UP);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (rotationChanged && mc.player != null) {
            mc.player.setPitch(originalPitch);
            mc.player.setYaw(originalYaw);
            rotationChanged = false;
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        rotationChanged = false;
        if (mc.player != null) {
            originalPitch = mc.player.getPitch();
            originalYaw = mc.player.getYaw();
        }
    }
}
