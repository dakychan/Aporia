package aporia.cc.base.rotation;



import aporia.cc.base.rotation.mods.config.api.RotationConfig;
import aporia.cc.utility.game.player.rotation.Rotation;

import java.util.function.Supplier;


public record RotationTarget(Rotation targetRotation, Supplier<Rotation> rotation, RotationConfig rotationConfigBack) {
}
