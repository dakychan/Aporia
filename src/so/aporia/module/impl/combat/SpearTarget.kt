package so.aporia.module.impl.combat

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.settings.SliderSetting
import so.aporia.module.settings.SelectSetting
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.TickEvent
import so.aporia.utils.imports.*
import so.aporia.utils.math.Angle
import so.aporia.utils.math.Prediction
import so.aporia.utils.math.Speed
import so.aporia.utils.user.player.movement.MoveUtil
import so.aporia.utils.user.player.rotation.RotationUtil
import com.chaos.annotation.ChaosNative
@ChaosNative
class SpearTarget : Module("SpearTarget", Category.COMBAT) {

    val mode = SelectSetting("Mode", "Spear attack mode")
        .value("Elytra", "Fly")
        .selected("Elytra")
    val bypass = SelectSetting("Bypass", "Anticheat bypass mode")
        .value("Grim", "Matrix", "Vulcan", "NCP", "Intave", "Smooth")
        .selected("Grim")
    val range = SliderSetting("Range", "Target search range", 8.0, 1.0, 15.0, 0.5)
    val minCps = SliderSetting("Min CPS", "Minimum attacks per second", 10.0, 1.0, 20.0, 1.0)
    val maxCps = SliderSetting("Max CPS", "Maximum attacks per second", 18.0, 1.0, 20.0, 1.0)
    val targets = SelectSetting("Targets", "Target types")
        .value("Players", "Mobs", "Animals", "Friends")
        .selected("Players")
    val rotationSpeed = SliderSetting("Rotation Speed", "Aim rotation speed (deg/s)", 120.0, 10.0, 360.0, 5.0)
    val attackSpeed = SliderSetting("Attack Speed", "Movement speed during attack", 1.5, 0.5, 4.0, 0.1)
    val recoil = SliderSetting("Recoil", "Step back distance after hit", 1.5, 0.0, 5.0, 0.1)

    private var lockedTarget: Entity? = null
    private var lastAttackTime = 0L
    private var nextAttackDelay = 0L
    private var isAttacking = false
    private var currentBypass = ""

    override fun onEnable() {
        bus.register(this)
    }

    override fun onDisable() {
        bus.unregister(this)
        RotationUtil.reset()
        MoveUtil.stop()
        lockedTarget = null
        isAttacking = false
    }

    private fun applyBypassMode() {
        val mode = bypass.get()
        if (mode == currentBypass) return
        currentBypass = mode
        RotationUtil.sync()
        when (mode) {
            "Grim" -> { RotationUtil.setMode(RotationUtil.RotationMode.GRIM); rotationSpeed.setValue(120.0) }
            "Matrix" -> { RotationUtil.setMode(RotationUtil.RotationMode.MATRIX); rotationSpeed.setValue(180.0) }
            "Vulcan" -> { RotationUtil.setMode(RotationUtil.RotationMode.VULCAN); rotationSpeed.setValue(160.0) }
            "NCP" -> { RotationUtil.setMode(RotationUtil.RotationMode.NCP); rotationSpeed.setValue(220.0) }
            "Intave" -> { RotationUtil.setMode(RotationUtil.RotationMode.INTAVE); rotationSpeed.setValue(100.0) }
            else -> { RotationUtil.setMode(RotationUtil.RotationMode.SMOOTH); rotationSpeed.setValue(360.0) }
        }
    }

    @EventHandler
    fun onTick(event: TickEvent) {
        if (mc.player == null || mc.level == null) return
        Speed.update()
        applyBypassMode()

        val canAttack = when (mode.get()) {
            "Elytra" -> mc.player!!.isFallFlying()
            "Fly" -> !mc.player!!.onGround()
            else -> false
        }
        if (!canAttack) {
            lockedTarget = null
            MoveUtil.stopHorizontal()
            return
        }

        val target = findTarget()
        if (target != null) {
            lockedTarget = target
            RotationUtil.update(target, rotationSpeed.getFloat())
            executeAttackLogic(target)
        } else {
            lockedTarget = null
            isAttacking = false
            RotationUtil.deactivate()
        }
    }

    private fun executeAttackLogic(target: Entity) {
        val p = mc.player!!
        val now = System.currentTimeMillis()
        val dist = p.distanceToSqr(target)
        val rangeSq = range.get() * range.get()

        if (dist <= rangeSq) {
            val lookVec = p.lookAngle
            val spd = attackSpeed.get()
            p.deltaMovement = Vec3(lookVec.x * spd, lookVec.y * spd, lookVec.z * spd)

            if (now - lastAttackTime >= nextAttackDelay) {
                mc.gameMode?.attack(p, target)
                p.swing(net.minecraft.world.InteractionHand.MAIN_HAND)
                lastAttackTime = now
                nextAttackDelay = getRandomDelay()
            }
        } else {
            MoveUtil.flyTowards(Prediction.center(target), attackSpeed.get() * 0.8, Prediction.center(target).y)
        }
    }

    private fun findTarget(): Entity? {
        val rng = range.getFloat()
        if (lockedTarget != null && isValidTarget(lockedTarget!!) &&
            mc.player!!.distanceToSqr(lockedTarget!!) <= (rng * 1.5f).let { it * it }
        ) return lockedTarget

        var best: Entity? = null
        var bestDist = Double.MAX_VALUE
        for (entity in mc.level!!.entitiesForRendering()) {
            if (!isValidTarget(entity)) continue
            val dist = mc.player!!.distanceToSqr(entity)
            if (dist <= rng.toDouble() * rng && dist < bestDist) {
                bestDist = dist; best = entity
            }
        }
        return best
    }

    private fun isValidTarget(entity: Entity?): Boolean {
        if (entity == mc.player || entity?.isAlive != true) return false
        if (entity is Player) {
            if (!targets.isSelected("Players")) return false
            if (fm.isFriend(entity.name.string) && !targets.isSelected("Friends")) return false
            return true
        }
        if (entity is Mob) return targets.isSelected("Mobs")
        if (entity is Animal) return targets.isSelected("Animals")
        return false
    }

    private fun getRandomDelay(): Long {
        val mn = minCps.getFloat().coerceAtLeast(1f)
        val mx = maxCps.getFloat().coerceAtLeast(mn)
        return (1000.0 / (mn + Math.random().toFloat() * (mx - mn))).toLong()
    }

    override val settings = listOf(mode, bypass, range, minCps, maxCps, targets, rotationSpeed, attackSpeed, recoil)
}
