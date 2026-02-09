package aporia.cc.client.modules.impl.render;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import aporia.cc.client.modules.api.Category;
import aporia.cc.client.modules.api.Module;
import aporia.cc.client.modules.api.ModuleAnnotation;
import aporia.cc.client.modules.api.setting.impl.BooleanSetting;
import aporia.cc.client.modules.api.setting.impl.ModeSetting;
import aporia.cc.client.modules.api.setting.impl.NumberSetting;

/**
 А ты не хочешь признавать, что фрик сделал это сам
 Но в комментариях же пишут, что его сделал гптшка
 А ты еблан, я твоё мнение ебал
 ***/
@ModuleAnnotation(name = "SwingAnimation", category = Category.RENDER, description = "Кастомные анимации замаха")
public final class SwingAnimation extends Module {
    public static final SwingAnimation INSTANCE = new SwingAnimation();

    private SwingAnimation() {
    }

    public ModeSetting animationMode = new ModeSetting(
            "Режим",
            "Обычный",
            "Первый",
            "Второй",
            "Третий",
            "Четвертый",
            "Пятый"
    );
    public NumberSetting swingPower = new NumberSetting("Сила", 5.0f, 1.0f, 10.0f, 0.05f);
    public final BooleanSetting onlyAura = new BooleanSetting("Только с аурой", false);

    public void renderSwordAnimation(MatrixStack matrices, float swingProgress, float equipProgress, Arm arm) {
        switch (animationMode.get()) {
            case "Обычный" -> {
                matrices.translate(0.56F, -0.52F, -0.72F);
                float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -60.0F));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(g * -30.0F));
            }
            case "Первый" -> {
                if (swingProgress > 0) {
                    float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                    matrices.translate(0.56F, equipProgress * -0.2f - 0.5F, -0.7F);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -85.0F));
                    matrices.translate(-0.1F, 0.28F, 0.2F);
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-85.0F));
                } else {
                    float n = -0.4f * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                    float m = 0.2f * MathHelper.sin(MathHelper.sqrt(swingProgress) * ((float) Math.PI * 2));
                    float f1 = -0.2f * MathHelper.sin(swingProgress * (float) Math.PI);
                    matrices.translate(n, m, f1);
                    applyEquipOffset(matrices, arm, equipProgress);
                    applySwingOffset(matrices, arm, swingProgress);
                }
            }
            case "Второй" -> {
                float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float)Math.PI);
                applyEquipOffset(matrices, arm, 0);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(50f));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-60f));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(110f + 20f * g));
            }
            case "Третий" -> {
                float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float)Math.PI);
                applyEquipOffset(matrices, arm, 0);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(50f));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-30f * (1f - g) - 30f));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(110f));
            }
            case "Четвертый" -> {
                float g = MathHelper.sin(swingProgress * (float) Math.PI);
                applyEquipOffset(matrices, arm, 0);
                matrices.translate(0.1F, -0.2F, -0.3F);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-30f * g - 36f));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(25f * g));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(12f));

            }
            case "Пятый" -> {
                float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float)Math.PI);
                applyEquipOffset(matrices, arm, 0);
                matrices.translate(0.0F, -0.2F, -0.4F);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-120f * g - 3f));


            }
        }
    }


    private void applyEquipOffset(MatrixStack matrices, Arm arm, float equipProgress) {
        int i = arm == Arm.RIGHT ? 1 : -1;
        matrices.translate((float) i * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
    }

    private void applySwingOffset(MatrixStack matrices, Arm arm, float swingProgress) {
        int i = arm == Arm.RIGHT ? 1 : -1;
        float f = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * (45.0F + f * -20.0F)));
        float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) i * g * -20.0F));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -80.0F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * -45.0F));
    }

}

