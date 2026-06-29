package so.aporia.utils.user.player.movement

import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import so.aporia.utils.imports.*

object MoveUtil {
    private fun player(): Player? = mc.player

    fun forward(speed: Double, ticks: Int = 1) {
        val p = player() ?: return
        val yawRad = Math.toRadians(p.yRot.toDouble())
        val motion = Vec3(
            -Math.sin(yawRad) * speed * ticks,
            0.0,
            Math.cos(yawRad) * speed * ticks
        )
        p.setDeltaMovement(motion)
    }

    fun backward(speed: Double, ticks: Int = 1) {
        forward(-speed, ticks)
    }

    fun strafeLeft(speed: Double, ticks: Int = 1) {
        val p = player() ?: return
        val yawRad = Math.toRadians(p.yRot.toDouble())
        val motion = Vec3(
            Math.cos(yawRad) * speed * ticks,
            0.0,
            Math.sin(yawRad) * speed * ticks
        )
        p.setDeltaMovement(motion)
    }

    fun strafeRight(speed: Double, ticks: Int = 1) {
        strafeLeft(-speed, ticks)
    }

    fun jump() {
        val p = player() ?: return
        p.setDeltaMovement(p.deltaMovement.add(0.0, 0.42, 0.0))
    }

    fun moveTowards(targetPos: Vec3, speed: Double) {
        val p = player() ?: return
        val diff = targetPos.subtract(p.position())
        val length = diff.length()
        if (length <= 0.01) return
        val vel = diff.scale(speed / length)
        p.setDeltaMovement(Vec3(vel.x, p.deltaMovement.y, vel.z))
    }

    fun flyTowards(targetPos: Vec3, speed: Double, targetY: Double) {
        val p = player() ?: return
        // Считаем разницу между нами и целью
        val diff = targetPos.subtract(p.position())

        val flatLength = Math.hypot(diff.x, diff.z)

        if (flatLength <= 0.1 && Math.abs(diff.y) <= 0.5) {
            // Если совсем близко — стоп
            p.setDeltaMovement(Vec3(0.0, 0.0, 0.0))
            return
        }

        // Нормализуем вектор и умножаем на скорость
        val velX = if (flatLength > 0.01) diff.x / flatLength * speed else 0.0
        val velZ = if (flatLength > 0.01) diff.z / flatLength * speed else 0.0

        // Высоту подгоняем плавно
        val velY = diff.y.coerceIn(-speed, speed)

        p.setDeltaMovement(Vec3(velX, velY, velZ))
    }

    fun stop() {
        val p = player() ?: return
        p.setDeltaMovement(Vec3(0.0, 0.0, 0.0))
    }

    fun stopHorizontal() {
        val p = player() ?: return
        p.setDeltaMovement(Vec3(0.0, p.deltaMovement.y, 0.0))
    }
}
