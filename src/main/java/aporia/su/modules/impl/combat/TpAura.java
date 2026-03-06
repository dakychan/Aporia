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
import aporia.su.modules.impl.combat.tpaura.IsxodHandler;
import aporia.su.modules.module.ModuleStructure;
import aporia.su.modules.module.category.ModuleCategory;
import aporia.su.modules.module.setting.implement.BooleanSetting;
import aporia.su.modules.module.setting.implement.MultiSelectSetting;
import aporia.su.modules.module.setting.implement.SliderSettings;
import aporia.su.util.Instance;
import aporia.su.util.user.render.math.TaskPriority;

/**
 * TpAura - атака со спуфом позиции.
 * Не телепортируемся физически, только спуфим пакеты.
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
    
    private final SliderSettings attackDelay = new SliderSettings("Задержка атаки", "Attack delay in ticks")
            .range(1.0f, 20.0f)
            .setValue(4.0f);
    
    private final SliderSettings maxHits = new SliderSettings("Макс ударов", "Max hits before cooldown")
            .range(1.0f, 10.0f)
            .setValue(3.0f);
    
    private final aporia.su.modules.module.setting.implement.SelectSetting rotationMode = 
            new aporia.su.modules.module.setting.implement.SelectSetting("Режим ротации", "Rotation mode")
            .value("None", "Smooth")
            .selected("None");
    
    private final MultiSelectSetting targetType = new MultiSelectSetting("Цели", "Target types")
            .value("Игроки", "Мобы", "Животные")
            .selected("Игроки");
    
    private final BooleanSetting onlyOnGround = new BooleanSetting("Только на земле", "Only attack when on ground")
            .setValue(false);
    
    private final BooleanSetting grimCrit = new BooleanSetting("Grim Crit", "Enable Grim Crit bypass")
            .setValue(true);
    
    /** Хендлеры */
    private final IsxodHandler isxodHandler = new IsxodHandler();
    private final TargetFinder targetFinder = new TargetFinder();
    
    /** Состояние */
    private LivingEntity target = null;
    private int ticksSinceLastAttack = 0;
    private int hitCount = 0;
    
    public TpAura() {
        super("TpAura", ModuleCategory.COMBAT);
        settings(attackRange, tpDistance, attackDelay, maxHits, rotationMode, targetType, onlyOnGround, grimCrit);
    }
    
    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void activate() {
        reset();
    }
    
    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void deactivate() {
        reset();
        AngleConnection.INSTANCE.startReturning();
    }
    
    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        
        ticksSinceLastAttack++;
        
        /** Проверяем условия */
        if (onlyOnGround.isValue() && !mc.player.isOnGround()) {
            return;
        }
        
        /** Ищем цель */
        findTarget();
        
        if (target == null || !target.isAlive()) {
            hitCount = 0;
            return;
        }
        
        /** Проверяем задержку */
        if (ticksSinceLastAttack < attackDelay.getValue()) {
            return;
        }
        
        /** Проверяем лимит ударов */
        if (hitCount >= (int)maxHits.getValue()) {
            return;
        }
        
        /** Атакуем */
        performSpoofAttack();
        ticksSinceLastAttack = 0;
        hitCount++;
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
     * Атака со спуфом позиции.
     * Алгоритм:
     * 1. Сохраняем реальную позицию
     * 2. Отправляем пакет что мы рядом с целью
     * 3. Атакуем
     * 4. Возвращаем позицию обратно
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
            false,
            false
        ));
        
        /** 5. Grim Crit (опционально) */
        if (grimCrit.isValue()) {
            mc.player.fallDistance = 1.0E-4f;
            mc.getNetworkHandler().sendPacket(
                new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly(false, false)
            );
        }
        
        /** 6. АТАКУЕМ */
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(mc.player.getActiveHand());
        
        /** 7. Возвращаем позицию обратно */
        mc.getNetworkHandler().sendPacket(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full(
            realPos.x,
            realPos.y,
            realPos.z,
            realYaw,
            realPitch,
            mc.player.isOnGround(),
            false
        ));
    }
    
    /**
     * Сброс состояния.
     */
    @Native(type = Native.Type.VMProtectBeginMutation)
    private void reset() {
        target = null;
        ticksSinceLastAttack = 0;
        hitCount = 0;
        targetFinder.releaseTarget();
    }
}
