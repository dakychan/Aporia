package so.aporia.module.impl.combat

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
import so.aporia.module.settings.SliderSetting
import so.aporia.module.settings.SelectSetting
import so.aporia.utils.events.EventBus
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.PacketEvent
import so.aporia.utils.events.impl.TickEvent
import so.aporia.utils.events.impl.WorldRenderEvent
import so.aporia.utils.imports.*
import so.aporia.utils.math.Angle
import so.aporia.utils.math.Prediction
import so.aporia.utils.user.player.rotation.RotationUtil
import java.util.ArrayDeque
import java.util.UUID
import kotlin.math.ceil
import com.chaos.annotation.ChaosNative
@ChaosNative
class TPAura : Module("TPAura", Category.COMBAT) {

    // ─── Settings ─────────────────────────────────────────
    val mode = SelectSetting("Mode", "TP Aura mode")
        .value("Vanilla", "Step", "Blink", "LiquidBounce")
        .selected("Vanilla")

    val bypass = SelectSetting("Bypass", "Anticheat bypass mode")
        .value("Grim", "Matrix", "Vulcan", "NCP", "Intave", "Smooth")
        .selected("Grim")

    val range = SliderSetting("Range", "Target search range", 100.0, 1.0, 100.0, 1.0)
    val tpRange = SliderSetting("TP Range", "Distance to teleport near target", 1.5, 0.5, 4.0, 0.1)
    val blinkTicks = SliderSetting("Blink Ticks", "Ticks to cache packets (Blink)", 5.0, 1.0, 20.0, 1.0)
    { mode.isSelected("Blink") }

    val minCps = SliderSetting("Min CPS", "Minimum attacks per second", 6.0, 1.0, 20.0, 1.0)
    val maxCps = SliderSetting("Max CPS", "Maximum attacks per second", 10.0, 1.0, 20.0, 1.0)

    val targets = SelectSetting("Targets", "Target types")
        .value("Players", "Mobs", "Animals", "Friends")
        .selected("Players")

    val rotationSpeed = SliderSetting("Rotation Speed", "Aim rotation speed (deg/s)", 360.0, 10.0, 360.0, 5.0)
    val tpBack = BooleanSetting("TP Back", "Teleport back after attack", true)

    // ─── State ────────────────────────────────────────────
    private var lockedTarget: Entity? = null
    private var lastAttackTime = 0L
    private var nextAttackDelay = 0L
    private var originalPos: Vec3? = null
    private var targetPos: Vec3? = null
    private var currentBypass = ""

    private val targetPosHistory = HashMap<UUID, ArrayDeque<Vec3>>()
    private var backtrackYaw = 0f
    private var backtrackPitch = 0f

    // Step mode state
    private val steps = ArrayDeque<Vec3>()
    private var stepPhase = 0 // 0=idle, 1=walking to target, 2=walking back

    // Blink mode state
    private var blinkActive = false
    private var blinkTimer = 0
    private var blinkDuration = 0
    private val cachedMovePackets = ArrayList<ServerboundMovePlayerPacket>()

    // LiquidBounce mode state — fake player near target
    var fakePlayerPos: Vec3? = null
        private set
    var fakePlayerYaw = 0f
        private set
    var fakePlayerPitch = 0f
        private set

    override fun onEnable() {
        bus.register(this)
        resetState()
        lastAttackTime = System.currentTimeMillis()
        nextAttackDelay = getRandomDelay()
        RotationUtil.sync()
        currentBypass = ""
        applyBypassMode()
    }

    override fun onDisable() {
        bus.unregister(this)
        resetState()
        RotationUtil.reset()
    }

    private fun applyBypassMode() {
        val m = bypass.get()
        if (m == currentBypass) return
        currentBypass = m
        RotationUtil.sync()
        when (m) {
            "Grim" -> { RotationUtil.setMode(RotationUtil.RotationMode.GRIM); rotationSpeed.setValue(180.0) }
            "Matrix" -> { RotationUtil.setMode(RotationUtil.RotationMode.MATRIX); rotationSpeed.setValue(220.0) }
            "Vulcan" -> { RotationUtil.setMode(RotationUtil.RotationMode.VULCAN); rotationSpeed.setValue(200.0) }
            "NCP" -> { RotationUtil.setMode(RotationUtil.RotationMode.NCP); rotationSpeed.setValue(360.0) }
            "Intave" -> { RotationUtil.setMode(RotationUtil.RotationMode.INTAVE); rotationSpeed.setValue(120.0) }
            else -> { RotationUtil.setMode(RotationUtil.RotationMode.SMOOTH); rotationSpeed.setValue(360.0) }
        }
    }

