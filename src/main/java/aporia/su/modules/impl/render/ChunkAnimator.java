package aporia.su.modules.impl.render;

import aporia.su.modules.module.ModuleStructure;
import aporia.su.modules.module.category.ModuleCategory;
import aporia.su.modules.module.setting.implement.SliderSettings;

public class ChunkAnimator extends ModuleStructure {
    private static ChunkAnimator instance;

    private final SliderSettings speed = new SliderSettings("Скорость", "").range(1, 20).setValue(10);

    public ChunkAnimator() {
        super("Chunk Animator", "Анимирует появляющиеся чанки", ModuleCategory.RENDER);
        instance = this;
        settings(speed);
    }

    public static ChunkAnimator getInstance() {
        return instance;
    }

    public float getSpeed() {
        return speed.getValue();
    }
}