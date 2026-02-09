package zenith.zov.utility.mixin.minecraft.render;

import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zenith.zov.client.modules.impl.render.WorldTweaks;


@Mixin(ClientWorld.Properties.class)
public class ClientWorldPropertiesMixin  {

    @Shadow private long timeOfDay;

    @Inject(method = "setTimeOfDay", at = @At("HEAD"), cancellable = true)
    public void setTimeOfDayHook(long timeOfDay, CallbackInfo ci) {
        WorldTweaks tweaks = WorldTweaks.INSTANCE;
        if (tweaks.isEnabled()) {
            this.timeOfDay = (long) (tweaks.timeSetting.getCurrent() * 1000L);
            ci.cancel();
        }
    }
}