    private fun resetState() {
        lockedTarget = null
        steps.clear(); stepPhase = 0
        blinkActive = false; blinkTimer = 0; blinkDuration = 0
        cachedMovePackets.clear()
        originalPos = null; targetPos = null
        targetPosHistory.clear()
        fakePlayerPos = null
    }

    // ─── Tick ─────────────────────────────────────────────
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
                val angles = Angle.calculateFromDiff(btPos.subtract(mc.player!!.getEyePosition(1f)))
                backtrackYaw = angles[0]
                backtrackPitch = angles[1].coerceIn(-90f, 90f)
            }
            RotationUtil.update(target, rotationSpeed.getFloat())
        } else {
            lockedTarget = null
            resetState()
            RotationUtil.deactivate()
            return
        }

        when (mode.get()) {
            "Vanilla" -> tickVanilla(target)
            "Step" -> tickStep(target)
            "Blink" -> tickBlink(target)
            "LiquidBounce" -> tickLiquidBounce(target)
        }
    }

    // ─── Vanilla: instant TP + attack + TP back ───────────
    private fun tickVanilla(target: Entity) {
        if (!canAttack()) return
        originalPos = mc.player!!.position()
        targetPos = findTpPosition(target) ?: return

        // TP to target
        sendPosRot(targetPos!!, backtrackYaw, backtrackPitch, false)
        doPacketAttack(target)

        // TP back
        if (tpBack.isEnabled && originalPos != null) {
            sendPosRot(originalPos!!, mc.player!!.yRot, mc.player!!.xRot, mc.player!!.onGround())
        }

        lastAttackTime = System.currentTimeMillis()
        nextAttackDelay = getRandomDelay()
    }

    // ─── Step: walk to target over ticks, attack, walk back
    private fun tickStep(target: Entity) {
        when (stepPhase) {
            0 -> {
                // Start: build steps to target
                if (!canAttack()) return
                originalPos = mc.player!!.position()
                targetPos = findTpPosition(target) ?: return
                buildSteps(originalPos!!, targetPos!!, 0.5).let { steps.clear(); steps.addAll(it) }
                stepPhase = 1
            }
            1 -> {
                // Walking to target
                if (steps.isNotEmpty()) {
                    val step = steps.poll()!!
                    sendPosRot(step, mc.player!!.yRot, mc.player!!.xRot, false)
                } else {
                    // Arrived — attack
                    doPacketAttack(target)
                    // Build steps back
                    if (tpBack.isEnabled && originalPos != null) {
                        buildSteps(targetPos!!, originalPos!!, 0.5).let { steps.clear(); steps.addAll(it) }
                        stepPhase = 2
                    } else {
                        stepPhase = 0
                        lastAttackTime = System.currentTimeMillis()
                        nextAttackDelay = getRandomDelay()
                    }
                }
            }
            2 -> {
                // Walking back
                if (steps.isNotEmpty()) {
                    val step = steps.poll()!!
                    sendPosRot(step, mc.player!!.yRot, mc.player!!.xRot, mc.player!!.onGround())
                } else {
                    stepPhase = 0
                    lastAttackTime = System.currentTimeMillis()
                    nextAttackDelay = getRandomDelay()
                }
            }
        }
    }

    // ─── Blink: cache packets, send as teleport for Grim bypass
    private fun tickBlink(target: Entity) {
        if (!blinkActive) {
            if (!canAttack()) return
            originalPos = mc.player!!.position()
            targetPos = findTpPosition(target) ?: return
            blinkActive = true
            blinkTimer = 0
            blinkDuration = blinkTicks.getFloat().toInt()
            cachedMovePackets.clear()
            return
        }

        blinkTimer++

        if (blinkTimer >= blinkDuration) {
            blinkActive = false

            // Grim bypass: send cached packets as a burst (server sees teleport)
            for (pkt in cachedMovePackets) {
                mc.player!!.connection.send(pkt)
            }
            cachedMovePackets.clear()

            // TP to target position
            sendPosRot(targetPos!!, backtrackYaw, backtrackPitch, false)
            doPacketAttack(target)

            // TP back immediately
            if (tpBack.isEnabled && originalPos != null) {
                sendPosRot(originalPos!!, mc.player!!.yRot, mc.player!!.xRot, mc.player!!.onGround())
            }

            lastAttackTime = System.currentTimeMillis()
            nextAttackDelay = getRandomDelay()
            blinkTimer = 0
        }
    }

    // ─── LiquidBounce: fake player near target, stay in place, rotate + attack
    private fun tickLiquidBounce(target: Entity) {
        if (!canAttack()) return

        val targetCenter = target.boundingBox.center
        // Calculate fake player position: slightly offset from target
        val offsetDir = mc.player!!.position().subtract(targetCenter).normalize()
        fakePlayerPos = targetCenter.add(offsetDir.scale(tpRange.getFloat().toDouble()))

        // Look at target from fake position
        val lookAngles = Angle.calculateFromDiff(targetCenter.subtract(fakePlayerPos!!))
        fakePlayerYaw = lookAngles[0]
        fakePlayerPitch = lookAngles[1].coerceIn(-90f, 90f)

        // Send rotation to server (server sees us looking at target)
        RotationUtil.update(target, rotationSpeed.getFloat())

        // Attack from real position (server-side we're in range if close enough)
        val dist = mc.player!!.distanceTo(target)
        if (dist <= 6.0) {
            doPacketAttack(target)
        }

        lastAttackTime = System.currentTimeMillis()
        nextAttackDelay = getRandomDelay()
    }

    // ─── Packet Attack ────────────────────────────────────
    private fun doPacketAttack(target: Entity) {
        if (mc.player == null || mc.connection == null) return
        val player = mc.player!!
        val connection = player.connection

        // Swing animation
        connection.send(ServerboundSwingPacket(InteractionHand.MAIN_HAND))

        // Attack interact packet
        connection.send(ServerboundInteractPacket(target.id, InteractionHand.MAIN_HAND, Vec3(target.x, target.y, target.z), false))

        player.resetAttackStrengthTicker()
    }

    // ─── Packet sending ───────────────────────────────────
    private fun sendPosRot(pos: Vec3, yaw: Float, pitch: Float, onGround: Boolean = false, horizontalCollision: Boolean = false) {
        if (mc.player == null) return
        mc.player!!.connection.send(
            ServerboundMovePlayerPacket.PosRot(pos.x, pos.y, pos.z, yaw, pitch, onGround, horizontalCollision)
        )
    }

    // ─── Step building ────────────────────────────────────
    private fun buildSteps(from: Vec3, to: Vec3, maxStep: Double = 0.5): List<Vec3> {
        val dist = from.distanceTo(to)
        val count = maxOf(1, ceil(dist / maxStep).toInt())
        return (1..count).map { i ->
            val t = i.toDouble() / count
            Vec3(
                Prediction.lerp(t, from.x, to.x),
                Prediction.lerp(t, from.y, to.y),
                Prediction.lerp(t, from.z, to.z)
            )
        }
    }

    private fun findTpPosition(target: Entity): Vec3? = target.boundingBox.center

    // ─── Target finding ───────────────────────────────────
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

    // ─── Backtrack ────────────────────────────────────────
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

    // ─── Cooldown ─────────────────────────────────────────
    private fun canAttack(): Boolean {
        val now = System.currentTimeMillis()
        return now - lastAttackTime >= nextAttackDelay
    }

    private fun getRandomDelay(): Long {
        val min = minCps.getFloat().coerceAtLeast(1.0f)
        val max = maxCps.getFloat().coerceAtLeast(min)
        val cps = min + Math.random().toFloat() * (max - min)
        return (1000.0 / cps).toLong()
    }

    // ─── Ghost player: PlayerESP checks fakePlayerPos ────

    // ─── Blink packet interception — cache and cancel ────
    @EventHandler
    fun onPacketSend(event: PacketEvent) {
        if (event.direction() != PacketEvent.Direction.OUTBOUND) return
        if (mc.player == null) return
        if (blinkActive) {
            val pkt = event.packet()
            if (pkt is ServerboundMovePlayerPacket) {
                cachedMovePackets.add(pkt)
                event.cancel()
            }
        }
    }

    override val settings = listOf(mode, bypass, range, tpRange, blinkTicks, minCps, maxCps, targets, rotationSpeed, tpBack)
}
