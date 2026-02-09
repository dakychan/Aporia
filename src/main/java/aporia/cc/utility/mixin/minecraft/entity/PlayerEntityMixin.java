package aporia.cc.utility.mixin.minecraft.entity;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import aporia.cc.Aporia;
import aporia.cc.utility.game.player.rotation.Rotation;

import static aporia.cc.utility.interfaces.IMinecraft.mc;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {



    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void tickMovement(CallbackInfo ci) {

    }

    @Mutable
    float savedYaw;
    @Mutable
    float savedPitch;
    @Inject(method="travel",at = @At(value = "HEAD"))
    public void fixElytra(CallbackInfo ci){
        if(((Object)this) instanceof ClientPlayerEntity player){
            Rotation currentRotation = Aporia.getInstance().getRotationManager().getCurrentRotation();
            savedYaw = mc.player.getYaw();
            savedPitch = mc.player.getPitch();
            player.setYaw(currentRotation.getYaw());
            player.setPitch(currentRotation.getPitch());
        }

    }
    @Inject(method="travel",at = @At(value = "RETURN"))
    public void fixElytraEnd(CallbackInfo ci){
        if(((Object)this) instanceof ClientPlayerEntity player){

            player.setYaw(savedYaw);
            player.setPitch(savedPitch);
        }

    }
}
