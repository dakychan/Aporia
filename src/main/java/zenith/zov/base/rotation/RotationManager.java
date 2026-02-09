package zenith.zov.base.rotation;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;
import com.darkmagician6.eventapi.types.Priority;

import lombok.Getter;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRotationS2CPacket;
import net.minecraft.util.math.MathHelper;
import zenith.zov.base.events.impl.other.EventSpawnEntity;
import zenith.zov.base.events.impl.player.EventDirection;
import zenith.zov.base.events.impl.player.EventRotate;
import zenith.zov.base.events.impl.player.EventUpdate;
import zenith.zov.base.events.impl.server.EventPacket;
import zenith.zov.base.request.RequestHandler;
import zenith.zov.client.modules.impl.render.Predictions;
import zenith.zov.utility.game.other.MessageUtil;
import zenith.zov.utility.game.player.rotation.Rotation;
import zenith.zov.utility.interfaces.IMinecraft;
import zenith.zov.client.modules.api.Module;

@Getter
public class RotationManager implements IMinecraft {

    private Rotation currentRotation = new Rotation(0, 0);
    private Rotation previousRotation = new Rotation(0, 0);
    private final RequestHandler<RotationTarget> requestHandler = new RequestHandler<>();
    private final AimManager aimManager = new AimManager();
    private RotationTarget previousRotationTarget = new RotationTarget(currentRotation, () -> currentRotation, aimManager.getInstantSetup());

    private boolean setRotation = true;

    public RotationManager() {
        EventManager.register(this);

    }

    @EventTarget
    public void addLocalPlayer(EventSpawnEntity eventSpawnLocalPlayer) {
        if (eventSpawnLocalPlayer.getEntity() instanceof ClientPlayerEntity player) {


            currentRotation = new Rotation(player.getYaw(), player.getPitch());
            previousRotation = new Rotation(player.getYaw(), player.getPitch());
            previousRotationTarget = new RotationTarget(currentRotation, () -> currentRotation, aimManager.getInstantSetup());
            setRotation = true;
        }
    }

    @EventTarget(Priority.LOW)
    public void update(EventUpdate event) {

//        mc.player.prevHeadYaw = previousRotation.getYaw();
//        mc.player.prevPitch = previousRotation.getPitch();
//        mc.player.prevBodyYaw = previousRotation.getYaw();

        EventManager.call(new EventRotate());

        RotationTarget targetRotation = requestHandler.getActiveRequestValue();
        if (targetRotation != null) {


            Rotation newRot = targetRotation.rotation().get();
            previousRotation = currentRotation;
            currentRotation = newRot;
            setRotation = false;
            this.previousRotationTarget = targetRotation;
        } else {
            if (setRotation) {
                previousRotation = currentRotation;

                currentRotation = aimManager.rotate(aimManager.getInstantSetup(), new Rotation(mc.player.getYaw(),mc.player.getPitch()));

            } else {
                Rotation back = new Rotation(mc.player.getYaw(), mc.player.getPitch());

                if (currentRotation.rotationDeltaTo(back).isInRange(5)) {
                    previousRotation = currentRotation;
                    currentRotation = aimManager.rotate(aimManager.getInstantSetup(), back);
                    setRotation = true;

                } else {

                    Rotation newRot = aimManager.rotate(previousRotationTarget.rotationConfigBack(), back);

                    previousRotation = currentRotation;
                    currentRotation = newRot;


                }

            }
        }




       if(!setRotation) {
            float delta = currentRotation.getYaw() - mc.player.lastYaw;
            {
                Rotation validing = new Rotation(currentRotation.getYaw(), currentRotation.getPitch());
                if (delta > 320)
                    validing = new Rotation(mc.player.lastYaw + 300, currentRotation.getPitch()).normalize(new Rotation(mc.player.lastYaw, mc.player.lastPitch));
                if (delta < -320)
                    validing = new Rotation(mc.player.lastYaw - 300, currentRotation.getPitch()).normalize(new Rotation(mc.player.lastYaw, mc.player.lastPitch));

                currentRotation = validing;
            }
        }

        //  MessageUtil.displayMessage(MessageUtil.LogLevel.WARN, "valid " + (MathHelper.wrapDegrees(mc.player.getYaw()) + "  " + MathHelper.wrapDegrees(currentRotation.getYaw())));

        currentRotation = new Rotation(currentRotation.getYaw(), MathHelper.clamp(currentRotation.getPitch(), -90, 90));


        requestHandler.tick();

//        mc.player.setYaw(currentRotation.getYaw());
//        mc.player.setPitch(currentRotation.getPitch()


    }

    public void setRotation(RotationTarget targetRotation, int priority, Module module) {
        requestHandler.request(new RequestHandler.Request<>(2, priority, module, targetRotation));
    }

    @EventTarget
    public void direction(EventDirection direction) {

        direction.setYaw(currentRotation.getYaw());
        direction.setPitch(currentRotation.getPitch());
    }

    @EventTarget
    public void packet(EventPacket eventPacket) {


        switch (eventPacket.getPacket()) {
            case PlayerRotationS2CPacket player -> {
                currentRotation = new Rotation(player.xRot(), player.yRot());
                previousRotationTarget = new RotationTarget(currentRotation, () -> currentRotation, aimManager.getInstantSetup());
                setRotation = true;
            }

            case PlayerPositionLookS2CPacket player -> {
                currentRotation = new Rotation(player.change().yaw(), player.change().pitch());

                previousRotationTarget = new RotationTarget(currentRotation, () -> currentRotation, aimManager.getInstantSetup());
                setRotation = true;
            }
            case PlayerInteractItemC2SPacket packetItem -> {
                packetItem.yaw = currentRotation.getYaw();
                packetItem.pitch = currentRotation.getPitch();
            }
            default -> {
            }
        }
    }

}
