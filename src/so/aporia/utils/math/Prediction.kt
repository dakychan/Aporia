package so.aporia.utils.math

import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3

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
}
