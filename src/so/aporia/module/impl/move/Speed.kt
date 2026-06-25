package so.aporia.module.impl.move

import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.settings.NumberSetting
import so.aporia.module.settings.SelectSetting
import so.aporia.utils.events.EventBus
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.PacketEvent
import so.aporia.utils.events.impl.TickEvent

class Speed : Module("Speed", Category.MOVE) {
    companion object {
        @JvmField val mc = Minecraft.getInstance()
    }


    val mode: SelectSetting
    val boost: NumberSetting
    val grimDrift: NumberSetting

    private var lastX = 0.0
    private var lastY = 0.0
    private var lastZ = 0.0
    private var isFirstPacket = true
    private var wasMoving = false
    private var grimTick = 0

    init {
        mode = SelectSetting("Mode", "Speed mode")
            .value("Packet", "Grim")
            .selected("Packet")

        boost = NumberSetting("Boost", "Packet position multiplier (Packet mode)", 2.0, 1.0, 5.0, 0.1,
            { mode.isSelected("Packet") })

        grimDrift = NumberSetting("Grim Drift", "Blocks per tick drift (Grim mode)", 0.03, 0.005, 0.1, 0.005,
            { mode.isSelected("Grim") })
    }

    override fun onEnable() {
        EventBus.register(this)
        isFirstPacket = true
        wasMoving = false
        grimTick = 0
    }

    override fun onDisable() {
        EventBus.unregister(this)
        isFirstPacket = true
        wasMoving = false
        grimTick = 0
    }

    @EventHandler
    fun onTick(event: TickEvent) {
        if (mc.player == null) return

        grimTick++

        if (isFirstPacket) {
            lastX = mc.player!!.x
            lastY = mc.player!!.y
            lastZ = mc.player!!.z
            isFirstPacket = false
        }

        if (mode.isSelected("Grim")) {
            wasMoving = mc.player!!.input.moveVector.lengthSquared() >= 0.01f
        }
    }

    @EventHandler
    fun onPacketSend(event: PacketEvent) {
        if (event.direction != PacketEvent.Direction.OUTBOUND) return
        val packet = event.packet as? ServerboundMovePlayerPacket ?: return
        if (mc.player == null) return

        if (mode.isSelected("Packet")) {
            handlePacketMode(event, packet)
        } else if (mode.isSelected("Grim")) {
            handleGrimMode(event, packet)
        }
    }

    private fun handlePacketMode(event: PacketEvent, packet: ServerboundMovePlayerPacket) {
        if (!mc.player!!.onGround()) return
        if (mc.player!!.input.moveVector.lengthSquared() < 0.01f) return

        val vanillaX = packet.getX(lastX)
        val vanillaY = packet.getY(lastY)
        val vanillaZ = packet.getZ(lastZ)

        val deltaX = vanillaX - lastX
        val deltaZ = vanillaZ - lastZ
        val mult = boost.getFloat()

        val hackedX = lastX + deltaX * mult
        val hackedZ = lastZ + deltaZ * mult

        val hackedPacket: ServerboundMovePlayerPacket

        if (packet.hasPosition() && packet.hasRotation()) {
            hackedPacket = ServerboundMovePlayerPacket.PosRot(
                hackedX, vanillaY, hackedZ,
                packet.getYRot(mc.player!!.yRot),
                packet.getXRot(mc.player!!.xRot),
                packet.isOnGround(),
                packet.horizontalCollision()
            )
        } else if (packet.hasPosition()) {
            hackedPacket = ServerboundMovePlayerPacket.Pos(
                hackedX, vanillaY, hackedZ,
                packet.isOnGround(),
                packet.horizontalCollision()
            )
        } else {
            return
        }

        event.setPacket(hackedPacket)
        mc.player!!.setPos(hackedX, vanillaY, hackedZ)

        lastX = hackedX
        lastY = vanillaY
        lastZ = hackedZ
    }

    private fun handleGrimMode(event: PacketEvent, packet: ServerboundMovePlayerPacket) {
        if (!mc.player!!.onGround()) return
        if (!wasMoving) return

        val vanillaX = packet.getX(lastX)
        val vanillaY = packet.getY(lastY)
        val vanillaZ = packet.getZ(lastZ)

        var inputX = mc.player!!.input.moveVector.x.toDouble()
        var inputZ = mc.player!!.input.moveVector.y.toDouble()

        val len = Math.sqrt(inputX * inputX + inputZ * inputZ)
        if (len < 1e-6) return

        inputX /= len
        inputZ /= len

        val drift = grimDrift.getFloat()
        val driftedX = vanillaX + inputX * drift
        val driftedZ = vanillaZ + inputZ * drift

        val hackedPacket: ServerboundMovePlayerPacket

        if (packet.hasPosition() && packet.hasRotation()) {
            hackedPacket = ServerboundMovePlayerPacket.PosRot(
                driftedX, vanillaY, driftedZ,
                packet.getYRot(mc.player!!.yRot),
                packet.getXRot(mc.player!!.xRot),
                packet.isOnGround(),
                packet.horizontalCollision()
            )
        } else if (packet.hasPosition()) {
            hackedPacket = ServerboundMovePlayerPacket.Pos(
                driftedX, vanillaY, driftedZ,
                packet.isOnGround(),
                packet.horizontalCollision()
            )
        } else {
            return
        }

        event.setPacket(hackedPacket)
        mc.player!!.setPos(driftedX, vanillaY, driftedZ)

        lastX = driftedX
        lastY = vanillaY
        lastZ = driftedZ
    }
}
