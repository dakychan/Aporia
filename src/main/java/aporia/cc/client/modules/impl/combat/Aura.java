package aporia.cc.client.modules.impl.combat;

import aporia.cc.utility.game.player.PointFinder;
import aporia.cc.utility.game.player.RaytracingUtil;
import aporia.cc.utility.game.player.SimulatedPlayer;
import aporia.cc.utility.game.player.TargetSelector;
import com.darkmagician6.eventapi.EventTarget;
import lombok.Getter;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import aporia.cc.Aporia;
import aporia.cc.base.events.impl.player.EventMoveInput;
import aporia.cc.base.events.impl.player.EventRotate;
import aporia.cc.base.player.AttackUtil;
import aporia.cc.base.rotation.RotationTarget;
import aporia.cc.client.modules.api.Category;
import aporia.cc.client.modules.api.Module;
import aporia.cc.client.modules.api.ModuleAnnotation;
import aporia.cc.client.modules.api.setting.impl.BooleanSetting;
import aporia.cc.client.modules.api.setting.impl.ModeSetting;
import aporia.cc.client.modules.api.setting.impl.MultiBooleanSetting;
import aporia.cc.client.modules.api.setting.impl.NumberSetting;
import aporia.cc.utility.game.player.*;
import aporia.cc.utility.game.player.rotation.Rotation;
import aporia.cc.utility.game.player.rotation.RotationUtil;

import java.util.List;

import static aporia.cc.utility.game.player.MovingUtil.fixMovement;

@ModuleAnnotation(name = "Aura", category = Category.COMBAT, description = "Бьет таргета")
public final class Aura extends Module {

    public static final Aura INSTANCE = new Aura();
    private Aura() {}

    private final ModeSetting rotationMode = new ModeSetting("Ротация");
    private final ModeSetting.Value hvh = new ModeSetting.Value(rotationMode, "ХВХ");
    private final ModeSetting.Value hollyworld = new ModeSetting.Value(rotationMode, "HollyWorld").select();

    private final ModeSetting sprintMode = new ModeSetting("Бег");
    private final ModeSetting.Value sprintHvh = new ModeSetting.Value(sprintMode, "ХВХ");
    private final ModeSetting.Value sprintNormal = new ModeSetting.Value(sprintMode, "Нормал").select();
    private final ModeSetting.Value sprintLegit = new ModeSetting.Value(sprintMode, "Легит");
    private final ModeSetting.Value sprintNone = new ModeSetting.Value(sprintMode, "Нет");

    private final ModeSetting correction = new ModeSetting("Коррекция");
    private final ModeSetting.Value correctionFocus = new ModeSetting.Value(correction, "Фокус");
    private final ModeSetting.Value correctionGood = new ModeSetting.Value(correction, "Свободная").select();
    private final ModeSetting.Value correctionNone = new ModeSetting.Value(correction, "Нет");

    private final NumberSetting distance = new NumberSetting("Дистанция", 3, 0.5f, 6, 0.1f, "Дистанция атаки");
    private final NumberSetting distanceRotation = new NumberSetting("Пре-дистанция", 0.1f, 0, 6, 0.1f);

    private final MultiBooleanSetting settings = new MultiBooleanSetting("Настройки");
    private final MultiBooleanSetting.Value shieldBreak = new MultiBooleanSetting.Value(settings, "Ломать щит", true);
    private final MultiBooleanSetting.Value shielRealese = new MultiBooleanSetting.Value(settings, "Отжимать щит", true);
    private final MultiBooleanSetting.Value eatUseAttack = new MultiBooleanSetting.Value(settings, "Бить и есть", true);
    private final MultiBooleanSetting.Value attackIgnoreWals = new MultiBooleanSetting.Value(settings, "Бить через стены", true);

    private final MultiBooleanSetting targetTypeSetting = MultiBooleanSetting.create("Атаковать", List.of("Игроков", "Враждебных", "Мирных"));

    private final BooleanSetting onlyCrit = new BooleanSetting("Только криты", true);
    private final BooleanSetting smartCrit = new BooleanSetting("Умные криты", "Бьет критами если зажата кнопка прыжка", false, onlyCrit::isEnabled);

    private final TargetSelector targetSelector = new TargetSelector();
    private final PointFinder pointFinder = new PointFinder();
    private LivingEntity target = null;
    private boolean legitBackStop = false; //Для легитного спринта бек
    @Getter
    private boolean preAttack =false;
    @Getter
    private boolean isCanAttack =false;
    @EventTarget
    public void eventRotate(EventRotate e) {
        if (legitBackStop) {
            legitBackStop = false;
            mc.options.forwardKey.setPressed(
                    InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.forwardKey.getDefaultKey().getCode())
            );
        }

        target = updateTarget();
        preAttack = false;
        isCanAttack = false;
        if (target == null) return;

