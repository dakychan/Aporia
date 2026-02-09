package zenith.zov.client.modules.impl.player;

import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import com.darkmagician6.eventapi.EventTarget;
import zenith.zov.base.events.impl.player.EventUpdate;
import zenith.zov.client.modules.api.Category;
import zenith.zov.client.modules.api.Module;
import zenith.zov.client.modules.api.ModuleAnnotation;
import zenith.zov.client.modules.api.setting.impl.NumberSetting;
import zenith.zov.utility.interfaces.IMinecraft;

@ModuleAnnotation(name = "AutoArmor", category = Category.PLAYER, description = "Автоматически экипирует броню")
public final class AutoArmor extends Module implements IMinecraft {
    public static final AutoArmor INSTANCE = new AutoArmor();
    
    private AutoArmor() {
    }

    private final NumberSetting delay = new NumberSetting("Задержка", 25f, 1f, 1000f, 1f);

    private long lastEquipTime = 0;

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;
        if (isMoving()) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastEquipTime < delay.getCurrent()) return;

        for (int i = 0; i < 4; ++i) {
            EquipmentSlot armorSlot = armorSlotFromIndex(i);
            if (armorSlot == null) continue;
            ItemStack currentArmor = mc.player.getEquippedStack(armorSlot);
            if (currentArmor.isEmpty()) {
                for (int j = 0; j < 36; ++j) {
                    ItemStack stack = mc.player.getInventory().getStack(j);
                    if (!stack.isEmpty()) {
                        EquippableComponent equippable = stack.get(DataComponentTypes.EQUIPPABLE);
                        if (equippable == null) continue;
                        int slotIndex = getArmorSlotIndex(equippable.slot());
                        if (slotIndex == i) {
                            int slotToEquip = j;
                            if (slotToEquip < 9) slotToEquip += 36;
                            
                            mc.interactionManager.clickSlot(0, slotToEquip, 0, SlotActionType.QUICK_MOVE, mc.player);
                            lastEquipTime = currentTime;
                            return;
                        }
                    }
                }
            }
        }
    }

    private boolean isMoving() {
        return mc.player.input.getMovementInput().lengthSquared() > 0;
    }

    private int getArmorSlotIndex(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> 3;
            case CHEST -> 2;
            case LEGS -> 1;
            case FEET -> 0;
            default -> -1;
        };
    }

    private EquipmentSlot armorSlotFromIndex(int index) {
        return switch (index) {
            case 0 -> EquipmentSlot.FEET;
            case 1 -> EquipmentSlot.LEGS;
            case 2 -> EquipmentSlot.CHEST;
            case 3 -> EquipmentSlot.HEAD;
            default -> null;
        };
    }
}
