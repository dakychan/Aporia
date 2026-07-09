package so.aporia.module.impl.combat
import com.chaos.annotation.Obfuscate
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.entity.player.Player
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.ModuleManager
import so.aporia.module.settings.BooleanSetting
import so.aporia.module.settings.MultiSelectSetting
import so.aporia.module.settings.SliderSetting
import so.aporia.module.settings.SelectSetting
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.PacketEvent
import so.aporia.utils.events.impl.TickEvent
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.world.InteractionHand
import so.aporia.utils.imports.*
import so.aporia.utils.math.Angle
import so.aporia.utils.math.Prediction
import so.aporia.utils.user.render.ui.clickgui.QuestManager
import so.aporia.utils.user.locale.LocaleManager
import so.aporia.utils.user.player.rotation.RotationUtil
import so.aporia.utils.user.player.rotation.RotationUtil.RotationMode
import com.chaos.annotation.ChaosNative
@Obfuscate
@ChaosNative
class Aura : Module("Aura", Category.COMBAT, -1) {

    val combatMode: SelectSetting
    val range: SliderSetting
    val rotationMode: SelectSetting
    val rotationSpeed: SliderSetting
    val fov: SliderSetting
    val minCps: SliderSetting
    val maxCps: SliderSetting
    val targets: MultiSelectSetting
    val targetMode: SelectSetting
    val manualCooldown: SliderSetting

    val jitterAmount: SliderSetting
    val aimOffset: SliderSetting
    val hitChance: SliderSetting

    val autoDisableOnDeath: BooleanSetting
    val tpsSync: BooleanSetting
    val onlyCriticals: BooleanSetting

    private var lastAttackTime = 0L
    private var nextAttackDelay = 0L
    private var lockedTarget: Entity? = null

    private var currentTps = 20.0
    private var tpsPacketCount = 0
    private var tpsMeasureStart = 0L

    private fun isCriticalsEnabled(): Boolean {
        val m = ModuleManager.get("Criticals")
        return m != null && m.isEnabled
    }

    private fun shouldCrit(): Boolean {
        return isCriticalsEnabled() && combatMode.isSelected("1.9+")
    }

    init {
        val lm = LocaleManager.getInstance()
        lm.init()

        combatMode = SelectSetting(lm.get("module.aura.combat_mode"), lm.get("module.aura.combat_mode.desc"))
            .value("1.8", "1.9+").selected("1.9+")
        rotationMode = SelectSetting("Rotation Mode", "Mode of rotation (Smooth, Snap, HVH, Matrix, Vulcan, Grim, NCP, Intave)")
            .value("Smooth", "Snap", "HVH", "Matrix", "Vulcan", "Grim", "NCP", "Intave").selected("Smooth")
        rotationSpeed = SliderSetting(lm.get("module.aura.rotation_speed"), lm.get("module.aura.rotation_speed.desc"), 90.0, 5.0, 180.0, 1.0)
        range = SliderSetting(lm.get("module.aura.range"), lm.get("module.aura.range.desc"), 3.5, 1.0, 6.0, 0.1)
        fov = SliderSetting(lm.get("module.aura.fov"), lm.get("module.aura.fov.desc"), 180.0, 30.0, 180.0, 5.0)
        minCps = SliderSetting(lm.get("module.aura.min_cps"), lm.get("module.aura.min_cps.desc"), 8.0, 1.0, 20.0, 1.0)
        maxCps = SliderSetting(lm.get("module.aura.max_cps"), lm.get("module.aura.max_cps.desc"), 12.0, 1.0, 20.0, 1.0)
        manualCooldown = SliderSetting("Manual Cooldown", "cooldown for 1.9+ in seconds", 0.85, 0.1, 1.0, 0.01)
        targets = MultiSelectSetting(lm.get("module.aura.targets"), lm.get("module.aura.targets.desc")).options("Players", "Mobs", "Animals", "Friends")
        targetMode = SelectSetting(lm.get("module.aura.target_mode"), lm.get("module.aura.target_mode.desc"))
            .value(lm.get("module.aura.target_closest"), lm.get("module.aura.target_health"))
            .selected(lm.get("module.aura.target_closest"))
        jitterAmount = SliderSetting("Jitter", "Random angle jitter on rotation per tick", 0.0, 0.0, 5.0, 0.1, { rotationMode.get() != "HVH" })
        aimOffset = SliderSetting("Aim Offset", "Randomized aim position offset", 0.0, 0.0, 5.0, 0.1, { rotationMode.get() != "HVH" })
        hitChance = SliderSetting("Hit Chance", "Chance to hit (%)", 100.0, 0.0, 100.0, 1.0, { rotationMode.get() != "HVH" })
        autoDisableOnDeath = BooleanSetting("Auto Disable", "Disable module on player death", false)
        tpsSync = BooleanSetting("TPS Sync", "Sync attack timing with server TPS", true)
        onlyCriticals = BooleanSetting("Only Criticals", "Only attack when a crit is possible", false)
    }

