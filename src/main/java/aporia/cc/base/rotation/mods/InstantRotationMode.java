package aporia.cc.base.rotation.mods;


import aporia.cc.base.rotation.mods.api.RotationMode;
import aporia.cc.utility.game.player.rotation.Rotation;

public class InstantRotationMode extends RotationMode {

    public Rotation process(Rotation target) {

        return rotationManager.getCurrentRotation().add(rotationManager.getCurrentRotation().rotationDeltaTo(target));
    }
}

