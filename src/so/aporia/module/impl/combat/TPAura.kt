package so.aporia.module.impl.combat

import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.*
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import net.minecraft.sounds.SoundEvents
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.settings.BooleanSetting
import so.aporia.module.settings.NumberSetting
import so.aporia.module.settings.SelectSetting
import so.aporia.utils.events.EventBus
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.PacketEvent
import so.aporia.utils.events.impl.TickEvent
import so.aporia.utils.user.friend.FriendManager
import so.aporia.utils.user.player.rotation.RotationUtil
import java.util.ArrayDeque
import java.util.UUID
import kotlin.math.ceil

class TPAura : Module("TPAura", Category.COMBAT) {

    companion object {
        @JvmField
        val mc = Minecraft.getInstance()
    }

    // Настройки
    val mode = SelectSetting("Mode", "TP Aura mode")
        .value("Vanilla", "Step", "Blink")
        .selected("Vanilla")

    val bypass = SelectSetting("Bypass", "Anticheat bypass mode")
        .value("Grim", "Matrix", "Vulcan", "NCP", "Intave", "Smooth")
        .selected("Grim")

    val range = NumberSetting("Range", "Target search range", 100.0, 1.0, 100.0, 1.0)
    val tpRange = NumberSetting("TP Range", "Distance to teleport near target", 1.5, 0.5, 4.0, 0.1)
    val blinkTicks = NumberSetting("Blink Ticks", "Ticks to cache packets (Blink)", 5.0, 1.0, 20.0, 1.0)
    { mode.isSelected("Blink") }

    val minCps = NumberSetting("Min CPS", "Minimum attacks per second", 6.0, 1.0, 20.0, 1.0)
    val maxCps = NumberSetting("Max CPS", "Maximum attacks per second", 10.0, 1.0, 20.0, 1.0)

    val targets = SelectSetting("Targets", "Target types")
        .value("Players", "Mobs", "Animals", "Friends")
        .selected("Players")

    val rotationSpeed = NumberSetting("Rotation Speed", "Aim rotation speed (deg/s)", 360.0, 10.0, 360.0, 5.0)
    val tpBack = BooleanSetting("TP Back", "Teleport back after attack", true)

    val maceExploit = BooleanSetting("Mace Exploit", "Use Mace packet exploit", true)
    val maceMode = SelectSetting("Mace Mode", "Mace exploit mode")
        .value("Default", "New")
        .selected("Default")
    val maceHeight = NumberSetting("Mace Height", "Fall height for exploit", 10.0, 5.0, 50.0, 1.0)
    { maceExploit.isEnabled }
    val maceDelay = NumberSetting("Mace Delay", "Delay before attack (ms)", 50.0, 0.0, 200.0, 10.0)
    { maceExploit.isEnabled }

    // Состояние
    private var lockedTarget: Entity? = null
    private var lastAttackTime = 0L
    private var nextAttackDelay = 0L
    private var originalPos: Vec3? = null
    private var targetPos: Vec3? = null
    private var currentBypass = ""

    private val targetPosHistory = HashMap<UUID, ArrayDeque<Vec3>>()
    private var backtrackYaw = 0f
    private var backtrackPitch = 0f

    private enum class State {
        IDLE, TP_TO, ATTACK, TP_BACK, STEP_TO, STEP_BACK, MACE_EXPLOIT
    }

    private var state = State.IDLE
    private val steps = ArrayDeque<Vec3>()
    private var blinkActive = false
    private var blinkTimer = 0
    private var blinkDuration = 0

    override fun onEnable() {
        EventBus.register(this)
        resetState()
        lastAttackTime = System.currentTimeMillis()
        nextAttackDelay = getRandomDelay()
        RotationUtil.sync()
        currentBypass = ""
        applyBypassMode()
    }

    override fun onDisable() {
        EventBus.unregister(this)
        resetState()
        RotationUtil.reset()
    }

    private fun applyBypassMode() {
        val mode = bypass.get()
        if (mode == currentBypass) return
        currentBypass = mode
        RotationUtil.sync()
        when (mode) {
            "Grim" -> {
                RotationUtil.setMode(RotationUtil.RotationMode.GRIM)
                rotationSpeed.setValue(180.0)
            }
            "Matrix" -> {
                RotationUtil.setMode(RotationUtil.RotationMode.MATRIX)
                rotationSpeed.setValue(220.0)
            }
            "Vulcan" -> {
                RotationUtil.setMode(RotationUtil.RotationMode.VULCAN)
                rotationSpeed.setValue(200.0)
            }
            "NCP" -> {
                RotationUtil.setMode(RotationUtil.RotationMode.NCP)
                rotationSpeed.setValue(360.0)
            }
            "Intave" -> {
                RotationUtil.setMode(RotationUtil.RotationMode.INTAVE)
                rotationSpeed.setValue(120.0)
            }
            else -> {
                RotationUtil.setMode(RotationUtil.RotationMode.SMOOTH)
                rotationSpeed.setValue(360.0)
            }
        }
    }

