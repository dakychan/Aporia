package zenith.zov.base.rotation.mods.config;


import zenith.zov.base.rotation.mods.config.api.RotationConfig;
import zenith.zov.base.rotation.mods.config.api.RotationModeType;

public class InstantRotationConfig extends RotationConfig {
    @Override
    public RotationModeType getType() {
        return RotationModeType.INSTANT;
    }
}
