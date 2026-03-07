package aporia.su.modules.impl.combat;

import anidumpproject.api.annotation.Native;
import lombok.Getter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.util.math.Vec3d;
import aporia.su.util.events.api.EventHandler;
import aporia.su.util.events.api.types.EventType;
import aporia.su.util.events.impl.entity.RotationUpdateEvent;
import aporia.su.util.events.impl.player.PacketEvent;
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
import aporia.su.modules.module.setting.implement.SliderSettings;
import aporia.su.util.Instance;
import aporia.su.util.user.render.math.TaskPriority;

/**
 * TpAura - телепорт к цели, атака, возврат.
 * Дробит пакеты если дистанция >20 блоков.
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
    
    private final SliderSettings tpDistance = new SliderSettings("Дистанция от цели", "Distance from target")
            .range(2.0f, 6.0f)
            .setValue(3.5f);
    
    private final SliderSettings cycleDelay = new SliderSettings("Задержка", "Delay between attack cycles in ticks")
            .range(1.0f, 40.0f)
            .setValue(10.0f);
    
    private final SliderSettings maxHits = new SliderSettings("Макс ударов", "Max hits per cycle")
            .range(1.0f, 10.0f)
            .setValue(3.0f);
    
    private final aporia.su.modules.module.setting.implement.SelectSetting rotationMode = 
            new aporia.su.modules.module.setting.implement.SelectSetting("Режим ротации", "Rotation mode")
            .value("None", "Smooth")
            .selected("None");
    
    private final MultiSelectSetting targetType = new MultiSelectSetting("Цели", "Target types")
            .value("Игроки", "Мобы", "Животные")
            .selected("Игроки");
    
    private final BooleanSetting autoReturn = new BooleanSetting("Авто-возврат", "Auto return to saved position")
            .setValue(true);
    
    private final BooleanSetting onlyOnGround = new BooleanSetting("Только на земле", "Only attack when on ground")
            .setValue(false);
    
    /** Хендлеры */
    private final IsxodHandler isxodHandler = new IsxodHandler();
    private final TargetFinder targetFinder = new TargetFinder();
    
    /** Состояние */
    private LivingEntity target = null;
    private int tickCounter = 0;
    private int currentHit = 0;
    private Vec3d savedPosition = null;
    private boolean attackSuccessful = false;
    
    public TpAura() {
        super("TpAura", ModuleCategory.COMBAT);
        settings(attackRange, tpDistance, cycleDelay, maxHits, rotationMode, targetType, autoReturn, onlyOnGround);
    }
    
    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void activate() {
        reset();
        /** Сохраняем позицию ДО начала ударов */
        if (mc.player != null) {
            savedPosition = mc.player.getEntityPos();
            isxodHandler.saveIsxod();
        }
    }
    
    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void deactivate() {
        returnToSavedPosition();
        reset();
        AngleConnection.INSTANCE.startReturning();
    }
    
    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        
        tickCounter++;
        
        /** Проверяем условия */
        if (onlyOnGround.isValue() && !mc.player.isOnGround()) {
            return;
        }
        
        /** Ищем цель */
        findTarget();
        
        if (target == null || !target.isAlive()) {
            if (currentHit > 0 && autoReturn.isValue()) {
                returnToSavedPosition();
            }
            currentHit = 0;
            tickCounter = 0;
            return;
        }
        
        /** Проверяем задержку цикла */
        if (tickCounter < cycleDelay.getValue()) {
            return;
        }
        
        /** Проверяем лимит ударов */
        if (currentHit >= (int)maxHits.getValue()) {
            if (autoReturn.isValue()) {
                returnToSavedPosition();
            }
            currentHit = 0;
            tickCounter = 0;
            return;
        }
        
        /** Выполняем цикл: ТП -> Атака -> Возврат */
        performAttackCycle();
        
        currentHit++;
        tickCounter = 0;
    }
    
    /**
     * Обработчик пакетов чата.
     * Проверяет сообщения "Вы атаковали" / "Вы были атакованы".
     */
    @EventHandler
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void onPacket(PacketEvent event) {
        if (event.getPacket() instanceof GameMessageS2CPacket packet) {
            String message = packet.content().getString().toLowerCase();
            
            if (message.contains("вы атаковали") || message.contains("вы были атакованы")) {
                attackSuccessful = true;
            }
        }
    }
    
    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onRotationUpdate(RotationUpdateEvent event) {
        if (mc.player == null || target == null) {
            return;
        }
        
        if (event.getType() == EventType.PRE) {
            if (rotationMode.isSelected("Smooth")) {
                rotateToTarget();
            }
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
     */
    private void rotateToTarget() {
        if (target == null) {
            return;
        }
        
        Angle targetAngle = MathAngle.calculateAngle(target.getEntityPos().add(0, target.getHeight() / 2, 0));
        AngleConfig config = new AngleConfig(new LinearConstructor(), true, false);
        Angle.VecRotation rotation = new Angle.VecRotation(targetAngle, targetAngle.toVector());
        AngleConnection.INSTANCE.rotateTo(rotation, target, 1, config, TaskPriority.HIGH_IMPORTANCE_1, this);
    }
    
    /**
     * Полный цикл атаки: ТП -> Атака -> Возврат.
     */
    private void performAttackCycle() {
        if (target == null || mc.interactionManager == null || mc.getNetworkHandler() == null || savedPosition == null) {
            return;
        }
        
        /** 1. Рассчитываем позицию атаки */
        Vec3d targetPos = target.getEntityPos();
        Vec3d direction = targetPos.subtract(savedPosition).normalize();
        Vec3d attackPos = targetPos.subtract(direction.multiply(tpDistance.getValue()));
        
        /** 2. Телепортируемся к цели (дробим если >20 блоков) */
        teleportToPosition(attackPos);
        
        /** 3. Атакуем */
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(mc.player.getActiveHand());
        
        /** 4. Возвращаемся обратно (дробим если >20 блоков) */
        if (autoReturn.isValue()) {
            teleportToPosition(savedPosition);
        }
    }
    
    /**
     * Телепортация к позиции.
     * Дробит пакеты если дистанция >20 блоков.
     */
    private void teleportToPosition(Vec3d targetPos) {
        if (mc.player == null || mc.getNetworkHandler() == null) {
            return;
        }
        
        Vec3d currentPos = mc.player.getEntityPos();
        double distance = currentPos.distanceTo(targetPos);
        
        /** Если дистанция >20 - дробим пакеты */
        if (distance > 20) {
            /** Рассчитываем количество пакетов (каждый ~10 блоков) */
            int packets = (int) Math.ceil(distance / 10.0);
            packets = Math.max(2, Math.min(packets, 15));
            
            /** Отправляем пакеты БЕЗ задержки */
            for (int i = 1; i <= packets; i++) {
                double progress = (double) i / packets;
                Vec3d intermediatePos = currentPos.lerp(targetPos, progress);
                
                mc.getNetworkHandler().sendPacket(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full(
                    intermediatePos.x,
                    intermediatePos.y,
                    intermediatePos.z,
                    mc.player.getYaw(),
                    mc.player.getPitch(),
                    false,
                    false
                ));
            }
        } else {
            /** Обычная телепортация для коротких дистанций */
            mc.getNetworkHandler().sendPacket(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full(
                targetPos.x,
                targetPos.y,
                targetPos.z,
                mc.player.getYaw(),
                mc.player.getPitch(),
                false,
                false
            ));
        }
        
        /** Обновляем позицию локально */
        mc.player.setPos(targetPos.x, targetPos.y, targetPos.z);
    }
    
    /**
     * Вернуться на сохраненную позицию.
     */
    private void returnToSavedPosition() {
        if (!autoReturn.isValue() || savedPosition == null) {
            return;
        }
        
        teleportToPosition(savedPosition);
    }
    
    /**
     * Сброс состояния.
     */
    @Native(type = Native.Type.VMProtectBeginMutation)
    private void reset() {
        target = null;
        tickCounter = 0;
        currentHit = 0;
        attackSuccessful = false;
        targetFinder.releaseTarget();
    }
}