    private fun resetState() {
        lockedTarget = null
        state = State.IDLE
        steps.clear()
        blinkActive = false
        blinkTimer = 0
        blinkDuration = 0
        originalPos = null
        targetPos = null
        targetPosHistory.clear()
    }

    @EventHandler
    fun onTick(event: TickEvent) {
        if (mc.player == null || mc.level == null) return
        applyBypassMode()

        val target = findTarget()

        if (target != null) {
            lockedTarget = target
            recordTargetPosition(target)
            val btPos = backtrackedPos(target)
            if (btPos != null) {
                val (y, p) = calcRot(mc.player!!.getEyePosition(1f), btPos)
                backtrackYaw = y
                backtrackPitch = p
            }
            RotationUtil.update(target, rotationSpeed.getFloat())
        } else {
            lockedTarget = null
        }

        handleStateMachine()

        if (state != State.IDLE) return
        if (target == null) return

        val hand = InteractionHand.MAIN_HAND
        val item = mc.player!!.getItemInHand(hand)
        if (item.isEmpty) return

        if (!canAttack()) return

        originalPos = mc.player!!.position()
        targetPos = findTpPosition(target) ?: return

        val isMace = item.item == net.minecraft.world.item.Items.MACE

        when {
            isMace && maceExploit.isEnabled -> {
                state = State.MACE_EXPLOIT
                performMaceExploit()
            }
            else -> {
                performNormalAttack(target)
            }
        }
    }

    // ============ ПАКЕТНАЯ АТАКА ДЛЯ 1.21.4 ============

    private fun performNormalAttack(target: Entity) {
        when (mode.get()) {
            "Vanilla" -> {
                val steps = buildSteps(originalPos!!, targetPos!!, 0.5)
                for (i in steps.indices) {
                    val step = steps[i]
                    if (i == steps.size - 1) {
                        sendPosRot(step, backtrackYaw, backtrackPitch, false)
                    } else {
                        sendPosRot(step, mc.player!!.yRot, mc.player!!.xRot, false)
                    }
                }
                doPacketAttack(target)
                if (tpBack.isEnabled) {
                    val backSteps = buildSteps(targetPos!!, originalPos!!, 0.5)
                    for (step in backSteps) {
                        sendPosRot(step, mc.player!!.yRot, mc.player!!.xRot, mc.player!!.onGround())
                    }
                }
                lastAttackTime = System.currentTimeMillis()
                nextAttackDelay = getRandomDelay()
            }
            "Step" -> {
                buildSteps(originalPos!!, targetPos!!, 0.5).let { steps.clear(); steps.addAll(it) }
                state = State.STEP_TO
            }
            "Blink" -> {
                blinkActive = true
                blinkTimer = 0
                blinkDuration = blinkTicks.getFloat().toInt()
                state = State.TP_TO
            }
        }
    }

    private fun performMaceExploit() {
        val player = mc.player!!
        val height = maceHeight.getFloat()
        val mode = maceMode.get()
        val tpPos = targetPos ?: run { state = State.IDLE; return }
        val target = lockedTarget ?: run { state = State.IDLE; return }

        val exploitBase = player.position()

        // Шаг 1: Фейк высоты
        if (tpPos.distanceToSqr(exploitBase) > 0.01) {
            when (mode) {
                "New" -> {
                    val steps = ceil(height / 10.0).toInt()
                    for (i in 0..steps) {
                        val t = i.toDouble() / steps
                        sendPosRot(Vec3(tpPos.x, tpPos.y + height * t, tpPos.z), player.yRot, player.xRot, false)
                    }
                    for (i in steps downTo 0) {
                        val t = i.toDouble() / steps
                        sendPosRot(Vec3(tpPos.x, tpPos.y + height * t, tpPos.z), player.yRot, player.xRot, false)
                    }
                }
                else -> {
                    sendPosRot(Vec3(tpPos.x, tpPos.y + height, tpPos.z), player.yRot, player.xRot, false)
                    sendPosRot(tpPos, player.yRot, player.xRot, false)
                }
            }
        } else {
            when (mode) {
                "New" -> {
                    val steps = ceil(height / 10.0).toInt()
                    for (i in 0..steps) {
                        val t = i.toDouble() / steps
                        sendPosRot(Vec3(exploitBase.x, exploitBase.y + height * t, exploitBase.z), player.yRot, player.xRot, false)
                    }
                    for (i in steps downTo 0) {
                        val t = i.toDouble() / steps
                        sendPosRot(Vec3(exploitBase.x, exploitBase.y + height * t, exploitBase.z), player.yRot, player.xRot, false)
                    }
                }
                else -> {
                    sendPosRot(Vec3(exploitBase.x, exploitBase.y + height, exploitBase.z), player.yRot, player.xRot, false)
                    sendPosRot(exploitBase, player.yRot, player.xRot, false)
                }
            }
        }

        player.playSound(SoundEvents.MACE_SMASH_AIR, 1.0f, 1.0f)

        val delay = maceDelay.getFloat().toLong()
        if (delay > 0) {
            try { Thread.sleep(delay) } catch (_: InterruptedException) {}
        }

        // Шаг 2: TP + атака
        sendPosRot(tpPos, backtrackYaw, backtrackPitch, false)
        doPacketAttack(target)
        lastAttackTime = System.currentTimeMillis()
        nextAttackDelay = getRandomDelay()

        if (tpBack.isEnabled && originalPos != null) {
            sendPosRot(originalPos!!, player.yRot, player.xRot, true)
        }

        state = State.IDLE
    }