    override fun onEnable() {
        super.onEnable()
        bus.register(this)
        RotationUtil.sync()
        applyRotationMode()
        lockedTarget = null
        lastAttackTime = System.currentTimeMillis()
        nextAttackDelay = getRandomDelay()
        currentTps = 20.0
        tpsPacketCount = 0
        tpsMeasureStart = 0
    }

    override fun onDisable() {
        super.onDisable()
        bus.unregister(this)
        RotationUtil.reset()
        lockedTarget = null
        currentTps = 20.0
        tpsPacketCount = 0
        tpsMeasureStart = 0
    }

    private fun isHvhMode(): Boolean {
        return rotationMode.get() == "HVH"
    }

    private fun applyRotationMode() {
        val sel = rotationMode.get()
        val mode = when (sel) {
            "HVH" -> RotationMode.HVH
            "Snap" -> RotationMode.SNAP
            "Matrix" -> RotationMode.MATRIX
            "Vulcan" -> RotationMode.VULCAN
            "Grim" -> RotationMode.GRIM
            "NCP" -> RotationMode.NCP
            "Intave" -> RotationMode.INTAVE
            else -> RotationMode.SMOOTH
        }
        RotationUtil.setMode(mode)
    }

    @EventHandler
    fun onPacketReceive(event: PacketEvent) {
        if (event.direction != PacketEvent.Direction.INBOUND) return
        if (!tpsSync.isEnabled) return
        tpsPacketCount++
        val now = System.nanoTime()
        if (tpsMeasureStart == 0L) {
            tpsMeasureStart = now
        } else if (now - tpsMeasureStart >= 1000000000L) {
            currentTps = 1.0.coerceAtLeast(20.0.coerceAtMost(tpsPacketCount.toDouble()))
            tpsPacketCount = 0
            tpsMeasureStart = now
        }
    }

    @EventHandler
    fun onTick(event: TickEvent) {
        if (mc.player == null || mc.level == null) return
        if (autoDisableOnDeath.isEnabled && !mc.player!!.isAlive) {
            disable()
            return
        }
        applyRotationMode()
        val target = findTarget() ?: run {
            lockedTarget = null
            RotationUtil.deactivate()
            return
        }
        lockedTarget = target
        val speed = if (isHvhMode()) 180f else rotationSpeed.getFloat()
        RotationUtil.update(target, speed)
        if (canAttack() && (isHvhMode() || RotationUtil.isOnTarget(6f))) {
            if (isHvhMode() || Math.random() * 100 < hitChance.getFloat()) {
                attackTarget(target)
            }
        }
    }

    private fun attackTarget(target: Entity) {
        doAttack(target)
    }

    private fun findTarget(): Entity? {
        if (lockedTarget != null && isValidTarget(lockedTarget!!) && mc.player!!.distanceTo(lockedTarget!!) <= range.getFloat() && isInFov(lockedTarget!!)) {
            return lockedTarget
        }
        var best: Entity? = null
        var bestValue = Double.MAX_VALUE
        val maxRange = range.getFloat()
        val lm = LocaleManager.getInstance()
        for (player in mc.level!!.players()) {
            if (!isValidTarget(player)) continue
            val dist = mc.player!!.distanceTo(player)
            if (dist > maxRange || !isInFov(player)) continue
            val value: Double = if (targetMode.isSelected(lm.get("module.aura.target_closest"))) dist.toDouble() else (player.health + player.absorptionAmount).toDouble()
            if (value < bestValue) {
                bestValue = value
                best = player
            }
        }
        return best
    }

