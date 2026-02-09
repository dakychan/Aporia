package zenith.zov.utility.mixin.client.render;

import com.darkmagician6.eventapi.EventManager;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import zenith.zov.Zenith;
import zenith.zov.base.events.impl.entity.EventEntityColor;

import zenith.zov.utility.interfaces.IMinecraft;
import zenith.zov.utility.render.level.Render3DUtil;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> implements IMinecraft {

    @ModifyExpressionValue(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;clampBodyYaw(Lnet/minecraft/entity/LivingEntity;FF)F"))
    public float changeYaw(float oldValue, LivingEntity entity) {
        if (entity.equals(mc.player) &&!Zenith.getInstance().getRotationManager().isSetRotation()) {
            return MathHelper.lerpAngleDegrees(Render3DUtil.getTickDelta(),Zenith.getInstance().getRotationManager().getPreviousRotation().getYaw(),Zenith.getInstance().getRotationManager().getCurrentRotation().getYaw());
        }
        return oldValue;
    }

    @ModifyExpressionValue(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerpAngleDegrees(FFF)F"))
    public float changeHeadYaw(float oldValue, LivingEntity entity) {
        if (entity.equals(mc.player)&&!Zenith.getInstance().getRotationManager().isSetRotation()) {
            return MathHelper.lerpAngleDegrees(Render3DUtil.getTickDelta(),Zenith.getInstance().getRotationManager().getPreviousRotation().getYaw(),Zenith.getInstance().getRotationManager().getCurrentRotation().getYaw());
        }
        return oldValue;
    }

    @ModifyExpressionValue(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getLerpedPitch(F)F"))
    public float changePitch(float oldValue, LivingEntity entity) {
        if (entity.equals(mc.player) &&!Zenith.getInstance().getRotationManager().isSetRotation()) {
            return   MathHelper.lerpAngleDegrees(Render3DUtil.getTickDelta(),Zenith.getInstance().getRotationManager().getPreviousRotation().getPitch(),Zenith.getInstance().getRotationManager().getCurrentRotation().getPitch());

        }
        return oldValue;
    }
    @Shadow
    @Nullable
    protected abstract RenderLayer getRenderLayer(LivingEntityRenderState state, boolean showBody, boolean translucent, boolean showOutline);

    @Redirect(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;getRenderLayer(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;ZZZ)Lnet/minecraft/client/render/RenderLayer;"))
    private RenderLayer renderHook(LivingEntityRenderer instance, LivingEntityRenderState state, boolean showBody, boolean translucent, boolean showOutline) {
        if (!translucent && state.width == 0.6F) {
            EventEntityColor event = new EventEntityColor(-1);
            EventManager.call(event);
            if (event.isCancelled()) translucent = true;
        }
        return this.getRenderLayer(state, showBody, translucent, showOutline);
    }

    @Redirect(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"))
    private void renderModelHook(EntityModel<?> instance, MatrixStack matrixStack, VertexConsumer vertexConsumer, int i, int j, int l, @Local(ordinal = 0, argsOnly = true) LivingEntityRenderState renderState) {
        EventEntityColor event = new EventEntityColor(l);
        if (renderState.invisibleToPlayer) EventManager.call(event);
        instance.render(matrixStack, vertexConsumer, i, j, event.getColor());
    }


}
