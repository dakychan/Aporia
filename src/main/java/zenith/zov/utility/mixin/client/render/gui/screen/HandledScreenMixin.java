package zenith.zov.utility.mixin.client.render.gui.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zenith.zov.client.modules.impl.misc.AHHelper;
import zenith.zov.utility.game.server.AutoBuyUtil;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
    @Unique
    @Mutable
    private boolean isAuc;
    @Unique
    @Mutable
    private Slot lowSumSlotId = null;
    @Unique
    @Mutable
    private Slot lowAllSumSlotId = null;

    @Shadow
    public abstract ScreenHandler getScreenHandler();


    @Shadow @Final protected ScreenHandler handler;

    @Inject(method = "tick", at = @At("HEAD"))
    private void tickScreen(CallbackInfo ci) {

        if (!isAuc &&AHHelper.INSTANCE.isEnabled()) {
            isAuc = AutoBuyUtil.isAuction(this.handler);

        }

        if (isAuc&&AHHelper.INSTANCE.isEnabled()) {
            int lowSum = Integer.MAX_VALUE;
            int allSum = Integer.MAX_VALUE;
            for (int i = 0; i < 44; i++) {
                Slot slot = this.getScreenHandler().slots.get(i);
                if (slot.getStack().isEmpty()) continue;
                int sum = AutoBuyUtil.getPrice(slot.getStack());
                if (sum < lowSum) {
                    lowSumSlotId = slot;
                    lowSum = sum;
                }
                if (sum / slot.getStack().getCount() < allSum) {
                    allSum = sum / slot.getStack().getCount();
                    lowAllSumSlotId = slot;
                }
            }
        }
    }



    @Inject(
            method = "drawSlot(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/screen/slot/Slot;)V",
            at = @At(
                    value = "HEAD"
            )
    )
    private void onDrawSlotInject(DrawContext context, Slot slot, CallbackInfo ci) {

        if (AHHelper.INSTANCE.isEnabled() ) {

            if((slot == lowSumSlotId) ){
                AHHelper.INSTANCE.renderCheat(context, slot);
            }else if((slot == lowAllSumSlotId) ){
                AHHelper.INSTANCE.renderGood(context, slot);
            }
        }
    }
}
