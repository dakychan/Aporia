
package zenith.zov.client.modules.impl.combat;


import net.minecraft.item.Item;
import net.minecraft.item.Items;
import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;

import zenith.zov.base.events.impl.input.EventKey;
import zenith.zov.client.modules.api.Category;
import zenith.zov.client.modules.api.Module;
import zenith.zov.client.modules.api.ModuleAnnotation;
import zenith.zov.client.modules.api.setting.impl.ModeSetting;
import zenith.zov.client.modules.api.setting.impl.KeySetting;
import zenith.zov.utility.game.player.PlayerInventoryComponent;
import zenith.zov.utility.game.player.PlayerInventoryUtil;

import java.util.Comparator;

@ModuleAnnotation(name = "AutoSwap", category = Category.COMBAT, description = "Автоматический свап предметов")
public final class AutoSwap extends Module {
    public static final AutoSwap INSTANCE = new AutoSwap();

    private final ModeSetting itemType = new ModeSetting("Предмет", "Щит", "Геплы", "Тотем", "Шар");
    private final ModeSetting swapType = new ModeSetting("Свапать на", "Щит", "Геплы", "Тотем", "Шар");

    private final KeySetting keyToSwap = new KeySetting("Кнопка", -1);

    private AutoSwap() {
    }

    @EventTarget
    public void onKey(EventKey event) {
        if (mc.currentScreen != null) return;
        if (event.getAction() != 1) return;

        if (event.is(keyToSwap.getKeyCode())) {
            Slot first = PlayerInventoryUtil.getSlot(getItemByType(itemType.get()), Comparator.comparing(s -> s.getStack().hasEnchantments()), s -> s.id != 46 && s.id != 45);
            Slot second = PlayerInventoryUtil.getSlot(getItemByType(swapType.get()), Comparator.comparing(s -> s.getStack().hasEnchantments()), s -> s.id != 46 && s.id != 45);
            Slot validSlot = first != null && mc.player.getOffHandStack().getItem() != first.getStack().getItem() ? first : second;
            PlayerInventoryComponent.addTask(() -> {
                PlayerInventoryUtil.swapHand(validSlot, Hand.OFF_HAND, false);
                PlayerInventoryUtil.closeScreen(true);
            });
        }
    }


    private Item getItemByType(String itemType) {
        return switch (itemType) {
            case "Щит" -> Items.SHIELD;
            case "Тотем" -> Items.TOTEM_OF_UNDYING;
            case "Геплы" -> Items.GOLDEN_APPLE;
            case "Шар" -> Items.PLAYER_HEAD;
            default -> Items.AIR;
        };
    }


}

