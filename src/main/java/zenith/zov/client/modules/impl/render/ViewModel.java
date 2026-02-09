package zenith.zov.client.modules.impl.render;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import zenith.zov.client.modules.api.Category;
import zenith.zov.client.modules.api.Module;
import zenith.zov.client.modules.api.ModuleAnnotation;
import zenith.zov.client.modules.api.setting.impl.NumberSetting;

@ModuleAnnotation(name = "ViewModel", category = Category.RENDER, description = "Настройка позиции")
public final class ViewModel extends Module {
    public static final ViewModel INSTANCE = new ViewModel();

    private ViewModel() {
    }

    public final NumberSetting leftX = new NumberSetting("Левая рука X", 0.0f, -1.0f, 1.0f, 0.1f);
    public final NumberSetting leftY = new NumberSetting("Левая рука Y", 0.0f, -1.0f, 1.0f, 0.1f);
    public final NumberSetting leftZ = new NumberSetting("Левая рука Z", 0.0f, -1.0f, 1.0f, 0.1f);
    public final NumberSetting leftScale = new NumberSetting("Левая рука размер", 1.0f, 0.5f, 1.5f, 0.05f);

    public final NumberSetting rightX = new NumberSetting("Правая рука X", 0.0f, -1.0f, 1.0f, 0.1f);
    public final NumberSetting rightY = new NumberSetting("Правая рука Y", 0.0f, -1.0f, 1.0f, 0.1f);
    public final NumberSetting rightZ = new NumberSetting("Правая рука Z", 0.0f, -1.0f, 1.0f, 0.1f);
    public final NumberSetting rightScale = new NumberSetting("Правая рука размер", 1.0f, 0.5f, 1.5f, 0.05f);


    public void applyHandScale(MatrixStack matrices, Arm arm) {
        if (this.isEnabled()) {
            if (arm == Arm.RIGHT) {
                matrices.scale(rightScale.getCurrent(), rightScale.getCurrent(), rightScale.getCurrent());
            } else {
                matrices.scale(leftScale.getCurrent(), leftScale.getCurrent(), leftScale.getCurrent());
            }
        } else {
            matrices.scale(1.0f, 1.0f, 1.0f);
        }
    }

    public void applyHandPosition(MatrixStack matrices, Arm arm) {
        if (this.isEnabled()) {
            if (arm == Arm.RIGHT) {
                matrices.translate(rightX.getCurrent(), rightY.getCurrent(), rightZ.getCurrent());
            } else {
                matrices.translate(-leftX.getCurrent(), leftY.getCurrent(), leftZ.getCurrent());
            }
        } else {
            matrices.translate(0.0f, 0.0f, 0.0f);
        }
    }
}