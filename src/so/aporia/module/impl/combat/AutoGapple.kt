package so.aporia.module.impl.combat

import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.Items
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.settings.BooleanSetting
import so.aporia.module.settings.SliderSetting
import so.aporia.module.settings.SelectSetting
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.StateMachine
import so.aporia.utils.events.StateMachineEngine
import so.aporia.utils.events.StateMachineRegister
import so.aporia.utils.events.impl.TickEvent
import so.aporia.utils.imports.*
import so.aporia.utils.user.player.inventory.InventoryUtil
import com.chaos.annotation.ChaosNative
@StateMachine
@ChaosNative
class AutoGapple : Module("AutoGapple", Category.COMBAT) {

    private val mode: SelectSetting
    private val healthThreshold: SliderSetting
    private val restoreSlot: BooleanSetting

    private enum class Phase { IDLE, SWAP_IN, EATING, SWAP_OUT }
    private val sm = StateMachineEngine(this, Phase::class, Phase.IDLE)

    private object AppleInOffhand
    private object EatingDone

    private var waitTicks = 0
    private var savedSlot = -1
    private var bufferSlot = -1

    private val apples = listOf(Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE)

    init {
        mode = SelectSetting("Mode", "AutoGapple mode")
            .value("Safe", "Always", "Off")
            .selected("Safe")
        healthThreshold = SliderSetting("Health", "Health threshold for Safe mode", 8.0, 2.0, 20.0, 0.5,
            { mode.isSelected("Safe") })
        restoreSlot = BooleanSetting("Restore Slot", "Return to previous hotbar slot after eating", true,
            { mode.isSelected("Safe") })
    }

    /* ── state transitions ───────────────────────────── */
    @StateMachineRegister(from = "IDLE", to = "SWAP_IN", on = TickEvent::class)
    @StateMachineRegister(from = "SWAP_IN", to = "EATING", on = AppleInOffhand::class)
    @StateMachineRegister(from = "EATING", to = "SWAP_OUT", on = EatingDone::class)
    @StateMachineRegister(from = "SWAP_OUT", to = "IDLE", on = TickEvent::class)
    private fun trans() {}
    override fun onEnable() { bus.register(this); reset() }
    override val settings = listOf(mode, healthThreshold, restoreSlot)

    override fun onDisable() {
        bus.unregister(this)
        if (sm.state() != Phase.IDLE) {
            if (sm.state() == Phase.EATING) {
                mc.connection?.send(ServerboundSetCarriedItemPacket(bufferSlot))
                mc.player?.inventory?.selectedSlot = bufferSlot
                InventoryUtil.sendSwapPacket()
                if (restoreSlot.isEnabled && savedSlot >= 0) {
                    mc.connection?.send(ServerboundSetCarriedItemPacket(savedSlot))
                    mc.player?.inventory?.selectedSlot = savedSlot
                }
            }
            sm.currentState = Phase.IDLE
        }
        reset()
    }

    private fun reset() {
        waitTicks = 0; savedSlot = -1; bufferSlot = -1
    }

    @EventHandler
    fun onTick(event: TickEvent) {
        val pl = mc.player ?: return
        if (mc.level == null || mc.gameMode == null) return
        if (waitTicks > 0) { waitTicks--; return }

        if (mode.isSelected("Off")) return

        // Always mode — keep gapple in offhand
        if (mode.isSelected("Always")) {
            if (!InventoryUtil.offhandHasAny(Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE)) {
                val slot = InventoryUtil.findItemInInventoryAny(Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE)
                if (slot != -1) InventoryUtil.swapToOffhand(slot)
            }
            return
        }

        // Safe mode — state machine
        when (sm.state()) {
            Phase.IDLE -> {
                if (pl.isUsingItem) return
                val hp = pl.health + pl.absorptionAmount
                val rightClick = mc.options.keyUse.isDown
                if (hp > healthThreshold.getFloat() && !rightClick) return

                // Already have apple in offhand — eat directly
                if (InventoryUtil.offhandHasAny(Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE)) {
                    mc.gameMode!!.useItem(pl, InteractionHand.OFF_HAND)
                    waitTicks = 5
                    return
                }

                val slot = InventoryUtil.findItemInInventoryAny(Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE)
                if (slot == -1) return

                savedSlot = pl.inventory.selectedSlot
                if (slot < 9) {
                    bufferSlot = slot
                } else {
                    var buf = InventoryUtil.findEmptyHotbarSlot()
                    if (buf == -1) buf = savedSlot
                    InventoryUtil.moveSlotToHotbar(slot, buf)
                    bufferSlot = buf
                }

                // Transition to SWAP_IN (on next tick)
                sm.transition(event)
                waitTicks = 1
            }

            Phase.SWAP_IN -> {
                if (bufferSlot < 0) { sm.transition(EatingDone); return }

                mc.connection?.send(ServerboundSetCarriedItemPacket(bufferSlot))
                mc.player!!.inventory.selectedSlot = bufferSlot
                InventoryUtil.sendSwapPacket()
                // Apple is now in offhand — start eating
                mc.gameMode!!.useItem(mc.player!!, InteractionHand.OFF_HAND)

                sm.transition(AppleInOffhand) // → EATING
                waitTicks = 2
            }

            Phase.EATING -> {
                if (mc.player!!.isUsingItem) {
                    waitTicks = 5; return
                }
                // Eating complete — restore original offhand
                if (bufferSlot >= 0) {
                    mc.connection?.send(ServerboundSetCarriedItemPacket(bufferSlot))
                    mc.player!!.inventory.selectedSlot = bufferSlot
                }
                InventoryUtil.sendSwapPacket()
                if (restoreSlot.isEnabled && savedSlot >= 0) {
                    mc.connection?.send(ServerboundSetCarriedItemPacket(savedSlot))
                    mc.player!!.inventory.selectedSlot = savedSlot
                }

                sm.transition(EatingDone) // → SWAP_OUT → IDLE on next tick
            }

            Phase.SWAP_OUT -> {
                sm.transition(event) // → IDLE on this tick
                reset()
            }
        }
    }
}