    private fun doAttack(target: Entity) {
        val player = mc.player!!
        player.connection.send(ServerboundInteractPacket.createAttackPacket(target, player.isShiftKeyDown()))
        player.attack(target)
        player.swing(InteractionHand.MAIN_HAND)
        player.resetAttackStrengthTicker()
        lastAttackTime = System.currentTimeMillis()
        nextAttackDelay = getRandomDelay()
        if (target is Player) {
            QuestManager.notifyKill()
        }
    }

    private fun closesToValidCooldown(): Boolean {
        val weaponCooldown = mc.player!!.getAttackStrengthScale(0.5f)
        if (!tpsSync.isEnabled) return weaponCooldown >= 0.9f
        val delay = mc.player!!.currentItemAttackStrengthDelay
        val ticksNeeded = Math.max(1.0, Math.ceil(0.9 * delay - 0.5)).toInt().toLong()
        val requiredMs = (ticksNeeded * 1000.0 / currentTps).toLong()
        return System.currentTimeMillis() - lastAttackTime >= requiredMs
    }

    private fun canAttack(): Boolean {
        if (mc.player!!.swingTime > 0) return false
        if (combatMode.isSelected("1.8")) {
            val now = System.currentTimeMillis()
            if (now - lastAttackTime < nextAttackDelay) return false
            return true
        }
        val weaponCooldown = mc.player!!.getAttackStrengthScale(0.5f)
        if (shouldCrit()) {
            if (!mc.player!!.onGround()) {
                return weaponCooldown >= 0.95f && mc.player!!.fallDistance > 0.08f
            }
            return weaponCooldown >= 0.92f
        }
        if (onlyCriticals.isEnabled) {
            val p = mc.player!!
            if (p.onGround() || p.fallDistance <= 0.0f) return false
            if (p.isSprinting() || p.onClimbable() || p.isInWater() || p.isMobilityRestricted() || p.isPassenger()) return false
            if (lockedTarget !is LivingEntity) return false
            return weaponCooldown >= 0.92f
        }

        val minMs = (manualCooldown.getFloat() * 1000.0).toLong()
        return System.currentTimeMillis() - lastAttackTime >= minMs && weaponCooldown >= 0.85f
    }

    private fun getRandomDelay(): Long {
        var minCpsVal = minCps.getFloat()
        var maxCpsVal = maxCps.getFloat()
        if (minCpsVal <= 0) minCpsVal = 1f
        if (maxCpsVal <= 0) maxCpsVal = 1f
        val targetCps = minCpsVal + Math.random().toFloat() * (maxCpsVal - minCpsVal)
        return (1000.0 / targetCps).toLong()
    }

    private fun isValidTarget(entity: Entity?): Boolean {
        if (entity == mc.player || !entity!!.isAlive) return false
        if (entity is Player) {
            if (fm.isFriend(entity.name.string) && !targets.isSelected("Friends")) return false
            return targets.isSelected("Players")
        }
        if (entity is Mob) return targets.isSelected("Mobs")
        if (entity is Animal) return targets.isSelected("Animals")
        return false
    }

    private fun isInFov(target: Entity): Boolean {
        if (fov.getFloat() >= 180.0f) return true
        val diff = target.getEyePosition(1.0f).subtract(mc.player!!.getEyePosition(1.0f))
        val targetYaw = Angle.calculateFromDiff(diff)[0]
        val playerYaw = if (RotationUtil.isActive()) RotationUtil.getServerYaw() else mc.player!!.yRot
        return Angle.diff(targetYaw, playerYaw) <= fov.getFloat() / 2.0f
    }

    override val settings = listOf(combatMode, range, rotationMode, rotationSpeed, fov, minCps, maxCps, targets, targetMode, manualCooldown, jitterAmount, aimOffset, hitChance, autoDisableOnDeath, tpsSync, onlyCriticals)
}