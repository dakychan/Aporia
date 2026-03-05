package aporia.su.util.user.player.inventory;

import net.minecraft.item.ItemStack;

@FunctionalInterface
public interface ItemSearcher {
    boolean matches(ItemStack stack);
}