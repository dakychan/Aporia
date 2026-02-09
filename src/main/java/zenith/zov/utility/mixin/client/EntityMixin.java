package zenith.zov.utility.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import zenith.zov.Zenith;
import zenith.zov.utility.interfaces.IMinecraft;

@Mixin(Entity.class)
public class EntityMixin implements IMinecraft {

    @ModifyExpressionValue(
            method = "move",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;isLogicalSideForUpdatingMovement()Z",
                    ordinal = 1
            )
    )
    public boolean fixFalldistanceValue(boolean original) {
        if ((Object) this == mc.player) {
            return true;
        }

        return original;
    }

    @Redirect(method = "updateVelocity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getYaw()F"))
    public float movementCorrection(Entity instance) {

        if (instance instanceof ClientPlayerEntity) { //ПРИВЕТ ЛЮДИ С БОЛЬШИМ МОНИТОРОМ
            return Zenith.getInstance().getRotationManager().getCurrentRotation().getYaw();
        }

        return instance.getYaw();
    }
    @ModifyVariable(
            method = "getRotationVector(FF)Lnet/minecraft/util/math/Vec3d;",
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    private float modifyPitch(float pitch) {
        if ((Object) this instanceof ClientPlayerEntity) {
            return Zenith.getInstance().getRotationManager().getCurrentRotation().getPitch();
        }
        return pitch;
    }

    @ModifyVariable(
            method = "getRotationVector(FF)Lnet/minecraft/util/math/Vec3d;",
            at = @At("HEAD"),
            ordinal = 1,
            argsOnly = true
    )
    private float modifyYaw(float yaw) {
        if ((Object) this instanceof ClientPlayerEntity) {
            return Zenith.getInstance().getRotationManager().getCurrentRotation().getYaw();
        }
        return yaw;
    }


}
