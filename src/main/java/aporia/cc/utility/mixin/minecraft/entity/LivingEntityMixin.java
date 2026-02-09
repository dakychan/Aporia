package aporia.cc.utility.mixin.minecraft.entity;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import aporia.cc.Aporia;
import aporia.cc.utility.interfaces.IMinecraft;

@Mixin(LivingEntity.class)
public class LivingEntityMixin implements IMinecraft {

    @Redirect(method = "jump", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getYaw()F"))
    public float replaceMovePacketPitch(LivingEntity instance) {
        if ((Object) this != mc.player) {
            return instance.getYaw();
        }else{
            return Aporia.getInstance().getRotationManager().getCurrentRotation().getYaw();
        }




    }


}

