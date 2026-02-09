package zenith.zov.base.rotation.deeplearnig;


import ai.djl.Model;
import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Activation;
import ai.djl.nn.Blocks;
import ai.djl.nn.SequentialBlock;
import ai.djl.nn.core.Linear;
import ai.djl.nn.norm.BatchNorm;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.EasyTrain;
import ai.djl.training.Trainer;
import ai.djl.training.TrainingConfig;
import ai.djl.training.dataset.ArrayDataset;
import ai.djl.training.initializer.XavierInitializer;
import ai.djl.training.listener.TrainingListener;
import ai.djl.training.listener.TrainingListenerAdapter;
import ai.djl.training.loss.Loss;
import ai.djl.training.optimizer.Adam;
import ai.djl.training.tracker.Tracker;
import ai.djl.translate.TranslateException;
import ai.djl.translate.Translator;
import zenith.zov.Zenith;
import zenith.zov.base.font.Fonts;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public abstract class ModelWrapper<I, O> implements Closeable {

    private static final int NUM_EPOCH = 500;
    private static final int BATCH_SIZE = 32;
    private final Translator<I, O> translator;
    private final long outputs;
    private final Model model;
    private final Predictor<I, O> predictor;
    private final  String name;
    public ModelWrapper(String name, Translator<I, O> translator, long outputs) {
        this.name = name;
        this.translator = translator;
        this.outputs = outputs;


        this.model = Model.newInstance(name);
        this.model.setBlock(createMlpBlock(outputs));
        this.predictor = model.newPredictor(translator);
    }

    public O predict(I input) throws TranslateException {

        return predictor.predict(input);
    }

    public void train(float[][] features, float[][] labels) throws ModelException, IOException {

        if (features.length != labels.length || features.length == 0) {
            throw new IllegalArgumentException("Features and labels must have the same size and be non-empty");
        }

        long inputs = features[0].length;

        TrainingConfig trainingConfig = new DefaultTrainingConfig(Loss.l2Loss())
                .optInitializer(new XavierInitializer(), "weight")
                .optOptimizer(Adam.builder().optLearningRateTracker(Tracker.fixed(0.001f)).build())
                .addTrainingListeners(TrainingListener.Defaults.logging("train"))
                .addTrainingListeners(new TrainingListenerAdapter() {
                    @Override
                    public void onEpoch(Trainer trainer) {

                        System.out.println("Epoch " + trainer.getTrainingResult().getEpoch() + " - Loss: " + trainer.getTrainingResult().getTrainLoss());
                    }

                    @Override
                    public void onTrainingBegin(Trainer trainer) {

                    }

                    @Override
                    public void onTrainingEnd(Trainer trainer) {

                    }
                });


        try (Trainer trainer = model.newTrainer(trainingConfig);
             NDManager manager = NDManager.newBaseManager()) {

            ArrayDataset trainingSet = new ArrayDataset.Builder()
                    .setData(manager.create(features))
                    .optLabels(manager.create(labels))
                    .setSampling(BATCH_SIZE, true)
                    .build();

            trainer.initialize(new Shape(BATCH_SIZE, inputs));
            EasyTrain.fit(trainer, NUM_EPOCH, trainingSet, null);
        } catch (TranslateException e) {

        }
    }

    public void load(InputStream stream) throws IOException, ModelException {
        model.load(stream);

    }

    public void load(Path path) throws IOException, ModelException {
        model.load(path, "tf");
    }

    public void load() {
        try (InputStream stream = getClass().getResourceAsStream("/assets/zenith/models/"+name+".params")) {

                load(stream);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public void save(Path path) throws IOException, ModelException {
        model.save(path, "tf");
    }

    @Override
    public void close() {
        predictor.close();
        model.close();
    }

    private static SequentialBlock createMlpBlock(long outputs) {
        return new SequentialBlock()
                .add(Linear.builder().setUnits(128).build())
                .add(Blocks.batchFlattenBlock())
                .add(BatchNorm.builder().build())
                .add(Activation.reluBlock())

                .add(Linear.builder().setUnits(64).build())
                .add(Blocks.batchFlattenBlock())
                .add(BatchNorm.builder().build())
                .add(Activation.reluBlock())

                .add(Linear.builder().setUnits(32).build())
                .add(Blocks.batchFlattenBlock())
                .add(BatchNorm.builder().build())
                .add(Activation.reluBlock())

                .add(Linear.builder().setUnits(outputs).build());
    }

}