        Pair<Vec3d, Box> point = pointFinder.computeVector(
                target,
                distance.getCurrent(),
                rotationManager.getCurrentRotation(),
                new Vec3d(0, 0, 0),
                attackIgnoreWals.isEnabled()
        );

        Vec3d eyes = SimulatedPlayer.simulateLocalPlayer(1).pos.add(0, mc.player.getDimensions(mc.player.getPose()).eyeHeight(), 0);
        Rotation angle = RotationUtil.fromVec3d(point.getLeft().subtract(eyes));

        Box box = point.getRight();
        preAttack = updatePreAttack();
        isCanAttack = isAttack();

        if (RaytracingUtil.rayTrace(rotationManager.getCurrentRotation().toVector(), distance.getCurrent(), box)
                && isCanAttack
                && (!Aporia.getInstance().getServerHandler().isServerSprint() ||mc.player.isGliding() || AttackUtil.hasMovementRestrictions() || sprintHvh.isSelected() || sprintNone.isSelected())) {

            if (sprintHvh.isSelected()) {
                mc.player.setSprinting(false);
                mc.player.sendSprintingPacket();
            }

            AttackUtil.attackEntity(target);

            mc.options.sprintKey.setPressed(true);
        }
        preAttack = updatePreAttack();
        isCanAttack = isAttack();
        if (hvh.isSelected()) {
            rotationManager.setRotation(
                    new RotationTarget(angle, () -> aimManager.rotate(aimManager.getInstantSetup(), angle), aimManager.getInstantSetup()),
                    3, this
            );
        }

        if (hollyworld.isSelected() && (preAttack || isCanAttack )) {
            rotationManager.setRotation(
                    new RotationTarget(angle, () -> aimManager.rotate(aimManager.getInstantSetup(), angle), aimManager.getAiSetup()),
                    3, this
            );
        }

        if (preAttack || isCanAttack) {
            updateSprint();
        }
    }

    private boolean updatePreAttack() {
        SimulatedPlayer simulatedPlayer = SimulatedPlayer.simulateLocalPlayer(1);

        if (mc.player.isUsingItem() && !eatUseAttack.isEnabled()) return false;
        if (mc.player.getAttackCooldownProgress(1) < 0.9) return false;

        if (onlyCrit.isEnabled() && !AttackUtil.hasPreMovementRestrictions(simulatedPlayer)) {
            return AttackUtil.isPrePlayerInCriticalState(simulatedPlayer) || (smartCrit.isEnabled() && !mc.options.jumpKey.isPressed());
        }
        return true;
    }

    private boolean isAttack() {
        if (mc.player.isUsingItem() && !eatUseAttack.isEnabled()) return false;
        if (mc.player.getAttackCooldownProgress(1) < 0.9) return false;

        if (onlyCrit.isEnabled() && !AttackUtil.hasMovementRestrictions()) {
            return AttackUtil.isPlayerInCriticalState() || (smartCrit.isEnabled() && !mc.options.jumpKey.isPressed());
        }
        return true;
    }

    public void updateSprint() {
        if (!hasStopSprint()) return;

        boolean sprint = mc.options.sprintKey.isPressed();
        boolean forward = mc.options.forwardKey.isPressed();

        if (sprintLegit.isSelected()) {
            sprint = false;
            if (mc.player.isSprinting()) {

                forward = false;
                legitBackStop = true;
            }
        }

        if (sprintNormal.isSelected()) {
            if (mc.player.isSprinting()) mc.player.setSprinting(false);
            sprint = false;
        }

        mc.options.sprintKey.setPressed(sprint);
        mc.options.forwardKey.setPressed(forward);
    }

    public boolean hasStopSprint() {
        return !sprintNone.isSelected() && !AttackUtil.hasMovementRestrictions();
    }

    private LivingEntity updateTarget() {
        TargetSelector.EntityFilter filter = new TargetSelector.EntityFilter(targetTypeSetting.getSelectedNames());
        targetSelector.searchTargets(mc.world.getEntities(), distance.getCurrent() + distanceRotation.getCurrent(), attackIgnoreWals.isEnabled());
        targetSelector.validateTarget(filter::isValid);
        return targetSelector.getCurrentTarget();
    }

    @EventTarget
    private void setCorrection(EventMoveInput eventMoveInput) {
        if (correctionNone.isSelected()) return;

        if (correctionFocus.isSelected()) {
            Rotation angle = RotationUtil.fromVec3d(target.getBoundingBox().getCenter().subtract(mc.player.getBoundingBox().getCenter()));
            fixMovement(eventMoveInput, rotationManager.getCurrentRotation().getYaw(), angle.getYaw());
        } else {
            fixMovement(eventMoveInput, rotationManager.getCurrentRotation().getYaw(), mc.player.getYaw());
        }
    }

    public LivingEntity getTarget() {
        return this.isEnabled()?target:null;
    }


}

