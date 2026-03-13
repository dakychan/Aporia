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
import net.minecraft.util.math.MathHelper;
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
import aporia.su.modules.impl.combat.tpaura.IsxodHandler;
import aporia.su.modules.module.ModuleStructure;
import aporia.su.modules.module.category.ModuleCategory;
import aporia.su.modules.module.setting.implement.BooleanSetting;
import aporia.su.modules.module.setting.implement.MultiSelectSetting;
import aporia.su.modules.module.setting.implement.SelectSetting;
import aporia.su.modules.module.setting.implement.SliderSettings;
import aporia.su.util.user.render.math.TaskPriority;

import java.util.concurrent.ThreadLocalRandom;

/**
 * TpAura - телепорт к цели с атакой и возвратом.
 *
 * Режимы:
 * - Default: ТП → Удар → Возврат (цикл)
 * - Spoof: Обход Matrix + Grim через spoof-пакеты
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

    // Default mode settings
    private final SliderSettings attackDistance = new SliderSettings("Дистанция атаки", "Attack distance from target")
            .range(0.0f, 3.0f)
            .setValue(0.0f);

    private final SliderSettings hitsPerSecond = new SliderSettings("Ударов в секунду", "Hits per second")
            .range(1.0f, 20.0f)
            .setValue(10.0f);

    // Spoof mode settings
    private final SliderSettings spoofRate = new SliderSettings("Частота ТП", "Real TP every N hits")
            .range(3.0f, 10.0f)
            .setValue(5.0f);

    private final BooleanSetting flightPackets = new BooleanSetting("Пакеты полёта", "Send flight packets for Grim")
            .setValue(true);

    // Target settings
    private final MultiSelectSetting targetType = new MultiSelectSetting("Цели", "Target types")
            .value("Игроки", "Мобы", "Животные")
            .selected("Игроки");

    private final BooleanSetting ignoreWalls = new BooleanSetting("Бить сквозь стены", "Attack through walls")
            .setValue(true);

    private final BooleanSetting autoReturn = new BooleanSetting("Авто-возврат", "Auto return to start position")
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
    int spoofHitCounter = 0;

    /** Время последней атаки */
    @NonFinal
    long lastAttackTime = 0;

    // ==================== CACHED FILTER ====================

    TargetFinder.EntityFilter entityFilter;

    public TpAura() {
        super("TpAura", ModuleCategory.COMBAT);
        settings(mode, attackDistance, hitsPerSecond, spoofRate, flightPackets,
                targetType, ignoreWalls, autoReturn, lockTargetBtn);
    }

    // ==================== LIFECYCLE ====================

    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void activate() {
        resetState();

        if (mc.player != null) {
            isxodHandler.saveIsxod();
        }

        updateEntityFilter();
        lastAttackTime = System.currentTimeMillis();
    }

    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void deactivate() {
        // Возвращаемся на исходную при выключении (если авто-возврат включён)
        if (autoReturn.isValue()) {
            returnToIsxodAuto();
        }

        // СБРОС РОТАЦИИ - важно!
        AngleConnection.INSTANCE.clear();
        AngleConnection.INSTANCE.setRotation(null);

        resetState();
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void resetState() {
        target = null;
        lockedTarget = null;
        spoofHitCounter = 0;
        lastAttackTime = 0;
        targetFinder.releaseTarget();
        isxodHandler.reset();
    }

    private void updateEntityFilter() {
        this.entityFilter = new TargetFinder.EntityFilter(targetType.getSelected());
    }

    // ==================== MAIN LOGIC ====================

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        // Проверка задержки атаки (CPS)
        long attackDelay = (long) (1000.0 / hitsPerSecond.getValue());
        if (System.currentTimeMillis() - lastAttackTime < attackDelay) {
            return;
        }

        // Поиск цели
        findTarget();

        // Если цели нет - возвращаемся на исходную (если авто-возврат включён)
        if (target == null || !target.isAlive()) {
            if (autoReturn.isValue()) {
                returnToIsxodAuto();
            }
            return;
        }

        // Выполняем атаку в зависимости от режима
        if (mode.isSelected("Default")) {
            performDefaultAttack();
        } else if (mode.isSelected("Spoof")) {
            performSpoofAttack();
        }

        lastAttackTime = System.currentTimeMillis();
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onRotationUpdate(RotationUpdateEvent event) {
        if (mc.player == null || target == null) {
            return;
        }

        // Ротация только для Spoof режима (плавная)
        if (mode.isSelected("Spoof") && event.getType() == EventType.PRE) {
            rotateToTargetSmooth();
        }
    }

    // ==================== TARGET FINDING ====================

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void findTarget() {
        // Если включён лок таргет и есть заблокированная цель
        if (lockTargetBtn.isValue() && lockedTarget != null && lockedTarget.isAlive()) {
            target = lockedTarget;
            return;
        }

        // Поиск новой цели
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
            // Если цель сменилась - сбрасываем счётчик только для Spoof
            if (target != null && target != newTarget && mode.isSelected("Spoof")) {
                spoofHitCounter = 0;
            }

            target = newTarget;

            // Сохраняем цель при локе
            if (lockTargetBtn.isValue() && lockedTarget == null) {
                lockedTarget = newTarget;
            }
        } else {
            target = null;
        }
    }

    // ==================== DEFAULT MODE ====================

    /**
     * Default режим: ТП → Удар → Возврат
     * Каждый удар - полный цикл
     */
    @Native(type = Native.Type.VMProtectBeginMutation)
    private void performDefaultAttack() {
        if (target == null || mc.interactionManager == null || !isxodHandler.hasSavedPosition()) {
            return;
        }

        Vec3d isxodPos = isxodHandler.getIsxodPosition();
        Vec3d targetPos = target.getEntityPos();

        // Позиция атаки (в хитбоксе или рядом)
        Vec3d attackPos = calculateAttackPosition(targetPos, isxodPos);

        // Snap ротация (мгновенная) к цели
        Angle targetAngle = MathAngle.calculateAngle(targetPos.add(0, target.getHeight() / 2, 0));
        snapRotation(targetAngle);

        // 1. ТП к цели
        teleportToPosition(attackPos, true);

        // 2. Атака
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);

        // 3. ТП обратно на исходную (если авто-возврат включён)
        if (autoReturn.isValue()) {
            teleportToPosition(isxodPos, true);
        }
    }

    // ==================== SPOOF MODE ====================

    /**
     * Spoof режим: Обход Matrix + Grim
     *
     * Обычные удары: spoof пакеты (игрок визуально на месте)
     * Каждый N-й удар: реальный ТП + flight packets
     */
    @Native(type = Native.Type.VMProtectBeginUltra)
    private void performSpoofAttack() {
        if (target == null || mc.interactionManager == null || !isxodHandler.hasSavedPosition()) {
            return;
        }

        Vec3d isxodPos = isxodHandler.getIsxodPosition();
        Vec3d targetPos = target.getEntityPos();

        // Позиция атаки
        Vec3d attackPos = calculateAttackPosition(targetPos, isxodPos);

        spoofHitCounter++;

        // Проверяем: это spoof удар или реальный ТП?
        boolean shouldRealTeleport = (spoofHitCounter % (int) spoofRate.getValue() == 0);

        if (shouldRealTeleport) {
            // ===== РЕАЛЬНЫЙ ТП (каждый N-й удар) =====

            // Flight packets ТУДА (имитация полёта к цели)
            if (flightPackets.isValue()) {
                sendFlightPackets(isxodPos, attackPos);
            }

            // ТП к цели
            teleportToPosition(attackPos, true);

            // Удар
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);

            // ТП обратно (если авто-возврат включён)
            if (autoReturn.isValue()) {
                teleportToPosition(isxodPos, true);

                // Flight packets ОБРАТНО
                if (flightPackets.isValue()) {
                    sendFlightPackets(attackPos, isxodPos);
                }
            }

        } else {
            // ===== SPOOF АТАКА (без реального ТП) =====
            // Сервер думает что мы у цели, но локально мы стоим на месте

            // Spoof пакет к цели (сервер думает мы там)
            sendSpoofPositionPacket(attackPos);

            // Удар (сервер засчитывает)
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);

            // Spoof пакет обратно (сервер думает мы вернулись)
            if (autoReturn.isValue()) {
                sendSpoofPositionPacket(isxodPos);
            }

            // ЛОКАЛЬНАЯ ПОЗИЦИЯ НЕ МЕНЯЕТСЯ!
        }
    }

    // ==================== TELEPORT UTILS ====================

    /**
     * Реальная телепортация с обновлением локальной позиции
     */
    @Native(type = Native.Type.VMProtectBeginMutation)
    private void teleportToPosition(Vec3d targetPos, boolean updateLocal) {
        if (mc.player == null || mc.getNetworkHandler() == null) {
            return;
        }

        if (!isValidPosition(targetPos)) {
            return;
        }

        Vec3d currentPos = mc.player.getEntityPos();
        double distance = currentPos.distanceTo(targetPos);

        // Если дистанция > 20 блоков - дробим на пакеты
        if (distance > 20.0) {
            int packets = (int) Math.ceil(distance / 10.0);
            packets = MathHelper.clamp(packets, 2, 15);

            for (int i = 1; i <= packets; i++) {
                double progress = (double) i / packets;
                Vec3d intermediatePos = currentPos.lerp(targetPos, progress);
                intermediatePos = addRandomOffset(intermediatePos, 0.01);
                sendPositionPacket(intermediatePos);
            }
        } else {
            sendPositionPacket(targetPos);
        }

        // Обновляем локальную позицию
        if (updateLocal) {
            mc.player.setPos(targetPos.x, targetPos.y, targetPos.z);
        }
    }

    /**
     * Отправить пакет позиции (реальный)
     */
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

    /**
     * Отправить spoof-пакет позиции
     * Сервер получает позицию, но клиентская позиция НЕ меняется
     */
    @Native(type = Native.Type.VMProtectBeginMutation)
    private void sendSpoofPositionPacket(Vec3d pos) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        // Микро-оффсет чтобы сервер не отклонил как дубликат
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Vec3d spoofPos = new Vec3d(
                pos.x + random.nextDouble(-0.0001, 0.0001),
                pos.y + random.nextDouble(-0.0001, 0.0001),
                pos.z + random.nextDouble(-0.0001, 0.0001)
        );

        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(
                spoofPos.x, spoofPos.y, spoofPos.z,
                mc.player.getYaw(),
                mc.player.getPitch(),
                mc.player.isOnGround(),
                mc.player.horizontalCollision
        ));

        // НЕ вызываем mc.player.setPos()!
        // Игрок остаётся визуально на месте
    }

    /**
     * Отправить пакеты "полёта" для Grim
     */
    @Native(type = Native.Type.VMProtectBeginMutation)
    private void sendFlightPackets(Vec3d from, Vec3d to) {
        if (!flightPackets.isValue() || mc.getNetworkHandler() == null || mc.player == null) return;

        double distance = from.distanceTo(to);

        // Grim любит пакеты каждые ~8 блоков
        int packets = (int) Math.ceil(distance / 8.0);
        packets = MathHelper.clamp(packets, 1, 5);

        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 1; i <= packets; i++) {
            double progress = (double) i / (packets + 1);
            Vec3d intermediatePos = from.lerp(to, progress);

            // Случайный оффсет
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

    /**
     * Мгновенная snap ротация (для Default режима)
     */
    private void snapRotation(Angle angle) {
        Angle adjusted = angle.adjustSensitivity();
        AngleConnection.INSTANCE.setRotation(adjusted);
    }

    /**
     * Плавная ротация к цели (для Spoof режима)
     */
    private void rotateToTargetSmooth() {
        if (target == null) return;

        Vec3d targetVec = target.getEntityPos().add(0, target.getHeight() / 2, 0);
        Angle targetAngle = MathAngle.calculateAngle(targetVec);

        AngleConfig config = new AngleConfig(new LinearConstructor(), true, false);
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

    // ==================== HELPERS ====================

    /**
     * Рассчитать позицию атаки
     */
    private Vec3d calculateAttackPosition(Vec3d targetPos, Vec3d fromPos) {
        double distance = attackDistance.getValue();

        if (distance <= 0) {
            // Прямо в хитбокс
            return targetPos.add(0, 0.1, 0);
        }

        // На расстоянии от цели
        Vec3d direction = fromPos.subtract(targetPos).normalize();
        return targetPos.add(direction.multiply(distance));
    }

    /**
     * Авто-возврат на исходную позицию
     * Вызывается когда цель умерла/пропала или модуль выключен
     */
    @Native(type = Native.Type.VMProtectBeginMutation)
    private void returnToIsxodAuto() {
        if (!isxodHandler.hasSavedPosition() || mc.player == null) return;

        Vec3d isxodPos = isxodHandler.getIsxodPosition();
        Vec3d currentPos = mc.player.getEntityPos();

        // Проверяем - мы уже на исходной? (допуск 1 блок)
        if (currentPos.distanceTo(isxodPos) < 1.0) return;

        // Реальный ТП на исходную
        teleportToPosition(isxodPos, true);
    }

    /**
     * Валидация координат
     */
    private boolean isValidPosition(Vec3d pos) {
        if (pos == null) return false;
        if (Double.isNaN(pos.x) || Double.isNaN(pos.y) || Double.isNaN(pos.z)) return false;
        if (Double.isInfinite(pos.x) || Double.isInfinite(pos.y) || Double.isInfinite(pos.z)) return false;
        if (Math.abs(pos.x) > 30000000 || Math.abs(pos.z) > 30000000) return false;
        return true;
    }

    /**
     * Добавить случайный оффсет
     */
    private Vec3d addRandomOffset(Vec3d pos, double maxOffset) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return new Vec3d(
                pos.x + random.nextDouble(-maxOffset, maxOffset),
                pos.y + random.nextDouble(-maxOffset, maxOffset),
                pos.z + random.nextDouble(-maxOffset, maxOffset)
        );
    }

    /**
     * Разблокировать цель
     */
    public void unlockTarget() {
        lockedTarget = null;
        lockTargetBtn.setValue(false);
    }

    /**
     * Заблокировать текущую цель
     */
    public void lockCurrentTarget() {
        if (target != null) {
            lockedTarget = target;
            lockTargetBtn.setValue(true);
        }
    }
}
