package so.aporia.utils.user.player.inventory

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket
import net.minecraft.core.component.DataComponents
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.item.*
import so.aporia.utils.imports.*
import com.chaos.annotation.ChaosNative

/**
 * Inventory utility methods for combat modules.
 * Handles offhand swap, item search, and hand state checks.
 */
@ChaosNative
object InventoryUtil {

    private var lastSwapTime = 0L
    private const val SWAP_COOLDOWN_MS = 200L

    // ── Item search ──

    @JvmStatic
    fun findItemInInventory(item: Item): Int {
        val inv = mc.player?.inventory ?: return -1
        for (i in 0 until 36) if (!inv.getItem(i).isEmpty && inv.getItem(i).`is`(item)) return i
        return -1
    }

    @JvmStatic
    fun findItemInHotbar(item: Item): Int {
        val inv = mc.player?.inventory ?: return -1
        for (i in 0 until 9) if (!inv.getItem(i).isEmpty && inv.getItem(i).`is`(item)) return i
        return -1
    }

    @JvmStatic
    fun offhandHas(item: Item): Boolean {
        val offhand = mc.player?.offhandItem ?: return false
        return !offhand.isEmpty && offhand.`is`(item)
    }

    @JvmStatic
    fun offhandHasAny(vararg items: Item): Boolean {
        val offhand = mc.player?.offhandItem ?: return false
        if (offhand.isEmpty) return false
        return items.any { offhand.`is`(it) }
    }

    @JvmStatic
    fun findItemInInventoryAny(vararg items: Item): Int {
        val inv = mc.player?.inventory ?: return -1
        for (i in 0 until 36) {
            val stack = inv.getItem(i)
            if (!stack.isEmpty && items.any { stack.`is`(it) }) return i
        }
        return -1
    }

    @JvmStatic
    fun findEmptyHotbarSlot(): Int {
        val inv = mc.player?.inventory ?: return -1
        for (i in 0 until 9) if (inv.getItem(i).isEmpty) return i
        return -1
    }

    // ── Hand state checks ──

    /** Items that block auto-eat: food, bow, trident, shield, blocks */
    private val handBlockingItems = listOf(
        Items.BOW, Items.CROSSBOW, Items.TRIDENT,
        Items.SHIELD,
        Items.BUCKET, Items.WATER_BUCKET, Items.LAVA_BUCKET,
        Items.MILK_BUCKET,
        Items.FLINT_AND_STEEL, Items.ENDER_PEARL, Items.ENDER_EYE,
        Items.EXPERIENCE_BOTTLE,
        Items.SPYGLASS, Items.CLOCK, Items.COMPASS
    )

    /** Check if mainhand holds a non-empty item that is NOT a weapon/tool the player might want */
    @JvmStatic
    fun mainhandHasBlockingItem(): Boolean {
        val stack = mc.player?.mainHandItem ?: return false
        if (stack.isEmpty) return false
        // Food items block auto-eat
        if (stack.has(DataComponents.FOOD)) return true
        // Bows, tridents, shields block auto-eat
        return handBlockingItems.any { stack.`is`(it) }
    }

    /** Check if mainhand holds a placeable block */
    @JvmStatic
    fun mainhandHasBlock(): Boolean {
        val stack = mc.player?.mainHandItem ?: return false
        return !stack.isEmpty && stack.item is BlockItem
    }

    /** Check if player is looking at a target entity */
    @JvmStatic
    fun isLookingAtTarget(): Boolean {
        val target = mc.crosshairPickEntity ?: return false
        return target is net.minecraft.world.entity.LivingEntity
    }

    /** Check if player is in the air (not on ground) */
    @JvmStatic
    fun isInAir(): Boolean {
        return mc.player?.onGround() == false
    }

    /** Should auto-eat be allowed? Only when looking at target or in air, and no blocking items in hand */
    @JvmStatic
    fun canAutoEat(): Boolean {
        if (mainhandHasBlockingItem()) return false
        if (mainhandHasBlock()) return false
        return isLookingAtTarget() || isInAir()
    }

    // ── Offhand swap ──

    /**
     * Directly swaps inventory slot with offhand (slot 40) via ContainerInput.SWAP.
     * No hotbar intermediate — item goes straight to offhand.
     */
    @JvmStatic
    fun swapToOffhand(inventorySlot: Int) {
        if (inventorySlot < 0) return
        val now = System.currentTimeMillis()
        if (now - lastSwapTime < SWAP_COOLDOWN_MS) return
        lastSwapTime = now

        val player = mc.player ?: return
        val gameMode = mc.gameMode ?: return
        // ContainerInput.SWAP with button=40 swaps the slot directly with offhand
        gameMode.handleContainerInput(player.containerMenu.containerId, inventorySlot, 40, ContainerInput.SWAP, player)
    }

    @JvmStatic
    fun swapFromOffhand() {
        sendSwapPacket()
    }

    @JvmStatic
    fun canSwap(): Boolean {
        return System.currentTimeMillis() - lastSwapTime >= SWAP_COOLDOWN_MS
    }

    @JvmStatic
    fun sendSwapPacket() {
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
        gameMode.handleContainerInput(player.containerMenu.containerId, fromSlot, toHotbarSlot, ContainerInput.SWAP, player)
    }
}
