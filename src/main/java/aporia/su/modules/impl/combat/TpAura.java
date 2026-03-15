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
 * TpAura - телепорт к цели с атакой и возвратом.
 *
 * Default: Реальный ТП → Удар → Возврат на ИСХОДНУЮ позицию
 * Spoof: 4 spoof атаки (пакеты) + 1 реальный ТП каждый 5-й удар
 */
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TpAura extends ModuleStructure {

    @Native(type = Native.Type.VMProtectBeginUltra)
    public static TpAura getInstance() {
        return Instance.get(TpAura.class);
    }

    // ==================== SETTINGS ====================

    private final SelectSetting mode = new SelectSetting("Режим", "Mode")
            .value("Default", "Spoof")
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

    private final SliderSettings realTpRate = new SliderSettings("Реальный ТП раз в N", "Real TP every N hits")
            .range(2.0f, 10.0f)
            .setValue(5.0f);

    private final BooleanSetting flightPackets = new BooleanSetting("Пакеты полёта", "Send flight packets for Grim")
            .setValue(true);

    private final BooleanSetting autoReturn = new BooleanSetting("Авто-возврат", "Auto return to start position")
            .setValue(true);

    private final MultiSelectSetting targetType = new MultiSelectSetting("Цели", "Target types")
            .value("Игроки", "Мобы", "Животные")
            .selected("Игроки");

    private final BooleanSetting ignoreWalls = new BooleanSetting("Бить сквозь стены", "Attack through walls")
            .setValue(true);

    private final BooleanSetting lockTargetBtn = new BooleanSetting("Фикс. цель", "Lock on single target")
            .setValue(false);

    // ==================== HANDLERS ====================

    private final IsxodHandler isxodHandler = new IsxodHandler();
    private final TargetFinder targetFinder = new TargetFinder();

    // ==================== STATE ====================

    @Setter
    @NonFinal
    LivingEntity target = null;

    @NonFinal
    LivingEntity lockedTarget = null;

    @NonFinal
    int spoofCounter = 0;

    @NonFinal
    long lastAttackTime = 0;

    // Флаг - мы сейчас в процессе ТП (не перезаписывать isxod!)
    @NonFinal
    boolean inTeleport = false;

    TargetFinder.EntityFilter entityFilter;

    public TpAura() {
        super("TpAura", ModuleCategory.COMBAT);
        settings(mode, rotationMode, attackDistance, hitsPerSecond, realTpRate, flightPackets,
                autoReturn, targetType, ignoreWalls, lockTargetBtn);
    }

    // ==================== LIFECYCLE ====================

    @Override
    public void activate() {
        resetState();
        updateEntityFilter();
        lastAttackTime = System.currentTimeMillis();
        spoofCounter = 0;
        inTeleport = false;

        // Сохраняем исходную позицию ПРИ АКТИВАЦИИ
        if (mc.player != null) {
            isxodHandler.saveIsxod();
        }
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
        spoofCounter = 0;
        lastAttackTime = 0;
        inTeleport = false;
        targetFinder.releaseTarget();
        isxodHandler.reset();
    }

    private void updateEntityFilter() {
        this.entityFilter = new TargetFinder.EntityFilter(targetType.getSelected());
    }

    // ==================== MAIN LOGIC ====================

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

        // Выполняем атаку
        if (mode.isSelected("Default")) {
            performDefaultAttack();
        } else if (mode.isSelected("Spoof")) {
            performSpoofAttack();
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

    // ==================== TARGET FINDING ====================

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
                spoofCounter = 0;
            }
            target = newTarget;

            if (lockTargetBtn.isValue() && lockedTarget == null) {
                lockedTarget = newTarget;
            }
        } else {
            target = null;
        }
    }

    // ==================== DEFAULT MODE ====================

    /**
     * Default: Реальный ТП → Удар → Возврат на ИСХОДНУЮ
     */
    private void performDefaultAttack() {
        if (target == null || mc.interactionManager == null || mc.player == null) {
            return;
        }

        if (!isxodHandler.hasSavedPosition()) {
            return;
        }

        // ИСХОДНАЯ позиция (сохранена при активации!)
        Vec3d isxodPos = isxodHandler.getIsxodPosition();

        // Позиция атаки рядом с целью
        Vec3d attackPos = calculateAttackPosition(target.getEntityPos());

        // Устанавливаем флаг чтобы не перезаписать isxod
        inTeleport = true;

        // 1. ТП к цели
        teleportReal(attackPos);

        // 2. Удар
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);

        // 3. Возврат на ИСХОДНУЮ позицию
        if (autoReturn.isValue()) {
            teleportReal(isxodPos);
        }

        inTeleport = false;
    }

    // ==================== SPOOF MODE ====================

    /**
     * Spoof режим:
     * - spoofCounter < N → spoof пакеты (игрок визуально на месте)
     * - spoofCounter >= N → реальный ТП + сброс
     */
    private void performSpoofAttack() {
        if (target == null || mc.interactionManager == null || mc.player == null) {
            return;
        }

        int tpRate = (int) realTpRate.getValue();

        spoofCounter++;

        boolean shouldRealTeleport = (spoofCounter >= tpRate);

        if (shouldRealTeleport) {
            performRealTeleportAttack();
            spoofCounter = 0;
        } else {
            performPureSpoofAttack();
        }
    }

    /**
     * Реальный ТП с атакой (для обхода античита)
     */
    private void performRealTeleportAttack() {
        if (!isxodHandler.hasSavedPosition()) return;

        Vec3d isxodPos = isxodHandler.getIsxodPosition();
        Vec3d attackPos = calculateAttackPosition(target.getEntityPos());

        inTeleport = true;

        // Flight packets ТУДА
        if (flightPackets.isValue()) {
            sendFlightPackets(mc.player.getEntityPos(), attackPos);
        }

        // ТП к цели
        teleportReal(attackPos);

        // Удар
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);

        // Возврат на ИСХОДНУЮ
        if (autoReturn.isValue()) {
            teleportReal(isxodPos);

            if (flightPackets.isValue()) {
                sendFlightPackets(attackPos, isxodPos);
            }
        }

        inTeleport = false;
    }

    /**
     * Чистый SPOOF - только пакеты, игрок визуально на месте
     */
    private void performPureSpoofAttack() {
        if (!isxodHandler.hasSavedPosition() || mc.player == null) return;

        // ИСХОДНАЯ позиция - сюда возвращаемся в пакете
        Vec3d isxodPos = isxodHandler.getIsxodPosition();

        // Позиция атаки рядом с целью
        Vec3d attackPos = calculateAttackPosition(target.getEntityPos());

        float yaw = mc.player.getYaw();
        float pitch = mc.player.getPitch();

        // ===== SPOOF К ЦЕЛИ (сервер думает мы там) =====
        sendSpoofPositionPacket(attackPos, yaw, pitch);

        // ===== АТАКА =====
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);

        // ===== SPOOF ВОЗВРАТ НА ИСХОДНУЮ =====
        if (autoReturn.isValue()) {
            sendSpoofPositionPacket(isxodPos, yaw, pitch);
        }

        // ВАЖНО: mc.player.setPos() НЕ вызываем!
        // Игрок визуально остаётся на ИСХОДНОЙ позиции!
    }

    /**
     * SPOOF пакет - отправляем позицию серверу, локальную НЕ меняем
     */
    private void sendSpoofPositionPacket(Vec3d pos, float yaw, float pitch) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        ThreadLocalRandom random = ThreadLocalRandom.current();

        PlayerMoveC2SPacket.Full packet = new PlayerMoveC2SPacket.Full(
                pos.x + random.nextDouble(-0.0001, 0.0001),
                pos.y + random.nextDouble(-0.0001, 0.0001),
                pos.z + random.nextDouble(-0.0001, 0.0001),
                yaw,
                pitch,
                false,
                false
        );

        PlayerInteractionHelper.sendPacketWithOutEvent(packet);

        // НЕ вызываем mc.player.setPos()!
    }

    // ==================== TELEPORT METHODS ====================

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

        // Обновляем локальную позицию (РЕАЛЬНЫЙ ТП)
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

    private void sendFlightPackets(Vec3d from, Vec3d to) {
        if (!flightPackets.isValue() || mc.getNetworkHandler() == null || mc.player == null) return;

        double distance = from.distanceTo(to);
        int packets = (int) Math.ceil(distance / 8.0);
        packets = Math.max(1, Math.min(packets, 5));

        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 1; i <= packets; i++) {
            double progress = (double) i / (packets + 1);
            Vec3d intermediatePos = from.lerp(to, progress);

            intermediatePos = new Vec3d(
                    intermediatePos.x + random.nextDouble(-0.05, 0.05),
                    intermediatePos.y + random.nextDouble(-0.02, 0.02),
                    intermediatePos.z + random.nextDouble(-0.05, 0.05)
            );

            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(
                    intermediatePos.x, intermediatePos.y, intermediatePos.z,
                    mc.player.getYaw() + random.nextFloat(-5, 5),
                    mc.player.getPitch() + random.nextFloat(-2, 2),
                    false,
                    false
            ));
        }
    }

    // ==================== ROTATION ====================

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

    // ==================== HELPERS ====================

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