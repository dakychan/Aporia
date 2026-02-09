package zenith.zov.base.rotation.mods;


import kotlin.Pair;
import net.minecraft.util.math.MathHelper;
import zenith.zov.base.rotation.mods.api.RotationMode;
import zenith.zov.base.rotation.mods.config.InterpolationRotationConfig;
import zenith.zov.utility.game.player.rotation.Rotation;
import zenith.zov.utility.game.player.rotation.RotationDelta;
import zenith.zov.utility.math.IntRange;

import java.util.Random;

public class InterpolationRotationMode extends RotationMode {


    public Rotation process(InterpolationRotationConfig config, Rotation modelOut, Rotation targetRotation
    ) {

        Pair<Float, Float> pair = calculateFactors(zenith.getRotationManager().getCurrentRotation(), targetRotation,config.getHorizontalSpeedSetting(),config.getVerticalSpeedSetting(),config.getDirectionChangeFactor(),config.getMidPoint());
        return modelOut.towardsLinear(targetRotation, pair.getFirst(), pair.getSecond());
    }

    private final Sigmoid sigmoid = new Sigmoid();
    private final Bezier bezier = new Bezier();
    private final Random random = new Random();


    public Pair<Float, Float> calculateFactors(Rotation currentRotation, Rotation targetRotation, IntRange horizontalSpeedSetting, IntRange verticalSpeedSetting, IntRange directionChangeFactor, float midpoint) {

        RotationDelta diff = currentRotation.rotationDeltaTo(targetRotation);
        float yawDiff = diff.getDeltaYaw();
        float pitchDiff = diff.getDeltaPitch();


        float directionChange = 0f;
        if (targetRotation != null && zenith.getRotationManager().getPreviousRotationTarget() != null) {
            directionChange = zenith.getRotationManager().getPreviousRotationTarget().targetRotation()
                    .angleTo(targetRotation);
            directionChange = MathHelper.clamp(directionChange, 0f, 1f);
            directionChange *= directionChangeFactor.random() / 100.0f;
        }

        float horizontalSpeed = (targetRotation != null ? horizontalSpeedSetting.random() : horizontalSpeedSetting.getStart()) / 100.0f;
        float verticalSpeed = (targetRotation != null ? verticalSpeedSetting.random() : verticalSpeedSetting.getStart()) / 100.0f;


        float horizontalFactor = calculateFactor("Yaw", Math.abs(yawDiff), MathHelper.clamp(horizontalSpeed, 0f, 1f), directionChange,midpoint);
        float verticalFactor = calculateFactor("Pitch", Math.abs(pitchDiff), MathHelper.clamp(verticalSpeed, 0f, 1f), directionChange,midpoint);

        return new Pair<>(horizontalFactor * Math.abs(yawDiff), verticalFactor * Math.abs(pitchDiff));
    }

    private float calculateFactor(String name, float rotationDifference, float turnSpeed, float directionChange,float midpoint) {
        float t = MathHelper.clamp(rotationDifference / 180f, 0f, 1f);

        float bezierSpeed = bezier.transform(0.05f, 1f, 1f - t);
        float sigmoidSpeed = sigmoid.transform(t);


        if (t > midpoint) {

            return bezierSpeed * turnSpeed;
        } else {
            return sigmoidSpeed * MathHelper.clamp(turnSpeed + directionChange, 0f, 1f);
        }
    }

    private static class Sigmoid {
        public float transform(float t) {
            return (float) (1.0 / (1.0 + Math.exp(-0.5 * (t - 0.3))));
        }
    }

    private static class Bezier {
        public float transform(float start, float end, float t) {
            return (1 - t) * (1 - t) * start + 2 * (1 - t) * t * 1.0f + t * t * end;
        }
    }
}
