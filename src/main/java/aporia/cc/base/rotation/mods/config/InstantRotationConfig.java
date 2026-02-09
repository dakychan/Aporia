package aporia.cc.base.rotation.mods.config;


import aporia.cc.base.rotation.mods.config.api.RotationConfig;
import aporia.cc.base.rotation.mods.config.api.RotationModeType;

public class InstantRotationConfig extends RotationConfig {
    @Override
    public RotationModeType getType() {
        return RotationModeType.INSTANT;
    }
}
