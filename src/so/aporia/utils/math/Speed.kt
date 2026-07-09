package so.aporia.utils.math
import net.minecraft.world.entity.Entity
import so.aporia.utils.imports.*
import com.chaos.annotation.ChaosNative
@ChaosNative
object Speed {
    private var prevX = 0.0
    private var prevZ = 0.0
    private var bps = 0.0
    private var initialized = false

    fun update() {
        val player = mc.player ?: return
        if (!initialized) {
            prevX = player.x
            prevZ = player.z
            initialized = true
            bps = 0.0
            return
        }
        val dx = player.x - prevX
        val dz = player.z - prevZ
        bps = Math.hypot(dx, dz) * 20.0
        prevX = player.x
        prevZ = player.z
    }

    fun getBPS(): Double = bps

    fun getBPS(entity: Entity): Double {
        val motion = entity.deltaMovement
        return Math.hypot(motion.x, motion.z) * 20.0
    }

    fun reset() {
        initialized = false
        bps = 0.0
    }
}