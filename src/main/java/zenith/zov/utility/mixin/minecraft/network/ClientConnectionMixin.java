package zenith.zov.utility.mixin.minecraft.network;

import com.darkmagician6.eventapi.EventManager;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zenith.zov.Zenith;

import zenith.zov.base.events.impl.server.EventPacket;
import zenith.zov.utility.interfaces.IMinecraft;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin implements IMinecraft {

    @Unique
    private static boolean stackOverflowFix;

    @Inject(method = "handlePacket", at = @At("HEAD"), cancellable = true)
    private static <T extends PacketListener> void triggerReceivePacketEvent(Packet<T> packet, PacketListener listener, CallbackInfo ci) {
        EventPacket event = new EventPacket(EventPacket.Action.RECEIVE,packet);
        EventManager.call(event);

        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
    public void triggerSendPacketEvent(Packet<?> packet, CallbackInfo ci) {

        if (stackOverflowFix) return;
        EventPacket event = new EventPacket(EventPacket.Action.SENT,packet);
        EventManager.call(event);

        if (event.isCancelled()) {
            ci.cancel();
        }

        Packet<?> newPacket = event.getPacket();
        if (newPacket != packet) {
            ci.cancel();

            stackOverflowFix = true;
            mc.getNetworkHandler().sendPacket(newPacket);
            stackOverflowFix = false;
        }
    }
}
