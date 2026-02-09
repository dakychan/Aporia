package zenith.zov.utility.mixin.client;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zenith.zov.Zenith;
import zenith.zov.client.modules.impl.render.SwingAnimation;

@Mixin(LivingEntity.class)
public abstract class PlayerEntityMixin {

    @Inject(method = "getHandSwingDuration", at = @At("HEAD"), cancellable = true)
    private void getArmSwingAnimationEnd(CallbackInfoReturnable<Integer> info) {
        SwingAnimation swingAnimation =SwingAnimation.INSTANCE;
        if ( swingAnimation.isEnabled()) {
            info.setReturnValue((int) swingAnimation.swingPower.getCurrent());
        }
    }
}


