package zenith.zov.base.rotation.deeplearnig;


import lombok.Getter;

@Getter
public class DeepLearningManager  {

    private  final MinaraiModel model ;
    private  final MinaraiModel speedModer ;
    private  final MinaraiModel slowModel ;

    public DeepLearningManager()  {
        model = new MinaraiModel("model");
        model.load();
        slowModel = new MinaraiModel("slow");
        slowModel.load();
        speedModer = new MinaraiModel("tf-0100");
        speedModer.load();

    }

}
