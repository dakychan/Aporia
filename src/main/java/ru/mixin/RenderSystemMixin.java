package ru.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.Aporia;

@Mixin(RenderSystem.class)
public class RenderSystemMixin {

    @Inject(method = "initRenderer", at = @At("TAIL"))
    private static void modifyInitRender(CallbackInfo ci) {
        Aporia.initRender();
    }
}