    private fun handleStateMachine() {
        if (state == State.IDLE) return

        when (state) {
            State.STEP_TO -> {
                if (steps.isEmpty()) {
                    if (lockedTarget != null) {
                        doPacketAttack(lockedTarget!!)
                        lastAttackTime = System.currentTimeMillis()
                        nextAttackDelay = getRandomDelay()
                        if (tpBack.isEnabled && originalPos != null) {
                            buildSteps(targetPos!!, originalPos!!, 0.5).let { steps.clear(); steps.addAll(it) }
                            state = State.STEP_BACK
                        } else {
                            state = State.IDLE
                        }
                    } else {
                        state = State.IDLE
                    }
                    return
                }
                steps.poll()?.let {
                    sendPosRot(it, mc.player!!.yRot, mc.player!!.xRot, false)
                }
            }
            State.STEP_BACK -> {
                if (steps.isEmpty()) {
                    state = State.IDLE
                    return
                }
                steps.poll()?.let {
                    sendPosRot(it, mc.player!!.yRot, mc.player!!.xRot, mc.player!!.onGround())
                }
            }
            State.TP_TO -> {
                if (blinkTimer >= blinkDuration) {
                    blinkActive = false
                    targetPos?.let {
                        sendPosRot(it, backtrackYaw, backtrackPitch, false)
                    }
                    if (lockedTarget != null) {
                        doPacketAttack(lockedTarget!!)
                        lastAttackTime = System.currentTimeMillis()
                        nextAttackDelay = getRandomDelay()
                        state = if (tpBack.isEnabled && originalPos != null) State.TP_BACK else State.IDLE
                    } else {
                        state = State.IDLE
                    }
                    blinkTimer = 0
                    return
                }
                blinkTimer++
            }
            State.TP_BACK -> {
                sendPosRot(originalPos!!, mc.player!!.yRot, mc.player!!.xRot, mc.player!!.onGround())
                state = State.IDLE
            }
            else -> {}
        }
    }

    // ============ ПАКЕТНАЯ АТАКА (ГЛАВНОЕ) ============

    private fun doPacketAttack(target: Entity) {
        if (mc.player == null || mc.connection == null) return

        val player = mc.player!!
        val connection = player.connection

        // 1. Анимация руки (пакет)
        connection.send(
            ServerboundSwingPacket(InteractionHand.MAIN_HAND)
        )

        // 2. АТАКА (пакет) - правильный способ для 1.21.4
        connection.send(
            ServerboundInteractPacket.createAttackPacket(
                target,
                false  // usingSecondaryAction
            )
        )

        // 3. Для булавы - использование предмета
        val item = player.getItemInHand(InteractionHand.MAIN_HAND)
        if (item.item == net.minecraft.world.item.Items.MACE) {
            // Звуки клиентские
            player.playSound(SoundEvents.MACE_SMASH_AIR, 1.0f, 1.0f)

            // Пакет использования предмета
            connection.send(
                ServerboundUseItemPacket(
                    InteractionHand.MAIN_HAND,
                    0,  // sequence
                    player.yRot,
                    player.xRot
                )
            )

            if (target.onGround()) {
                player.playSound(SoundEvents.MACE_SMASH_GROUND, 1.0f, 1.0f)
            }

            if (player.fallDistance > 1.5) {
                player.playSound(SoundEvents.MACE_SMASH_GROUND_HEAVY, 1.0f, 1.0f)
            }
        }
    }

    // ============ МЕТОДЫ ОТПРАВКИ ПАКЕТОВ ДВИЖЕНИЯ ============

    private fun sendPos(pos: Vec3, onGround: Boolean = false, horizontalCollision: Boolean = false) {
        if (mc.player == null) return
        mc.player!!.connection.send(
            ServerboundMovePlayerPacket.Pos(
                pos.x, pos.y, pos.z,
                onGround,
                horizontalCollision
            )
        )
    }

