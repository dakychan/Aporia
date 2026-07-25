package so.aporia.module.impl.combat

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.MaceItem
import net.minecraft.world.phys.Vec3
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.settings.BooleanSetting
import so.aporia.module.settings.SliderSetting
import so.aporia.module.settings.SelectSetting
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.StateMachine
import so.aporia.utils.events.StateMachineEngine
import so.aporia.utils.events.StateMachineRegister
import so.aporia.utils.events.impl.TickEvent
import so.aporia.utils.imports.*
import so.aporia.utils.math.Angle
import so.aporia.utils.math.Prediction
import so.aporia.utils.math.Speed
import so.aporia.utils.user.player.movement.MoveUtil
import so.aporia.utils.user.player.rotation.RotationUtil
import com.chaos.annotation.ChaosNative
@StateMachine
@ChaosNative
class ElytraTarget : Module("ElytraTarget", Category.COMBAT) {

    object TargetBackedOff
    object TargetInRange
    object AttackExecuted

    val mode = SelectSetting("Mode", "Elytra or Fly mode")
        .value("Elytra", "Fly")
        .selected("Elytra")
    val bypass = SelectSetting("Bypass", "Anticheat bypass mode")
        .value("Grim", "Matrix", "Vulcan", "NCP", "Intave", "Smooth")
        .selected("Grim")
    val range = SliderSetting("Range", "Target search range", 100.0, 1.0, 100.0, 1.0)
    val backOffDist = SliderSetting("Back Off", "Distance to back off before attack", 9.0, 3.0, 20.0, 1.0)
    val heightOffset = SliderSetting("Height Offset", "Height above target to fly at", 3.0, -5.0, 10.0, 0.5)
    val flightSpeed = SliderSetting("Flight Speed", "Speed when flying towards target", 2.0, 0.5, 5.0, 0.25)
    val minCps = SliderSetting("Min CPS", "Minimum LMB spam CPS", 15.0, 5.0, 20.0, 1.0)
    val maxCps = SliderSetting("Max CPS", "Maximum LMB spam CPS", 20.0, 5.0, 20.0, 1.0)
    val targets = SelectSetting("Targets", "Target types")
        .value("Players", "Mobs", "Animals", "Friends")
        .selected("Players")
    val rotationSpeed = SliderSetting("Rotation Speed", "Aim rotation speed (deg/s)", 360.0, 10.0, 360.0, 5.0)
    val requireMace = BooleanSetting("Require Mace", "Only work when holding a mace", true)

    private enum class Phase { IDLE, BACKING_OFF, ACCELERATING, ATTACKING }
    private val sm = StateMachineEngine(this, Phase::class, Phase.IDLE)
    private var lockedTarget: Entity? = null
    private var lastLmbTime = 0L
    private var nextLmbDelay = 0L
    private var tickCounter = 0
    private var currentBypass = ""

    @StateMachineRegister(from = "IDLE", to = "BACKING_OFF", on = TickEvent::class)
    fun onIdleTick() {}

    @StateMachineRegister(from = "BACKING_OFF", to = "ACCELERATING", on = TargetBackedOff::class)
    fun onBackOffDone() {}

    @StateMachineRegister(from = "ACCELERATING", to = "ATTACKING", on = TargetInRange::class)
    fun onAccelerateDone() {}

    @StateMachineRegister(from = "ATTACKING", to = "IDLE", on = AttackExecuted::class)
    fun onAttackDone() {}

    override fun onEnable() {
        bus.register(this)
        Speed.reset()
    }

    override fun onDisable() {
        bus.unregister(this)
        RotationUtil.reset()
        while (sm.state() != Phase.IDLE) sm.transition(AttackExecuted)
        lockedTarget = null
    }

    private fun applyBypassMode() {
        val mode = bypass.get()
        if (mode == currentBypass) return
        currentBypass = mode
        RotationUtil.sync()
        when (mode) {
            "Grim" -> { RotationUtil.setMode(RotationUtil.RotationMode.GRIM); rotationSpeed.setValue(200.0) }
            "Matrix" -> { RotationUtil.setMode(RotationUtil.RotationMode.MATRIX); rotationSpeed.setValue(240.0) }
            "Vulcan" -> { RotationUtil.setMode(RotationUtil.RotationMode.VULCAN); rotationSpeed.setValue(220.0) }
            "NCP" -> { RotationUtil.setMode(RotationUtil.RotationMode.NCP); rotationSpeed.setValue(300.0) }
            "Intave" -> { RotationUtil.setMode(RotationUtil.RotationMode.INTAVE); rotationSpeed.setValue(150.0) }
            else -> { RotationUtil.setMode(RotationUtil.RotationMode.SMOOTH); rotationSpeed.setValue(360.0) }
        }
    }

