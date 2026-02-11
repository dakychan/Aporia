package ru.mixin.render;

import com.ferra13671.cometrenderer.plugins.minecraft.AbstractMinecraftPlugin;
import com.mojang.blaze3d.opengl.GlStateManager;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.Aporia;
import ru.gui.GuiManager;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/state/GuiRenderState;reset()V", shift = At.Shift.AFTER))
    public void modifyRenderBeforeGui(DeltaTracker deltaTracker, boolean tick, CallbackInfo ci) {
        AbstractMinecraftPlugin.getInstance().setupUIProjection();
        GlStateManager._disableDepthTest();
        Aporia.render();
    }
}
