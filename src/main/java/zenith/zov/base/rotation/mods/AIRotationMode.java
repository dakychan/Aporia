package zenith.zov.base.rotation.mods;


import zenith.zov.base.rotation.deeplearnig.MinaraiModel;
import zenith.zov.base.rotation.mods.api.RotationMode;
import zenith.zov.base.rotation.mods.config.AiRotationConfig;
import zenith.zov.utility.game.player.rotation.Rotation;
import zenith.zov.utility.game.player.rotation.RotationDelta;

public class AIRotationMode extends RotationMode {
    private Rotation lerpTargetRotation = Rotation.ZERO;
    public Rotation process(AiRotationConfig config, Rotation targetRotation) {

        RotationDelta prevDelta = zenith.getRotationManager().getPreviousRotation().rotationDeltaTo(zenith.getRotationManager().getCurrentRotation());

        Rotation currentRotation = zenith.getRotationManager().getCurrentRotation();
        if(Math.abs(targetRotation.rotationDeltaTo(lerpTargetRotation).getDeltaYaw())>80 ){
            lerpTargetRotation = targetRotation;
        }
        for (int i = 0; i < 3; i++) {
            Rotation newOut = process(config, currentRotation, targetRotation, prevDelta, i == config.getTick() - 1);
            prevDelta = currentRotation.rotationDeltaTo(newOut);
            currentRotation = newOut;
        }

        if(currentRotation.rotationDeltaTo(lerpTargetRotation).isInRange(10)){
            lerpTargetRotation = targetRotation;
        }

        return currentRotation;
    }
    private Rotation process(AiRotationConfig config, Rotation currentRotation,Rotation targetRotation,RotationDelta prevDelta, boolean tickUpdate) {


        MinaraiModel model = zenith.getDeepLearningManager().getSlowModel();
        try {

            RotationDelta deltaLerpTarget = currentRotation.rotationDeltaTo(lerpTargetRotation);


            if(Math.copySign(1,prevDelta.getDeltaYaw())!=Math.copySign(1, deltaLerpTarget.getDeltaYaw())) {
              //  prevDelta = new RotationDelta(prevDelta.getDeltaYaw()*0.2f,prevDelta.getDeltaPitch()*0.2f);
              //   prevDelta = new RotationDelta(MathUtil.lerp(prevDelta.getDeltaYaw(),deltaLerpTarget.getDeltaYaw(),0.1f),MathUtil.lerp(prevDelta.getDeltaPitch(),deltaLerpTarget.getDeltaPitch(),0.1f));
            }


            //float[] input = new float[]{prevDeltaYaw2, prevDeltaPitch2, prevDeltaYaw, prevDeltaPitch, prevTargetDiffYaw, prevTargetDiffPitch, diffa, diffb};
            float[] input = new float[]{prevDelta.getDeltaYaw(), prevDelta.getDeltaPitch(), deltaLerpTarget.getDeltaYaw(), deltaLerpTarget.getDeltaPitch()};

          //  prevTargetDelta = deltaLerpTarget;
            float[] result = model.predict(input);



            float diffYaw = result[0];
            float diffPitch = result[1];

            RotationDelta newDelta = new RotationDelta(diffYaw, diffPitch);



            return  currentRotation.add(newDelta);

        } catch (
                Exception e) {
            e.printStackTrace();
        }
        return currentRotation;
    }

    public void resetLerp(Rotation targetRotation) {
        this.lerpTargetRotation = targetRotation;
    }
}
