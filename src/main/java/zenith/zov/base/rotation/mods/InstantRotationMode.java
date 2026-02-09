package zenith.zov.base.rotation.mods;


import zenith.zov.base.rotation.mods.api.RotationMode;
import zenith.zov.utility.game.player.rotation.Rotation;
import zenith.zov.utility.game.player.rotation.RotationDelta;

public class InstantRotationMode extends RotationMode {

    public Rotation process(Rotation target) {

        return rotationManager.getCurrentRotation().add(rotationManager.getCurrentRotation().rotationDeltaTo(target));
    }
}
