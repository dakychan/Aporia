package aporia.su.modules.impl.combat;

import anidumpproject.api.annotation.Native;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import aporia.su.util.Instance;
import aporia.su.util.events.api.EventHandler;
import aporia.su.util.events.api.types.EventType;
import aporia.su.util.events.impl.entity.RotationUpdateEvent;
import aporia.su.util.events.impl.TickEvent;
import aporia.su.modules.impl.combat.aura.Angle;
import aporia.su.modules.impl.combat.aura.AngleConfig;
import aporia.su.modules.impl.combat.aura.AngleConnection;
import aporia.su.modules.impl.combat.aura.MathAngle;
import aporia.su.modules.impl.combat.aura.impl.LinearConstructor;
import aporia.su.modules.impl.combat.aura.target.TargetFinder;
import aporia.su.modules.impl.combat.aura.target.Vector;
import aporia.su.modules.impl.combat.aura.rotations.MatrixAngle;
import aporia.su.modules.impl.combat.aura.rotations.SPAngle;
import aporia.su.modules.impl.combat.aura.impl.RotateConstructor;
import aporia.su.modules.impl.combat.tpaura.IsxodHandler;
import aporia.su.modules.module.ModuleStructure;
import aporia.su.modules.module.category.ModuleCategory;
import aporia.su.modules.module.setting.implement.BooleanSetting;
import aporia.su.modules.module.setting.implement.MultiSelectSetting;
import aporia.su.modules.module.setting.implement.SelectSetting;
import aporia.su.modules.module.setting.implement.SliderSettings;
import aporia.su.util.user.render.math.TaskPriority;
import aporia.su.util.user.string.PlayerInteractionHelper;

import java.util.concurrent.ThreadLocalRandom;

