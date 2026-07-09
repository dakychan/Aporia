package so.aporia.utils.math
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3
import so.aporia.utils.imports.*
import com.chaos.annotation.ChaosNative
@ChaosNative
object Angle {

    fun calculate(entity: Entity): FloatArray {
        val player = mc.player ?: return floatArrayOf(0f, 0f)
        val eyes = player.getEyePosition(1.0F)
        val targetPos = entity.boundingBox.center
        val diff = targetPos.subtract(eyes)
        return calculateFromDiff(diff)
    }

    fun calculateFromDiff(diff: Vec3): FloatArray {
        val yaw = (Math.toDegrees(Math.atan2(diff.z, diff.x)).toFloat() - 90.0f)
        val horizontal = Math.hypot(diff.x, diff.z)
        val pitch = (-Math.toDegrees(Math.atan2(diff.y, horizontal))).toFloat()
        return floatArrayOf(yaw, Mth.clamp(pitch, -90f, 90f))
    }

    fun wrap(angle: Float): Float = Mth.wrapDegrees(angle)

    fun diff(a: Float, b: Float): Float = Math.abs(Mth.wrapDegrees(a - b))

    fun calculateDelta(from: Float, to: Float): Float = Mth.wrapDegrees(to - from)
}