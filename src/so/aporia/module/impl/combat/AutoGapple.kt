package so.aporia.module.impl.combat

import com.chaos.annotation.Obfuscate
import net.minecraft.world.InteractionHand
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.item.Items
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.settings.BooleanSetting
import so.aporia.module.settings.NumberSetting
import so.aporia.module.settings.SelectSetting
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.TickEvent
import net.minecraft.client.Minecraft
import so.aporia.utils.imports.*
import so.aporia.utils.user.player.inventory.InventoryUtil

@Obfuscate
class AutoGapple : Module("AutoGapple", Category.COMBAT) {
    companion object {
        @JvmField val mc = Minecraft.getInstance()
    }

    val mode: SelectSetting
    val healthThreshold: NumberSetting
    val backItem: BooleanSetting

    private var wasUsePressed = false
    private var hasEaten = false
    private var eatQueued = false
    private var waitTicks = 0

    init {
        mode = SelectSetting("Mode", "AutoGapple mode")
            .value("Safe", "Always", "Off")
            .selected("Safe")

        healthThreshold = NumberSetting(
            "Health", "Health threshold for Safe mode auto-eat",
            8.0, 2.0, 20.0, 0.5,
            { mode.isSelected("Safe") })

        backItem = BooleanSetting(
            "Back Item", "Return previous offhand item after eating", true,
            { mode.isSelected("Safe") })
    }

    override fun onEnable() {
        bus.register(this)
        wasUsePressed = false
        hasEaten = false
        eatQueued = false
        waitTicks = 0
    }

    override fun onDisable() {
        bus.unregister(this)
        wasUsePressed = false
        hasEaten = false
        eatQueued = false
        waitTicks = 0
    }

    @EventHandler
    fun onTick(event: TickEvent) {
        if (mc.player == null) return

        if (mc.player!!.isUsingItem()) return

        if (hasEaten) {
            if (backItem.isEnabled && !mode.get().equals("Always")) {
                InventoryUtil.swapFromOffhand()
            }
            hasEaten = false
            return
        }

        var shouldEat = eatQueued

        val rightClick = mc.options.keyUse.isDown && !wasUsePressed
        wasUsePressed = mc.options.keyUse.isDown
        if (rightClick) {
            shouldEat = true
            eatQueued = true
        }

        val currentMode = mode.get()
        if (!shouldEat && currentMode == "Safe") {
            val health = mc.player!!.health + mc.player!!.absorptionAmount
            if (health <= healthThreshold.getFloat()) {
                shouldEat = true
                eatQueued = true
            }
        }

        if (!shouldEat && currentMode == "Always") {
            if (!mc.player!!.hasEffect(MobEffects.ABSORPTION)) {
                shouldEat = true
                eatQueued = true
            }
        }

        if (!shouldEat) return

        if (waitTicks > 0) {
            waitTicks--
            return
        }

        if (!InventoryUtil.offhandHas(Items.GOLDEN_APPLE)) {
            val gappleSlot = InventoryUtil.findItemInInventory(Items.GOLDEN_APPLE)
            if (gappleSlot == -1) {
                eatQueued = false
                return
            }
            InventoryUtil.swapToOffhand(gappleSlot)
            waitTicks = 1
            return
        }

        val gm = mc.gameMode
        val pl = mc.player
        if (gm != null && pl != null) {
            gm.useItem(pl, InteractionHand.OFF_HAND)
            hasEaten = true
            eatQueued = false
            waitTicks = 0
        }
    }
}