/**
 * TpAura - атака через spoof пакетов.
 *
 * ================================
 * ВАЖНО: НЕ ИСПОЛЬЗУЕТ mc.player.setPos()!
 * ================================
 *
 * Default: Реальный ТП → Удар → Возврат
 * PureSpoof: ТОЛЬКО пакеты, игрок визуально НА МЕСТЕ!
 * Hybrid: Nspoof ударов + 1 реальный ТП для обхода
 */
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TpAura extends ModuleStructure {

    @Native(type = Native.Type.VMProtectBeginUltra)
    public static TpAura getInstance() {
        return Instance.get(TpAura.class);
    }

    private final SelectSetting mode = new SelectSetting("Режим", "Mode")
            .value("Default", "PureSpoof", "Hybrid")
            .selected("Default");

    private final SelectSetting rotationMode = new SelectSetting("Ротация", "Rotation mode")
            .value("Matrix", "SpookyTime", "Linear")
            .selected("Matrix");

    private final SliderSettings attackDistance = new SliderSettings("Дистанция атаки", "Attack distance from target")
            .range(0.0f, 3.0f)
            .setValue(0.0f);

    private final SliderSettings hitsPerSecond = new SliderSettings("Ударов в секунду", "Hits per second")
            .range(1.0f, 20.0f)
            .setValue(10.0f);

    private final SliderSettings realTpRate = new SliderSettings("Реальный ТП раз в N", "Real TP every N hits (Hybrid)")
            .range(3.0f, 15.0f)
            .setValue(5.0f);

    private final BooleanSetting autoReturn = new BooleanSetting("Авто-возврат", "Auto return spoof packet")
            .setValue(true);

    private final BooleanSetting spoofGround = new BooleanSetting("Spoof onGround", "Spoof ground status in return packet")
            .setValue(true);

    private final MultiSelectSetting targetType = new MultiSelectSetting("Цели", "Target types")
            .value("Игроки", "Мобы", "Животные")
            .selected("Игроки");

    private final BooleanSetting ignoreWalls = new BooleanSetting("Бить сквозь стены", "Attack through walls")
            .setValue(true);

    private final BooleanSetting lockTargetBtn = new BooleanSetting("Фикс. цель", "Lock on single target")
            .setValue(false);

    private final IsxodHandler isxodHandler = new IsxodHandler();
    private final TargetFinder targetFinder = new TargetFinder();

    @Setter
    @NonFinal
    LivingEntity target = null;

    @NonFinal
    LivingEntity lockedTarget = null;

    @NonFinal
    int hybridCounter = 0;

    @NonFinal
    long lastAttackTime = 0;

    TargetFinder.EntityFilter entityFilter;

    public TpAura() {
        super("TpAura", ModuleCategory.COMBAT);
        settings(mode, rotationMode, attackDistance, hitsPerSecond, realTpRate, autoReturn,
                spoofGround, targetType, ignoreWalls, lockTargetBtn);
    }

    @Override
    public void activate() {
        resetState();
        if (mc.player != null) {
            isxodHandler.saveIsxod();
        }
        updateEntityFilter();
        lastAttackTime = System.currentTimeMillis();
        hybridCounter = 0;
    }

    @Override
    public void deactivate() {
        AngleConnection.INSTANCE.clear();
        AngleConnection.INSTANCE.setRotation(null);
        resetState();
    }

    private void resetState() {
        target = null;
        lockedTarget = null;
        hybridCounter = 0;
        lastAttackTime = 0;
        targetFinder.releaseTarget();
        isxodHandler.reset();
    }

    private void updateEntityFilter() {
        this.entityFilter = new TargetFinder.EntityFilter(targetType.getSelected());
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        long attackDelay = (long) (1000.0 / hitsPerSecond.getValue());
        if (System.currentTimeMillis() - lastAttackTime < attackDelay) {
            return;
        }
        findTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        if (!isxodHandler.hasSavedPosition()) {
            isxodHandler.saveIsxod();
        }
        switch (mode.getSelected()) {
            case "Default" -> performDefaultAttack();
            case "PureSpoof" -> performPureSpoofAttack();
            case "Hybrid" -> performHybridAttack();
        }
        lastAttackTime = System.currentTimeMillis();
    }

    @EventHandler
    public void onRotationUpdate(RotationUpdateEvent event) {
        if (mc.player == null || target == null) {
            return;
        }
        if (event.getType() == EventType.PRE) {
            rotateToTargetConstant();
        }
    }

    private void findTarget() {
        if (lockTargetBtn.isValue() && lockedTarget != null && lockedTarget.isAlive()) {
            target = lockedTarget;
            return;
        }
        float searchRange = mode.isSelected("Default") ? 50.0f : 30.0f;
        targetFinder.searchTargets(
                mc.world.getEntities(),
                searchRange,
                360,
                ignoreWalls.isValue()
        );
        targetFinder.validateTarget(entityFilter::isValid);
        LivingEntity newTarget = targetFinder.getCurrentTarget();
        if (newTarget != null) {
            if (target != null && target != newTarget) {
                hybridCounter = 0;
            }
            target = newTarget;
            if (lockTargetBtn.isValue() && lockedTarget == null) {
                lockedTarget = newTarget;
            }
        } else {
            target = null;
        }
    }

    /**
     * Default: Реальный ТП → Удар → Возврат
     */
    private void performDefaultAttack() {
        if (target == null || mc.interactionManager == null || mc.player == null) {
            return;
        }
        if (!isxodHandler.hasSavedPosition()) {
            return;
        }
        Vec3d isxodPos = isxodHandler.getIsxodPosition();
        Vec3d attackPos = calculateAttackPosition(target.getEntityPos());
        teleportReal(attackPos);
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
        if (autoReturn.isValue()) {
            teleportReal(isxodPos);
        }
    }

    /**
     * PureSpoof: ТОЛЬКО ПАКЕТЫ!
     *
     * ВАЖНО: mc.player.setPos() НЕ ВЫЗЫВАЕТСЯ!
     * Игрок визуально остаётся на месте!
     */
    private void performPureSpoofAttack() {
        if (target == null || mc.interactionManager == null || mc.player == null) {
            return;
        }
        Vec3d realPos = mc.player.getEntityPos();
        Vec3d attackPos = calculateAttackPosition(target.getEntityPos());
        float yaw = mc.player.getYaw();
        float pitch = mc.player.getPitch();
        sendSpoofPacket(attackPos, yaw, pitch, false);
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
        if (autoReturn.isValue()) {
            sendSpoofPacket(realPos, yaw, pitch, spoofGround.isValue());
        }
    }

    /**
     * Hybrid: N spoof ударов + 1 реальный ТП
     */
    private void performHybridAttack() {
        if (target == null || mc.interactionManager == null || mc.player == null) {
            return;
        }
        hybridCounter++;
        int tpRate = (int) realTpRate.getValue();
        if (hybridCounter >= tpRate) {
            performRealTpAttack();
            hybridCounter = 0;
        } else {
            performPureSpoofAttack();
        }
    }

    /**
     * Реальный ТП с атакой (для Hybrid режима)
     */
    private void performRealTpAttack() {
        if (!isxodHandler.hasSavedPosition()) return;
        Vec3d isxodPos = isxodHandler.getIsxodPosition();
        Vec3d attackPos = calculateAttackPosition(target.getEntityPos());
        teleportReal(attackPos);
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
        if (autoReturn.isValue()) {
            teleportReal(isxodPos);
        }
    }

    /**
     * SPOOF пакет - отправляем позицию серверу БЕЗ изменения локальной позиции!
     *
     * КЛЮЧЕВОЕ: mc.player.setPos() НЕ вызывается!
     */
    private void sendSpoofPacket(Vec3d pos, float yaw, float pitch, boolean onGround) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        PlayerMoveC2SPacket.Full packet = new PlayerMoveC2SPacket.Full(
                pos.x + random.nextDouble(-0.0001, 0.0001),
                pos.y + random.nextDouble(-0.0001, 0.0001),
                pos.z + random.nextDouble(-0.0001, 0.0001),
                yaw,
                pitch,
                onGround,
                false
        );
        PlayerInteractionHelper.sendPacketWithOutEvent(packet);
    }

    /**
     * Реальная телепортация С обновлением локальной позиции
     */
    private void teleportReal(Vec3d targetPos) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        if (!isValidPosition(targetPos)) return;
        Vec3d currentPos = mc.player.getEntityPos();
        double distance = currentPos.distanceTo(targetPos);
        if (distance > 20.0) {
            int packets = (int) Math.ceil(distance / 10.0);
            packets = Math.max(2, Math.min(packets, 15));
            for (int i = 1; i <= packets; i++) {
                double progress = (double) i / packets;
                Vec3d intermediatePos = currentPos.lerp(targetPos, progress);
                sendPositionPacket(intermediatePos);
            }
        } else {
            sendPositionPacket(targetPos);
        }
        mc.player.setPos(targetPos.x, targetPos.y, targetPos.z);
    }

    private void sendPositionPacket(Vec3d pos) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(
                pos.x, pos.y, pos.z,
                mc.player.getYaw(),
                mc.player.getPitch(),
                mc.player.isOnGround(),
                mc.player.horizontalCollision
        ));
    }

    private void rotateToTargetConstant() {
        if (target == null) return;
        Vec3d aimPoint = Vector.hitbox(target, 1, target.isOnGround() ? 0.9F : 1.4F, 1, 2);
        Angle targetAngle = MathAngle.calculateAngle(aimPoint);
        RotateConstructor rotator = getRotationConstructor();
        AngleConfig config = new AngleConfig(rotator, true, false);
        Angle.VecRotation rotation = new Angle.VecRotation(targetAngle, targetAngle.toVector());
        AngleConnection.INSTANCE.rotateTo(
                rotation,
                target,
                1,
                config,
                TaskPriority.HIGH_IMPORTANCE_1,
                this
        );
    }

    private RotateConstructor getRotationConstructor() {
        return switch (rotationMode.getSelected()) {
            case "Matrix" -> new MatrixAngle();
            case "SpookyTime" -> new SPAngle();
            default -> new LinearConstructor();
        };
    }

    private Vec3d calculateAttackPosition(Vec3d targetPos) {
        double distance = attackDistance.getValue();
        if (distance <= 0) {
            return targetPos.add(0, 0.1, 0);
        }
        if (mc.player != null) {
            Vec3d playerPos = mc.player.getEntityPos();
            Vec3d direction = playerPos.subtract(targetPos).normalize();
            return targetPos.add(direction.multiply(distance));
        }
        return targetPos;
    }

    private boolean isValidPosition(Vec3d pos) {
        if (pos == null) return false;
        if (Double.isNaN(pos.x) || Double.isNaN(pos.y) || Double.isNaN(pos.z)) return false;
        if (Double.isInfinite(pos.x) || Double.isInfinite(pos.y) || Double.isInfinite(pos.z)) return false;
        if (Math.abs(pos.x) > 30000000 || Math.abs(pos.z) > 30000000) return false;
        return true;
    }

    public void unlockTarget() {
        lockedTarget = null;
        lockTargetBtn.setValue(false);
    }

    public void lockCurrentTarget() {
        if (target != null) {
            lockedTarget = target;
            lockTargetBtn.setValue(true);
        }
    }
}