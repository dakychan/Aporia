package aporia.su.modules.impl.combat;

import anidumpproject.api.annotation.Native;
import lombok.Getter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
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
import aporia.su.modules.impl.combat.tpaura.AfterAttack;
import aporia.su.modules.impl.combat.tpaura.AttackTP;
import aporia.su.modules.impl.combat.tpaura.IsxodHandler;
import aporia.su.modules.module.ModuleStructure;
import aporia.su.modules.module.category.ModuleCategory;
import aporia.su.modules.module.setting.implement.BooleanSetting;
import aporia.su.modules.module.setting.implement.MultiSelectSetting;
import aporia.su.modules.module.setting.implement.SliderSettings;
import aporia.su.util.Instance;
import aporia.su.util.user.render.math.TaskPriority;

/**
 * TpAura - телепортируется к цели, атакует и возвращается обратно.
 * 
 * <p>Этапы работы:</p>
 * <ol>
 *   <li>IsxodHandler сохраняет исходную позицию</li>
 *   <li>Телепортация на 30 блоков выше цели</li>
 *   <li>2 удара сверху БЕЗ телепорта</li>
 *   <li>Добивание с приближением к хитбоксу</li>
 *   <li>AfterAttack возвращается на исходную позицию</li>
 * </ol>
 */
@Getter
public class TpAura extends ModuleStructure {
    
    @Native(type = Native.Type.VMProtectBeginUltra)
    public static TpAura getInstance() {
        return Instance.get(TpAura.class);
    }
    
    private final SliderSettings attackRange = new SliderSettings("Дистанция поиска", "Search range")
            .range(10.0f, 100.0f)
            .setValue(30.0f);
    
    private final SliderSettings tpDistance = new SliderSettings("Дистанция ТП", "TP distance from target")
            .range(0.0f, 4.0f)
            .setValue(3.0f);
    
    private final SliderSettings attackDelay = new SliderSettings("Задержка атаки", "Attack delay in ticks")
            .range(0.0f, 20.0f)
            .setValue(2.0f);
    
    private final aporia.su.modules.module.setting.implement.SelectSetting tpMode = 
            new aporia.su.modules.module.setting.implement.SelectSetting("Режим ТП", "TP mode")
            .value("Classic", "Aerial", "Spoof")
            .selected("Spoof");
    
    private final SliderSettings spoofHits = new SliderSettings("Ударов (Spoof)", "Hits before real TP in Spoof mode")
            .range(1.0f, 10.0f)
            .setValue(3.0f);
    
    private final aporia.su.modules.module.setting.implement.SelectSetting rotationMode = 
            new aporia.su.modules.module.setting.implement.SelectSetting("Режим ротации", "Rotation mode")
            .value("None", "Smooth")
            .selected("None");
    
    private final MultiSelectSetting targetType = new MultiSelectSetting("Цели", "Target types")
            .value("Игроки", "Мобы", "Животные")
            .selected("Игроки");
    
    private final BooleanSetting autoReturn = new BooleanSetting("Авто-возврат", "Auto return to isxod")
            .setValue(true);
    
    private final BooleanSetting onlyOnGround = new BooleanSetting("Только на земле", "Only attack when on ground")
            .setValue(false);
    
    /** Хендлеры */
    private final IsxodHandler isxodHandler = new IsxodHandler();
    private final AttackTP attackTP = new AttackTP();
    private final AfterAttack afterAttack = new AfterAttack();
    private final TargetFinder targetFinder = new TargetFinder();
    
    /** Состояние */
    private LivingEntity target = null;
    private TpAuraState state = TpAuraState.IDLE;
    private int ticksInState = 0;
    private boolean attackPending = false;
    private boolean teleportConfirmed = false;
    private int attacksFromAbove = 0;
    private int spoofAttackCount = 0;
    private static final int MAX_ATTACKS_FROM_ABOVE = 2;
    
