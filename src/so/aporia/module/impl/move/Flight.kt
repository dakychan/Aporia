package so.aporia.module.impl.move
import so.aporia.utils.imports.*
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.network.protocol.game.ServerboundSwingPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.MaceItem
import net.minecraft.world.phys.Vec3
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.settings.SliderSetting
import so.aporia.module.settings.SelectSetting
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.TickEvent
import com.chaos.annotation.ChaosNative
@ChaosNative
class Flight : Module("Flight", Category.MOVE) {
    companion object {
        @JvmField val mc = Minecraft.getInstance()
    }

    val mode = SelectSetting("Mode", "Flight mode")
        .value("Default", "Spear", "DragonFly")
        .selected("Default")
    val xzSpeed = SliderSetting("XZ Speed", "Horizontal acceleration speed", 0.5, 0.05, 5.0, 0.05)
    val ySpeed = SliderSetting("Y Speed", "Vertical speed", 0.5, 0.05, 5.0, 0.05)

    private var lastAttackTime = 0L
    private var attackDelay = 50L

    override val settings = listOf(mode, xzSpeed, ySpeed)

    override fun onEnable() {
        bus.register(this)
        lastAttackTime = System.currentTimeMillis()
        attackDelay = 50L
    }

    override fun onDisable() {
        bus.unregister(this)
    }

    @EventHandler
    fun onTick(event: TickEvent) {
        if (mc.player == null || mc.level == null) return

        when (mode.get()) {
            "Default" -> handleDefault()
            "Spear" -> handleSpear()
            "DragonFly" -> handleDragonFly()
        }
    }

    private fun hasSpear(): Boolean {
        val player = mc.player ?: return false
        val mainHand = player.getItemBySlot(EquipmentSlot.MAINHAND)
        val offHand = player.getItemBySlot(EquipmentSlot.OFFHAND)
        return mainHand.item is MaceItem || offHand.item is MaceItem
    }

    private fun calcMove(forward: Float, strafe: Float, yaw: Float, speed: Float): Pair<Float, Float> {
        val yawRad = yaw * (Math.PI.toFloat() / 180f)
        // Forward vector: (-sin(yaw), 0, cos(yaw))
        // Right vector: (cos(yaw), 0, sin(yaw))
        var moveX = (-Math.sin(yawRad.toDouble()) * forward).toFloat() + (Math.cos(yawRad.toDouble()) * strafe).toFloat()
        var moveZ = (Math.cos(yawRad.toDouble()) * forward).toFloat() + (Math.sin(yawRad.toDouble()) * strafe).toFloat()
        val len = Math.sqrt((moveX * moveX + moveZ * moveZ).toDouble()).toFloat()
        if (len > 0f) {
            moveX = moveX / len * speed
            moveZ = moveZ / len * speed
        }
        return Pair(moveX, moveZ)
    }

    private fun handleDefault() {
        val player = mc.player ?: return
        if (player.onGround()) return

        val jumping = player.input.keyPresses.jump()
        val sneaking = player.input.keyPresses.shift()
        val speed = xzSpeed.getFloat()
        val ySpd = ySpeed.getFloat()

        val (moveX, moveZ) = calcMove(player.zza, player.xxa, player.yRot, speed)

        var motionY = player.deltaMovement.y
        if (jumping) {
            motionY = ySpd.toDouble()
        } else if (sneaking) {
            motionY = (-ySpd).toDouble()
        }

        player.setDeltaMovement(Vec3(moveX.toDouble(), motionY, moveZ.toDouble()))
    }

    private fun handleSpear() {
        val player = mc.player ?: return
        if (player.onGround()) return
        if (!hasSpear()) return

        val speed = xzSpeed.getFloat()

        val now = System.currentTimeMillis()
        if (now - lastAttackTime >= attackDelay && mc.gameMode != null) {
            player.connection.send(ServerboundSwingPacket(InteractionHand.MAIN_HAND))
            player.connection.send(ServerboundMovePlayerPacket.PosRot(
                player.x, player.y, player.z,
                player.yRot, player.xRot,
                player.onGround(), false
            ))
            lastAttackTime = now
            attackDelay = (30 + (Math.random() * 20).toInt()).toLong()
        }

        val (moveX, moveZ) = calcMove(player.zza, player.xxa, player.yRot, speed)
        player.setDeltaMovement(Vec3(
            moveX.toDouble(),
            player.deltaMovement.y,
            moveZ.toDouble()
        ))
    }

    private fun handleDragonFly() {
        val player = mc.player ?: return
        if (player.onGround()) return

        val forward = player.zza
        val strafe = player.xxa
        val jumping = player.input.keyPresses.jump()
        val sneaking = player.input.keyPresses.shift()
        val speed = xzSpeed.getFloat()
        val ySpd = ySpeed.getFloat()

        if (forward == 0f && strafe == 0f) {
            if (!jumping && !sneaking) {
                player.setDeltaMovement(Vec3(0.0, 0.0, 0.0))
            } else {
                player.setDeltaMovement(Vec3(0.0, player.deltaMovement.y, 0.0))
            }
        } else {
            val (moveX, moveZ) = calcMove(forward, strafe, player.yRot, speed)

            var motionY = player.deltaMovement.y
            if (jumping) {
                motionY = ySpd.toDouble()
            } else if (sneaking) {
                motionY = (-ySpd).toDouble()
            }

            player.setDeltaMovement(Vec3(moveX.toDouble(), motionY, moveZ.toDouble()))
        }
    }
}