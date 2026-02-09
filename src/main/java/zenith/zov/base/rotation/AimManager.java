package zenith.zov.base.rotation;


import lombok.Getter;

import net.minecraft.util.math.MathHelper;
import zenith.zov.base.rotation.mods.AIRotationMode;
import zenith.zov.base.rotation.mods.InstantRotationMode;
import zenith.zov.base.rotation.mods.InterpolationRotationMode;
import zenith.zov.base.rotation.mods.config.AiRotationConfig;
import zenith.zov.base.rotation.mods.config.InstantRotationConfig;
import zenith.zov.base.rotation.mods.config.InterpolationRotationConfig;
import zenith.zov.base.rotation.mods.config.api.RotationConfig;
import zenith.zov.base.rotation.mods.config.api.RotationModeType;
import zenith.zov.utility.game.other.MessageUtil;
import zenith.zov.utility.game.player.rotation.Rotation;
import zenith.zov.utility.game.player.rotation.RotationDelta;
import zenith.zov.utility.interfaces.IClient;

public class AimManager implements IClient {

    private final InterpolationRotationMode interpolationMod = new InterpolationRotationMode();
    private final InstantRotationMode instantMod = new InstantRotationMode();
    private final AIRotationMode smoothMod = new AIRotationMode();
    @Getter
    private final RotationConfig instantSetup = new InstantRotationConfig();
    @Getter
    private final RotationConfig aiSetup = AiRotationConfig.builder().build();


    public Rotation rotate(RotationConfig config, Rotation targetRotation) {
        if (config.getType() != RotationModeType.INSTANT) {
            RotationDelta deltaToTarget = zenith.getRotationManager().getCurrentRotation().rotationDeltaTo(targetRotation);
            float maxInitialDiff = 270f; // 180 + 90
            float progress = MathHelper.clamp(1f - (Math.abs(deltaToTarget.getDeltaYaw()) + Math.abs(deltaToTarget.getDeltaPitch())) / maxInitialDiff, 0, 1);

            RotationDelta offsets = getOffset(deltaToTarget, progress);

            targetRotation = targetRotation.add(offsets);
        }

        Rotation newRotation;

        switch (config.getType()) {
            case INSTANT -> newRotation = instantMod.process(targetRotation);
            case INTERPOLATION -> newRotation = interpolationMod.process((InterpolationRotationConfig) config,zenith.getRotationManager().getCurrentRotation(), targetRotation);
            case AI -> newRotation = interpolationMod.process(((AiRotationConfig) config ).getInterpolationRotationConfig() ,smoothMod.process((AiRotationConfig) config ,targetRotation), targetRotation);
            default -> newRotation = zenith.getRotationManager().getCurrentRotation();
        }

        if(config.getType()!= RotationModeType.AI) {
           // smoothMod.resetLerp(targetRotation);
        }
        if(rotationManager.getCurrentRotation().equals(newRotation)) {
            return newRotation;
        }
          return newRotation.normalize(new Rotation(mc.player.lastYaw,mc.player.lastPitch));
    }

    @Getter
    int index = 0;

    public void incrementIndex() {
        index++;
        if (index >= values.length) {
            index = 0;
        }
    }

    public float getDiff(float prevTargetYawDiff, float targetYawDiff) {


        return targetYawDiff * values[index];
    }


    float[] values = new float[]{
            0.0123967f,
            0.053719f,
            0.109504f,
            0.17562f,
            0.21281f,
            0.272727f,
            0.11157f,
            0.0392562f,
            0.0103306f,
            0.00206612f
    };

    public static RotationDelta getOffset(RotationDelta deltaToTarget, float progress) {
        float curveStrength = 3.0f;
        float smoothness = 1.0f - (float) Math.cos(progress * Math.PI);

        float yawSign = Math.signum(deltaToTarget.getDeltaYaw());
        float pitchSign = Math.signum(deltaToTarget.getDeltaPitch());

        float offsetYaw = 0f;
        float offsetPitch = 0f;

        boolean verticalAiming = Math.abs(deltaToTarget.getDeltaYaw()) < 1.5f && Math.abs(deltaToTarget.getDeltaPitch()) > 10f;

        if (verticalAiming) {
            offsetYaw = pitchSign * curveStrength * (1f - progress);
            offsetPitch = 0f;
        } else {

            if (pitchSign > 0 && yawSign >= 0) {
                offsetYaw = -curveStrength * (1f - progress);
                offsetPitch = -curveStrength * smoothness;
            } else if (pitchSign > 0 && yawSign < 0) {
                offsetYaw = curveStrength * (1f - progress);
                offsetPitch = -curveStrength * smoothness;
            } else if (pitchSign < 0 && yawSign >= 0) {
                offsetYaw = -curveStrength * (1f - progress);
                offsetPitch = curveStrength * smoothness;
            } else if (pitchSign < 0 && yawSign < 0) {
                offsetYaw = curveStrength * (1f - progress);
                offsetPitch = curveStrength * smoothness;
            }
        }

        return new RotationDelta(offsetYaw, offsetPitch);
    }

    public void reset() {
        this.index = 0;
    }


}
