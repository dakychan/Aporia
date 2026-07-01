package so.aporia.module.impl.combat

import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.settings.BooleanSetting
import so.aporia.module.settings.SelectSetting
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.PacketEvent
import so.aporia.utils.events.impl.TickEvent
import so.aporia.utils.imports.*


class Criticals : Module("Criticals", Category.COMBAT) {
    companion object {
        @JvmField val mc = Minecraft.getInstance()
    }

    val mode: SelectSetting
    val movingBypass: BooleanSetting
    val moveFucker: BooleanSetting

    private var matrixActive = false
    private var oldX = 0.0
    private var oldY = 0.0
    private var oldZ = 0.0
    private var matrixTimer = 0L
    private var critTicks = 0

    init {
        mode = SelectSetting("Mode", "Criticals mode")
            .value("NCP", "Matrix", "MatrixLasted", "MatrixNew")
            .selected("NCP")
        movingBypass = BooleanSetting("MovingBypass", "", false)
        moveFucker = BooleanSetting("MoveFucker", "", false)
    }

    override fun onEnable() {
        bus.register(this)
        critTicks = 0
        matrixActive = false
    }

    override fun onDisable() {
        bus.unregister(this)
        critTicks = 0
        matrixActive = false
    }

    @EventHandler
    fun onTick(event: TickEvent) {
        if (mc.player == null || mc.level == null) return

        if (critTicks > 0) critTicks--

        if (matrixActive && mode.isSelected("MatrixNew") && System.currentTimeMillis() - matrixTimer > 200L) {
            mc.player!!.setPos(oldX, oldY, oldZ)
            matrixActive = false
        }
    }

    @EventHandler
    fun onPacketSend(event: PacketEvent) {
        if (mc.player == null) return
        if (event.direction != PacketEvent.Direction.OUTBOUND) return
        if (event.packet !is ServerboundInteractPacket) return

        if (critTicks > 0) return
        critTicks = 2

        doCrit()
    }

    private fun doCrit() {
        val player = mc.player ?: return
        val level = mc.level ?: return

        when (mode.get()) {
            "NCP" -> {
                critPacket(0.0625)
                critPacket(0.0)
            }
            "Matrix" -> {
                critPacket(0.08)
                critPacket(0.021)
            }
            "MatrixLasted" -> {
                critPacket(-2.02E-4)
            }
            "MatrixNew" -> {
                if (!matrixActive) {
                    oldX = player.x
                    oldY = player.y
                    oldZ = player.z
                    val targetY = oldY + 0.5
                    if (level.noCollision(player, player.boundingBox.expandTowards(0.0, targetY - oldY, 0.0))) {
                        player.setPos(oldX, targetY, oldZ)
                    }
                    applyCritMethod(-0.0022, false)
                    applyCritMethod(-0.0032, false)
                    matrixTimer = System.currentTimeMillis()
                    matrixActive = true
                }
            }
        }
    }

    private fun applyCritMethod(yOffset: Double, onGround: Boolean) {
        val player = mc.player ?: return
        player.connection.send(ServerboundMovePlayerPacket.Pos(
            player.x, player.y + yOffset, player.z, onGround, false
        ))
    }

    private fun critPacket(yOffset: Double) {
        val player = mc.player ?: return
        player.connection.send(ServerboundMovePlayerPacket.Pos(
            player.x, player.y + yOffset, player.z, false, false
        ))
    }
}