    private fun sendPosRot(pos: Vec3, yaw: Float, pitch: Float, onGround: Boolean = false, horizontalCollision: Boolean = false) {
        if (mc.player == null) return
        mc.player!!.connection.send(
            ServerboundMovePlayerPacket.PosRot(
                pos.x, pos.y, pos.z,
                yaw, pitch,
                onGround,
                horizontalCollision
            )
        )
    }

    private fun sendRot(yaw: Float, pitch: Float, onGround: Boolean = false, horizontalCollision: Boolean = false) {
        if (mc.player == null) return
        mc.player!!.connection.send(
            ServerboundMovePlayerPacket.Rot(
                yaw, pitch,
                onGround,
                horizontalCollision
            )
        )
    }

    private fun sendStatus(onGround: Boolean, horizontalCollision: Boolean = false) {
        if (mc.player == null) return
        mc.player!!.connection.send(
            ServerboundMovePlayerPacket.StatusOnly(
                onGround,
                horizontalCollision
            )
        )
    }

    // ============ ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ============

    private fun buildSteps(from: Vec3, to: Vec3, maxStep: Double = 0.5): List<Vec3> {
        val result = ArrayList<Vec3>()
        val dist = from.distanceTo(to)
        val count = maxOf(1, ceil(dist / maxStep).toInt())
        for (i in 1..count) {
            val t = i.toDouble() / count
            result.add(Vec3(
                lerp(t, from.x, to.x),
                lerp(t, from.y, to.y),
                lerp(t, from.z, to.z)
            ))
        }
        return result
    }

    private fun lerp(t: Double, a: Double, b: Double): Double = a + t * (b - a)

    private fun findTpPosition(target: Entity): Vec3? {
        return target.boundingBox.getCenter()
    }

    private fun findTarget(): Entity? {
        val currentRange = range.getFloat()
        if (lockedTarget != null && isValidTarget(lockedTarget!!) &&
            mc.player!!.distanceTo(lockedTarget!!) <= currentRange) {
            return lockedTarget
        }

        var best: Entity? = null
        var bestDist = Double.MAX_VALUE

        for (player in mc.level!!.players()) {
            if (!isValidTarget(player)) continue
            val dist = mc.player!!.distanceTo(player)
            if (dist <= currentRange && dist.toDouble() < bestDist) {
                bestDist = dist.toDouble()
                best = player
            }
        }

        if (targets.isSelected("Mobs") || targets.isSelected("Animals")) {
            // Здесь можно добавить мобов и животных
        }

        return best
    }

    private fun isValidTarget(entity: Entity?): Boolean {
        if (entity == mc.player || entity?.isAlive != true) return false

        if (entity is Player) {
            if (!targets.isSelected("Players")) return false
            if (FriendManager.isFriend(entity.name.string) && !targets.isSelected("Friends")) return false
            return true
        }
        if (entity is Mob) return targets.isSelected("Mobs")
        if (entity is Animal) return targets.isSelected("Animals")
        return false
    }

    private fun recordTargetPosition(target: Entity) {
        val queue = targetPosHistory.getOrPut(target.uuid) { ArrayDeque(20) }
        queue.addLast(target.position())
        if (queue.size > 20) queue.removeFirst()
    }

    private fun backtrackedPos(target: Entity): Vec3? {
        val ping = mc.player!!.connection.getPlayerInfo(mc.player!!.uuid)?.latency ?: 0
        val ticksAgo = (ping / 50).coerceIn(0, 15)
        val queue = targetPosHistory[target.uuid] ?: return null
        val idx = (queue.size - 1 - ticksAgo).coerceIn(0, queue.size - 1)
        return queue.elementAtOrNull(idx)
    }

    private fun calcRot(from: Vec3, to: Vec3): Pair<Float, Float> {
        val diff = to.subtract(from).normalize()
        val yaw = (Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0).toFloat()
        val pitch = (-Math.toDegrees(Math.atan2(diff.y, Math.hypot(diff.x, diff.z)))).toFloat()
        return yaw to pitch.coerceIn(-90f, 90f)
    }

    private fun canAttack(): Boolean {
        val now = System.currentTimeMillis()
        val elapsed = now - lastAttackTime
        if (elapsed < 50L) return false
        return elapsed >= nextAttackDelay
    }

    private fun getRandomDelay(): Long {
        val min = minCps.getFloat().coerceAtLeast(1.0f)
        val max = maxCps.getFloat().coerceAtLeast(min)
        val cps = min + Math.random().toFloat() * (max - min)
        return (1000.0 / cps).toLong()
    }

    @EventHandler
    fun onPacketSend(event: PacketEvent) {
        if (event.direction() != PacketEvent.Direction.OUTBOUND) return
        if (mc.player == null) return
        if (blinkActive && event.packet() is ServerboundMovePlayerPacket) {
            event.cancel()
        }
    }
}