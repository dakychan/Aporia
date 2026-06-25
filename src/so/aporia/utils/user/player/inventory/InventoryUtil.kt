package so.aporia.utils.user.player.inventory

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.world.inventory.ClickType

object InventoryUtil {

    private val mc = Minecraft.getInstance()

    @JvmStatic
    fun findItemInInventory(item: net.minecraft.world.item.Item): Int {
        val player = mc.player ?: return -1
        val inv = player.inventory
        for (i in 0 until 36) {
            if (!inv.getItem(i).isEmpty && inv.getItem(i).`is`(item)) return i
        }
        return -1
    }

    @JvmStatic
    fun findItemInHotbar(item: net.minecraft.world.item.Item): Int {
        val player = mc.player ?: return -1
        val inv = player.inventory
        for (i in 0 until 9) {
            if (!inv.getItem(i).isEmpty && inv.getItem(i).`is`(item)) return i
        }
        return -1
    }

    @JvmStatic
    fun offhandHas(item: net.minecraft.world.item.Item): Boolean {
        val player = mc.player ?: return false
        val offhand = player.offhandItem
        return !offhand.isEmpty && offhand.`is`(item)
    }

    @JvmStatic
    fun findEmptyHotbarSlot(): Int {
        val player = mc.player ?: return -1
        val inv = player.inventory
        for (i in 0 until 9) {
            if (inv.getItem(i).isEmpty) return i
        }
        return -1
    }

    @JvmStatic
    fun swapToOffhand(inventorySlot: Int) {
        val player = mc.player ?: return
        if (inventorySlot < 0) return

        val inv = player.inventory
        if (inventorySlot < 9) {
            inv.selectedSlot = inventorySlot
            sendSwapPacket()
        } else {
            var hotbarSlot = findEmptyHotbarSlot()
            if (hotbarSlot == -1) hotbarSlot = inv.selectedSlot
            moveSlotToHotbar(inventorySlot, hotbarSlot)
            inv.selectedSlot = hotbarSlot
            sendSwapPacket()
        }
    }

    @JvmStatic
    fun swapFromOffhand() {
        sendSwapPacket()
    }

    @JvmStatic
    fun sendSwapPacket() {
        val player = mc.player ?: return
        mc.connection?.send(
            ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                BlockPos.ZERO, Direction.DOWN
            )
        )
    }

    @JvmStatic
    fun moveSlotToHotbar(fromSlot: Int, toHotbarSlot: Int) {
        val player = mc.player ?: return
        val gameMode = mc.gameMode ?: return
        gameMode.handleInventoryMouseClick(player.containerMenu.containerId, fromSlot, toHotbarSlot, ClickType.SWAP, player)
    }
}
