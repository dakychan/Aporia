package so.aporia.module.impl.combat

import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket
import net.minecraft.world.item.Items
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.settings.BooleanSetting
import so.aporia.module.settings.SliderSetting
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
class AutoTotem : Module("AutoTotem", Category.COMBAT) {

    val fallCheck: BooleanSetting
    val fallDistance: SliderSetting
    val crystalCheck: BooleanSetting
    val crystalRange: SliderSetting
    val hpCheck: BooleanSetting
    val hpThreshold: SliderSetting

    private enum class Phase { IDLE, SWAP_TOTEM, WATCHING, RESTORE }
    private val sm = StateMachineEngine(this, Phase::class, Phase.IDLE)

    private object TotemSwapped
    private object TotemPopped

    private var restoreSlot = -1
    private var hadTotem = false

    init {
        fallCheck = BooleanSetting("Fall Check", "Swap totem when falling from lethal height", true)
        fallDistance = SliderSetting("Fall Distance", "Minimum fall distance to trigger", 10.0, 3.0, 50.0, 1.0,
            { fallCheck.isEnabled })

        crystalCheck = BooleanSetting("Crystal Check", "Swap totem when near end crystal", true)
        crystalRange = SliderSetting("Crystal Range", "Max distance to crystal", 6.0, 1.0, 12.0, 1.0,
            { crystalCheck.isEnabled })

        hpCheck = BooleanSetting("HP Check", "Swap totem when health is low", true)
        hpThreshold = SliderSetting("HP Threshold", "Health threshold to trigger", 6.0, 1.0, 20.0, 1.0,
            { hpCheck.isEnabled })
    }

    /* ── transitions ──────────────────────────────────── */
    @StateMachineRegister(from = "SWAP_TOTEM", to = "WATCHING", on = TotemSwapped::class)
    @StateMachineRegister(from = "WATCHING", to = "RESTORE", on = TotemPopped::class)
    @StateMachineRegister(from = "WATCHING", to = "IDLE", on = TickEvent::class)
    @StateMachineRegister(from = "RESTORE", to = "IDLE", on = TickEvent::class)
    @StateMachineRegister(from = "IDLE", to = "SWAP_TOTEM", on = TickEvent::class)
    private fun trans() {}

    override val settings = listOf(fallCheck, fallDistance, crystalCheck, crystalRange, hpCheck, hpThreshold)

    override fun onEnable() {
        bus.register(this)
        restoreSlot = -1; hadTotem = false
    }

    override fun onDisable() {
        bus.unregister(this)
        if (sm.state() != Phase.IDLE) {
            if (restoreSlot >= 0) {
                val pl = mc.player
                if (pl != null) {
                    val sel = pl.inventory.selectedSlot
                    mc.connection?.send(ServerboundSetCarriedItemPacket(restoreSlot))
                    pl.inventory.selectedSlot = restoreSlot
                    InventoryUtil.sendSwapPacket()
                    if (sel != restoreSlot) {
                        mc.connection?.send(ServerboundSetCarriedItemPacket(sel))
                        pl.inventory.selectedSlot = sel
                    }
                }
            }
            sm.currentState = Phase.IDLE
        }
        restoreSlot = -1
    }

    @EventHandler
    fun onTick(event: TickEvent) {
        val pl = mc.player ?: return
        if (mc.level == null) return

        val oh = pl.offhandItem
        val hasTotemNow = !oh.isEmpty && oh.item === Items.TOTEM_OF_UNDYING

        when (sm.state()) {
            Phase.IDLE -> {
                if (hasTotemNow) return
                if (!needsTotem()) { hadTotem = false; return }

                val slot = InventoryUtil.findItemInInventory(Items.TOTEM_OF_UNDYING)
                if (slot == -1) { hadTotem = false; return }

                val inv = pl.inventory
                val savedSel = inv.selectedSlot
                val hotbarSlot: Int

                if (slot < 9) {
                    hotbarSlot = slot
                } else {
                    var buf = InventoryUtil.findEmptyHotbarSlot()
                    if (buf == -1) buf = savedSel
                    InventoryUtil.moveSlotToHotbar(slot, buf)
                    hotbarSlot = buf
                }

                // Save original offhand position
                if (!oh.isEmpty && oh.item !== Items.TOTEM_OF_UNDYING) {
                    restoreSlot = hotbarSlot
                } else {
                    restoreSlot = -1
                }

                mc.connection?.send(ServerboundSetCarriedItemPacket(hotbarSlot))
                inv.selectedSlot = hotbarSlot
                InventoryUtil.sendSwapPacket()

                if (savedSel != hotbarSlot) {
                    mc.connection?.send(ServerboundSetCarriedItemPacket(savedSel))
                    inv.selectedSlot = savedSel
                }

                hadTotem = true
                sm.transition(event) // → SWAP_TOTEM
            }

            Phase.SWAP_TOTEM -> {
                hadTotem = true
                sm.transition(TotemSwapped) // → WATCHING
            }

            Phase.WATCHING -> {
                if (!hasTotemNow && hadTotem) {
                    // Totem popped — restore original offhand
                    hadTotem = false
                    if (restoreSlot >= 0) {
                        sm.transition(TotemPopped) // → RESTORE
                        return
                    }
                }
                hadTotem = hasTotemNow
                // If no totem and needs new one → transition back via TickEvent to IDLE
                if (!hasTotemNow) {
                    sm.transition(event) // → IDLE
                }
            }

            Phase.RESTORE -> {
                if (restoreSlot >= 0) {
                    val sel = mc.player!!.inventory.selectedSlot
                    mc.connection?.send(ServerboundSetCarriedItemPacket(restoreSlot))
                    mc.player!!.inventory.selectedSlot = restoreSlot
                    InventoryUtil.sendSwapPacket()
                    if (sel != restoreSlot) {
                        mc.connection?.send(ServerboundSetCarriedItemPacket(sel))
                        mc.player!!.inventory.selectedSlot = sel
                    }
                    restoreSlot = -1
                }
                sm.transition(event) // → IDLE
            }
        }
    }

    private fun needsTotem(): Boolean {
        val pl = mc.player ?: return false

        if (hpCheck.isEnabled) {
            val hp = pl.health + pl.absorptionAmount
            if (hp <= hpThreshold.getFloat()) return true
        }
        if (fallCheck.isEnabled) {
            if (pl.fallDistance >= fallDistance.getFloat()) return true
        }
        if (crystalCheck.isEnabled && mc.level != null) {
            val range = crystalRange.getFloat()
            for (entity in mc.level!!.entitiesForRendering()) {
                if (entity.type == net.minecraft.world.entity.EntityTypes.END_CRYSTAL
                    && pl.distanceTo(entity) <= range) return true
            }
        }
        return false
    }
}
