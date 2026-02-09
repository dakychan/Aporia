package zenith.zov.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import lombok.Getter;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;


import zenith.zov.base.events.impl.player.EventUpdate;
import zenith.zov.client.modules.api.Category;
import zenith.zov.client.modules.api.Module;
import zenith.zov.client.modules.api.ModuleAnnotation;
import zenith.zov.utility.math.Timer;

import java.util.ArrayList;
import java.util.List;

@ModuleAnnotation(name = "AntiBot", category = Category.COMBAT,description = "")
public final class AntiBot extends Module {
    public static final AntiBot INSTANCE = new AntiBot();
    private AntiBot() {
    }
    private final List<PlayerEntity> bots = new ArrayList<>();
    private final Timer timer = new Timer();

    @EventTarget
    public void onTick(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;

        if (timer.finished(10000) && !bots.isEmpty()) {
            bots.clear();
            timer.reset();
        }

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == null) continue;
            if (player == mc.player) continue;
            //drugduck tech.
            if (armorCheck(player) && !bots.contains(player)) bots.add(player);
        }
    }

    private boolean armorCheck(PlayerEntity entity) {
        return (getArmor(entity, 3).getItem() == Items.LEATHER_HELMET && isNotColored(entity, 3) && !getArmor(entity, 3).hasEnchantments()
                || getArmor(entity, 2).getItem() == Items.LEATHER_CHESTPLATE && isNotColored(entity, 2) && !getArmor(entity, 2).hasEnchantments()
                || getArmor(entity, 1).getItem() == Items.LEATHER_LEGGINGS && isNotColored(entity, 1) && !getArmor(entity, 1).hasEnchantments()
                || getArmor(entity, 0).getItem() == Items.LEATHER_BOOTS && isNotColored(entity, 0) && !getArmor(entity, 0).hasEnchantments()
                || getArmor(entity, 2).getItem() == Items.IRON_CHESTPLATE && !getArmor(entity, 2).hasEnchantments()
                || getArmor(entity, 1).getItem() == Items.IRON_LEGGINGS && !getArmor(entity, 1).hasEnchantments());
    }

    private ItemStack getArmor(PlayerEntity entity, int slot) {
        EquipmentSlot armorSlot = switch (slot) {
            case 0 -> EquipmentSlot.FEET;
            case 1 -> EquipmentSlot.LEGS;
            case 2 -> EquipmentSlot.CHEST;
            case 3 -> EquipmentSlot.HEAD;
            default -> null;
        };
        return armorSlot == null ? ItemStack.EMPTY : entity.getEquippedStack(armorSlot);
    }

    private boolean isNotColored(PlayerEntity entity, int slot) {
        return !getArmor(entity, slot).contains(DataComponentTypes.DYED_COLOR);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (!bots.isEmpty()) bots.clear();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (!bots.isEmpty()) bots.clear();
    }

    public boolean isBot(PlayerEntity player) {
        return this.bots.contains(player);
    }
}
