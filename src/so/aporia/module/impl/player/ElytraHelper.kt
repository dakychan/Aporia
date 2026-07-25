package so.aporia.module.impl.player

import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.settings.BindSetting
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.KeyInputEvent
import so.aporia.utils.events.impl.KeyInputEvent.Action
import net.minecraft.client.Minecraft
import so.aporia.utils.imports.*
import com.chaos.annotation.ChaosNative
@ChaosNative
class ElytraHelper : Module("ElytraHelper", Category.PLAYER) {
    companion object {
        @JvmField val mc = Minecraft.getInstance()
        private const val CHESTPLATE_SLOT_ID = 6
    }

    val elytraSwapBind = BindSetting("Elytra Swap", "Keybind to swap elytra with chestplate", -1)
    val fireworkBind = BindSetting("Firework", "Keybind to use a firework rocket", -1)

    private var fireworkSlot = -1
    private var oldSlot = -1
    private var waitTicks = 0

    override val settings = listOf(elytraSwapBind, fireworkBind)

    override fun onEnable() {
        bus.register(this)
    }

    override fun onDisable() {
        bus.unregister(this)
        waitTicks = 0
    }

    @EventHandler
    fun onKey(event: KeyInputEvent) {
        if (mc.player == null || event.action != Action.PRESS) return

        if (elytraSwapBind.isBound() && event.scancode() == elytraSwapBind.getKey()) {
            swapElytra()
        }
        if (fireworkBind.isBound() && event.scancode() == fireworkBind.getKey()) {
            useFirework()
        }
    }

    @EventHandler
    fun onTick() {
        // Эту хуйню надо вызвать, если у тебя нет TickEvent
    }

    // Если у тебя есть отдельный TickEvent, перемести логику ожидания туда.
    // Я предполагаю, что ты можешь добавить обработку тика:
    @EventHandler
    fun onTick(event: so.aporia.utils.events.impl.TickEvent) {
        if (waitTicks > 0) {
            waitTicks--
            if (waitTicks == 0 && fireworkSlot != -1) {
                val gm = mc.gameMode
                val pl = mc.player
                if (gm != null && pl != null && pl.isFallFlying) {
                    gm.useItem(pl, InteractionHand.MAIN_HAND)
                }

                // Возвращаем старый слот обратно
                mc.connection?.send(ServerboundSetCarriedItemPacket(oldSlot))
                fireworkSlot = -1
            }
        }
    }

    private fun isElytra(stack: ItemStack): Boolean {
        return stack.`is`(Items.ELYTRA)
    }

    private fun isFirework(stack: ItemStack): Boolean {
        return stack.`is`(Items.FIREWORK_ROCKET)
    }

    private fun swapElytra() {
        val player = mc.player ?: return
        val gameMode = mc.gameMode ?: return

        val chestItem = player.getItemBySlot(EquipmentSlot.CHEST)

        if (isElytra(chestItem)) {
            gameMode.handleContainerInput(player.containerMenu.containerId, CHESTPLATE_SLOT_ID, 0, ContainerInput.QUICK_MOVE, player)
        } else {
            for (i in 9 until 45) {
                val slotItem = player.containerMenu.getSlot(i).item
                if (isElytra(slotItem)) {
                    gameMode.handleContainerInput(player.containerMenu.containerId, i, 0, ContainerInput.QUICK_MOVE, player)
                    return
                }
            }
        }
    }

    private fun useFirework() {
        val player = mc.player ?: return
        if (!player.isFallFlying) return

        for (i in 0 until 9) {
            val invItem = player.inventory.getItem(i)
            if (isFirework(invItem)) {
                oldSlot = player.inventory.selectedSlot

                // ОЧЕНЬ ВАЖНО: Отправляем пакет смены слота на сервер!
                mc.connection?.send(ServerboundSetCarriedItemPacket(i))

                // Ждем 1 тик, чтобы сервер успел обработать смену слота
                fireworkSlot = i
                waitTicks = 1
                return
            }
        }
    }
}