package so.aporia.utils.events.impl

import net.minecraft.world.entity.player.Player

class PlayerDeathEvent(val player: Player?) {
    val playerName: String = player?.name?.string ?: "unknown"

    fun player(): Player? = player
    fun playerName(): String = playerName
}
