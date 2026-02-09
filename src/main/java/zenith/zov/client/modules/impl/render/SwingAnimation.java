package zenith.zov.client.modules.impl.render;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import zenith.zov.Zenith;
import zenith.zov.client.modules.api.Category;
import zenith.zov.client.modules.api.Module;
import zenith.zov.client.modules.api.ModuleAnnotation;
import zenith.zov.client.modules.api.setting.impl.BooleanSetting;
import zenith.zov.client.modules.api.setting.impl.ModeSetting;
import zenith.zov.client.modules.api.setting.impl.NumberSetting;
import zenith.zov.client.modules.impl.combat.Aura;

/**
 А ты не хочешь признавать, что фрик сделал это сам
 Но в комментариях же пишут, что его сделал гптшка
 А ты еблан, я твоё мнение ебал
 ***/
//PS: БЛЯ Я В АХУЕ РОДНОЙ
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
//    public void renderFirstPersonItem(
//            AbstractClientPlayerEntity player,
//            float tickDelta,
//            float pitch,
//            Hand hand,
//            float swingProgress,
//            ItemStack item,
//            float equipProgress,
//            MatrixStack matrices,
//            VertexConsumerProvider vertexConsumers,
//            int light
//    ) {
//
//        if (!player.isUsingSpyglass()) {
//            boolean bl = hand == Hand.MAIN_HAND;
//            Arm arm = bl ? player.getMainArm() : player.getMainArm().getOpposite();
//            ViewModel viewModel = ViewModel.INSTANCE;
//            matrices.push();
//
//
//
//
//
//
//
//            if (item.isOf(Items.CROSSBOW)) {
//                boolean bl2 = CrossbowItem.isCharged(item);
//                boolean bl3 = arm == Arm.RIGHT;
//                int i = bl3 ? 1 : -1;
//                viewModel.applyHandPosition(matrices, arm);
//
//                if (player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand) {
//                    this.applyEquipOffset(matrices, arm, equipProgress);
//                    matrices.translate((float) i * -0.4785682F, -0.094387F, 0.05731531F);
//                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-11.935F));
//                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * 65.3F));
//                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) i * -9.785F));
//                    float f = (float) item.getMaxUseTime(mc.player) - ((float) mc.player.getItemUseTimeLeft() - tickDelta + 1.0F);
//                    float g = f / (float) CrossbowItem.getPullTime(item, mc.player);
//                    if (g > 1.0F) {
//                        g = 1.0F;
//                    }
//
//                    if (g > 0.1F) {
//                        float h = MathHelper.sin((f - 0.1F) * 1.3F);
//                        float j = g - 0.1F;
//                        float k = h * j;
//                        matrices.translate(k * 0.0F, k * 0.004F, k * 0.0F);
//                    }
//
//                    matrices.translate(g * 0.0F, g * 0.0F, g * 0.04F);
//                    matrices.scale(1.0F, 1.0F, 1.0F + g * 0.2F);
//                    matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float) i * 45.0F));
//                } else {
//                    float fx = -0.4F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
//                    float gx = 0.2F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) (Math.PI * 2));
//                    float h = -0.2F * MathHelper.sin(swingProgress * (float) Math.PI);
//                    matrices.translate((float) i * fx, gx, h);
//                    this.applyEquipOffset(matrices, arm, equipProgress);
//                    this.applySwingOffset(matrices, arm, swingProgress);
//                    if (bl2 && swingProgress < 0.001F && bl) {
//                        matrices.translate((float) i * -0.641864F, 0.0F, 0.0F);
//                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * 10.0F));
//                    }
//                }
//
//                viewModel.applyHandScale(matrices, arm);
//
//                this.renderItem(
//                        player,
//                        item,
//                        bl3 ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND,
//                        !bl3,
//                        matrices,
//                        vertexConsumers,
//                        light
//                );
//            } else {
//                boolean bl2 = arm == Arm.RIGHT;
//
//                viewModel.applyHandPosition(matrices, arm);
//
//                if (player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand) {
//                    int l = bl2 ? 1 : -1;
//                    switch (item.getUseAction()) {
//                        case NONE, BLOCK:
//                            this.applyEquipOffset(matrices, arm, equipProgress);
//                            break;
//                        case EAT:
//                        case DRINK:
//                            this.applyEatOrDrinkTransformation(matrices, tickDelta, arm, item);
//                            this.applyEquipOffset(matrices, arm, equipProgress);
//                            break;
//                        case BOW:
//                            this.applyEquipOffset(matrices, arm, equipProgress);
//                            matrices.translate((float) l * -0.2785682F, 0.18344387F, 0.15731531F);
//                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-13.935F));
//                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) l * 35.3F));
//                            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) l * -9.785F));
//                            float mx = (float) item.getMaxUseTime(mc.player) - ((float) mc.player.getItemUseTimeLeft() - tickDelta + 1.0F);
//                            float fxx = mx / 20.0F;
//                            fxx = (fxx * fxx + fxx * 2.0F) / 3.0F;
//                            if (fxx > 1.0F) {
//                                fxx = 1.0F;
//                            }
//
//                            if (fxx > 0.1F) {
//                                float gx = MathHelper.sin((mx - 0.1F) * 1.3F);
//                                float h = fxx - 0.1F;
//                                float j = gx * h;
//                                matrices.translate(j * 0.0F, j * 0.004F, j * 0.0F);
//                            }
//
//                            matrices.translate(0.0F, 0.0F, fxx * 0.2F);
//                            matrices.scale(1.0F, 1.0F, 1.0F + fxx * 0.2F);
//                            matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float) l * 45.0F));
//                            break;
//                    }
//                } else if (player.isUsingRiptide()) {
//                    this.applyEquipOffset(matrices, arm, equipProgress);
//                    int l = bl2 ? 1 : -1;
//                    matrices.translate((float) l * -0.4F, 0.8F, 0.3F);
//                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) l * 65.0F));
//                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) l * -85.0F));
//                } else {
//                    if (this.isEnabled()) {
//                        if (arm == Arm.RIGHT) {
//                            if (onlyAura.isEnabled()) {
//                                Module aura = Aura.INSTANCE;
//                                if (aura == null || !aura.isEnabled()) {
//                                    float n = -0.4F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
//                                    float mxx = 0.2F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) (Math.PI * 2));
//                                    float fxxx = -0.2F * MathHelper.sin(swingProgress * (float) Math.PI);
//                                    int o = bl2 ? 1 : -1;
//                                    matrices.translate((float) o * n, mxx, fxxx);
//                                    this.applyEquipOffset(matrices, arm, equipProgress);
//                                    this.applySwingOffset(matrices, arm, swingProgress);
//                                } else {
//                                    renderSwordAnimation(matrices, swingProgress, equipProgress, arm);
//                                }
//                            } else {
//                                renderSwordAnimation(matrices, swingProgress, equipProgress, arm);
//                            }
//                        } else {
//                            float n = -0.4F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
//                            float mxx = 0.2F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) (Math.PI * 2));
//                            float fxxx = -0.2F * MathHelper.sin(swingProgress * (float) Math.PI);
//                            int o = bl2 ? 1 : -1;
//                            matrices.translate((float) o * n, mxx, fxxx);
//                            this.applyEquipOffset(matrices, arm, equipProgress);
//                            this.applySwingOffset(matrices, arm, swingProgress);
//                        }
//                    } else {
//                        this.applyEquipOffset(matrices, arm, equipProgress);
//                    }
//                }
//
//                viewModel.applyHandScale(matrices, arm);
//
//                this.renderItem(
//                        player,
//                        item,
//                        bl2 ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND,
//                        !bl2,
//                        matrices,
//                        vertexConsumers,
//                        light
//                );
//            }
//
//            matrices.pop();
//        }
//    }

