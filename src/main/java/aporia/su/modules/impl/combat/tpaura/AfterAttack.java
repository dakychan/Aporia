package aporia.su.modules.impl.combat.tpaura;

import lombok.Getter;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;
import aporia.su.util.interfaces.IMinecraft;

/**
 * AfterAttack - возвращает игрока на исходную позицию после атаки.
 */
@Getter
public class AfterAttack implements IMinecraft {
    
    private boolean returned = false;
    
    /**
     * Вернуться на исходную позицию.
     * 
     * @param isxodPosition Исходная позиция
     */
    public void returnToIsxod(Vec3d isxodPosition) {
        if (isxodPosition == null || mc.player == null) {
            return;
        }
        
        /** Выполняем телепортацию обратно */
        performReturn(isxodPosition);
        returned = true;
    }
    
    /**
     * Выполнить возврат через пакеты.
     */
    private void performReturn(Vec3d position) {
        if (mc.player == null || mc.getNetworkHandler() == null) {
            return;
        }
        
        /** Текущая позиция */
        Vec3d currentPos = mc.player.getEntityPos();
        
        /** Рассчитываем количество пакетов */
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
     * Проверить, был ли выполнен возврат.
     */
    public boolean hasReturned() {
        return returned;
    }
    
    /**
     * Сбросить состояние.
     */
    public void reset() {
        returned = false;
    }
}
