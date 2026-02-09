package zenith.zov.base.rotation;



import zenith.zov.base.rotation.mods.config.api.RotationConfig;
import zenith.zov.utility.game.player.rotation.Rotation;

import java.util.function.Supplier;


public record RotationTarget(Rotation targetRotation, Supplier<Rotation> rotation, RotationConfig rotationConfigBack) {
}
