package zenith.zov.utility.interfaces;

import zenith.zov.Zenith;
import zenith.zov.base.rotation.AimManager;
import zenith.zov.base.rotation.RotationManager;
import zenith.zov.base.rotation.deeplearnig.DeepLearningManager;

public interface IClient extends IWindow{
    Zenith zenith = Zenith.getInstance();
    DeepLearningManager deepLearningManager = Zenith.getInstance().getDeepLearningManager();
    RotationManager rotationManager = Zenith.getInstance().getRotationManager();
    AimManager aimManager = rotationManager.getAimManager();

}
