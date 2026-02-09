package zenith.zov.utility.mixin.client;

import com.darkmagician6.eventapi.EventManager;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zenith.zov.Zenith;


import zenith.zov.base.events.impl.other.EventCloseScreen;

import zenith.zov.base.events.impl.player.EventMove;
import zenith.zov.base.events.impl.player.EventSlowWalking;
import zenith.zov.base.events.impl.player.EventSprintUpdate;
import zenith.zov.base.events.impl.player.EventUpdate;
import zenith.zov.utility.game.other.MessageUtil;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayerEntity {

    public ClientPlayerEntityMixin(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }


    @Shadow protected abstract void sendSprintingPacket();

    @Shadow @Final protected MinecraftClient client;

    @Shadow protected abstract void autoJump(float dx, float dz);

    @Inject(method = "tick", at = @At("HEAD"))
    public void tick(CallbackInfo ci) {
        EventManager.call(new EventUpdate());

    }
    @Redirect(method = "sendMovementPackets", at = @At(value = "INVOKE", target ="Lnet/minecraft/client/network/ClientPlayerEntity;sendSprintingPacket()V"))
    public void invokeSprintUpdate(ClientPlayerEntity instance) {
        EventSprintUpdate eventSprintUpdate = new EventSprintUpdate();
        EventManager.call(eventSprintUpdate);
        if (!eventSprintUpdate.isCancelled()) {
            this.sendSprintingPacket();
        }
    }

    @Redirect(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getYaw()F"))
    public float replaceMovePacketYaw(ClientPlayerEntity instance) {

        return Zenith.getInstance().getRotationManager().getCurrentRotation().getYaw();

    }

    @Redirect(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"), require = 0)
    private boolean onIsUsingItemRedirect(ClientPlayerEntity player) {
        if(player.isUsingItem()) {
            EventSlowWalking slowDownEvent = new EventSlowWalking();
            EventManager.call(slowDownEvent);
            return player.isUsingItem() && player.getVehicle() == null && !slowDownEvent.isCancelled();
        }else {
            return player.isUsingItem() && player.getVehicle() == null;
        }
    }

    @Redirect(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getPitch()F"))
    public float replaceMovePacketPitch(ClientPlayerEntity instance) {

        return Zenith.getInstance().getRotationManager().getCurrentRotation().getPitch();

    }
    @Inject(method = "closeHandledScreen", at = @At(value = "HEAD"), cancellable = true)
    private void closeHandledScreenHook(CallbackInfo info) {
        EventCloseScreen event = new EventCloseScreen(client.currentScreen);
        EventManager.call(event);
        if (event.isCancelled()) info.cancel();
    }
    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V"), cancellable = true)
    public void onMoveHook(MovementType movementType, Vec3d movement, CallbackInfo ci) {
        EventMove event = new EventMove(movement);
        EventManager.call(event);
        double d = this.getX();
        double e = this.getZ();
        super.move(movementType, event.getMovePos());
        this.autoJump((float) (this.getX() - d), (float) (this.getZ() - e));
        ci.cancel();
    }

}
