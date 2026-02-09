package zenith.zov.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.Items;


import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import zenith.zov.base.events.impl.player.EventRotate;
import zenith.zov.base.events.impl.player.EventUpdate;
import zenith.zov.base.request.ScriptManager;
import zenith.zov.client.hud.elements.component.InventoryComponent;
import zenith.zov.client.modules.api.Category;
import zenith.zov.client.modules.api.Module;
import zenith.zov.client.modules.api.ModuleAnnotation;
import zenith.zov.client.modules.api.setting.impl.BooleanSetting;
import zenith.zov.client.modules.api.setting.impl.NumberSetting;
import zenith.zov.utility.game.other.InventoryUtil;
import zenith.zov.utility.game.other.MessageUtil;
import zenith.zov.utility.game.player.MovingUtil;
import zenith.zov.utility.game.player.PlayerInventoryComponent;
import zenith.zov.utility.game.player.PlayerInventoryUtil;

import static net.minecraft.item.Items.TOTEM_OF_UNDYING;

@ModuleAnnotation(
        name = "AutoTotem",
        category = Category.COMBAT, description = "При условиях берет тотем в руку"
)
public final class AutoTotem extends Module {
    public static final AutoTotem INSTANCE = new AutoTotem();

    private AutoTotem() {
    }

    private final NumberSetting health = new NumberSetting("Здоровье", 5f, 0, 36, 0.1f);

    private final BooleanSetting elytra = new BooleanSetting("Элитры", true);
    private final NumberSetting elytraHealth = new NumberSetting("На элитрах", 10f, 0, 36, 0.1f, elytra::isEnabled);

    private final BooleanSetting fall = new BooleanSetting("Падение", true);
    private final NumberSetting fallDistance = new NumberSetting("При падении", 20f, 10, 50, 0.1f, fall::isEnabled);

    private int cooldownTicks = 0;
    private Item previousItem = null;

    @EventTarget
    public void onPlayerTick(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) return;

        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }

        final Item current = mc.player.getOffHandStack().isEmpty()
                ? null : mc.player.getOffHandStack().getItem();

        if (shouldUseTotem()) {
            if (current != TOTEM_OF_UNDYING) {

                Slot slot = PlayerInventoryUtil.getSlot(TOTEM_OF_UNDYING);
                if (slot != null) {
                    swapToOffhand(slot);
                    previousItem = current;
                }
            }
        } else if (current == TOTEM_OF_UNDYING && previousItem != null) {
            Slot slot = PlayerInventoryUtil.getSlot(previousItem);
            if (slot != null) {
                swapToOffhand(slot);
            }
            previousItem = null;
        }
    }

    private void swapToOffhand(Slot slot) {
        PlayerInventoryComponent.addTask( ()-> {
            PlayerInventoryUtil.swapHand(slot, Hand.OFF_HAND,false);
            PlayerInventoryUtil.closeScreen(true);
        });
        cooldownTicks = 0;

    }

    private boolean shouldUseTotem() {
        float healthValue = mc.player.getHealth() + mc.player.getAbsorptionAmount();

        if (healthValue <= health.getCurrent()) return true;

        if (fall.isEnabled() && mc.player.fallDistance >= fallDistance.getCurrent()) return true;

        return elytra.isEnabled()
                && mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA
                && healthValue <= elytraHealth.getCurrent();
    }
}
