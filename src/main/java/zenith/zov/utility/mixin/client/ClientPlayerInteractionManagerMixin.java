package zenith.zov.utility.mixin.client;

import com.darkmagician6.eventapi.EventManager;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zenith.zov.base.events.impl.other.EventClickSlot;
import zenith.zov.base.events.impl.player.EventAttack;
import zenith.zov.client.modules.impl.misc.NoInteract;
import zenith.zov.utility.game.server.AutoBuyUtil;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {


    @Inject(method = "clickSlot", at = @At("HEAD"), cancellable = true)
    public void clickSlotHook(int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo info) {
        EventClickSlot event = new EventClickSlot(syncId,slotId,button,actionType);
        EventManager.call(event);
        if (event.isCancelled()) info.cancel();
    }

    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    private void interactBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        Block bs = null;
        if (MinecraftClient.getInstance().world != null) {
            bs = MinecraftClient.getInstance().world.getBlockState(hitResult.getBlockPos()).getBlock();
        }

        NoInteract noInteract =NoInteract.INSTANCE;
        if (noInteract != null && noInteract.isEnabled() && (
                bs == Blocks.CHEST ||
                bs == Blocks.TRAPPED_CHEST ||
                bs == Blocks.FURNACE ||
                bs == Blocks.ANVIL ||
                bs == Blocks.CRAFTING_TABLE ||
                bs == Blocks.HOPPER ||
                bs == Blocks.JUKEBOX ||
                bs == Blocks.NOTE_BLOCK ||
                bs == Blocks.ENDER_CHEST ||
                bs == Blocks.DISPENSER ||
                bs == Blocks.DROPPER ||
                bs instanceof ShulkerBoxBlock ||
                bs instanceof FenceBlock ||
                bs instanceof FenceGateBlock)) {
            cir.setReturnValue(ActionResult.PASS);
        }
    }

}
