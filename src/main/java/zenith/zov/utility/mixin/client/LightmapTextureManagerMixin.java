package zenith.zov.utility.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zenith.zov.client.modules.impl.render.WorldTweaks;

@Mixin(LightmapTextureManager.class)
public class LightmapTextureManagerMixin {

    @ModifyExpressionValue(method = "update(F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;"))
    private Object injectXRayFullBright(Object original) {
        WorldTweaks tweaks = WorldTweaks.INSTANCE;
        if (tweaks.isEnabled() && tweaks.modeSetting.isEnable(0)) {
            return Math.max((double) original, tweaks.brightSetting.getCurrent() * 10);
        }
        return original;
    }
}
