package so.aporia.utils.packets

import com.chaos.annotation.Obfuscate
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.util.Mth
import so.aporia.utils.events.impl.PacketEvent
import so.aporia.utils.imports.*
import so.aporia.utils.user.player.rotation.RotationUtil

@Obfuscate
object PacketInterceptor {
    private var lastSentX = 0.0
    private var lastSentY = 0.0
    private var lastSentZ = 0.0
    private var lastSentYaw = 0f
    private var hasLastSent = false
    private var sending = false

    @JvmStatic
    fun onPacketSend(event: PacketEvent) {
        if (sending) return
        if (event.direction() != PacketEvent.Direction.OUTBOUND) return
        if (!RotationUtil.isActive()) return
        val pkt = event.packet()
        if (pkt !is ServerboundMovePlayerPacket) return
        if (mc.player == null) return

        val serverYaw = RotationUtil.getServerYaw()
        val serverPitch = RotationUtil.getServerPitch()
        val onGround = pkt.isOnGround

        if (!hasLastSent) {
            lastSentX = mc.player!!.x
            lastSentY = mc.player!!.y
            lastSentZ = mc.player!!.z
            lastSentYaw = mc.player!!.yRot
            hasLastSent = true
        }

        val currentX = mc.player!!.x
        val currentY = mc.player!!.y
        val currentZ = mc.player!!.z

        val deltaX = currentX - lastSentX
        val deltaZ = currentZ - lastSentZ

        val realYaw = mc.player!!.yRot
        val yawDiff = Mth.wrapDegrees(serverYaw - realYaw)

        if (pkt.hasPosition() && pkt.hasRotation()) {
            if (Math.abs(yawDiff) > 0.5f) {
                val rad = Math.toRadians(yawDiff.toDouble())
                val cos = Math.cos(rad)
                val sin = Math.sin(rad)

                val rotatedDx = deltaX * cos - deltaZ * sin
                val rotatedDz = deltaX * sin + deltaZ * cos

                val newX = lastSentX + rotatedDx
                val newZ = lastSentZ + rotatedDz

                lastSentX = newX
                lastSentY = currentY
                lastSentZ = newZ
                lastSentYaw = serverYaw

                event.cancel()
                sending = true
                mc.player!!.connection.send(
                    ServerboundMovePlayerPacket.PosRot(
                        newX, currentY, newZ,
                        serverYaw, serverPitch,
                        pkt.isOnGround, false
                    )
                )
                sending = false
            } else {
                event.cancel()
                sending = true
                mc.player!!.connection.send(
                    ServerboundMovePlayerPacket.PosRot(
                        currentX, currentY, currentZ,
                        serverYaw, serverPitch,
                        pkt.isOnGround, false
                    )
                )
                sending = false

                lastSentX = currentX
                lastSentY = currentY
                lastSentZ = currentZ
                lastSentYaw = serverYaw
            }
        } else if (pkt.hasRotation()) {
            event.cancel()
            sending = true
            mc.player!!.connection.send(
                ServerboundMovePlayerPacket.Rot(
                    serverYaw, serverPitch,
                    pkt.isOnGround, false
                )
            )
            sending = false
            lastSentYaw = serverYaw
        } else if (pkt.hasPosition()) {
            lastSentX = currentX
            lastSentY = currentY
            lastSentZ = currentZ
        }
    }

    @JvmStatic
    fun reset() {
        hasLastSent = false
        lastSentX = 0.0
        lastSentY = 0.0
        lastSentZ = 0.0
        lastSentYaw = 0f
    }
}
