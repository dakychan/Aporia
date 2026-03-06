package aporia.su.modules.impl.combat.tpaura;

import lombok.Getter;
import net.minecraft.util.math.Vec3d;
import aporia.su.util.interfaces.IMinecraft;

/**
 * IsxodHandler - сохраняет исходную позицию игрока перед атакой.
 */
@Getter
public class IsxodHandler implements IMinecraft {
    
    private Vec3d isxodPosition = null;
    private boolean saved = false;
    
    /**
     * Сохранить текущую позицию как исходную.
     */
    public void saveIsxod() {
        if (mc.player == null) {
            return;
        }
        
        isxodPosition = mc.player.getEntityPos();
        saved = true;
    }
    
    /**
     * Проверить, сохранена ли исходная позиция.
     */
    public boolean hasSavedPosition() {
        return saved && isxodPosition != null;
    }
    
    /**
     * Получить сохраненную исходную позицию.
     */
    public Vec3d getIsxodPosition() {
        return isxodPosition;
    }
    
    /**
     * Сбросить сохраненную позицию.
     */
    public void reset() {
        isxodPosition = null;
        saved = false;
    }
}
