package so.aporia.utils.math
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.random.Random
import com.chaos.annotation.ChaosNative
@ChaosNative
object Prediction {

    fun position(entity: Entity, ticks: Int = 1): Vec3 {
        val pos = entity.position()
        val motion = entity.deltaMovement
        return Vec3(
            pos.x + motion.x * ticks,
            pos.y + motion.y * ticks,
            pos.z + motion.z * ticks
        )
    }

    fun eyePosition(entity: Entity, ticks: Int = 1): Vec3 {
        val pos = position(entity, ticks)
        return Vec3(pos.x, pos.y + entity.eyeHeight, pos.z)
    }

    fun center(entity: Entity, ticks: Int = 1): Vec3 {
        val pos = position(entity, ticks)
        val box = entity.boundingBox
        return Vec3(
            (box.minX + box.maxX) / 2.0,
            (box.minY + box.maxY) / 2.0,
            (box.minZ + box.maxZ) / 2.0
        )
    }

    fun predicted(target: Entity): Vec3 {
        val center = target.boundingBox.center
        val motion = target.deltaMovement
        var vy = motion.y
        if (!target.onGround()) vy -= 0.08 * 0.5
        val ox = (Random.nextDouble() - 0.5) * 0.2
        val oy = (Random.nextDouble() - 0.5) * 0.3
        val oz = (Random.nextDouble() - 0.5) * 0.2
        return Vec3(
            center.x + motion.x * 0.5 + ox,
            center.y + vy + oy,
            center.z + motion.z * 0.5 + oz
        )
    }

    fun lerp(t: Double, a: Double, b: Double): Double = a + t * (b - a)

    fun lerp(t: Float, a: Float, b: Float): Float = a + t * (b - a)

    fun wrapDegrees(value: Float): Float {
        var v = value % 360f
        if (v >= 180f) v -= 360f
        if (v < -180f) v += 360f
        return v
    }

    fun wrapDegrees(value: Double): Double {
        var v = value % 360.0
        if (v >= 180.0) v -= 360.0
        if (v < -180.0) v += 360.0
        return v
    }
}