    private fun canActivate(): Boolean {
        val p = mc.player ?: return false
        return when (mode.get()) {
            "Elytra" -> p.isFallFlying()
            "Fly" -> !p.onGround() && !p.isFallFlying()
            else -> false
        }
    }

    private fun hasMace(): Boolean {
        if (!requireMace.isEnabled) return true
        val p = mc.player ?: return false
        return p.getItemBySlot(EquipmentSlot.MAINHAND).item is MaceItem
            || p.getItemBySlot(EquipmentSlot.OFFHAND).item is MaceItem
    }

    @EventHandler
    fun onTick(event: TickEvent) {
        if (mc.player == null || mc.level == null) return
        applyBypassMode()
        if (!canActivate() || !hasMace()) {
            while (sm.state() != Phase.IDLE) sm.transition(AttackExecuted)
            lockedTarget = null; return
        }

        Speed.update()
        tickCounter++

        // Авто-переход: IDLE → BACKING_OFF по первому тику
        sm.transition(event)

        val target = findTarget()
        if (target == null) {
            while (sm.state() != Phase.IDLE) sm.transition(AttackExecuted)
            lockedTarget = null; RotationUtil.deactivate(); return
        }
        lockedTarget = target
        RotationUtil.update(target, rotationSpeed.getFloat())

        when (sm.state()) {
            Phase.IDLE -> {}
            Phase.BACKING_OFF -> updateBackOff(target)
            Phase.ACCELERATING -> updateAccelerate(target)
            Phase.ATTACKING -> updateAttack(target)
        }
    }

    private fun updateBackOff(target: Entity) {
        val p = mc.player ?: return
        val dist = p.distanceTo(target)
        if (dist >= backOffDist.getFloat()) { sm.transition(TargetBackedOff); return }
        val away = p.position().subtract(target.position()).normalize().scale(flightSpeed.getFloat() * 1.5)
        p.setDeltaMovement(Vec3(away.x, 0.0, away.z))
    }

    private fun updateAccelerate(target: Entity) {
        val p = mc.player ?: return
        val dist = p.distanceTo(target)
        if (dist <= 2.0) { sm.transition(TargetInRange); return }

        MoveUtil.flyTowards(
            target.boundingBox.center,
            flightSpeed.getFloat().toDouble(),
            target.boundingBox.center.y + heightOffset.getFloat()
        )

        val now = System.currentTimeMillis()
        if (now - lastLmbTime >= nextLmbDelay) {
            p.swing(net.minecraft.world.InteractionHand.MAIN_HAND)
            p.connection.send(net.minecraft.network.protocol.game.ServerboundSwingPacket(net.minecraft.world.InteractionHand.MAIN_HAND))
            lastLmbTime = now
            nextLmbDelay = getRandomCpsDelay()
        }
    }

    private fun updateAttack(target: Entity) {
        val p = mc.player ?: return
        val dist = p.distanceTo(target)
        if (dist > 4.0) { sm.transition(TargetInRange); return }

        p.connection.send(net.minecraft.network.protocol.game.ServerboundInteractPacket(target.id, net.minecraft.world.InteractionHand.MAIN_HAND, Vec3(target.x, target.y, target.z), false))
        p.swing(net.minecraft.world.InteractionHand.MAIN_HAND)
        mc.gameMode?.attack(p, target)
        sm.transition(AttackExecuted)
    }

    private fun findTarget(): Entity? {
        if (lockedTarget != null && isValidTarget(lockedTarget!!) &&
            mc.player!!.distanceTo(lockedTarget!!) <= range.getFloat()
        ) return lockedTarget

        var best: Entity? = null
        var bestDist = Double.MAX_VALUE
        for (entity in mc.level!!.entitiesForRendering()) {
            if (!isValidTarget(entity)) continue
            val dist = mc.player!!.distanceTo(entity).toDouble()
            if (dist <= range.getFloat().toDouble() && dist < bestDist) {
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

    private fun getRandomCpsDelay(): Long {
        val mn = minCps.getFloat().coerceAtLeast(1f)
        val mx = maxCps.getFloat().coerceAtLeast(mn)
        return (1000.0 / (mn + Math.random().toFloat() * (mx - mn))).toLong()
    }

    override val settings = listOf(mode, bypass, range, backOffDist, heightOffset, flightSpeed, minCps, maxCps, targets, rotationSpeed, requireMace)
}