//    private void applyEatOrDrinkTransformation(MatrixStack matrices, float tickDelta, Arm arm, ItemStack stack) {
//        float f = (float) mc.player.getItemUseTimeLeft() - tickDelta + 1.0F;
//        float g = f / (float) stack.getMaxUseTime(mc.player);
//        if (g < 0.8F) {
//            float h = MathHelper.abs(MathHelper.cos(f / 4.0F * (float) Math.PI) * 0.1F);
//            matrices.translate(0.0F, h, 0.0F);
//        }
//
//        float h = 1.0F - (float) Math.pow(g, 27.0);
//        int i = arm == Arm.RIGHT ? 1 : -1;
//        matrices.translate(h * 0.6F * (float) i, h * -0.5F, h * 0.0F);
//        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * h * 90.0F));
//        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(h * 10.0F));
//        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) i * h * 30.0F));
//    }

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

//    public void renderItem(
//            LivingEntity entity,
//            ItemStack stack,
//            ModelTransformationMode renderMode,
//            boolean leftHanded,
//            MatrixStack matrices,
//            VertexConsumerProvider vertexConsumers,
//            int light
//    ) {
//        if (!stack.isEmpty()) {
//            mc.getItemRenderer().renderItem(
//                    entity,
//                    stack,
//                    renderMode,
//                    leftHanded,
//                    matrices,
//                    vertexConsumers,
//                    entity.getWorld(),
//                    light,
//                    OverlayTexture.DEFAULT_UV,
//                    entity.getId() + renderMode.ordinal()
//            );
//        }
//    }
}
