package aporia.cc.base.rotation.mods.config;


import lombok.AllArgsConstructor;
import lombok.Getter;
import aporia.cc.base.rotation.mods.config.api.RotationConfig;
import aporia.cc.base.rotation.mods.config.api.RotationModeType;
import aporia.cc.utility.math.IntRange;

@Getter
@AllArgsConstructor
public class InterpolationRotationConfig extends RotationConfig {

    private final IntRange horizontalSpeedSetting;
    private final IntRange verticalSpeedSetting  ;
    private final IntRange directionChangeFactor ;
    private final float midPoint ;

    @Override
    public RotationModeType getType() {
        return RotationModeType.INTERPOLATION;
    }
}

