package aporia.su.modules.impl.combat.tpaura;

import lombok.Getter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;
import aporia.su.util.interfaces.IMinecraft;

/**
 * AttackTP - рассчитывает и выполняет телепортацию к цели для атаки.
 */
@Getter
public class AttackTP implements IMinecraft {
    
    private Vec3d targetPosition = null;
    private boolean teleported = false;
    
    /**
     * Рассчитать позицию для телепортации к цели.
     * 
     * @param target Цель атаки
     * @param distance Дистанция от цели (обычно 3.0)
     * @return Позиция для телепортации
     */
    public Vec3d calculateTpPosition(LivingEntity target, float distance) {
        if (target == null || mc.player == null) {
            return null;
        }
        
        Vec3d targetPos = target.getEntityPos();
        Vec3d playerPos = mc.player.getEntityPos();
        
        /** Вектор направления от игрока к цели */
        Vec3d direction = targetPos.subtract(playerPos).normalize();
        
        /** Позиция на расстоянии distance от цели */
        Vec3d tpPos = targetPos.subtract(direction.multiply(distance));
        
        /** Корректируем Y координату (на уровне цели) */
        tpPos = new Vec3d(tpPos.x, targetPos.y, tpPos.z);
        
        return tpPos;
    }
    
    /**
     * Выполнить телепортацию к цели.
     * 
     * @param target Цель атаки
     * @param distance Дистанция от цели
     */
    public void teleportToTarget(LivingEntity target, float distance) {
        if (target == null || mc.player == null) {
            return;
        }
        
        targetPosition = calculateTpPosition(target, distance);
        
        if (targetPosition == null) {
            return;
        }
        
        /** Отправляем пакеты телепортации */
        performTeleport(targetPosition);
        teleported = true;
    }
    
    /**
     * Выполнить телепортацию через пакеты.
     */
    private void performTeleport(Vec3d position) {
        if (mc.player == null || mc.getNetworkHandler() == null) {
            return;
        }
        
        /** Текущая позиция */
        Vec3d currentPos = mc.player.getEntityPos();
        
        /** Рассчитываем количество пакетов (каждый пакет = ~10 блоков) */
        double distance = currentPos.distanceTo(position);
        int packets = (int) Math.ceil(distance / 10.0);
        packets = Math.max(1, Math.min(packets, 10)); // Ограничиваем 1-10 пакетами
        
        /** Отправляем пакеты постепенно */
        for (int i = 1; i <= packets; i++) {
            double progress = (double) i / packets;
            Vec3d intermediatePos = currentPos.lerp(position, progress);
            
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(
                intermediatePos.x,
                intermediatePos.y,
                intermediatePos.z,
                mc.player.getYaw(),
                mc.player.getPitch(),
                mc.player.isOnGround(),
                false
            ));
        }
        
        /** Обновляем позицию игрока локально */
        mc.player.setPos(position.x, position.y, position.z);
    }
    
    /**
     * Проверить, была ли выполнена телепортация.
     */
    public boolean hasTeleported() {
        return teleported;
    }
    
    /**
     * Сбросить состояние.
     */
    public void reset() {
        targetPosition = null;
        teleported = false;
    }
}
