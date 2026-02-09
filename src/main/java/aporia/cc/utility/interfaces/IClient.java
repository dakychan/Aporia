package aporia.cc.utility.interfaces;

import aporia.cc.Aporia;
import aporia.cc.base.rotation.AimManager;
import aporia.cc.base.rotation.RotationManager;
import aporia.cc.base.rotation.deeplearnig.DeepLearningManager;

public interface IClient extends IWindow{
    Aporia Aporia = Aporia.getInstance();
    DeepLearningManager deepLearningManager = Aporia.getInstance().getDeepLearningManager();
    RotationManager rotationManager = Aporia.getInstance().getRotationManager();
    AimManager aimManager = rotationManager.getAimManager();

}
