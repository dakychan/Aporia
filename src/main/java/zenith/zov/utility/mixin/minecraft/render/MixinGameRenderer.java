package zenith.zov.utility.mixin.minecraft.render;

import com.darkmagician6.eventapi.EventManager;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import zenith.zov.base.events.impl.render.*;
import zenith.zov.utility.render.display.base.UIContext;
import zenith.zov.utility.render.level.Render3DUtil;
import zenith.zov.utility.render.display.shader.DrawUtil;

import static zenith.zov.utility.interfaces.IMinecraft.mc;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {

    @Shadow public abstract float getFarPlaneDistance();

    @Inject(method = "getBasicProjectionMatrix", at = @At("TAIL"), cancellable = true)
    public void getBasicProjectionMatrixHook(float fovDegrees, CallbackInfoReturnable<Matrix4f> cir) {
        EventAspectRatio eventAspectRatio = new EventAspectRatio();
        EventManager.call(eventAspectRatio);
        if (eventAspectRatio.isCancelled()) {
            Matrix4f matrix4f = new Matrix4f();
            matrix4f.perspective(fovDegrees * 0.01745329238474369F, eventAspectRatio.getRatio(), 0.05f, getFarPlaneDistance());
            cir.setReturnValue(matrix4f);
        }
    }

    @ModifyExpressionValue(method = "getFov", at = @At(value = "INVOKE", target = "Ljava/lang/Integer;intValue()I", remap = false))
    private int hookGetFov(int original) {
        EventFov event = new EventFov();
        EventManager.call(event);
        if (event.isCancelled()) return event.getFov();
        return original;
    }

    @Inject(
            method = "renderWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/RenderSystem;setProjectionMatrix(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/systems/ProjectionType;)V",
                    ordinal = 1,
                    shift = At.Shift.BEFORE
            )
    )
    public void hookWorldRender(
            RenderTickCounter tickCounter,
            CallbackInfo ci,
            @Local(ordinal = 0) Matrix4f projectionMatrix,
            @Local(ordinal = 2) Matrix4f viewRotationMatrix
    ) {
        MatrixStack matrixStack = new MatrixStack();
        matrixStack.multiplyPositionMatrix(viewRotationMatrix);

        Render3DUtil.setLastProjMat(new Matrix4f(projectionMatrix));
        Render3DUtil.setLastModMat(RenderSystem.getModelViewMatrix());
        Render3DUtil.setLastWorldSpaceMatrix(new Matrix4f(viewRotationMatrix));

        EventRender3D event = new EventRender3D(matrixStack, tickCounter.getTickProgress(false));
        EventManager.call(event);
        Render3DUtil.onEventRender3D(event.getMatrix());
    }

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
                    shift = At.Shift.AFTER
            ),
            locals = LocalCapture.CAPTURE_FAILHARD //Пиздец
    )
    private void renderScreenHook(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci, @Local(index = 5) int mouseX, @Local(index = 6) int mouseY, @Local(index = 8) DrawContext drawContext) {
        DrawUtil.beginGui();
        try {
            EventManager.call(new EventRenderScreen(UIContext.of(drawContext, mouseX, mouseY, tickCounter.getTickProgress(false))));
        } finally {
            DrawUtil.endGui();
        }

    }

}
