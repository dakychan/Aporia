package aporia.cc.base.rotation.mods.config;


import lombok.Builder;
import lombok.Getter;
import aporia.cc.base.rotation.mods.config.api.RotationConfig;
import aporia.cc.base.rotation.mods.config.api.RotationModeType;
import aporia.cc.utility.math.IntRange;

@Getter
@Builder
public class AiRotationConfig extends RotationConfig {
    @Builder.Default
    private int tick = 3;
    @Builder.Default
    private InterpolationRotationConfig interpolationRotationConfig =new InterpolationRotationConfig(new IntRange(2,5),new IntRange(5,8),new IntRange(20,30),0.35f);

    @Override
    public RotationModeType getType() {
        return RotationModeType.AI;
    }
}

