package zenith.zov.utility.mixin.minecraft.entity;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import zenith.zov.Zenith;
import zenith.zov.utility.interfaces.IMinecraft;

@Mixin(LivingEntity.class)
public class LivingEntityMixin implements IMinecraft {

    //ЛИКВИДБАБУНС ЧТО ДЕЛАЕТ????
    @Redirect(method = "jump", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getYaw()F"))
    public float replaceMovePacketPitch(LivingEntity instance) {
        if ((Object) this != mc.player) {
            return instance.getYaw();
        }else{
            return Zenith.getInstance().getRotationManager().getCurrentRotation().getYaw();
        }




    }


}
