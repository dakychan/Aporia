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
import so.aporia.module.settings.NumberSetting
import so.aporia.module.settings.SelectSetting
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.TickEvent
import net.minecraft.client.Minecraft
import so.aporia.utils.imports.*
import so.aporia.utils.user.player.movement.MoveUtil
import so.aporia.utils.user.player.rotation.RotationUtil

class ElytraTarget : Module("ElytraTarget", Category.COMBAT) {
    companion object {
        @JvmField val mc = Minecraft.getInstance()
    }

    val mode = SelectSetting("Mode", "Elytra or Fly mode")
        .value("Elytra", "Fly")
        .selected("Elytra")
    val bypass = SelectSetting("Bypass", "Anticheat bypass mode")
        .value("Grim", "Matrix", "Vulcan", "NCP", "Intave", "Smooth")
        .selected("Grim")
    val range = NumberSetting("Range", "Target search range", 100.0, 1.0, 100.0, 1.0)
    val backOffDist = NumberSetting("Back Off", "Distance to back off before attack", 9.0, 3.0, 20.0, 1.0)
    val heightOffset = NumberSetting("Height Offset", "Height above target to fly at", 3.0, -5.0, 10.0, 0.5)
    val flightSpeed = NumberSetting("Flight Speed", "Speed when flying towards target", 2.0, 0.5, 5.0, 0.25)
    val minCps = NumberSetting("Min CPS", "Minimum LMB spam CPS", 15.0, 5.0, 20.0, 1.0)
    val maxCps = NumberSetting("Max CPS", "Maximum LMB spam CPS", 20.0, 5.0, 20.0, 1.0)
    val targets = SelectSetting("Targets", "Target types")
        .value("Players", "Mobs", "Animals", "Friends")
        .selected("Players")
    val rotationSpeed = NumberSetting("Rotation Speed", "Aim rotation speed (deg/s)", 360.0, 10.0, 360.0, 5.0)
    val requireMace = BooleanSetting("Require Mace", "Only work when holding a mace", true)

    private enum class Phase { IDLE, BACKING_OFF, ACCELERATING, ATTACKING }
    private var phase = Phase.IDLE
    private var lockedTarget: Entity? = null
    private var lastLmbTime = 0L
    private var nextLmbDelay = 0L
    private var lastRmbTime = 0L
    private var tickCounter = 0
    private var currentBypass = ""

    override fun onEnable() {
        bus.register(this)
    }

    override fun onDisable() {
        bus.unregister(this)
        RotationUtil.reset()
        phase = Phase.IDLE
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
        val player = mc.player ?: return false
        return when (mode.get()) {
            "Elytra" -> player.isFallFlying()
            "Fly" -> !player.onGround() && !player.isFallFlying()
            else -> false
        }
    }

    private fun hasMace(): Boolean {
        if (!requireMace.isEnabled) return true
        val player = mc.player ?: return false
        val main = player.getItemBySlot(EquipmentSlot.MAINHAND)
        val off = player.getItemBySlot(EquipmentSlot.OFFHAND)
        return main.item is MaceItem || off.item is MaceItem
    }

    @EventHandler
    fun onTick(event: TickEvent) {
        if (mc.player == null || mc.level == null) return
        applyBypassMode()
        if (!canActivate() || !hasMace()) {
            phase = Phase.IDLE
            lockedTarget = null
            return
        }

        tickCounter++

        val target = findTarget()
        if (target == null) {
            phase = Phase.IDLE
            lockedTarget = null
            RotationUtil.deactivate()
            return
        }
        lockedTarget = target

        RotationUtil.update(target, rotationSpeed.getFloat())

        when (phase) {
            Phase.IDLE -> startBackOff(target)
            Phase.BACKING_OFF -> updateBackOff(target)
            Phase.ACCELERATING -> updateAccelerate(target)
            Phase.ATTACKING -> updateAttack(target)
        }
    }

    private fun startBackOff(target: Entity) { phase = Phase.BACKING_OFF }

    private fun updateBackOff(target: Entity) {
        val player = mc.player ?: return
        val dist = player.distanceTo(target)
        if (dist >= backOffDist.getFloat()) { phase = Phase.ACCELERATING; return }
        val away = player.position().subtract(target.position()).normalize().scale(flightSpeed.getFloat() * 1.5)
        player.setDeltaMovement(Vec3(away.x, 0.0, away.z))
    }

    private fun updateAccelerate(target: Entity) {
        val player = mc.player ?: return
        val dist = player.distanceTo(target)
        if (dist <= 2.0) { phase = Phase.ATTACKING; return }

        val targetCenter = target.boundingBox.center
        val targetHeight = targetCenter.y + heightOffset.getFloat()

        MoveUtil.flyTowards(targetCenter, flightSpeed.getFloat().toDouble(), targetHeight)

        val now = System.currentTimeMillis()
        if (now - lastLmbTime >= nextLmbDelay) {
            player.swing(net.minecraft.world.InteractionHand.MAIN_HAND)
            player.connection.send(net.minecraft.network.protocol.game.ServerboundSwingPacket(net.minecraft.world.InteractionHand.MAIN_HAND))
            lastLmbTime = now
            nextLmbDelay = getRandomCpsDelay()
        }
    }

    private fun updateAttack(target: Entity) {
        val player = mc.player ?: return
        val dist = player.distanceTo(target)
        if (dist > 4.0) { phase = Phase.ACCELERATING; return }

        player.connection.send(net.minecraft.network.protocol.game.ServerboundInteractPacket.createAttackPacket(target, false))
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND)
        mc.gameMode?.attack(player, target)

        phase = Phase.IDLE
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
                bestDist = dist
                best = entity
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
        var min = minCps.getFloat()
        var max = maxCps.getFloat()
        if (min <= 0) min = 1f
        if (max <= 0) max = 1f
        return (1000.0 / (min + Math.random().toFloat() * (max - min))).toLong()
    }
}
