package aporia.cc.base.rotation.mods;


import aporia.cc.base.rotation.deeplearnig.MinaraiModel;
import aporia.cc.base.rotation.mods.api.RotationMode;
import aporia.cc.base.rotation.mods.config.AiRotationConfig;
import aporia.cc.utility.game.player.rotation.Rotation;
import aporia.cc.utility.game.player.rotation.RotationDelta;

public class AIRotationMode extends RotationMode {
    private Rotation lerpTargetRotation = Rotation.ZERO;
    public Rotation process(AiRotationConfig config, Rotation targetRotation) {

        RotationDelta prevDelta = aporia.getRotationManager().getPreviousRotation().rotationDeltaTo(aporia.getRotationManager().getCurrentRotation());

        Rotation currentRotation = aporia.getRotationManager().getCurrentRotation();
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


        MinaraiModel model = aporia.getDeepLearningManager().getSlowModel();
        try {

            RotationDelta deltaLerpTarget = currentRotation.rotationDeltaTo(lerpTargetRotation);


            if(Math.copySign(1,prevDelta.getDeltaYaw())!=Math.copySign(1, deltaLerpTarget.getDeltaYaw())) {
            }


            float[] input = new float[]{prevDelta.getDeltaYaw(), prevDelta.getDeltaPitch(), deltaLerpTarget.getDeltaYaw(), deltaLerpTarget.getDeltaPitch()};

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

