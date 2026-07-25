package so.aporia.module.impl.combat

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

/**
 * AutoGapple — auto-eats golden apples.
 *
 * Modes:
 * - Always: keeps golden apple in offhand at all times
 * - Safe: eats when HP is low OR right-click held, with safety checks
 *
 * Uses direct inventory↔offhand swap via ContainerInput.SWAP(slot=40).
 */
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
        if (sm.state() == Phase.EATING) {
            // Eating was interrupted — restore offhand
            if (savedSlot >= 0) InventoryUtil.swapToOffhand(savedSlot)
        }
        sm.currentState = Phase.IDLE
        reset()
    }

    private fun reset() {
        waitTicks = 0; savedSlot = -1
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

                // Skip if holding non-eatable items (food/bow/shield/blocks)
                if (!InventoryUtil.canAutoEat()) return

                // Already have apple in offhand — eat directly
                if (InventoryUtil.offhandHasAny(Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE)) {
                    mc.gameMode!!.useItem(pl, InteractionHand.OFF_HAND)
                    waitTicks = 5
                    return
                }

                val slot = InventoryUtil.findItemInInventoryAny(Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE)
                if (slot == -1) return

                // Save original offhand for restore
                savedSlot = pl.inventory.selectedSlot

                // Transition to SWAP_IN (on next tick)
                sm.transition(event)
                waitTicks = 1
            }

            Phase.SWAP_IN -> {
                // Direct swap apple into offhand (no hotbar)
                InventoryUtil.swapToOffhand(InventoryUtil.findItemInInventoryAny(Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE))
                // Start eating
                mc.gameMode!!.useItem(mc.player!!, InteractionHand.OFF_HAND)

                sm.transition(AppleInOffhand) // → EATING
                waitTicks = 2
            }

            Phase.EATING -> {
                if (mc.player!!.isUsingItem) {
                    waitTicks = 5; return
                }
                // Eating complete — restore original offhand item
                if (restoreSlot.isEnabled && savedSlot >= 0) {
                    InventoryUtil.swapToOffhand(savedSlot)
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