    /**
     * Состояния TpAura.
     */
    private enum TpAuraState {
        IDLE,                   // Ожидание
        SAVING_ISXOD,           // Сохранение исходной позиции
        SPOOF_ATTACKING,        // Атака со спуфом позиции (Spoof режим)
        TELEPORTING_UP,         // Телепортация вверх (30 блоков над целью)
        ATTACKING_FROM_ABOVE,   // Атака сверху (первые 2 удара)
        DESCENDING_ATTACK,      // Добивание с приближением
        TELEPORTING,            // Телепортация к цели (Classic режим)
        WAITING_TP,             // Ожидание подтверждения ТП (1-2 тика)
        ATTACKING,              // Атака цели
        RETURNING               // Возврат на исходную позицию
    }
    
    public TpAura() {
        super("TpAura", ModuleCategory.COMBAT);
        settings(attackRange, tpDistance, attackDelay, tpMode, spoofHits, rotationMode, targetType, autoReturn, onlyOnGround);
    }
    
    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void activate() {
        resetState();
        attacksFromAbove = 0;
        spoofAttackCount = 0;
    }
    
    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void deactivate() {
        resetState();
        AngleConnection.INSTANCE.startReturning();
    }
    
    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) {
            resetState();
            return;
        }
        
        ticksInState++;
        
        switch (state) {
            case IDLE -> handleIdleState();
            case SAVING_ISXOD -> handleSavingIsxodState();
            case SPOOF_ATTACKING -> handleSpoofAttackingState();
            case TELEPORTING_UP -> handleTeleportingUpState();
            case ATTACKING_FROM_ABOVE -> handleAttackingFromAboveState();
            case DESCENDING_ATTACK -> handleDescendingAttackState();
            case TELEPORTING -> handleTeleportingState();
            case WAITING_TP -> handleWaitingTpState();
            case ATTACKING -> handleAttackingState();
            case RETURNING -> handleReturningState();
        }
    }
    
    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onRotationUpdate(RotationUpdateEvent event) {
        if (mc.player == null || target == null) {
            return;
        }
        
        if (event.getType() == EventType.PRE) {
            /** Поворачиваемся к цели ТОЛЬКО в режиме Smooth */
            if (rotationMode.isSelected("Smooth")) {
                if (state == TpAuraState.TELEPORTING || state == TpAuraState.WAITING_TP || 
                    state == TpAuraState.ATTACKING || state == TpAuraState.ATTACKING_FROM_ABOVE || 
                    state == TpAuraState.DESCENDING_ATTACK || state == TpAuraState.SPOOF_ATTACKING) {
                    rotateToTarget();
                }
            }
        }
        
        if (event.getType() == EventType.POST) {
            /** Выполняем телепортацию после поворота (или сразу если None) */
            if (state == TpAuraState.TELEPORTING && !teleportConfirmed) {
                attackTP.teleportToTarget(target, tpDistance.getValue());
                teleportConfirmed = true;
                setState(TpAuraState.WAITING_TP);
            }
            
            /** Атакуем после подтверждения ТП и задержки */
            if (attackPending && (state == TpAuraState.ATTACKING || state == TpAuraState.ATTACKING_FROM_ABOVE || state == TpAuraState.SPOOF_ATTACKING)) {
                performAttack();
                attackPending = false;
            }
        }
    }
    
    /**
     * Обработка состояния IDLE.
     */
    @Native(type = Native.Type.VMProtectBeginMutation)
    private void handleIdleState() {
        /** Проверяем условия для начала атаки */
        if (onlyOnGround.isValue() && !mc.player.isOnGround()) {
            return;
        }
        
        /** Ищем цель */
        findTarget();
        
        if (target != null && target.isAlive()) {
            /** Переходим к сохранению исходной позиции */
            setState(TpAuraState.SAVING_ISXOD);
        }
    }
    
    /**
     * Обработка состояния SAVING_ISXOD.
     */
    private void handleSavingIsxodState() {
        /** Сохраняем исходную позицию */
        isxodHandler.saveIsxod();
        
        /** Выбираем режим атаки */
        if (ticksInState >= 1) {
            if (tpMode.isSelected("Spoof")) {
                setState(TpAuraState.SPOOF_ATTACKING);
                spoofAttackCount = 0;
            } else if (tpMode.isSelected("Aerial")) {
                setState(TpAuraState.TELEPORTING_UP);
            } else {
                setState(TpAuraState.TELEPORTING);
            }
        }
    }
    
    /**
     * Обработка состояния SPOOF_ATTACKING.
     * Атака со спуфом позиции - НЕ телепортируемся физически!
     * Просто отправляем пакет что мы рядом, атакуем, и возвращаем позицию.
     */
    private void handleSpoofAttackingState() {
        if (target == null || !target.isAlive()) {
            if (autoReturn.isValue()) {
                setState(TpAuraState.RETURNING);
            } else {
                resetState();
            }
            return;
        }
        
        /** Атакуем с задержкой */
        if (ticksInState >= attackDelay.getValue() && !attackPending) {
            attackPending = true;
        }
        
        if (attackPending) {
            /** Выполняем атаку со спуфом позиции */
            performSpoofAttack();
            spoofAttackCount++;
            attackPending = false;
            
            /** После N ударов делаем реальный ТП и возвращаемся */
            if (spoofAttackCount >= (int)spoofHits.getValue()) {
                /** Реальный ТП к цели для финального удара */
                attackTP.teleportToTarget(target, tpDistance.getValue());
                
                /** Ждем 1 тик и возвращаемся */
                if (autoReturn.isValue()) {
                    setState(TpAuraState.RETURNING);
                } else {
                    resetState();
                }
            } else {
                /** Сбрасываем счетчик тиков для следующего удара */
                ticksInState = 0;
            }
        }
    }
    
    /**
     * Обработка состояния TELEPORTING_UP.
     * Телепортируемся на 30 блоков выше цели.
     */
    private void handleTeleportingUpState() {
        if (target == null || !target.isAlive()) {
            resetState();
            return;
        }
        
        /** Рассчитываем позицию над целью */
        Vec3d targetPos = target.getEntityPos();
        Vec3d abovePos = new Vec3d(targetPos.x, targetPos.y + 30, targetPos.z);
        
        /** Телепортируемся с микшированием если дистанция >10 */
        performMixedTeleport(abovePos);
        
        /** Переходим к атаке сверху */
        setState(TpAuraState.ATTACKING_FROM_ABOVE);
        attacksFromAbove = 0;
    }
    
    /**
     * Обработка состояния ATTACKING_FROM_ABOVE.
     * Первые 2 удара сверху БЕЗ телепорта.
     */
    private void handleAttackingFromAboveState() {
        if (target == null || !target.isAlive()) {
            resetState();
            return;
        }
        
        /** Проверяем дистанцию по Y */
        double yDist = Math.abs(mc.player.getY() - target.getY());
        if (yDist < 25) {
            /** Если упали ниже 25 блоков - переходим к добиванию */
            setState(TpAuraState.DESCENDING_ATTACK);
            return;
        }
        
        /** Атакуем с задержкой */
        if (ticksInState >= attackDelay.getValue() && !attackPending) {
            attackPending = true;
        }
        
        if (attackPending) {
            performAttack();
            attacksFromAbove++;
            attackPending = false;
            
            /** После 2 ударов переходим к добиванию */
            if (attacksFromAbove >= MAX_ATTACKS_FROM_ABOVE) {
                setState(TpAuraState.DESCENDING_ATTACK);
            } else {
                /** Сбрасываем счетчик тиков для следующего удара */
                ticksInState = 0;
            }
        }
    }
    
    /**
     * Обработка состояния DESCENDING_ATTACK.
     * Добивание с приближением к хитбоксу цели.
     */
    private void handleDescendingAttackState() {
        if (target == null || !target.isAlive()) {
            if (autoReturn.isValue()) {
                setState(TpAuraState.RETURNING);
            } else {
                resetState();
            }
            return;
        }
        
        Vec3d targetPos = target.getEntityPos();
        Vec3d currentPos = mc.player.getEntityPos();
        
        /** Вектор к цели */
        Vec3d direction = targetPos.subtract(currentPos).normalize();
        
        /** Горизонтальная дистанция */
        double horizontalDist = Math.sqrt(
            Math.pow(targetPos.x - currentPos.x, 2) + 
            Math.pow(targetPos.z - currentPos.z, 2)
        );
        
        /** Если уже близко - добиваем */
        if (horizontalDist < 3.0) {
            if (ticksInState >= attackDelay.getValue() && !attackPending) {
                attackPending = true;
            }
            
            if (attackPending) {
                performMaceFinish();
                attackPending = false;
                
                /** После добивания возвращаемся */
                if (autoReturn.isValue()) {
                    setState(TpAuraState.RETURNING);
                } else {
                    resetState();
                }
            }
            return;
        }
        
        /** Плавно приближаемся к цели (0.5 блока за тик) */
        double speed = 0.5;
        Vec3d newPos = currentPos.add(direction.multiply(speed));
        
        /** Отправляем пакеты движения */
        sendMovementPackets(newPos);
    }
    
    /**
     * Обработка состояния TELEPORTING.
     */
    private void handleTeleportingState() {
        if (target == null || !target.isAlive()) {
            resetState();
            return;
        }
        
        /** Телепортация происходит в onRotationUpdate POST */
        /** Здесь просто ждем */
    }
    
    /**
     * Обработка состояния WAITING_TP.
     */
    private void handleWaitingTpState() {
        if (target == null || !target.isAlive()) {
            resetState();
            return;
        }
        
        /** Ждем 2 тика для подтверждения телепортации сервером */
        if (ticksInState >= 2) {
            setState(TpAuraState.ATTACKING);
        }
    }
    
    /**
     * Обработка состояния ATTACKING.
     */
    private void handleAttackingState() {
        if (target == null || !target.isAlive()) {
            /** Цель умерла, возвращаемся */
            if (autoReturn.isValue()) {
                setState(TpAuraState.RETURNING);
            } else {
                resetState();
            }
            return;
        }
        
        /** Ждём хотя бы 2 тика ПОСЛЕ подтверждения ТП */
        if (ticksInState >= attackDelay.getValue() && !attackPending) {
            /** Проверяем дистанцию до цели перед атакой */
            double distance = mc.player.distanceTo(target);
            if (distance <= 4.5) { // Чуть больше чем tpDistance
                attackPending = true;
            }
        }
        
        /** Выполняем атаку в следующем тике после установки флага */
        if (attackPending) {
            performAttack();
            attackPending = false;
        }
        
        /** После атаки ждем еще 3 тика и возвращаемся */
        if (ticksInState >= attackDelay.getValue() + 3) {
            if (autoReturn.isValue()) {
                setState(TpAuraState.RETURNING);
            } else {
                resetState();
            }
        }
    }
    
    /**
     * Обработка состояния RETURNING.
     */
    private void handleReturningState() {
        if (!isxodHandler.hasSavedPosition()) {
            resetState();
            return;
        }
        
        /** Возвращаемся на исходную позицию в первом тике */
        if (ticksInState == 1) {
            afterAttack.returnToIsxod(isxodHandler.getIsxodPosition());
        }
        
        /** Ждем 2 тика для подтверждения возврата */
        if (ticksInState >= 3) {
            resetState();
        }
    }
    
    /**
     * Найти цель.
     */
    private void findTarget() {
        TargetFinder.EntityFilter filter = new TargetFinder.EntityFilter(targetType.getSelected());
        targetFinder.searchTargets(mc.world.getEntities(), attackRange.getValue(), 360, true);
        targetFinder.validateTarget(filter::isValid);
        target = targetFinder.getCurrentTarget();
    }
    
    /**
     * Повернуться к цели.
     * Используется ТОЛЬКО в режиме Smooth.
     */
    private void rotateToTarget() {
        if (target == null) {
            return;
        }
        
        /** Рассчитываем угол к цели */
        Angle targetAngle = MathAngle.calculateAngle(target.getEntityPos().add(0, target.getHeight() / 2, 0));
        
        /** Плавная ротация через AngleConnection (как в Aura Matrix режиме) */
        AngleConfig config = new AngleConfig(new LinearConstructor(), true, false);
        Angle.VecRotation rotation = new Angle.VecRotation(targetAngle, targetAngle.toVector());
        AngleConnection.INSTANCE.rotateTo(rotation, target, 1, config, TaskPriority.HIGH_IMPORTANCE_1, this);
    }
    
    /**
     * Выполнить атаку с синхронизацией позиции и критами.
     * Встроенная логика Grim Crit для обхода античита.
     */
    private void performAttack() {
        if (target == null || mc.interactionManager == null || mc.getNetworkHandler() == null) {
            return;
        }
        
        /** 1. Принудительный поворот к цели (для режима None) */
        if (rotationMode.isSelected("None")) {
            double deltaX = target.getX() - mc.player.getX();
            double deltaZ = target.getZ() - mc.player.getZ();
            double deltaY = (target.getY() + target.getHeight() / 2) - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
            double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            
            float yaw = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0f;
            float pitch = (float) -Math.toDegrees(Math.atan2(deltaY, distance));
            
            mc.player.setYaw(yaw);
            mc.player.setPitch(pitch);
        }
        
        /** 2. Отправляем пакет синхронизации позиции (каждый второй тик) */
        if (mc.player.age % 2 == 0) {
            mc.getNetworkHandler().sendPacket(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full(
                mc.player.getX(),
                mc.player.getY(),
                mc.player.getZ(),
                mc.player.getYaw(),
                mc.player.getPitch(),
                false, // OnGround FALSE - мы в воздухе после ТП
                mc.player.horizontalCollision
            ));
        }
        
        /** 3. Grim Crit - только если недавно телепортировались */
        if ((state == TpAuraState.ATTACKING || state == TpAuraState.ATTACKING_FROM_ABOVE) && ticksInState < 5) {
            float fakeFallDistance = 1.0E-4f;
            mc.player.fallDistance = fakeFallDistance;
            
            /** Отправляем пакет "Я падаю" */
            mc.getNetworkHandler().sendPacket(
                new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly(false, false)
            );
        }
        
        /** 4. Атакуем цель */
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(mc.player.getActiveHand());
    }
    
    /**
     * Специальная атака булавой для добивания.
     * Максимальный урон с Grim Crit.
     */
    private void performMaceFinish() {
        if (target == null || mc.interactionManager == null || mc.getNetworkHandler() == null) {
            return;
        }
        
        /** 1. Grim Crit для максимального урона */
        mc.player.fallDistance = 1.0E-4f;
        mc.getNetworkHandler().sendPacket(
            new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly(false, false)
        );
        
        /** 2. Атака булавой */
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(mc.player.getActiveHand());
        
        /** 3. Отбрасываем цель (эффект) */
        Vec3d knockback = target.getEntityPos().subtract(mc.player.getEntityPos()).normalize().multiply(2);
        target.addVelocity(knockback.x, 0.5, knockback.z);
    }
    
    /**
     * Атака со спуфом позиции.
     * НЕ телепортируемся физически! Только спуфим пакеты.
     * 
     * Алгоритм:
     * 1. Сохраняем реальную позицию
     * 2. Отправляем пакет что мы рядом с целью
     * 3. Атакуем
     * 4. МГНОВЕННО возвращаем позицию обратно
     */
    private void performSpoofAttack() {
        if (target == null || mc.interactionManager == null || mc.getNetworkHandler() == null) {
            return;
        }
        
        /** 1. Сохраняем реальную позицию */
        Vec3d realPos = mc.player.getEntityPos();
        float realYaw = mc.player.getYaw();
        float realPitch = mc.player.getPitch();
        
        /** 2. Рассчитываем позицию рядом с целью */
        Vec3d targetPos = target.getEntityPos();
        Vec3d direction = targetPos.subtract(realPos).normalize();
        Vec3d spoofPos = targetPos.subtract(direction.multiply(tpDistance.getValue()));
        
        /** 3. Рассчитываем ротацию к цели */
        double deltaX = target.getX() - spoofPos.x;
        double deltaZ = target.getZ() - spoofPos.z;
        double deltaY = (target.getY() + target.getHeight() / 2) - (spoofPos.y + mc.player.getEyeHeight(mc.player.getPose()));
        double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        
        float spoofYaw = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0f;
        float spoofPitch = (float) -Math.toDegrees(Math.atan2(deltaY, distance));
        
        /** 4. СПУФ: Отправляем пакет что мы рядом с целью */
        mc.getNetworkHandler().sendPacket(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full(
            spoofPos.x,
            spoofPos.y,
            spoofPos.z,
            spoofYaw,
            spoofPitch,
            false, // OnGround FALSE
            false
        ));
        
        /** 5. Grim Crit */
        mc.player.fallDistance = 1.0E-4f;
        mc.getNetworkHandler().sendPacket(
            new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly(false, false)
        );
        
        /** 6. АТАКУЕМ (сервер думает мы рядом) */
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(mc.player.getActiveHand());
        
        /** 7. МГНОВЕННО возвращаем позицию обратно */
        mc.getNetworkHandler().sendPacket(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full(
            realPos.x,
            realPos.y,
            realPos.z,
            realYaw,
            realPitch,
            mc.player.isOnGround(),
            false
        ));
        
        /** НЕ обновляем локальную позицию - остаемся на месте! */
    }
    
    /**
     * Телепортация с микшированием для дистанций >10 блоков.
     * Отправляет пакеты по 8-10 блоков с микрозадержками.
     */
    private void performMixedTeleport(Vec3d targetPosition) {
        if (mc.player == null || mc.getNetworkHandler() == null) {
            return;
        }
        
        Vec3d currentPos = mc.player.getEntityPos();
        double distance = currentPos.distanceTo(targetPosition);
        
        /** Если дистанция >10 - микшируем */
        if (distance > 10) {
            /** Рассчитываем количество пакетов (каждый пакет = 8-10 блоков) */
            int packets = (int) Math.ceil(distance / 9.0); // Среднее 9 блоков
            packets = Math.max(1, Math.min(packets, 15)); // Ограничиваем 1-15 пакетами
            
            /** Отправляем пакеты постепенно */
            for (int i = 1; i <= packets; i++) {
                double progress = (double) i / packets;
                Vec3d intermediatePos = currentPos.lerp(targetPosition, progress);
                
                mc.getNetworkHandler().sendPacket(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full(
                    intermediatePos.x,
                    intermediatePos.y,
                    intermediatePos.z,
                    mc.player.getYaw(),
                    mc.player.getPitch(),
                    false, // OnGround FALSE
                    false
                ));
                
                /** Микрозадержка между пакетами (симуляция) */
                try {
                    Thread.sleep(1); // 1ms задержка
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } else {
            /** Обычная телепортация для коротких дистанций */
            mc.getNetworkHandler().sendPacket(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full(
                targetPosition.x,
                targetPosition.y,
                targetPosition.z,
                mc.player.getYaw(),
                mc.player.getPitch(),
                false,
                false
            ));
        }
        
        /** Обновляем позицию игрока локально */
        mc.player.setPos(targetPosition.x, targetPosition.y, targetPosition.z);
    }
    
    /**
     * Отправить пакеты движения к новой позиции.
     */
    private void sendMovementPackets(Vec3d newPos) {
        if (mc.player == null || mc.getNetworkHandler() == null) {
            return;
        }
        
        mc.getNetworkHandler().sendPacket(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full(
            newPos.x,
            newPos.y,
            newPos.z,
            mc.player.getYaw(),
            mc.player.getPitch(),
            false, // OnGround FALSE - мы в воздухе
            false
        ));
        
        /** Обновляем позицию локально */
        mc.player.setPos(newPos.x, newPos.y, newPos.z);
    }
    
    /**
     * Установить новое состояние.
     */
    private void setState(TpAuraState newState) {
        state = newState;
        ticksInState = 0;
    }
    
    /**
     * Сбросить состояние.
     */
    @Native(type = Native.Type.VMProtectBeginMutation)
    private void resetState() {
        state = TpAuraState.IDLE;
        ticksInState = 0;
        target = null;
        attackPending = false;
        teleportConfirmed = false;
        attacksFromAbove = 0;
        spoofAttackCount = 0;
        isxodHandler.reset();
        attackTP.reset();
        afterAttack.reset();
        targetFinder.releaseTarget();
    }
}
