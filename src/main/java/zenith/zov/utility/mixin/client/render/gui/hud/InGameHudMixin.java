package zenith.zov.utility.mixin.client.render.gui.hud;

import com.darkmagician6.eventapi.EventManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zenith.zov.base.events.impl.render.EventHudRender;
import zenith.zov.base.events.impl.render.EventRender2D;
import zenith.zov.client.modules.impl.render.Crosshair;
import zenith.zov.client.modules.impl.render.Interface;
import zenith.zov.utility.render.display.base.CustomDrawContext;
import zenith.zov.utility.render.display.shader.DrawUtil;
import zenith.zov.client.modules.api.Module;

import static zenith.zov.utility.interfaces.IMinecraft.mc;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    @Inject(method = "render", at = @At("HEAD"))
    public void onRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        DrawUtil.blurProgram.draw();
        DrawUtil.beginGui();
        try {
            CustomDrawContext customDrawContext = new CustomDrawContext(context);
            EventManager.call(new EventRender2D(customDrawContext, tickCounter.getTickProgress(false)));
        } finally {
            DrawUtil.endGui();
        }

    }

    @Inject(method = "render", at = @At("TAIL"))
    public void onRenderTail(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        DrawUtil.beginGui();
        try {
            CustomDrawContext customDrawContext = new CustomDrawContext(context);
            EventManager.call(new EventHudRender(customDrawContext, tickCounter.getTickProgress(false)));
        } finally {
            DrawUtil.endGui();
        }
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void removeVanillaCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        try {
            if (mc.currentScreen != null) {
                ci.cancel();
                return;
            }
            Module crosshairModule = Crosshair.INSTANCE;
            if ( crosshairModule.isEnabled()) {
                ci.cancel();
            }
        } catch (Exception e) {
            // PIZDEC
        }
    }


    @Inject(method = "renderMainHud", at = @At(value = "HEAD"), cancellable = true)
    private void renderMainHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (mc.interactionManager.getCurrentGameMode() != GameMode.SPECTATOR) {
            Interface interfaceModule = Interface.INSTANCE;
            if (interfaceModule.isEnabled() && interfaceModule.isEnableHotBar()) {
                ci.cancel();
            }
        }
    }
    @Inject(method = "renderPlayerList", at = @At(value = "HEAD"), cancellable = true)
    private void inject(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
               Interface interfaceModule = Interface.INSTANCE;
            if (interfaceModule.isEnabled() && interfaceModule.isEnableTab()) {
                ci.cancel();
            }

    }
    @Inject(method = "renderOverlayMessage", at = @At(value = "HEAD"), cancellable = true)
    private void injectRenderOverlayMessage(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (mc.interactionManager.getCurrentGameMode() != GameMode.SPECTATOR) {
            Interface interfaceModule = Interface.INSTANCE;
            if (interfaceModule.isEnabled() && interfaceModule.isEnableHotBar()) {
                ci.cancel();
            }
        }
    }
    @Inject(method = "renderScoreboardSidebar*", at = @At(value = "HEAD"), cancellable = true)
    private void injectRenderScoreboardSidebar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {

            Interface interfaceModule = Interface.INSTANCE;
            if (interfaceModule.isEnabled() && interfaceModule.isEnableScoreBar()) {
                ci.cancel();
            }

    }


    @ModifyVariable(
            method = "renderStatusBars",
            at = @At(value = "STORE"),
            ordinal = 3
    )
    private int modifyM(int original, DrawContext context) {
        if (mc.interactionManager.getCurrentGameMode() != GameMode.SPECTATOR) {
            Interface interfaceModule = Interface.INSTANCE;
            if (interfaceModule.isEnabled() && interfaceModule.isEnableHotBar()) {

                return context.getScaledWindowWidth() / 2 +90 + 36;
            }
        }
        return original;
    }


}
